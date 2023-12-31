@file:Suppress("TestFunctionName", "NonAsciiCharacters")

package jp.takke.mfm_kt

import jp.takke.mfm_kt.token_parser.MfmTokenParser
import jp.takke.mfm_kt.token_parser.Token
import jp.takke.mfm_kt.token_parser.TokenType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class MfmTokenParserTest {

    @Test
    fun tokenize_String() {

        MfmTokenParser.tokenize("")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly()
            }

        MfmTokenParser.tokenize("hoge")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.string("hoge")
                )
            }

        MfmTokenParser.tokenize("hoge\nfuga")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.string("hoge\nfuga")
                )
            }
    }

    @Test
    fun tokenize_Quote() {

        MfmTokenParser.tokenize(">")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.string(">")
                )
            }

        MfmTokenParser.tokenize("> ")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.string("> ")
                )
            }

        // 末尾が改行コードなしの場合はマッチしないので注意
        MfmTokenParser.tokenize("> a")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.string("> a")
                )
            }

        MfmTokenParser.tokenize("> a\n")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token(TokenType.QuoteLine1, "a\n", "> a\n")
                )
            }

        MfmTokenParser.tokenize(">> a\n")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token(TokenType.QuoteLine2, "a\n", ">> a\n")
                )
            }

        // 連続する引用はマージされること
        MfmTokenParser.tokenize("> a\n> b\n")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token(TokenType.QuoteLine1, "a\nb\n", "> a\n> b\n")
                )
            }

        // その後ろに別のデータがある場合
        MfmTokenParser.tokenize("> a\n> b\nhoge")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token(TokenType.QuoteLine1, "a\nb\n", "> a\n> b\n"),
                    Token.string("hoge")
                )
            }

        MfmTokenParser.tokenize(">> a\n>> b\n")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token(TokenType.QuoteLine2, "a\nb\n", ">> a\n>> b\n")
                )
            }
    }

    @Test
    fun tokenize_Center() {

        MfmTokenParser.tokenize("<center>abc</center>")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.centerStart(),
                    Token.string("abc"),
                    Token.centerEnd()
                )
            }
    }

    @Test
    fun tokenize_Centerと改行() {

        // https://github.com/misskey-dev/mfm.js/blob/develop/src/internal/parser.ts#L264
        // によると <center>のあとの改行、</center>の前後の改行は無視するらしい
        MfmTokenParser.tokenize("<center>\nabc</center>")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token(TokenType.CenterStart, "<center>", "<center>\n"),
                    Token.string("abc"),
                    Token.centerEnd()
                )
            }

        MfmTokenParser.tokenize("<center>\nabc\n</center>")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token(TokenType.CenterStart, "<center>", "<center>\n"),
                    Token.string("abc"),
                    Token(TokenType.CenterEnd, "</center>", "\n</center>")
                )
            }
    }

    @Test
    fun tokenize_CenterBoldItalic() {

        MfmTokenParser.tokenize("<center>**Hello**, *World*!</center>")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.centerStart(),
                    Token.boldAsta(),
                    Token.string("Hello"),
                    Token.boldAsta(),
                    Token.string(", "),
                    Token.italicAsta(),
                    Token.string("World"),
                    Token.italicAsta(),
                    Token.string("!"),
                    Token.centerEnd()
                )
            }
    }

    @Test
    fun tokenize_BoldAsta() {

        MfmTokenParser.tokenize("**abc**")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.boldAsta(),
                    Token.string("abc"),
                    Token.boldAsta()
                )
            }
    }

    @Test
    fun tokenize_Big() {

        MfmTokenParser.tokenize("***abc***")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.big(),
                    Token.string("abc"),
                    Token.big()
                )
            }
    }

    @Test
    fun tokenize_BoldTag() {

        MfmTokenParser.tokenize("<b>abc</b>")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.boldTagStart(),
                    Token.string("abc"),
                    Token.boldTagEnd()
                )
            }

        MfmTokenParser.tokenize("aaa<b><i>hoge</i></b>bbb")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.string("aaa"),
                    Token.boldTagStart(),
                    Token.italicTagStart(),
                    Token.string("hoge"),
                    Token.italicTagEnd(),
                    Token.boldTagEnd(),
                    Token.string("bbb")
                )
            }
    }

    @Test
    fun tokenize_BoldUnder() {

        MfmTokenParser.tokenize("__abc__")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.boldUnder(),
                    Token.string("abc"),
                    Token.boldUnder()
                )
            }
    }

    @Test
    fun tokenize_BoldItalic() {

        MfmTokenParser.tokenize("**Hello**, *World*!")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.boldAsta(),
                    Token.string("Hello"),
                    Token.boldAsta(),
                    Token.string(", "),
                    Token.italicAsta(),
                    Token.string("World"),
                    Token.italicAsta(),
                    Token.string("!")
                )
            }
    }

    @Test
    fun tokenize_Small() {

        MfmTokenParser.tokenize("<small>abc</small>")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.smallStart(),
                    Token.string("abc"),
                    Token.smallEnd()
                )
            }

        MfmTokenParser.tokenize("<small>**Hello**, *World*!</small>")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.smallStart(),
                    Token.boldAsta(),
                    Token.string("Hello"),
                    Token.boldAsta(),
                    Token.string(", "),
                    Token.italicAsta(),
                    Token.string("World"),
                    Token.italicAsta(),
                    Token.string("!"),
                    Token.smallEnd()
                )
            }
    }

    @Test
    fun tokenize_Italic() {

        MfmTokenParser.tokenize("*abc*")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.italicAsta(),
                    Token.string("abc"),
                    Token.italicAsta()
                )
            }

        MfmTokenParser.tokenize("<i>abc</i>")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.italicTagStart(),
                    Token.string("abc"),
                    Token.italicTagEnd()
                )
            }

        MfmTokenParser.tokenize("_abc_")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.italicUnder(),
                    Token.string("abc"),
                    Token.italicUnder(),
                )
            }
    }

    @Test
    fun tokenize_Strike() {

        MfmTokenParser.tokenize("<s>abc</s>")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.strikeTagStart(),
                    Token.string("abc"),
                    Token.strikeTagEnd()
                )
            }

        MfmTokenParser.tokenize("~~abc~~")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.strikeWave(),
                    Token.string("abc"),
                    Token.strikeWave()
                )
            }
    }

    @Test
    fun tokenize_Url() {

        MfmTokenParser.tokenize("https://misskey.io/@ai")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.url("https://misskey.io/@ai")
                )
            }
    }

    @Test
    fun tokenize_UrlWithTitle() {

        MfmTokenParser.tokenize("[ai](https://misskey.io/@ai)")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.urlWithTitle("[ai](https://misskey.io/@ai)")
                )
            }

        MfmTokenParser.tokenize("[すごーく 長い 説明文](https://twitpane.com/)")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.urlWithTitle("[すごーく 長い 説明文](https://twitpane.com/)")
                )
            }
    }

    @Test
    fun tokenize_Function() {

        MfmTokenParser.tokenize("$[x2 abc]")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.functionStart("x2"),
                    Token.string("abc"),
                    Token.functionEnd()
                )
            }

        MfmTokenParser.tokenize("$[x2 $[rotate.deg=340 abc]]")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.functionStart("x2"),
                    Token.functionStart("rotate.deg=340"),
                    Token.string("abc"),
                    Token.functionEnd(),
                    Token.functionEnd()
                )
            }

        MfmTokenParser.tokenize("\$[x2 大きな文字！ :hyper_vibecat:]")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.functionStart("x2"),
                    Token.string("大きな文字！ "),
                    Token.emojiCode(":hyper_vibecat:"),
                    Token.functionEnd()
                )
            }
    }

    @Test
    fun tokenize_smallが大量にあるパターン() {

        // https://misskey.io/notes/9h3z1y499e
        MfmTokenParser.tokenize("<small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small>v<small><small>うんち</small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small>")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.string("v"),
                    Token.smallStart(),
                    Token.smallStart(),
                    Token.string("うんち"),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                    Token.smallEnd(),
                )
            }
    }

    @Test
    fun tokenize_その他() {

        MfmTokenParser.tokenize("\$[x2 **:vjtakagi_confused:**]")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.functionStart("x2"),
                    Token.boldAsta(),
                    Token.emojiCode(":vjtakagi_confused:"),
                    Token.boldAsta(),
                    Token.functionEnd()
                )
            }

        MfmTokenParser.tokenize("あれこれ[第1話]ほげほげ")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.string("あれこれ[第1話"),
                    Token.functionEnd(),
                    Token.string("ほげほげ"),
                )
            }
    }

    @Test
    fun tokenize_InlineCode() {

        MfmTokenParser.tokenize("hoge\ntest=>`fuga`")
            .let {
                assertThat(it.success).isEqualTo(true)
                assertThat(it.holder.tokenList).containsExactly(
                    Token.string("hoge\ntest=>"),
                    Token.inlineCode(),
                    Token.string("fuga"),
                    Token.inlineCode()
                )
            }
    }


}