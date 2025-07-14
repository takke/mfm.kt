package jp.takke.mfm_kt.token_parser

data class Token(
    // 識別結果
    val type: TokenType,
    // 抽出した文字列
    val extractedValue: String,
    // マッチした全体の文字列(これを連結することで元の文字列が復元できること)
    val wholeText: String = extractedValue,
) {
    companion object {
        // テスト用
        fun string(string: String) = Token(TokenType.String, string)
        fun centerStart() = Token(TokenType.CenterStart, "<center>")
        fun centerEnd() = Token(TokenType.CenterEnd, "</center>")
        fun big() = Token(TokenType.Big, "***")
        fun boldAsta() = Token(TokenType.BoldAsta, "**")
        fun boldTagStart() = Token(TokenType.BoldTagStart, "<b>")
        fun boldTagEnd() = Token(TokenType.BoldTagEnd, "</b>")
        fun boldUnder() = Token(TokenType.BoldUnder, "__")
        fun smallStart() = Token(TokenType.SmallStart, "<small>")
        fun smallEnd() = Token(TokenType.SmallEnd, "</small>")
        fun italicTagStart() = Token(TokenType.ItalicTagStart, "<i>")
        fun italicTagEnd() = Token(TokenType.ItalicTagEnd, "</i>")
        fun italicAsta() = Token(TokenType.ItalicAsta, "*")
        fun italicUnder() = Token(TokenType.ItalicUnder, "_")
        fun strikeTagStart() = Token(TokenType.StrikeTagStart, "<s>")
        fun strikeTagEnd() = Token(TokenType.StrikeTagEnd, "</s>")
        fun strikeWave() = Token(TokenType.StrikeWave, "~~")
        fun functionStart(s: String) = Token(TokenType.FunctionStart, s, "$[$s ")
        fun functionEnd() = Token(TokenType.FunctionEnd, "]", "]")
        fun inlineCode() = Token(TokenType.InlineCode, "`", "`")
        fun emojiCode(emojiCode: String) = Token(TokenType.EmojiCode, emojiCode)
        fun url(string: String) = Token(TokenType.Url, string)
        fun urlWithTitle(string: String) = Token(TokenType.UrlWithTitle, string)
    }
}

data class TokenParseResult(val success: Boolean, val holder: TokenHolder, val next: String)

data class TokenHolder(
    val tokenList: List<Token>
) {
    fun append(newResult: Token): TokenHolder {
        return TokenHolder(mutableListOf<Token>().also {
            it.addAll(this.tokenList)
            it.add(newResult)
        })
    }
}

typealias TokenParser = (String, TokenHolder) -> TokenParseResult