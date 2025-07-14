@file:Suppress("TestFunctionName", "NonAsciiCharacters")

package jp.takke.mfm_kt

import jp.takke.mfm_kt.token_parser.MfmTokenParser
import jp.takke.mfm_kt.token_parser.Token
import jp.takke.mfm_kt.token_parser.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse


class MfmTokenParserTest {

    @Test
    fun tokenize_String() {

        MfmTokenParser.tokenize("")
            .let {
                assertTrue(it.success)
                assertEquals(emptyList(), it.holder.tokenList)
            }

        MfmTokenParser.tokenize("hoge")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.string("hoge")
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("hoge\nfuga")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.string("hoge\nfuga")
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_Quote() {

        MfmTokenParser.tokenize(">")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.string(">")
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("> ")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.string("> ")
                    ), it.holder.tokenList
                )
            }

        // 末尾が改行コードなしの場合はマッチしないので注意
        MfmTokenParser.tokenize("> a")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.string("> a")
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("> a\n")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token(TokenType.QuoteLine1, "a\n", "> a\n")
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize(">> a\n")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token(TokenType.QuoteLine2, "a\n", ">> a\n")
                    ), it.holder.tokenList
                )
            }

        // 連続する引用はマージされること
        MfmTokenParser.tokenize("> a\n> b\n")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token(TokenType.QuoteLine1, "a\nb\n", "> a\n> b\n")
                    ), it.holder.tokenList
                )
            }

        // その後ろに別のデータがある場合
        MfmTokenParser.tokenize("> a\n> b\nhoge")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token(TokenType.QuoteLine1, "a\nb\n", "> a\n> b\n"),
                        Token.string("hoge")
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize(">> a\n>> b\n")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token(TokenType.QuoteLine2, "a\nb\n", ">> a\n>> b\n")
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_Center() {

        MfmTokenParser.tokenize("<center>abc</center>")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.centerStart(),
                        Token.string("abc"),
                        Token.centerEnd()
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_Centerと改行() {

        // https://github.com/misskey-dev/mfm.js/blob/develop/src/internal/parser.ts#L264
        // によると <center>のあとの改行、</center>の前後の改行は無視するらしい
        MfmTokenParser.tokenize("<center>\nabc</center>")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token(TokenType.CenterStart, "<center>", "<center>\n"),
                        Token.string("abc"),
                        Token.centerEnd()
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("<center>\nabc\n</center>")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token(TokenType.CenterStart, "<center>", "<center>\n"),
                        Token.string("abc"),
                        Token(TokenType.CenterEnd, "</center>", "\n</center>")
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_CenterBoldItalic() {

        MfmTokenParser.tokenize("<center>**Hello**, *World*!</center>")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
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
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_BoldAsta() {

        MfmTokenParser.tokenize("**abc**")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.boldAsta(),
                        Token.string("abc"),
                        Token.boldAsta()
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_Big() {

        MfmTokenParser.tokenize("***abc***")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.big(),
                        Token.string("abc"),
                        Token.big()
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_BoldTag() {

        MfmTokenParser.tokenize("<b>abc</b>")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.boldTagStart(),
                        Token.string("abc"),
                        Token.boldTagEnd()
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("aaa<b><i>hoge</i></b>bbb")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.string("aaa"),
                        Token.boldTagStart(),
                        Token.italicTagStart(),
                        Token.string("hoge"),
                        Token.italicTagEnd(),
                        Token.boldTagEnd(),
                        Token.string("bbb")
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_BoldUnder() {

        MfmTokenParser.tokenize("__abc__")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.boldUnder(),
                        Token.string("abc"),
                        Token.boldUnder()
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_BoldItalic() {

        MfmTokenParser.tokenize("**Hello**, *World*!")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.boldAsta(),
                        Token.string("Hello"),
                        Token.boldAsta(),
                        Token.string(", "),
                        Token.italicAsta(),
                        Token.string("World"),
                        Token.italicAsta(),
                        Token.string("!")
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_Small() {

        MfmTokenParser.tokenize("<small>abc</small>")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.smallStart(),
                        Token.string("abc"),
                        Token.smallEnd()
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("<small>**Hello**, *World*!</small>")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
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
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_Italic() {

        MfmTokenParser.tokenize("*abc*")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.italicAsta(),
                        Token.string("abc"),
                        Token.italicAsta()
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("<i>abc</i>")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.italicTagStart(),
                        Token.string("abc"),
                        Token.italicTagEnd()
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("_abc_")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.italicUnder(),
                        Token.string("abc"),
                        Token.italicUnder(),
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_Strike() {

        MfmTokenParser.tokenize("<s>abc</s>")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.strikeTagStart(),
                        Token.string("abc"),
                        Token.strikeTagEnd()
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("~~abc~~")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.strikeWave(),
                        Token.string("abc"),
                        Token.strikeWave()
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_Url() {

        MfmTokenParser.tokenize("https://misskey.io/@ai")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.url("https://misskey.io/@ai")
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_UrlWithTitle() {

        MfmTokenParser.tokenize("[ai](https://misskey.io/@ai)")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.urlWithTitle("[ai](https://misskey.io/@ai)")
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("[すごーく 長い 説明文](https://twitpane.com/)")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.urlWithTitle("[すごーく 長い 説明文](https://twitpane.com/)")
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_Function() {

        MfmTokenParser.tokenize("$[x2 abc]")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.functionStart("x2"),
                        Token.string("abc"),
                        Token.functionEnd()
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("$[x2 $[rotate.deg=340 abc]]")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.functionStart("x2"),
                        Token.functionStart("rotate.deg=340"),
                        Token.string("abc"),
                        Token.functionEnd(),
                        Token.functionEnd()
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("\$[x2 大きな文字！ :hyper_vibecat:]")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.functionStart("x2"),
                        Token.string("大きな文字！ "),
                        Token.emojiCode(":hyper_vibecat:"),
                        Token.functionEnd()
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_smallが大量にあるパターン() {

        // https://misskey.io/notes/9h3z1y499e
        MfmTokenParser.tokenize("<small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small>v<small><small>うんち</small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small>")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
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
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_その他() {

        MfmTokenParser.tokenize("\$[x2 **:vjtakagi_confused:**]")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.functionStart("x2"),
                        Token.boldAsta(),
                        Token.emojiCode(":vjtakagi_confused:"),
                        Token.boldAsta(),
                        Token.functionEnd()
                    ), it.holder.tokenList
                )
            }

        MfmTokenParser.tokenize("あれこれ[第1話]ほげほげ")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.string("あれこれ[第1話"),
                        Token.functionEnd(),
                        Token.string("ほげほげ"),
                    ), it.holder.tokenList
                )
            }
    }

    @Test
    fun tokenize_InlineCode() {

        MfmTokenParser.tokenize("hoge\ntest=>`fuga`")
            .let {
                assertTrue(it.success)
                assertEquals(
                    listOf(
                        Token.string("hoge\ntest=>"),
                        Token.inlineCode(),
                        Token.string("fuga"),
                        Token.inlineCode()
                    ), it.holder.tokenList
                )
            }
    }


}