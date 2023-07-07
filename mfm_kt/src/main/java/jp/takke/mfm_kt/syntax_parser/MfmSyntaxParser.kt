package jp.takke.mfm_kt.syntax_parser

import jp.takke.mfm_kt.token_parser.MfmTokenParser
import jp.takke.mfm_kt.token_parser.TokenParseResult
import jp.takke.mfm_kt.token_parser.TokenType


/**
 * 構文解析を行い、構文解析木を返す
 */
class MfmSyntaxParser(tokenizedResult: TokenParseResult, private val option: Option) {

    // 解析対象
    private val tokenList = tokenizedResult.holder.tokenList
    private var tokenPos = 0

    data class Option(
        val enableQuote: Boolean = true,
        val enableCenter: Boolean = true,
        val enableBig: Boolean = true,
        val enableBold: Boolean = true,
        val enableSmall: Boolean = true,
        val enableItalic: Boolean = true,
        val enableStrike: Boolean = true,
        val enableFunction: Boolean = true,
        val enableInline: Boolean = true,
        val enableEmoji: Boolean = true,
    )

    fun parse(): List<MfmNode> {
        return parse(ParseState.Normal).nodes
    }

    private enum class ParseState {
        Normal,
        Center,
        Big,
        BoldAsta,
        BoldTag,
        BoldUnder,
        Small,
        ItalicTag,
        ItalicAsta,
        ItalicUnder,
        StrikeTag,
        StrikeWave,
        Function,
        InlineCode,
    }

    private data class ParseResult(
        val success: Boolean,
        val nodes: List<MfmNode>,
    )

    private fun parse(state: ParseState): ParseResult {

        val nodes = ArrayList<MfmNode>(tokenList.size)

        while (tokenPos < tokenList.size) {
            val token = tokenList[tokenPos]
            tokenPos++

            // オプションで無効化されたタグはそのまま出力する
            if (!isOptionEnabled(token.type)) {
                when (token.type) {
                    TokenType.QuoteLine1 -> {
                        // extractedValue をさらに字句解析から行う
                        val children = MfmSyntaxParser(MfmTokenParser.tokenize(token.extractedValue), option).parse()
                        nodes.addOrMergeText(">")
                        nodes.addAllWithMergeText(children)
                    }
                    TokenType.QuoteLine2 -> {
                        // extractedValue をさらに字句解析から行う
                        val children = MfmSyntaxParser(MfmTokenParser.tokenize(token.extractedValue), option).parse()
                        nodes.addOrMergeText(">>")
                        nodes.addAllWithMergeText(children)
                    }
                    else -> {
                        // オプションで無効化されたタグなのでそのまま出力する
                        nodes.addOrMergeText(token.wholeText)
                    }
                }
                continue
            }

            // 初期状態
            when (token.type) {
                TokenType.Char,
                TokenType.String -> {
                    nodes.addOrMergeText(token.wholeText)
                }

                TokenType.QuoteLine1 -> {
                    // Quote
                    // ここで extractedValue をさらに字句解析から行う
                    val children = MfmSyntaxParser(MfmTokenParser.tokenize(token.extractedValue), option).parse()
                    nodes.add(MfmNode.Quote(MfmNode.QuoteLevel.Level1, children))
                }

                TokenType.QuoteLine2 -> {
                    // Quote
                    // ここで extractedValue をさらに字句解析から行う
                    val children = MfmSyntaxParser(MfmTokenParser.tokenize(token.extractedValue), option).parse()
                    nodes.add(MfmNode.Quote(MfmNode.QuoteLevel.Level2, children))
                }

                TokenType.CenterStart -> {
                    // Center 開始
                    val savedPos = tokenPos
                    val centerResult = parse(ParseState.Center)
                    if (centerResult.success) {
                        nodes.add(MfmNode.Center(centerResult.nodes))
                    } else {
                        // Center が終了しないまま終端に達した
                        nodes.addOrMergeText(token.wholeText)
                        // pos を戻して再度パースする
                        tokenPos = savedPos
                    }
                }

                TokenType.CenterEnd -> {
                    // Center 終了
                    if (state == ParseState.Center) {
                        return ParseResult(true, nodes)
                    } else {
                        // <center>じゃないところで</center>が来たので無視する
                        nodes.addOrMergeText(token.wholeText)
                    }
                }

                TokenType.Big -> {
                    if (state == ParseState.Big) {
                        // *** 終了
                        return ParseResult(true, nodes)
                    } else {
                        // *** 開始
                        val savedPos = tokenPos
                        val bigResult = parse(ParseState.Big)
                        if (bigResult.success) {
                            nodes.add(MfmNode.Big(bigResult.nodes))
                        } else {
                            // Big が終了しないまま終端に達した
                            nodes.addOrMergeText(token.wholeText)
                            // pos を戻して再度パースする
                            tokenPos = savedPos
                        }
                    }
                }

                TokenType.BoldAsta -> {
                    if (state == ParseState.BoldAsta) {
                        // ** 終了
                        return ParseResult(true, nodes)
                    } else {
                        // ** 開始
                        val savedPos = tokenPos
                        val boldResult = parse(ParseState.BoldAsta)
                        if (boldResult.success) {
                            nodes.add(MfmNode.Bold(boldResult.nodes))
                        } else {
                            // Bold が終了しないまま終端に達した
                            nodes.addOrMergeText(token.wholeText)
                            // pos を戻して再度パースする
                            tokenPos = savedPos
                        }
                    }
                }

                TokenType.BoldTagStart -> {
                    // Bold 開始: <b>
                    val savedPos = tokenPos
                    val boldTagResult = parse(ParseState.BoldTag)
                    if (boldTagResult.success) {
                        nodes.add(MfmNode.Bold(boldTagResult.nodes))
                    } else {
                        // Bold が終了しないまま終端に達した
                        nodes.addOrMergeText(token.wholeText)
                        // pos を戻して再度パースする
                        tokenPos = savedPos
                    }
                }

                TokenType.BoldTagEnd -> {
                    // Bold 終了: </b>
                    if (state == ParseState.BoldTag) {
                        return ParseResult(true, nodes)
                    } else {
                        // <bold>じゃないところで</bold>が来たので無視する
                        nodes.addOrMergeText(token.wholeText)
                    }
                }

                TokenType.BoldUnder -> {
                    if (state == ParseState.BoldUnder) {
                        // __ 終了
                        return ParseResult(true, nodes)
                    } else {
                        // __ 開始
                        val savedPos = tokenPos
                        val boldResult = parse(ParseState.BoldUnder)
                        if (boldResult.success) {
                            // __ の間は[a-zA-Z0-9_ ]のみ許可
                            // https://github.com/misskey-dev/mfm.js/blob/develop/src/internal/parser.ts#L354
                            if (boldResult.nodes.size == 1 &&
                                (boldResult.nodes[0] as? MfmNode.Text)?.value?.matches(Regex("^[a-zA-Z0-9 ]+")) == true
                            ) {
                                nodes.add(MfmNode.Bold(boldResult.nodes))
                            } else {
                                // __ の間に対象外の文字がある場合は無視する
                                nodes.addOrMergeText("__")
                                nodes.addAllWithMergeText(boldResult.nodes)
                                nodes.addOrMergeText("__")
                            }
                        } else {
                            // __ が終了しないまま終端に達した
                            nodes.addOrMergeText(token.wholeText)
                            // pos を戻して再度パースする
                            tokenPos = savedPos
                        }
                    }
                }

                TokenType.SmallStart -> {
                    // Small 開始
                    val savedPos = tokenPos
                    val smallResult = parse(ParseState.Small)
                    if (smallResult.success) {
                        nodes.add(MfmNode.Small(smallResult.nodes))
                    } else {
                        // Small が終了しないまま終端に達した
                        nodes.addOrMergeText(token.wholeText)
                        // pos を戻して再度パースする
                        tokenPos = savedPos
                    }
                }

                TokenType.SmallEnd -> {
                    // Small 終了
                    if (state == ParseState.Small) {
                        return ParseResult(true, nodes)
                    } else {
                        // <small>じゃないところで</small>が来たので無視する
                        nodes.addOrMergeText(token.wholeText)
                    }
                }

                TokenType.ItalicTagStart -> {
                    // Italic 開始
                    val savedPos = tokenPos
                    val italicResult = parse(ParseState.ItalicTag)
                    if (italicResult.success) {
                        nodes.add(MfmNode.Italic(italicResult.nodes))
                    } else {
                        // Italic が終了しないまま終端に達した
                        nodes.addOrMergeText(token.wholeText)
                        // pos を戻して再度パースする
                        tokenPos = savedPos
                    }
                }

                TokenType.ItalicTagEnd -> {
                    // Italic 終了
                    if (state == ParseState.ItalicTag) {
                        return ParseResult(true, nodes)
                    } else {
                        // <i>じゃないところで</i>が来たので無視する
                        nodes.addOrMergeText(token.wholeText)
                    }
                }

                TokenType.ItalicAsta -> {
                    if (state == ParseState.ItalicAsta) {
                        // * 終了
                        return ParseResult(true, nodes)
                    } else {
                        // * 開始
                        val savedPos = tokenPos
                        val italicResult = parse(ParseState.ItalicAsta)

                        if (italicResult.success) {
                            // * の間は[a-zA-Z0-9_]のみ許可
                            // https://github.com/misskey-dev/mfm.js/blob/develop/src/internal/parser.ts#L354
                            if (italicResult.nodes.size == 1 &&
                                (italicResult.nodes[0] as? MfmNode.Text)?.value?.matches(Regex("^[a-zA-Z0-9]+")) == true
                            ) {
                                nodes.add(MfmNode.Italic(italicResult.nodes))
                            } else {
                                // * の間に対象外の文字がある場合は無視する
                                nodes.addOrMergeText("*")
                                nodes.addAllWithMergeText(italicResult.nodes)
                                nodes.addOrMergeText("*")
                            }
                        } else {
                            // * が終了しないまま終端に達した
                            nodes.addOrMergeText(token.wholeText)
                            // pos を戻して再度パースする
                            tokenPos = savedPos
                        }
                    }
                }

                TokenType.ItalicUnder -> {
                    if (state == ParseState.ItalicUnder) {
                        // _ 終了
                        return ParseResult(true, nodes)
                    } else {
                        // _ 開始
                        val savedPos = tokenPos
                        val italicResult = parse(ParseState.ItalicUnder)

                        if (italicResult.success) {
                            // _ の間は[a-zA-Z0-9_]のみ許可
                            // https://github.com/misskey-dev/mfm.js/blob/develop/src/internal/parser.ts#L354
                            if (italicResult.nodes.size == 1 &&
                                (italicResult.nodes[0] as? MfmNode.Text)?.value?.matches(Regex("^[a-zA-Z0-9]+")) == true
                            ) {
                                nodes.add(MfmNode.Italic(italicResult.nodes))
                            } else {
                                // _ の間に対象外の文字がある場合は無視する
                                nodes.addOrMergeText("_")
                                nodes.addAllWithMergeText(italicResult.nodes)
                                nodes.addOrMergeText("_")
                            }
                        } else {
                            // _ が終了しないまま終端に達した
                            nodes.addOrMergeText(token.wholeText)

                            // pos を戻して再度パースする
                            tokenPos = savedPos
                        }
                    }
                }

                TokenType.StrikeTagStart -> {
                    // Strike 開始: <s>
                    val savedPos = tokenPos
                    val strikeTagResult = parse(ParseState.StrikeTag)
                    if (strikeTagResult.success) {
                        nodes.add(MfmNode.Strike(strikeTagResult.nodes))
                    } else {
                        // Strike が終了しないまま終端に達した
                        nodes.addOrMergeText(token.wholeText)
                        // pos を戻して再度パースする
                        tokenPos = savedPos
                    }
                }

                TokenType.StrikeTagEnd -> {
                    // Strike 終了: </s>
                    if (state == ParseState.StrikeTag) {
                        return ParseResult(true, nodes)
                    } else {
                        // <s>じゃないところで</s>が来たので無視する
                        nodes.addOrMergeText(token.wholeText)
                    }
                }

                TokenType.StrikeWave -> {
                    if (state == ParseState.StrikeWave) {
                        // StrikeWave 終了
                        return ParseResult(true, nodes)
                    } else {
                        // StrikeWave 開始
                        val strikeResult = parse(ParseState.StrikeWave)

                        if (strikeResult.success) {
                            // ~~ の間は改行不可
                            if (strikeResult.nodes.size == 1 &&
                                (strikeResult.nodes[0] as? MfmNode.Text)?.value?.matches(Regex("^[^\n]+")) == true
                            ) {
                                nodes.add(MfmNode.Strike(strikeResult.nodes))
                            } else {
                                // ~~ の間に対象外の文字がある場合は無視する
                                nodes.addOrMergeText("~~")
                                nodes.addAllWithMergeText(strikeResult.nodes)
                                nodes.addOrMergeText("~~")
                            }
                        } else {
                            // StrikeWave が終了しないまま終端に達した
                            nodes.addOrMergeText(token.wholeText)
                            // TODO pos を戻して再度パースすべき
                            nodes.addAllWithMergeText(strikeResult.nodes)
                        }
                    }
                }

                TokenType.FunctionStart -> {
                    // Function 開始
                    val functionResult = parse(ParseState.Function)
                    if (functionResult.success) {
                        nodes.add(MfmNode.Function(token.extractedValue, functionResult.nodes))
                    } else {
                        // Function が終了しないまま終端に達した
                        nodes.addOrMergeText(token.wholeText)
                        // TODO pos を戻して再度パースすべき
                        nodes.addAllWithMergeText(functionResult.nodes)
                    }
                }

                TokenType.FunctionEnd -> {
                    // Function 終了
                    if (state == ParseState.Function) {
                        return ParseResult(true, nodes)
                    } else {
                        // "$[" じゃないところで "]" が来たので無視する
                        nodes.addOrMergeText(token.wholeText)
                    }
                }

                TokenType.InlineCode -> {
                    if (state == ParseState.InlineCode) {
                        // InlineCode 終了
                        return ParseResult(true, nodes)
                    } else {
                        // InlineCode 開始
                        val strikeResult = parse(ParseState.InlineCode)

                        println("InlineCode: ${strikeResult.nodes} (${strikeResult.success})")

                        if (strikeResult.success) {
                            // ` の間は改行および´不可
                            if (strikeResult.nodes.size == 1 &&
                                (strikeResult.nodes[0] as? MfmNode.Text)?.value?.matches(Regex("^[^´\n]+")) == true
                            ) {
                                nodes.add(MfmNode.InlineCode(strikeResult.nodes))
                            } else {
                                // ` の間に対象外の文字がある場合は無視する
                                nodes.addOrMergeText("`")
                                nodes.addAllWithMergeText(strikeResult.nodes)
                                nodes.addOrMergeText("`")
                            }
                        } else {
                            // InlineCode が終了しないまま終端に達した
                            nodes.addOrMergeText(token.wholeText)
                            // TODO pos を戻して再度パースすべき
                            nodes.addAllWithMergeText(strikeResult.nodes)
                        }
                    }
                }

                TokenType.EmojiCode -> {
                    nodes.add(MfmNode.EmojiCode(token.extractedValue))
                }
            }
        }

        // Normal 以外で終端に達したらエラー
        return ParseResult(state == ParseState.Normal, nodes)
    }

    private fun isOptionEnabled(type: TokenType): Boolean {
        return when (type) {
            TokenType.Char -> true
            TokenType.String -> true
            TokenType.QuoteLine1 -> option.enableQuote
            TokenType.QuoteLine2 -> option.enableQuote
            TokenType.CenterStart -> option.enableCenter
            TokenType.CenterEnd -> option.enableCenter
            TokenType.Big -> option.enableBig
            TokenType.BoldAsta -> option.enableBold
            TokenType.BoldTagStart -> option.enableBold
            TokenType.BoldTagEnd -> option.enableBold
            TokenType.BoldUnder -> option.enableBold
            TokenType.SmallStart -> option.enableSmall
            TokenType.SmallEnd -> option.enableSmall
            TokenType.ItalicTagStart -> option.enableItalic
            TokenType.ItalicTagEnd -> option.enableItalic
            TokenType.ItalicAsta -> option.enableItalic
            TokenType.ItalicUnder -> option.enableItalic
            TokenType.StrikeTagStart -> option.enableStrike
            TokenType.StrikeTagEnd -> option.enableStrike
            TokenType.StrikeWave -> option.enableStrike
            TokenType.FunctionStart -> option.enableFunction
            TokenType.FunctionEnd -> option.enableFunction
            TokenType.InlineCode -> option.enableInline
            TokenType.EmojiCode -> option.enableEmoji
        }
    }

    private fun ArrayList<MfmNode>.addOrMergeText(s: String) {
        // 末尾が Text なら結合する
        if (this.isNotEmpty() && this.last() is MfmNode.Text) {
            val text = this.last() as MfmNode.Text
            this[this.size - 1] = MfmNode.Text(text.value + s)
        } else {
            this.add(MfmNode.Text(s))
        }
    }

    private fun ArrayList<MfmNode>.addAllWithMergeText(nodes: List<MfmNode>) {
        for (node in nodes) {
            if (node is MfmNode.Text) {
                this.addOrMergeText(node.value)
            } else {
                this.add(node)
            }
        }
    }

}

