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
            // 再帰で書くとトークン数に比例してスタックが深くなり、
            // 長文でスタックオーバーフローする(特にiOSのメインスレッドはスタックが小さい)ためループで処理する
            var currentText = text
            var currentHolder = holder
            while (true) {
                val parseResult = parser(currentText, currentHolder)
                if (!parseResult.success) {
                    break
                }
                val consumed = currentText.length - parseResult.next.length
                currentText = parseResult.next
                currentHolder = parseResult.holder
                if (consumed <= 0) {
                    // 消費のない成功は無限ループになるため打ち切る(安全弁)
                    break
                }
            }
            TokenParseResult(true, currentHolder, currentText)
        }
    }


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
    // 注意: regex は必ず "^" アンカー付きであること
    // (find は先頭でマッチしない場合に全位置を走査してしまうため、先頭のみ試行する matchAt を使う)
    val pRegex: (TokenType, Regex) -> TokenParser = { type, regex ->
        { text, holder ->
            val m = regex.matchAt(text, 0)
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
    // バックティック(`)をFunction名から除外する（InlineCode構文と干渉するため）
    val pFunctionStart: () -> TokenParser = { pRegex(TokenType.FunctionStart, "^\\$\\[([^`\\s]+) ".toRegex()) }
    val pFunctionEnd: () -> TokenParser = { pWord(TokenType.FunctionEnd, "]") }

    // `$abc <- 1` のような形式
    val pInlineCode: () -> TokenParser = { pWord(TokenType.InlineCode, "`") }

    val pPlainStart: () -> TokenParser = { pWord(TokenType.PlainStart, "<plain>") }
    val pPlainEnd: () -> TokenParser = { pWord(TokenType.PlainEnd, "</plain>") }

    // 閉じ:の直後に半角英数がある場合はマッチしない
    val pEmojiCode: () -> TokenParser = { pRegex(TokenType.EmojiCode, "^(:[a-zA-Z0-9_+-]+:)(?![a-zA-Z0-9])".toRegex()) }

    // mfm.js 互換のメンションパーサー
    // ユーザー名・ホスト名にドットを許可し、末尾/先頭の [.-] をバリデーションする
    private val mentionRegex = Regex("^@([a-zA-Z0-9_.-]+)(@([a-zA-Z0-9_.-]+))?")
    private val trailingDotHyphenRegex = Regex("[.-]+$")

    val pMention: () -> TokenParser = {
        { text, holder ->
            val m = mentionRegex.matchAt(text, 0)
            if (m == null) {
                toNGParseResult(text)
            } else {
                // 直前が半角英数字の場合はメンションとして認識しない（メールアドレス対策）
                val lastToken = holder.tokenList.lastOrNull()
                val lastChar = lastToken?.extractedValue?.lastOrNull()
                if (lastChar != null && (lastChar in 'a'..'z' || lastChar in 'A'..'Z' || lastChar in '0'..'9')) {
                    toNGParseResult(text)
                } else {
                    var username = m.groupValues[1]
                    var hostname: String? = m.groupValues[3].ifEmpty { null }
                    var invalidMention = false

                    // ホスト名末尾の [.-] を除去
                    if (hostname != null) {
                        val trimmed = hostname.replace(trailingDotHyphenRegex, "")
                        if (trimmed.isEmpty()) {
                            invalidMention = true
                            hostname = null
                        } else {
                            hostname = trimmed
                        }
                    }

                    // ユーザー名末尾の [.-] を除去
                    val userTailMatch = trailingDotHyphenRegex.find(username)
                    if (userTailMatch != null) {
                        if (hostname == null) {
                            username = username.substring(0, username.length - userTailMatch.value.length)
                        } else {
                            invalidMention = true
                        }
                    }

                    // ユーザー名先頭の [.-] を不許可
                    if (username.isEmpty() || username[0] == '.' || username[0] == '-') {
                        invalidMention = true
                    }

                    // ホスト名先頭の [.-] を不許可
                    if (hostname != null && (hostname[0] == '.' || hostname[0] == '-')) {
                        invalidMention = true
                    }

                    if (invalidMention) {
                        toNGParseResult(text)
                    } else {
                        val acct = if (hostname != null) "@$username@$hostname" else "@$username"
                        TokenParseResult(
                            true,
                            holder.append(Token(TokenType.Mention, acct)),
                            text.substring(acct.length)
                        )
                    }
                }
            }
        }
    }

    private const val URL_C = ".,a-zA-Z0-9_/:%#@\$&?!~=+-"

    // https://twitpane.com/hoge(abc) のように末尾が (xxx) のパターン
    // 旧実装の正規表現 "^https?://([C]+|\(C+\))+(\(C+\)|[TAIL])" は入れ子の量指定子を含み、
    // Kotlin/Native の正規表現エンジン(再帰実装)で指数的なバックトラックや深い再帰を引き起こし
    // メインスレッドのハング・クラッシュの原因になるため、同等の判定を行う手書きスキャナで実装する
    val pUrl: () -> TokenParser = {
        { text, holder ->
            val urlLength = scanUrlLength(text)
            if (urlLength <= 0) {
                toNGParseResult(text)
            } else {
                val url = text.substring(0, urlLength)
                TokenParseResult(
                    true,
                    holder.append(Token(TokenType.Url, url)),
                    text.substring(urlLength)
                )
            }
        }
    }

    private fun isUrlChar(c: Char): Boolean {
        // URL_C = ".,a-zA-Z0-9_/:%#@$&?!~=+-" と同じ文字集合
        return c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' ||
                c == '.' || c == ',' || c == '_' || c == '/' || c == ':' || c == '%' || c == '#' ||
                c == '@' || c == '$' || c == '&' || c == '?' || c == '!' || c == '~' || c == '=' ||
                c == '+' || c == '-'
    }

    // 旧正規表現 "^https?://([C]+|\(C+\))+(\(C+\)|[TAIL])" と同等の判定を行う
    // 戻り値: マッチした場合はURL全体の長さ、マッチしない場合は -1
    private fun scanUrlLength(text: String): Int {
        val schemeLength = when {
            text.startsWith("https://") -> 8
            text.startsWith("http://") -> 7
            else -> return -1
        }

        // 「URL文字の連続」または「"(URL文字+)" ブロック」の繰り返しを最長でスキャンする
        var index = schemeLength
        var blockCount = 0          // "(...)" ブロックの数
        var firstBlockStart = -1    // 最初のブロックの開始位置
        var lastBlockEnd = -1       // 最後のブロックの終了位置(排他)
        while (index < text.length) {
            val c = text[index]
            if (isUrlChar(c)) {
                index++
            } else if (c == '(') {
                // "(URL文字+)" ブロックの判定 (閉じ括弧がなければブロック不成立でスキャン終了)
                var j = index + 1
                while (j < text.length && isUrlChar(text[j])) {
                    j++
                }
                if (j > index + 1 && j < text.length && text[j] == ')') {
                    if (blockCount == 0) {
                        firstBlockStart = index
                    }
                    index = j + 1
                    lastBlockEnd = index
                    blockCount++
                } else {
                    break
                }
            } else {
                break
            }
        }

        // 末尾の "," "." は URL に含めない (旧正規表現の末尾 [TAIL] 判定と同等)
        var end = index
        while (end > schemeLength && (text[end - 1] == '.' || text[end - 1] == ',')) {
            end--
        }

        // スキーム以降が2文字未満はマッチしない (旧正規表現は「本体+」と「末尾」の2要素が必須)
        if (end - schemeLength < 2) {
            return -1
        }

        // 全体が単独の "(...)" ブロックのみの場合はマッチしない (「本体+」と「末尾」に分割できないため)
        if (blockCount == 1 && firstBlockStart == schemeLength && lastBlockEnd == end) {
            return -1
        }

        return end
    }

    // [abc](https://twitpane.com/hoge) または [abc](<https://twitpane.com/hoge>) のようなパターン
    val pUrlWithTitle: () -> TokenParser = {
        pRegex(
            TokenType.UrlWithTitle,
            ("^" +
                    "(" +
                    "\\[" +
                    "[^\n\\]]+" +
                    "\\]" +
                    "\\(" +
                    "<?(" +  // オプションの < を追加
                    "https?://[${URL_C}]+" +
                    ")>?" +  // オプションの > を追加
                    "\\)" +
                    ")"
                    ).toRegex()
        )
    }

    // ?[abc](https://twitpane.com/hoge) または ?[abc](<https://twitpane.com/hoge>) のようなパターン（サイレントリンク）
    val pSilentLink: () -> TokenParser = {
        pRegex(
            TokenType.SilentLink,
            ("^" +
                    "(" +
                    "\\?\\[" +  // ?[ で開始
                    "[^\n\\]]+" +
                    "\\]" +
                    "\\(" +
                    "<?(" +  // オプションの < を追加
                    "https?://[${URL_C}]+" +
                    ")>?" +  // オプションの > を追加
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
                // "<plain>"
                pPlainStart() or pPlainEnd() or
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
                // "?[title](url)" - サイレントリンク（UrlWithTitleより先にマッチさせる）
                pSilentLink() or
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
        // (TokenHolder はバッキング配列を共有するため、呼び出し毎に新規生成すること)
        val result = mfmParser(text, TokenHolder(emptyList()))

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
