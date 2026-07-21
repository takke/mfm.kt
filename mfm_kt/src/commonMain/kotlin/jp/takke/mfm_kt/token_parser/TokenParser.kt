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
        fun plainStart() = Token(TokenType.PlainStart, "<plain>")
        fun plainEnd() = Token(TokenType.PlainEnd, "</plain>")
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
        fun mention(string: String) = Token(TokenType.Mention, string)
        fun url(string: String) = Token(TokenType.Url, string)
        fun urlWithTitle(string: String) = Token(TokenType.UrlWithTitle, string)
    }
}

data class TokenParseResult(val success: Boolean, val holder: TokenHolder, val next: String)

class TokenHolder private constructor(
    // append 毎の全リストコピー(O(n^2))を避けるため、バッキング配列を後続の TokenHolder と共有し、
    // 自身のサイズをスナップショットとして保持する。
    // 「自分が配列の先端である場合のみ直接追記、分岐した場合のみコピー」とすることで
    // 直線的なパースでは O(1) で追記できる。
    private val backing: ArrayList<Token>,
    private val size: Int,
) {
    constructor(tokenList: List<Token>) : this(ArrayList(tokenList), tokenList.size)

    val tokenList: List<Token>
        get() = if (backing.size == size) backing else ArrayList(backing.subList(0, size))

    fun append(newResult: Token): TokenHolder {
        return if (backing.size == size) {
            // 自分が先端: 共有バッキング配列に直接追記する
            backing.add(newResult)
            TokenHolder(backing, size + 1)
        } else {
            // 分岐が発生している: 自分のサイズまでをコピーしてから追記する
            val copied = ArrayList<Token>(size + 1)
            for (i in 0 until size) {
                copied.add(backing[i])
            }
            copied.add(newResult)
            TokenHolder(copied, size + 1)
        }
    }

    override fun equals(other: Any?): Boolean = other is TokenHolder && tokenList == other.tokenList
    override fun hashCode(): Int = tokenList.hashCode()
    override fun toString(): String = "TokenHolder(tokenList=$tokenList)"
}

typealias TokenParser = (String, TokenHolder) -> TokenParseResult