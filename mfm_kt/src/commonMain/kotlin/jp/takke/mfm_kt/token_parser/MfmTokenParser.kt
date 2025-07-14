@file:Suppress("ObjectPropertyName", "NonAsciiCharacters")

package jp.takke.mfm_kt.token_parser

/**
 * 字句解析
 *
 * https://osima.jp/posts/parser-combinator-with-kotlin-2/ の Parser を拡張して MFM の字句解析器を作った
 */
object MfmTokenParser {

    // https://pages.michinobu.jp/t/misc/unicodecodechars.html
    // TODO 記号とかちょっと足りないので見直すこと
    private const val ANY_ASCII_CLS = "\u0020-\u007D"
    private const val ANY_ASCII_WITHOUT_SPACE_CLS = "\u0021-\u007D"
    private const val ANY_記号_CLS = "\u3000-\u303f\uFF00-\uFFEF"
    private const val ANY_ひらがなカタカナ_CLS = "\u3000-\u303F\u3040-\u309f\u30A0-\u30FF"
    private const val ANY_漢字_CLS = "\u4E00-\u9FCF"
    private const val ANY_ひらがなカナカナ漢字_CLS = ANY_ひらがなカタカナ_CLS + ANY_記号_CLS + ANY_漢字_CLS

    val toNGParseResult: (String) -> TokenParseResult = { next ->
        TokenParseResult(false, TokenHolder(emptyList()), next)
    }

    @Suppress("unused")
    private infix fun TokenParser.and(parser1: TokenParser): TokenParser {
        val parser0 = this

        return { text, holder ->
            val parseResult0 = parser0(text, holder)
            if (parseResult0.success) {
                val parseResult1 = parser1(parseResult0.next, parseResult0.holder)
                if (parseResult1.success) {
                    TokenParseResult(true, parseResult1.holder, parseResult1.next)
                } else {
                    toNGParseResult(text)
                }
            } else {
                toNGParseResult(text)
            }
        }
    }

    private infix fun TokenParser.or(parser1: TokenParser): TokenParser {
        val parser0 = this

        return { text, holder ->
            val parseResult0 = parser0(text, holder)

            if (parseResult0.success) {
                parseResult0
            } else {
                val parseResult1 = parser1(text, holder)

                if (parseResult1.success) {
                    parseResult1
                } else {
                    toNGParseResult(text)
                }
            }
        }
    }

    private fun many(parser: TokenParser): TokenParser {
        return { text, holder ->
            val parseResult = parser(text, holder)
            if (!parseResult.success) {
                TokenParseResult(true, holder, text)
            } else {
//                println("*many, next[${parseResult.next}], result[${parseResult.result}]")
                many(parser)(parseResult.next, parseResult.holder)
            }
        }
    }

    private val EMPTY_HOLDER = TokenHolder(emptyList())

    val pAnyChar: () -> TokenParser = {
        { text, holder ->
            if (text.isNotEmpty()) {
                TokenParseResult(
                    true,
                    holder.append(Token(TokenType.Char, text[0].toString())),
                    text.substring(1)
                )
            } else {
                toNGParseResult(text)
            }
        }
    }

    val pWord: (TokenType, String) -> TokenParser = { type, word ->
        { text, holder ->
            val invalid = (text.length < word.length)
            if (!invalid && text.substring(0, word.length) == word) {
                TokenParseResult(
                    true,
                    holder.append(Token(type, word)),
                    text.substring(word.length)
                )
            } else {
                toNGParseResult(text)
            }
        }
    }

    // regex は "()" で囲まれた部分を1つだけ持つこと
    val pRegex: (TokenType, Regex) -> TokenParser = { type, regex ->
        { text, holder ->
            val m = regex.find(text)
            if (m != null) {
                TokenParseResult(
                    true,
                    holder.append(Token(type, m.groupValues[1], m.groupValues[0])),
                    text.substring(m.groupValues[0].length)
                )
            } else {
                toNGParseResult(text)
            }
        }
    }

    // 末尾が改行であることに注意(改行コードなしの場合はマッチしない)
    val pQuoteLine1: () -> TokenParser = { pRegex(TokenType.QuoteLine1, "^> ?([$ANY_ASCII_CLS$ANY_ひらがなカナカナ漢字_CLS]+\n)".toRegex()) }
    val pQuoteLine2: () -> TokenParser = { pRegex(TokenType.QuoteLine2, "^>> ?([$ANY_ASCII_CLS$ANY_ひらがなカナカナ漢字_CLS]+\n)".toRegex()) }

    val pCenterStart: () -> TokenParser = { pRegex(TokenType.CenterStart, "^(<center>)\n?".toRegex()) }
    val pCenterEnd: () -> TokenParser = { pRegex(TokenType.CenterEnd, "^\n?(</center>)".toRegex()) }

    val pBig: () -> TokenParser = { pWord(TokenType.Big, "***") }

    val pBoldAsta: () -> TokenParser = { pWord(TokenType.BoldAsta, "**") }
    val pBoldTagStart: () -> TokenParser = { pWord(TokenType.BoldTagStart, "<b>") }
    val pBoldTagEnd: () -> TokenParser = { pWord(TokenType.BoldTagEnd, "</b>") }
    val pBoldUnder: () -> TokenParser = { pWord(TokenType.BoldUnder, "__") }

    val pSmallStart: () -> TokenParser = { pWord(TokenType.SmallStart, "<small>") }
    val pSmallEnd: () -> TokenParser = { pWord(TokenType.SmallEnd, "</small>") }

    val pItalicTagStart: () -> TokenParser = { pWord(TokenType.ItalicTagStart, "<i>") }
    val pItalicTagEnd: () -> TokenParser = { pWord(TokenType.ItalicTagEnd, "</i>") }

    val pItalicAsta: () -> TokenParser = { pWord(TokenType.ItalicAsta, "*") }
    val pItalicUnder: () -> TokenParser = { pWord(TokenType.ItalicUnder, "_") }

    val pStrikeTagStart: () -> TokenParser = { pWord(TokenType.StrikeTagStart, "<s>") }
    val pStrikeTagEnd: () -> TokenParser = { pWord(TokenType.StrikeTagEnd, "</s>") }
    val pStrikeWave: () -> TokenParser = { pWord(TokenType.StrikeWave, "~~") }

    // $[shake ...] のような形式のうち $[shake まで。
    val pFunctionStart: () -> TokenParser = { pRegex(TokenType.FunctionStart, "^\\$\\[([$ANY_ASCII_WITHOUT_SPACE_CLS]+) ".toRegex()) }
    val pFunctionEnd: () -> TokenParser = { pWord(TokenType.FunctionEnd, "]") }

    // `$abc <- 1` のような形式
    val pInlineCode: () -> TokenParser = { pWord(TokenType.InlineCode, "`") }

    val pEmojiCode: () -> TokenParser = { pRegex(TokenType.EmojiCode, "^(:[a-zA-Z0-9_+-]+:)".toRegex()) }

    val pMention: () -> TokenParser = { pRegex(TokenType.Mention, "^(@[a-zA-Z0-9_-]+(@[.a-zA-Z0-9_-]+)?)".toRegex()) }

    private const val URL_C = ".,a-zA-Z0-9_/:%#@\$&?!~=+-"
    private const val URL_TAIL_C = "a-zA-Z0-9_/:%#@\$&?!~=+-"   // 末尾は ",." 不可

    // https://twitpane.com/hoge(abc) のように末尾が (xxx) のパターン
    val pUrl: () -> TokenParser = {
        pRegex(
            TokenType.Url,
            ("^" +
                    "(" +
                    "https?://" +
                    "([${URL_C}]+|\\([${URL_C}]+\\))+" +
                    // 末尾は "(...)" または ",." 以外
                    "(\\([${URL_C}]+\\)|[${URL_TAIL_C}])" +
                    ")"
                    ).toRegex()
        )
    }

    // [abc](https://twitpane.com/hoge) のようなパターン
    val pUrlWithTitle: () -> TokenParser = {
        pRegex(
            TokenType.UrlWithTitle,
            ("^" +
                    "(" +
                    "\\[" +
                    "[^\n\\]]+" +
                    "\\]" +
                    "\\(" +
                    "(https?://[${URL_C}]+)" +
                    "\\)" +
                    ")"
                    ).toRegex()
        )
    }

    val mfmParser = many(
        // ">" block
        pQuoteLine2() or pQuoteLine1() or
                // "<center>" block
                pCenterStart() or pCenterEnd() or
                // "***"
                pBig() or
                // "**"
                pBoldAsta() or
                // "<b>"
                pBoldTagStart() or pBoldTagEnd() or
                // "__"
                pBoldUnder() or
                // "<small>"
                pSmallStart() or pSmallEnd() or
                // "<i>"
                pItalicTagStart() or pItalicTagEnd() or
                // "*"
                pItalicAsta() or
                // "_"
                pItalicUnder() or
                // "<s>"
                pStrikeTagStart() or pStrikeTagEnd() or
                // "~~"
                pStrikeWave() or
                // "$[xxx ...]"
                pFunctionStart() or pFunctionEnd() or
                // "@"
                pMention() or
                // http
                pUrl() or
                // "[title](url)"
                pUrlWithTitle() or
                // "`"
                pInlineCode() or
                // ":"
                pEmojiCode() or
                pAnyChar()
    )

    fun tokenize(text: String): TokenParseResult {

        // 主に字句解析
        val result = mfmParser(text, EMPTY_HOLDER)

        if (!result.success) {
            return result
        }

        // Charの結合
        val newTokens = integrateChars(result.holder.tokenList)

        // Quote1の結合
        val newTokens1 = if (newTokens.count { it.type == TokenType.QuoteLine1 } >= 2) {
            integrateQuoteLines(newTokens, quoteLineType = TokenType.QuoteLine1)
        } else {
            newTokens
        }

        // Quote2の結合
        val newTokens2 = if (newTokens.count { it.type == TokenType.QuoteLine2 } >= 2) {
            integrateQuoteLines(newTokens1, quoteLineType = TokenType.QuoteLine2)
        } else {
            newTokens1
        }

        return TokenParseResult(true, TokenHolder(newTokens2), result.next)
    }

    private fun integrateChars(tokenList: List<Token>): MutableList<Token> {

        // 連続するCharをStringにする

        val newTokens = mutableListOf<Token>()

        val sb = StringBuilder()

        for (token in tokenList) {

            if (token.type == TokenType.Char) {
                sb.append(token.extractedValue)
            } else {
                if (sb.isNotEmpty()) {
                    // Char終了
                    newTokens.add(Token(TokenType.String, sb.toString()))
                    sb.clear()
                }
                newTokens.add(token)
            }
        }
        if (sb.isNotEmpty()) {
            newTokens.add(Token(TokenType.String, sb.toString()))
        }

        return newTokens
    }

    private fun integrateQuoteLines(tokenList: List<Token>, quoteLineType: TokenType = TokenType.QuoteLine1): List<Token> {

        // 連続するquoteLineを1つに統合する

        val newTokens = mutableListOf<Token>()

        val sbExtracted = StringBuilder()
        val sbOriginal = StringBuilder()

        for (token in tokenList) {

            if (token.type == quoteLineType) {
                sbExtracted.append(token.extractedValue)
                sbOriginal.append(token.wholeText)
            } else {
                if (sbExtracted.isNotEmpty()) {
                    newTokens.add(Token(quoteLineType, sbExtracted.toString(), sbOriginal.toString()))
                    sbExtracted.clear()
                    sbOriginal.clear()
                }
                newTokens.add(token)
            }
        }
        if (sbExtracted.isNotEmpty()) {
            newTokens.add(Token(quoteLineType, sbExtracted.toString(), sbOriginal.toString()))
        }

        return newTokens
    }

}
