@file:Suppress("TestFunctionName", "NonAsciiCharacters", "SpellCheckingInspection")

package jp.takke.mfm_kt

import jp.takke.mfm_kt.syntax_parser.MfmNode
import jp.takke.mfm_kt.syntax_parser.MfmParseException
import jp.takke.mfm_kt.syntax_parser.MfmSyntaxParser
import jp.takke.mfm_kt.token_parser.MfmTokenParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class MfmSyntaxParserTest {

    private val optionAll = MfmSyntaxParser.Option(enableEmoji = false)

    @Test
    fun parse_simple() {

        MfmSyntaxParser(MfmTokenParser.tokenize(""), MfmSyntaxParser.Option())
            .parse()
            .let {
                assertEquals(emptyList(), it)
            }

        MfmSyntaxParser(MfmTokenParser.tokenize("hoge"), MfmSyntaxParser.Option())
            .parse()
            .let {
                assertEquals(
                    listOf(MfmNode.Text("hoge")),
                    it
                )
            }
    }

    @Test
    fun parse_option_all_off() {

        // オプションを全てオフにするとただのテキストになること
        val option = MfmSyntaxParser.Option(
            enableBig = false,
            enableBold = false,
            enableItalic = false,
            enableStrike = false,
            enableCenter = false,
            enableSmall = false,
            enableQuote = false,
            enableFunction = false,
            enableInline = false,
            enableEmoji = false,
            enableMention = false,
            enableUrl = false,
        )

        checkSyntaxParser(
            "1 word",
            "hoge",
            option,
            listOf(
                MfmNode.Text("hoge"),
            )
        )

        checkSyntaxParser(
            "big",
            "hoge***big***",
            option,
            listOf(
                MfmNode.Text("hoge***big***"),
            )
        )

        checkSyntaxParser(
            "strike",
            "hoge<s>big</s>",
            option,
            listOf(
                MfmNode.Text("hoge<s>big</s>"),
            )
        )

        checkSyntaxParser(
            "bold",
            "hoge**bold**",
            option,
            listOf(
                MfmNode.Text("hoge**bold**"),
            )
        )

        checkSyntaxParser(
            "inline",
            "hoge`bold`",
            option,
            listOf(
                MfmNode.Text("hoge`bold`"),
            )
        )

        checkSyntaxParser(
            "bold+italic",
            "hoge**bold**and*italic*",
            option,
            listOf(
                MfmNode.Text("hoge**bold**and*italic*"),
            )
        )

        checkSyntaxParser(
            "bold+italic2",
            "hoge**bold**and*italic*_a_<i>hoge</i>",
            option,
            listOf(
                MfmNode.Text("hoge**bold**and*italic*_a_<i>hoge</i>"),
            )
        )

        checkSyntaxParser(
            "bold+italic+center",
            "<center>hoge**bold**and*italic*</center>",
            option,
            listOf(
                MfmNode.Text("<center>hoge**bold**and*italic*</center>"),
            )
        )

        checkSyntaxParser(
            "bold+italic+center+small",
            "<center>hoge**bold**and*italic*</center><small>ちいさい</small>",
            option,
            listOf(
                MfmNode.Text("<center>hoge**bold**and*italic*</center><small>ちいさい</small>"),
            )
        )

        checkSyntaxParser(
            "bold+italic+center+small+quote",
            "<center>hoge**bold**and*italic*</center><small>ちいさい</small>\n>a\n>>b\n",
            option,
            listOf(
                MfmNode.Text("<center>hoge**bold**and*italic*</center><small>ちいさい</small>\n>a\n>>b\n"),
            )
        )

        checkSyntaxParser(
            "bold+italic+center+small+quote+fn+big+mention",
            "<center>hoge**bold**$[x2 and]*italic*</center><small>ちいさい</small>\n>a\n>>b\n***hou***@hoge@huga",
            option,
            listOf(
                MfmNode.Text("<center>hoge**bold**$[x2 and]*italic*</center><small>ちいさい</small>\n>a\n>>b\n***hou***@hoge@huga")
            )
        )

        checkSyntaxParser(
            "bold+italic+center+small+quote+fn+big+url",
            "<center>hoge**bold**$[x2 and]*italic*</center><small>ちいさい</small>\n>a\n>>b\n***hou***@hoge@hugahttps://misskey.io/@ai",
            option,
            listOf(
                MfmNode.Text("<center>hoge**bold**$[x2 and]*italic*</center><small>ちいさい</small>\n>a\n>>b\n***hou***@hoge@hugahttps://misskey.io/@ai")
            )
        )
    }

    @Test
    fun parse_quote() {

        val option = MfmSyntaxParser.Option(
            enableBold = true,
            enableItalic = true,
            enableCenter = true,
            enableSmall = true,
            enableQuote = true,
            enableFunction = false
        )

        // >aaa
        // > aaa
        // >bbb
        // >>a
        // >> b

        checkSyntaxParser(
            "quote1 1行(改行なし)",
            ">aaa",
            option,
            listOf(
                MfmNode.Text(">aaa"),
            )
        )

        checkSyntaxParser(
            "quote1 1行",
            ">aaa\n",
            option,
            listOf(
                MfmNode.Quote(
                    MfmNode.QuoteLevel.Level1,
                    MfmNode.Text("aaa\n")
                ),
            )
        )

        checkSyntaxParser(
            "quote1 2行 => 結合されること",
            ">aaa\n>bbb\n",
            option,
            listOf(
                MfmNode.Quote(
                    MfmNode.QuoteLevel.Level1,
                    MfmNode.Text("aaa\nbbb\n")
                ),
            )
        )

        checkSyntaxParser(
            "quote1 2行 => 行頭のスペースは無視されること",
            ">aaa\n> bbb\n",
            option,
            listOf(
                MfmNode.Quote(
                    MfmNode.QuoteLevel.Level1,
                    MfmNode.Text("aaa\nbbb\n")
                ),
            )
        )

        checkSyntaxParser(
            "quote2 2行 => 行頭のスペースは無視されること",
            ">>aaa\n>> bbb\n",
            option,
            listOf(
                MfmNode.Quote(
                    MfmNode.QuoteLevel.Level2,
                    MfmNode.Text("aaa\nbbb\n")
                ),
            )
        )

        checkSyntaxParser(
            "quote1+2 2行 => 行頭のスペースは無視されること",
            ">>aaa\n>> bbb\n>c\n",
            option,
            listOf(
                MfmNode.Quote(
                    MfmNode.QuoteLevel.Level2,
                    MfmNode.Text("aaa\nbbb\n")
                ),
                MfmNode.Quote(
                    MfmNode.QuoteLevel.Level1,
                    MfmNode.Text("c\n")
                ),
            )
        )
    }

    @Test
    fun parse_center() {

        val option = MfmSyntaxParser.Option(
            enableBold = true,
            enableItalic = true,
            enableCenter = true,
            enableSmall = false,
            enableQuote = false,
            enableFunction = false
        )

        // <center>...</center>

        checkSyntaxParser(
            "center",
            "<center>hoge</center>",
            option,
            listOf(
                MfmNode.Center(
                    MfmNode.Text("hoge")
                ),
            )
        )

        checkSyntaxParser(
            "center 複数行1",
            "a\n<center>hoge</center>\nb",
            option,
            listOf(
                MfmNode.Text("a\n"),
                MfmNode.Center(
                    MfmNode.Text("hoge")
                ),
                MfmNode.Text("\nb"),
            )
        )

        checkSyntaxParser(
            "center 複数行2",
            "a\n<center>\nhoge1\nhoge2\n</center>\nb",
            option,
            listOf(
                MfmNode.Text("a\n"),
                MfmNode.Center(
                    MfmNode.Text("hoge1\nhoge2")
                ),
                MfmNode.Text("\nb"),
            )
        )

        checkSyntaxParser(
            "center 複数行+bold",
            "a\n<center>\n**hoge1**\nhoge2\n</center>\nb",
            option,
            listOf(
                MfmNode.Text("a\n"),
                MfmNode.Center(
                    MfmNode.Bold(
                        MfmNode.Text("hoge1")
                    ),
                    MfmNode.Text("\nhoge2")
                ),
                MfmNode.Text("\nb"),
            )
        )
    }

    @Test
    fun parse_center_閉じず() {

        val option = MfmSyntaxParser.Option(
            enableBold = true,
            enableItalic = true,
            enableCenter = true,
            enableSmall = false,
            enableQuote = false,
            enableFunction = false
        )

        checkSyntaxParser(
            "center",
            "<center>hoge",
            option,
            listOf(
                MfmNode.Text("<center>hoge"),
            )
        )

        checkSyntaxParser(
            "center 複数行1",
            "a\n<center>hoge\nb",
            option,
            listOf(
                MfmNode.Text("a\n<center>hoge\nb"),
            )
        )

        checkSyntaxParser(
            "center 複数行+bold",
            "a\n<center>\n**hoge1**\nhoge2\n\nb",
            option,
            listOf(
                MfmNode.Text("a\n<center>\n"),
                MfmNode.Bold(
                    MfmNode.Text("hoge1")
                ),
                MfmNode.Text("\nhoge2\n\nb"),
            )
        )
    }

    @Test
    fun parse_big() {

        val option = optionAll

        checkSyntaxParser(
            "big1",
            "***hoge***",
            option,
            listOf(
                MfmNode.Big(
                    MfmNode.Text("hoge")
                ),
            )
        )

        checkSyntaxParser(
            "big2",
            "aaa***hoge***bbb",
            option,
            listOf(
                MfmNode.Text("aaa"),
                MfmNode.Big(
                    MfmNode.Text("hoge")
                ),
                MfmNode.Text("bbb"),
            )
        )

        // *** の間は いろいろOK
        checkSyntaxParser(
            "big + tag",
            "aaa***<i>hoge</i>***bbb",
            option,
            listOf(
                MfmNode.Text("aaa"),
                MfmNode.Big(
                    MfmNode.Italic(
                        MfmNode.Text("hoge")
                    )
                ),
                MfmNode.Text("bbb"),
            )
        )
    }

    @Test
    fun parse_bold() {

        val option = MfmSyntaxParser.Option(
            enableBold = true,
            enableItalic = true,
            enableCenter = false,
            enableSmall = false,
            enableQuote = false,
            enableFunction = false
        )

        checkSyntaxParser(
            "1 word",
            "hoge",
            option,
            listOf(
                MfmNode.Text("hoge"),
            )
        )

        checkSyntaxParser(
            "bold1",
            "**hoge**",
            option,
            listOf(
                MfmNode.Bold(
                    MfmNode.Text("hoge")
                ),
            )
        )

        checkSyntaxParser(
            "bold2",
            "aaa**hoge**bbb",
            option,
            listOf(
                MfmNode.Text("aaa"),
                MfmNode.Bold(
                    MfmNode.Text("hoge")
                ),
                MfmNode.Text("bbb"),
            )
        )

        checkSyntaxParser(
            "bold tag",
            "aaa<b>hoge</b>bbb",
            option,
            listOf(
                MfmNode.Text("aaa"),
                MfmNode.Bold(
                    MfmNode.Text("hoge")
                ),
                MfmNode.Text("bbb"),
            )
        )

        checkSyntaxParser(
            "bold tag 2",
            "aaa<b><i>hoge</i></b>bbb",
            option,
            listOf(
                MfmNode.Text("aaa"),
                MfmNode.Bold(
                    MfmNode.Italic(
                        MfmNode.Text("hoge")
                    )
                ),
                MfmNode.Text("bbb"),
            )
        )

        checkSyntaxParser(
            "bold under",
            "aaa__hoge__bbb",
            option,
            listOf(
                MfmNode.Text("aaa"),
                MfmNode.Bold(
                    MfmNode.Text("hoge")
                ),
                MfmNode.Text("bbb"),
            )
        )

        checkSyntaxParser(
            "bold under",
            "aaa__ho ge__bbb",
            option,
            listOf(
                MfmNode.Text("aaa"),
                MfmNode.Bold(
                    MfmNode.Text("ho ge")
                ),
                MfmNode.Text("bbb"),
            )
        )

        // __ の間は alpha+num+sp
        checkSyntaxParser(
            "bold under 2",
            "aaa__<i>hoge</i>__bbb",
            option,
            listOf(
                MfmNode.Text("aaa__"),
                MfmNode.Italic(
                    MfmNode.Text("hoge")
                ),
                MfmNode.Text("__bbb"),
            )
        )
    }

    @Test
    fun parse_bold_閉じず() {

        checkSyntaxParser(
            "bold 閉じず",
            "aaa**hoge",
            optionAll,
            listOf(
                MfmNode.Text("aaa**hoge"),
            )
        )
    }

    @Test
    fun parse_emoji() {

        checkSyntaxParser(
            "emoji",
            ":hoge:",
            MfmSyntaxParser.Option(),
            listOf(
                MfmNode.EmojiCode(":hoge:"),
            )
        )

        checkSyntaxParser(
            "emoji2",
            "aaa:hoge:bbb",
            MfmSyntaxParser.Option(),
            listOf(
                MfmNode.Text("aaa"),
                MfmNode.EmojiCode(":hoge:"),
                MfmNode.Text("bbb"),
            )
        )
    }

    @Test
    fun parse_mention() {

        checkSyntaxParser(
            "mention1",
            "@hoge",
            optionAll,
            listOf(
                MfmNode.Mention("@hoge"),
            )
        )

        checkSyntaxParser(
            "mention2",
            "@hoge@fuga",
            optionAll,
            listOf(
                MfmNode.Mention("@hoge@fuga"),
            )
        )
    }

    @Test
    fun parse_small() {

        val option = MfmSyntaxParser.Option(
            enableBold = true,
            enableItalic = true,
            enableCenter = true,
            enableSmall = true,
            enableQuote = false,
            enableFunction = false
        )

        // <small>...</small>

        checkSyntaxParser(
            "small",
            "<small>hoge</small>",
            option,
            listOf(
                MfmNode.Small(
                    MfmNode.Text("hoge")
                ),
            )
        )

        checkSyntaxParser(
            "small 複数行1",
            "a\n<small>hoge</small>\nb",
            option,
            listOf(
                MfmNode.Text("a\n"),
                MfmNode.Small(
                    MfmNode.Text("hoge")
                ),
                MfmNode.Text("\nb"),
            )
        )

        checkSyntaxParser(
            "small 複数行2",
            "a\n<small>\nhoge1\nhoge2\n</small>\nb",
            option,
            listOf(
                MfmNode.Text("a\n"),
                MfmNode.Small(
                    MfmNode.Text("\nhoge1\nhoge2\n")
                ),
                MfmNode.Text("\nb"),
            )
        )

        checkSyntaxParser(
            "small 複数行+bold",
            "a\n<small>\n**hoge1**\nhoge2\n</small>\nb",
            option,
            listOf(
                MfmNode.Text("a\n"),
                MfmNode.Small(
                    MfmNode.Text("\n"),
                    MfmNode.Bold(
                        MfmNode.Text("hoge1")
                    ),
                    MfmNode.Text("\nhoge2\n")
                ),
                MfmNode.Text("\nb"),
            )
        )
    }

    @Test
    fun parse_small_閉じず() {

        val option = MfmSyntaxParser.Option(
            enableBold = true,
            enableItalic = true,
            enableCenter = true,
            enableSmall = false,
            enableQuote = false,
            enableFunction = false
        )

        checkSyntaxParser(
            "small",
            "<small>hoge",
            option,
            listOf(
                MfmNode.Text("<small>hoge"),
            )
        )

        checkSyntaxParser(
            "small 複数行1",
            "a\n<small>hoge\nb",
            option,
            listOf(
                MfmNode.Text("a\n<small>hoge\nb"),
            )
        )

        checkSyntaxParser(
            "small 複数行+bold",
            "a\n<small>\n**hoge1**\nhoge2\n\nb",
            option,
            listOf(
                MfmNode.Text("a\n<small>\n"),
                MfmNode.Bold(
                    MfmNode.Text("hoge1")
                ),
                MfmNode.Text("\nhoge2\n\nb"),
            )
        )
    }

    @Test
    fun parse_italic() {

        val option = MfmSyntaxParser.Option(
            enableBold = true,
            enableItalic = true,
            enableCenter = false,
            enableSmall = false,
            enableQuote = false,
            enableFunction = false
        )

        // *...* と <i>...</i>, _..._ の3パターンある

        checkSyntaxParser(
            "italic*",
            "*hoge1*",
            option,
            listOf(
                MfmNode.Italic(
                    MfmNode.Text("hoge1")
                ),
            )
        )

        // *と*の間はローマ字と数値のみ許可
        checkSyntaxParser(
            "italic* ローマ字と数値以外",
            "*ほげ*",
            option,
            listOf(
                MfmNode.Text("*ほげ*"),
            )
        )

        checkSyntaxParser(
            "italic tag",
            "<i>hoge</i>",
            option,
            listOf(
                MfmNode.Italic(
                    MfmNode.Text("hoge")
                ),
            )
        )

        checkSyntaxParser(
            "italic + bold",
            "<i>**hoge**</i>",
            option,
            listOf(
                MfmNode.Italic(
                    MfmNode.Bold(
                        MfmNode.Text("hoge")
                    )
                ),
            )
        )

        checkSyntaxParser(
            "italic_",
            "_hoge1_",
            option,
            listOf(
                MfmNode.Italic(
                    MfmNode.Text("hoge1")
                ),
            )
        )

        // _と_の間はローマ字と数値のみ許可
        checkSyntaxParser(
            "italic_ ローマ字と数値以外",
            "_ほげ_",
            option,
            listOf(
                MfmNode.Text("_ほげ_"),
            )
        )
    }

    @Test
    fun parse_italic_閉じず() {

        checkSyntaxParser(
            "italic* 閉じず",
            "*hoge",
            optionAll,
            listOf(
                MfmNode.Text("*hoge"),
            )
        )

        checkSyntaxParser(
            "italic tag 閉じず",
            "<i>hoge",
            optionAll,
            listOf(
                MfmNode.Text("<i>hoge"),
            )
        )

        checkSyntaxParser(
            "italic + bold 閉じず",
            "<i>**hoge**",
            optionAll,
            listOf(
                MfmNode.Text("<i>"),
                MfmNode.Bold(
                    MfmNode.Text("hoge")
                )
            )
        )
    }

    @Test
    fun parse_strike() {

        val option = optionAll

        checkSyntaxParser(
            "<s>strike</s>",
            "<s>strike</s>",
            option,
            listOf(
                MfmNode.Strike(
                    MfmNode.Text("strike")
                ),
            )
        )

        checkSyntaxParser(
            "strike wave",
            "~~strike~~",
            option,
            listOf(
                MfmNode.Strike(
                    MfmNode.Text("strike")
                ),
            )
        )

        checkSyntaxParser(
            "strike wave2",
            "~~str\nike~~",
            option,
            listOf(
                MfmNode.Text("~~str\nike~~")
            )
        )
    }

    @Test
    fun parse_function() {

        checkSyntaxParser(
            "Function x2",
            "$[x2 hoge]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "x2",
                    MfmNode.Text("hoge")
                ),
            )
        )
        checkSyntaxParser(
            "Function x3",
            "$[x3 hoge]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "x3",
                    MfmNode.Text("hoge")
                ),
            )
        )
        checkSyntaxParser(
            "Function x4",
            "$[x4 hoge]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "x4",
                    MfmNode.Text("hoge")
                ),
            )
        )

        // 実際にはなくてもパースはできること
        checkSyntaxParser(
            "Function x1",
            "$[x1 hoge]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "x1",
                    MfmNode.Text("hoge")
                ),
            )
        )
        checkSyntaxParser(
            "Function x5",
            "$[x5 hoge]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "x5",
                    MfmNode.Text("hoge")
                ),
            )
        )

        checkSyntaxParser(
            "Function + Bold",
            "$[x4 **hoge**]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "x4",
                    MfmNode.Bold(
                        MfmNode.Text("hoge")
                    )
                ),
            )
        )

        checkSyntaxParser(
            "Function quote+function",
            "#ラッキーカラー診断\n" +
                    "今日のラッキーカラーは`#86DB67`でした！\n" +
                    "今日のラッキーカラーはこんにゃ色 -> \$[bg.color=86DB67 :blank::blank::blank:]",
            optionAll.copy(enableQuote = false, enableEmoji = false),
            listOf(
                MfmNode.Text("#ラッキーカラー診断\n今日のラッキーカラーは"),
                MfmNode.InlineCode(
                    MfmNode.Text("#86DB67")
                ),
                MfmNode.Text("でした！\n今日のラッキーカラーはこんにゃ色 -> "),
                MfmNode.Function(
                    "bg.color=86DB67",
                    MfmNode.Text(":blank::blank::blank:")
                ),
            )
        )

        checkSyntaxParser(
            "Function x2 + _",
            "\$[x2 大きな文字！ :hyper_vibecat:]",
            optionAll.copy(enableQuote = false, enableEmoji = false),
            listOf(
                MfmNode.Function(
                    "x2",
                    MfmNode.Text("大きな文字！ :hyper_vibecat:")
                ),
            )
        )
    }

    @Test
    fun parse_function_ruby() {

        checkSyntaxParser(
            "Function ruby",
            "$[ruby 三須木 みすき]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "ruby",
                    MfmNode.Text("三須木 みすき"),
                ),
            )
        )
    }

    @Test
    fun parse_function_閉じず() {

        checkSyntaxParser(
            "Function x2",
            "$[x2 hoge",
            optionAll,
            listOf(
                MfmNode.Text("$[x2 hoge"),
            )
        )

        checkSyntaxParser(
            "Function + Bold",
            "$[x4 **hoge**",
            optionAll,
            listOf(
                MfmNode.Text("$[x4 "),
                MfmNode.Bold(
                    MfmNode.Text("hoge")
                ),
            )
        )
    }

    @Test
    fun parse_inline() {

        val option = optionAll

        checkSyntaxParser(
            "`inline`",
            "`inline`",
            option,
            listOf(
                MfmNode.InlineCode(
                    MfmNode.Text("inline")
                ),
            )
        )

        checkSyntaxParser(
            "inline 2",
            "`in\nline`",
            option,
            listOf(
                MfmNode.Text("`in\nline`")
            )
        )

        checkSyntaxParser(
            "inline 3",
            "なにか(´･ω･`)あれこれ(´･ω･`)",
            option,
            listOf(
                MfmNode.Text("なにか(´･ω･`)あれこれ(´･ω･`)")
            )
        )

        checkSyntaxParser(
            "inline + underline",
            "`foo _bar_ baz`",
            option,
            listOf(
                MfmNode.InlineCode(
                    MfmNode.Text("foo _bar_ baz")
                ),
            )
        )
    }

    // https://github.com/misskey-dev/mfm.js/blob/develop/test/parser.ts#L647
    @Test
    fun parse_inlineCode() {
        // 		test('basic', () => {
        //			const input = '`var x = "Strawberry Pasta";`';
        //			const output = [INLINE_CODE('var x = "Strawberry Pasta";')];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "basic",
            "`var x = \"Strawberry Pasta\";`",
            optionAll,
            listOf(
                MfmNode.InlineCode(
                    MfmNode.Text("var x = \"Strawberry Pasta\";")
                ),
            )
        )

        //		test('disallow line break', () => {
        //			const input = '`foo\nbar`';
        //			const output = [TEXT('`foo\nbar`')];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "disallow line break",
            "`foo\nbar`",
            optionAll,
            listOf(
                MfmNode.Text("`foo\nbar`")
            )
        )

        //		test('disallow ´', () => {
        //			const input = '`foo´bar`';
        //			const output = [TEXT('`foo´bar`')];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "disallow ´",
            "`foo´bar`",
            optionAll,
            listOf(
                MfmNode.Text("`foo´bar`")
            )
        )
    }

    // https://github.com/misskey-dev/mfm.js/blob/develop/test/parser.ts#L906
    @Test
    fun parse_url() {

        val option = MfmSyntaxParser.Option()

        //		test('basic', () => {
        //			const input = 'https://misskey.io/@ai';
        //			const output = [
        //				N_URL('https://misskey.io/@ai'),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "basic",
            "https://misskey.io/@ai",
            option,
            listOf(
                MfmNode.Url("https://misskey.io/@ai"),
            )
        )

        // 		test('with other texts', () => {
        //			const input = 'official instance: https://misskey.io/@ai.';
        //			const output = [
        //				TEXT('official instance: '),
        //				N_URL('https://misskey.io/@ai'),
        //				TEXT('.')
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "with other texts",
            "official instance: https://misskey.io/@ai.",
            option,
            listOf(
                MfmNode.Text("official instance: "),
                MfmNode.Url("https://misskey.io/@ai"),
                MfmNode.Text("."),
            )
        )

        // 		test('ignore trailing period', () => {
        //			const input = 'https://misskey.io/@ai.';
        //			const output = [
        //				N_URL('https://misskey.io/@ai'),
        //				TEXT('.')
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "ignore trailing periods",
            "https://misskey.io/@ai.",
            option,
            listOf(
                MfmNode.Url("https://misskey.io/@ai"),
                MfmNode.Text("."),
            )
        )

        //		test('disallow period only', () => {
        //			const input = 'https://.';
        //			const output = [
        //				TEXT('https://.')
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "disallow period only",
            "https://.",
            option,
            listOf(
                MfmNode.Text("https://."),
            )
        )

        //		test('ignore trailing periods', () => {
        //			const input = 'https://misskey.io/@ai...';
        //			const output = [
        //				N_URL('https://misskey.io/@ai'),
        //				TEXT('...')
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "ignore trailing periods",
            "https://misskey.io/@ai...",
            option,
            listOf(
                MfmNode.Url("https://misskey.io/@ai"),
                MfmNode.Text("..."),
            )
        )

        //		test('with comma', () => {
        //			const input = 'https://example.com/foo?bar=a,b';
        //			const output = [
        //				N_URL('https://example.com/foo?bar=a,b'),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "with comma",
            "https://example.com/foo?bar=a,b",
            option,
            listOf(
                MfmNode.Url("https://example.com/foo?bar=a,b"),
            )
        )

        //		test('ignore trailing comma', () => {
        //			const input = 'https://example.com/foo, bar';
        //			const output = [
        //				N_URL('https://example.com/foo'),
        //				TEXT(', bar')
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "ignore trailing comma",
            "https://example.com/foo, bar",
            option,
            listOf(
                MfmNode.Url("https://example.com/foo"),
                MfmNode.Text(", bar"),
            )
        )

        //		test('with brackets', () => {
        //			const input = 'https://example.com/foo(bar)';
        //			const output = [
        //				N_URL('https://example.com/foo(bar)'),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "with brackets",
            "https://example.com/foo(bar)",
            option,
            listOf(
                MfmNode.Url("https://example.com/foo(bar)"),
            )
        )

        //		test('ignore parent brackets', () => {
        //			const input = '(https://example.com/foo)';
        //			const output = [
        //				TEXT('('),
        //				N_URL('https://example.com/foo'),
        //				TEXT(')'),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "ignore parent brackets",
            "(https://example.com/foo)",
            option,
            listOf(
                MfmNode.Text("("),
                MfmNode.Url("https://example.com/foo"),
                MfmNode.Text(")"),
            )
        )

        //		test('ignore parent brackets (2)', () => {
        //			const input = '(foo https://example.com/foo)';
        //			const output = [
        //				TEXT('(foo '),
        //				N_URL('https://example.com/foo'),
        //				TEXT(')'),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "ignore parent brackets (2)",
            "(foo https://example.com/foo)",
            option,
            listOf(
                MfmNode.Text("(foo "),
                MfmNode.Url("https://example.com/foo"),
                MfmNode.Text(")"),
            )
        )

        //		test('ignore parent brackets with internal brackets', () => {
        //			const input = '(https://example.com/foo(bar))';
        //			const output = [
        //				TEXT('('),
        //				N_URL('https://example.com/foo(bar)'),
        //				TEXT(')'),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "ignore parent brackets with internal brackets",
            "(https://example.com/foo(bar))",
            option,
            listOf(
                MfmNode.Text("("),
                MfmNode.Url("https://example.com/foo(bar)"),
                MfmNode.Text(")"),
            )
        )

        //		test('ignore parent []', () => {
        //			const input = 'foo [https://example.com/foo] bar';
        //			const output = [
        //				TEXT('foo ['),
        //				N_URL('https://example.com/foo'),
        //				TEXT('] bar'),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "ignore parent []",
            "foo [https://example.com/foo] bar",
            option,
            listOf(
                MfmNode.Text("foo ["),
                MfmNode.Url("https://example.com/foo"),
                MfmNode.Text("] bar"),
            )
        )

        //		test('ignore non-ascii characters contained url without angle brackets', () => {
        //			const input = 'https://大石泉すき.example.com';
        //			const output = [
        //				TEXT('https://大石泉すき.example.com'),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "ignore non-ascii characters contained url without angle brackets",
            "https://大石泉すき.example.com",
            option,
            listOf(
                MfmNode.Text("https://大石泉すき.example.com"),
            )
        )

        //		test('match non-ascii characters contained url with angle brackets', () => {
        //			const input = '<https://大石泉すき.example.com>';
        //			const output = [
        //				N_URL('https://大石泉すき.example.com', true),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
//        checkSyntaxParser(
//            "match non-ascii characters contained url with angle brackets",
//            "<https://大石泉すき.example.com>",
//            option,
//            listOf(
//                MfmNode.Url("https://大石泉すき.example.com", true),
//            )
//        )

        //		test('prevent xss', () => {
        //			const input = 'javascript:foo';
        //			const output = [
        //				TEXT('javascript:foo')
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "prevent xss",
            "javascript:foo",
            option,
            listOf(
                MfmNode.Text("javascript:foo")
            )
        )
    }

    // https://github.com/misskey-dev/mfm.js/blob/develop/test/parser.ts#L1041
    @Test
    fun parse_urlWithTitle() {

        val option = MfmSyntaxParser.Option()

        checkSyntaxParser(
            "urlWithTitle",
            "[title](https://twitpane.com/)",
            option,
            listOf(
                MfmNode.UrlWithTitle("https://twitpane.com/", MfmNode.Text("title"))
            )
        )

        //		test('basic', () => {
        //			const input = '[official instance](https://misskey.io/@ai).';
        //			const output = [
        //				LINK(false, 'https://misskey.io/@ai', [
        //					TEXT('official instance')
        //				]),
        //				TEXT('.')
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "basic",
            "[official instance](https://misskey.io/@ai).",
            option,
            listOf(
                MfmNode.UrlWithTitle("https://misskey.io/@ai", MfmNode.Text("official instance")),
                MfmNode.Text(".")
            )
        )

        // TODO ?[title](url) の形式をサポートしたら下記を有効にすること

        //		test('silent flag', () => {
        //			const input = '?[official instance](https://misskey.io/@ai).';
        //			const output = [
        //				LINK(true, 'https://misskey.io/@ai', [
        //					TEXT('official instance')
        //				]),
        //				TEXT('.')
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});

        // TODO ?[title](<url>) の形式をサポートしたら下記を有効にすること

        //		test('with angle brackets url', () => {
        //			const input = '[official instance](<https://misskey.io/@ai>).';
        //			const output = [
        //				LINK(false, 'https://misskey.io/@ai', [
        //					TEXT('official instance')
        //				]),
        //				TEXT('.')
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});

        //		test('prevent xss', () => {
        //			const input = '[click here](javascript:foo)';
        //			const output = [
        //				TEXT('[click here](javascript:foo)')
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        checkSyntaxParser(
            "prevent xss",
            "[click here](javascript:foo)",
            option,
            listOf(
                MfmNode.Text("[click here](javascript:foo)")
            )
        )

        //		describe('cannot nest a url in a link label', () => {
        //			test('basic', () => {
        //				const input = 'official instance: [https://misskey.io/@ai](https://misskey.io/@ai).';
        //				const output = [
        //					TEXT('official instance: '),
        //					LINK(false, 'https://misskey.io/@ai', [
        //						TEXT('https://misskey.io/@ai'),
        //					]),
        //					TEXT('.'),
        //				];
        //				assert.deepStrictEqual(mfm.parse(input), output);
        //			});
        checkSyntaxParser(
            "cannot nest a url in a link label: basic",
            "official instance: [https://misskey.io/@ai](https://misskey.io/@ai).",
            option,
            listOf(
                MfmNode.Text("official instance: "),
                MfmNode.UrlWithTitle("https://misskey.io/@ai", MfmNode.Text("https://misskey.io/@ai")),
                MfmNode.Text(".")
            )
        )

        //			test('nested', () => {
        //				const input = 'official instance: [https://misskey.io/@ai**https://misskey.io/@ai**](https://misskey.io/@ai).';
        //				const output = [
        //					TEXT('official instance: '),
        //					LINK(false, 'https://misskey.io/@ai', [
        //						TEXT('https://misskey.io/@ai'),
        //						BOLD([
        //							TEXT('https://misskey.io/@ai'),
        //						]),
        //					]),
        //					TEXT('.'),
        //				];
        //				assert.deepStrictEqual(mfm.parse(input), output);
        //			});
        //		});
        checkSyntaxParser(
            "cannot nest a url in a link label: nested",
            "official instance: [https://misskey.io/@ai**https://misskey.io/@ai**](https://misskey.io/@ai).",
            option,
            listOf(
                MfmNode.Text("official instance: "),
                MfmNode.UrlWithTitle(
                    "https://misskey.io/@ai",
                    MfmNode.Text("https://misskey.io/@ai"),
                    MfmNode.Bold(
                        MfmNode.Text("https://misskey.io/@ai")
                    )
                ),
                MfmNode.Text(".")
            )
        )

        //		describe('cannot nest a link in a link label', () => {
        //			test('basic', () => {
        //				const input = 'official instance: [[https://misskey.io/@ai](https://misskey.io/@ai)](https://misskey.io/@ai).';
        //				const output = [
        //					TEXT('official instance: '),
        //					LINK(false, 'https://misskey.io/@ai', [
        //						TEXT('[https://misskey.io/@ai'),
        //					]),
        //					TEXT(']('),
        //					N_URL('https://misskey.io/@ai'),
        //					TEXT(').'),
        //				];
        //				assert.deepStrictEqual(mfm.parse(input), output);
        //			});
        checkSyntaxParser(
            "cannot nest a link in a link label: basic",
            "official instance: [[https://misskey.io/@ai](https://misskey.io/@ai)](https://misskey.io/@ai).",
            option,
            listOf(
                MfmNode.Text("official instance: "),
                MfmNode.UrlWithTitle(
                    "https://misskey.io/@ai",
                    MfmNode.Text("[https://misskey.io/@ai"),
                ),
                MfmNode.Text("]("),
                MfmNode.Url("https://misskey.io/@ai"),
                MfmNode.Text(").")
            )
        )

        //			test('nested', () => {
        //				const input = 'official instance: [**[https://misskey.io/@ai](https://misskey.io/@ai)**](https://misskey.io/@ai).';
        //				const output = [
        //					TEXT('official instance: '),
        //					LINK(false, 'https://misskey.io/@ai', [
        //						BOLD([
        //							TEXT('[https://misskey.io/@ai](https://misskey.io/@ai)'),
        //						]),
        //					]),
        //					TEXT('.'),
        //				];
        //			});
        //		});

        // mfm.js は上記の動きだが mfm.kt では下記の動作を許容する
        checkSyntaxParser(
            "cannot nest a link in a link label: nested",
            "official instance: [**[https://misskey.io/@ai](https://misskey.io/@ai)**](https://misskey.io/@ai).",
            option,
            listOf(
                MfmNode.Text("official instance: "),
                MfmNode.UrlWithTitle(
                    "https://misskey.io/@ai",
                    MfmNode.Text("**[https://misskey.io/@ai"),
                ),
                MfmNode.Text("**]("),
                MfmNode.Url("https://misskey.io/@ai"),
                MfmNode.Text(").")
            )
        )

        //		describe('cannot nest a mention in a link label', () => {
        //			test('basic', () => {
        //				const input = '[@example](https://example.com)';
        //				const output = [
        //					LINK(false, 'https://example.com', [
        //						TEXT('@example'),
        //					]),
        //				];
        //				assert.deepStrictEqual(mfm.parse(input), output);
        //			});
        checkSyntaxParser(
            "cannot nest a mention in a link label: basic",
            "[@example](https://example.com)",
            option,
            listOf(
                MfmNode.UrlWithTitle(
                    "https://example.com",
                    MfmNode.Text("@example")
                )
            )
        )

        //			test('nested', () => {
        //				const input = '[@example**@example**](https://example.com)';
        //				const output = [
        //					LINK(false, 'https://example.com', [
        //						TEXT('@example'),
        //						BOLD([
        //							TEXT('@example'),
        //						]),
        //					]),
        //				];
        //				assert.deepStrictEqual(mfm.parse(input), output);
        //			});
        //		});
        checkSyntaxParser(
            "cannot nest a mention in a link label: nested",
            "[@example**@example**](https://example.com)",
            option,
            listOf(
                MfmNode.UrlWithTitle(
                    "https://example.com",
                    MfmNode.Text("@example"),
                    MfmNode.Bold(
                        MfmNode.Text("@example")
                    )
                )
            )
        )

        // TODO URL 内の () 等も必要であれば対応すること

        //		test('with brackets', () => {
        //			const input = '[foo](https://example.com/foo(bar))';
        //			const output = [
        //				LINK(false, 'https://example.com/foo(bar)', [
        //					TEXT('foo')
        //				]),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        //
        //		test('with parent brackets', () => {
        //			const input = '([foo](https://example.com/foo(bar)))';
        //			const output = [
        //				TEXT('('),
        //				LINK(false, 'https://example.com/foo(bar)', [
        //					TEXT('foo')
        //				]),
        //				TEXT(')'),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        //
        //		test('with brackets before', () => {
        //			const input = '[test] foo [bar](https://example.com)';
        //			const output = [
        //				TEXT('[test] foo '),
        //				LINK(false, 'https://example.com', [
        //					TEXT('bar')
        //				]),
        //			];
        //			assert.deepStrictEqual(mfm.parse(input), output);
        //		});
        //	});
    }

    @Test
    fun parse_カッコを含む() {

        checkSyntaxParser(
            "[]を含む",
            "あれこれ[第1話]ほげほげ",
            optionAll,
            listOf(
                MfmNode.Text("あれこれ[第1話]ほげほげ")
            )
        )
    }

    @Test
    fun parse_アカウント名に_を含む() {

        checkSyntaxParser(
            "アカウント名に_を含む",
            "@hoge_fuga_",
            MfmSyntaxParser.Option(),
            listOf(
                MfmNode.Mention("@hoge_fuga_")
            )
        )

        checkSyntaxParser(
            "アカウント名に_を含む",
            "@hoge_fuga_",
            MfmSyntaxParser.Option(enableMention = false),
            listOf(
                MfmNode.Text("@hoge_fuga_")
            )
        )
    }

    @Test
    fun parse_URLに_を含む() {

        checkSyntaxParser(
            "URLに_を含む",
            "https://twitpane.com/left_right_",
            optionAll,
            listOf(
                MfmNode.Url("https://twitpane.com/left_right_")
            )
        )

        checkSyntaxParser(
            "URLに_を含む",
            "https://twitpane.com/left_right_",
            MfmSyntaxParser.Option(enableUrl = false),
            listOf(
                MfmNode.Text("https://twitpane.com/left_right_")
            )
        )
    }

    @Test
    fun parse_Functionの途中に含むパターン() {

        checkSyntaxParser(
            "Functionに**を含む",
            "\$[x2 **:vjtakagi_confused:**]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "x2",
                    MfmNode.Bold(
                        MfmNode.Text(":vjtakagi_confused:")
                    )
                ),
            )
        )

        checkSyntaxParser(
            "途中に*を含むパターン",
            "\$[bg.color=ECB1C6 hoge*:･ﾟ✧\n]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "bg.color=ECB1C6",
                    MfmNode.Text("hoge*:･ﾟ✧\n")
                ),
            )
        )

        checkSyntaxParser(
            "途中に***を含むパターン",
            "\$[bg.color=ECB1C6 hoge***:･ﾟ✧\n]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "bg.color=ECB1C6",
                    MfmNode.Text("hoge***:･ﾟ✧\n")
                ),
            )
        )

        checkSyntaxParser(
            "途中に<b>を含むパターン",
            "\$[bg.color=ECB1C6 hoge<b>:･ﾟ✧\n]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "bg.color=ECB1C6",
                    MfmNode.Text("hoge<b>:･ﾟ✧\n")
                ),
            )
        )

        checkSyntaxParser(
            "途中に**を含むパターン",
            "\$[bg.color=ECB1C6 hoge**:･ﾟ✧\n]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "bg.color=ECB1C6",
                    MfmNode.Text("hoge**:･ﾟ✧\n")
                ),
            )
        )

        checkSyntaxParser(
            "途中に<center>を含むパターン",
            "\$[bg.color=ECB1C6 hoge<center>:･ﾟ✧\n]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "bg.color=ECB1C6",
                    MfmNode.Text("hoge<center>:･ﾟ✧\n")
                ),
            )
        )

        checkSyntaxParser(
            "途中に__を含むパターン",
            "\$[bg.color=ECB1C6 hoge__:･ﾟ✧\n]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "bg.color=ECB1C6",
                    MfmNode.Text("hoge__:･ﾟ✧\n")
                ),
            )
        )

        checkSyntaxParser(
            "途中に~~を含むパターン",
            "\$[bg.color=ECB1C6 hoge~~:･ﾟ✧\n]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "bg.color=ECB1C6",
                    MfmNode.Text("hoge~~:･ﾟ✧\n")
                ),
            )
        )

        checkSyntaxParser(
            "途中に`を含むパターン",
            "\$[bg.color=ECB1C6 hoge`:･ﾟ✧\n]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "bg.color=ECB1C6",
                    MfmNode.Text("hoge`:･ﾟ✧\n")
                ),
            )
        )

        checkSyntaxParser(
            "途中に<small>を含むパターン",
            "\$[bg.color=ECB1C6 hoge<small>:･ﾟ✧\n]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "bg.color=ECB1C6",
                    MfmNode.Text("hoge<small>:･ﾟ✧\n")
                ),
            )
        )

        checkSyntaxParser(
            "途中に<i>を含むパターン",
            "\$[bg.color=ECB1C6 hoge<i>:･ﾟ✧\n]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "bg.color=ECB1C6",
                    MfmNode.Text("hoge<i>:･ﾟ✧\n")
                ),
            )
        )

        checkSyntaxParser(
            "途中に<s>を含むパターン",
            "\$[bg.color=ECB1C6 hoge<s>:･ﾟ✧\n]",
            optionAll,
            listOf(
                MfmNode.Function(
                    "bg.color=ECB1C6",
                    MfmNode.Text("hoge<s>:･ﾟ✧\n")
                ),
            )
        )

        checkSyntaxParser(
            "途中に \$[ を含むパターン",
            "<b>hoge\$[bg.color :･ﾟ✧\n</b>",
            optionAll,
            listOf(
                MfmNode.Bold(
                    MfmNode.Text("hoge\$[bg.color :･ﾟ✧\n")
                ),
            )
        )
    }

    @Test
    fun parse_smallが大量にあるパターン() {

        // https://misskey.io/notes/9h3z1y499e
        checkSyntaxParser(
            "smallが大量にあるパターン(深すぎる＆閉じタグがあっていない)",
            "<small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small>v<small><small>うんち</small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small></small>",
            optionAll,
            listOf(
                MfmNode.Small(
                    MfmNode.Small(
                        MfmNode.Small(
                            MfmNode.Small(
                                MfmNode.Small(
                                    MfmNode.Small(
                                        MfmNode.Small(
                                            MfmNode.Small(
                                                MfmNode.Small(
                                                    MfmNode.Text("<small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small><small>v<small><small>うんち")
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                MfmNode.Text("</small></small></small></small></small></small></small></small></small></small></small>")
            )
        )
    }

    @Test
    fun parse_small_閉じタグが多いパターン() {

        checkSyntaxParser(
            "small_閉じタグが多いパターン",
            "<small><small>aaa</small></small></small>",
            optionAll,
            listOf(
                MfmNode.Small(
                    MfmNode.Small(
                        MfmNode.Text("aaa")
                    )
                ),
                MfmNode.Text("</small>")
            )
        )
    }

    @Test
    fun parse_small_開きタグが多いパターン() {

        checkSyntaxParser(
            "small_開きタグが多いパターン",
            "<small><small><small><small>aaa</small></small></small>",
            optionAll,
            listOf(
                MfmNode.Text("<small>"),
                MfmNode.Small(
                    MfmNode.Small(
                        MfmNode.Small(
                            MfmNode.Text("aaa")
                        )
                    )
                ),
            )
        )

        checkSyntaxParser(
            "small_開きタグが多いパターン2",
            "<small><small><small><small><small><small><small><small><small><small><small><small>aaa</small></small></small>",
            optionAll,
            listOf(
                MfmNode.Text("<small><small><small><small><small><small><small><small><small>"),
                MfmNode.Small(
                    MfmNode.Small(
                        MfmNode.Small(
                            MfmNode.Text("aaa")
                        )
                    )
                ),
            )
        )
    }

    @Test
    fun function_props() {

        val option = optionAll

        // $[x2 ] => []
        MfmSyntaxParser(MfmTokenParser.tokenize("$[x2 ]"), option).parse().also {
            val function = it[0] as MfmNode.Function
            assertEquals("x2", function.name)
            assertEquals(emptyMap<String, String>(), function.args)
        }

        // $[font.serif ] => [("serif", "")]
        MfmSyntaxParser(MfmTokenParser.tokenize("$[font.serif ]"), option).parse().also {
            val function = it[0] as MfmNode.Function
            assertEquals("font", function.name)
            assertEquals(mapOf("serif" to ""), function.args)
        }

        // $[bg.color=00ee22 ] => (["color", "00ee22")]
        MfmSyntaxParser(MfmTokenParser.tokenize("$[bg.color=00ee22 ]"), option).parse().also {
            val function = it[0] as MfmNode.Function
            assertEquals("bg", function.name)
            assertEquals(mapOf("color" to "00ee22"), function.args)
        }

        // $[scale.x=1.2,y=1.5 ] => [("x", "1.2"), ("y", "1.5")]
        MfmSyntaxParser(MfmTokenParser.tokenize("$[scale.x=1.2,y=1.5 :waai:]"), option).parse().also {
            val function = it[0] as MfmNode.Function
            assertEquals("scale", function.name)
            assertEquals(mapOf("x" to "1.2", "y" to "1.5"), function.args)
        }
    }

    //--------------------------------------------------
    // custom checker
    //--------------------------------------------------

    private fun checkSyntaxParser(
        scenarioName: String,
        inputText: String,
        option: MfmSyntaxParser.Option,
        expected: List<MfmNode>
    ) {

        val tokens = MfmTokenParser.tokenize(inputText)
        val result = MfmSyntaxParser(tokens, option).parse()

        println("---- [$scenarioName] start")
        println("input: [$inputText]")

//        println("tokens:")
//        println(tokens.holder.tokenList.joinToString("\n"))

        println("actual:")
        dump(result)
        println("expected:")
        dump(expected)
        println("---- [$scenarioName] end")

        assertEquals(expected, result)
    }

    private fun dump(it: List<MfmNode>) {
        traverse(it, 1)
    }

    private fun traverse(parsedResult: List<MfmNode>, level: Int) {

        parsedResult.forEach { spr ->

            print("   ".repeat(level))
            when (spr) {
                is MfmNode.Text -> {
                    println("Text: \"${spr.value.replace("\n", "\\n")}\"")
                }

                is MfmNode.Quote -> {
                    println("Quote: (${spr.level})")
                    traverse(spr.children, level + 1)
                }

                is MfmNode.Center -> {
                    println("Center: ")
                    traverse(spr.children, level + 1)
                }

                is MfmNode.Big -> {
                    println("Big: ")
                    traverse(spr.children, level + 1)
                }

                is MfmNode.Bold -> {
                    println("Bold: ")
                    traverse(spr.children, level + 1)
                }

                is MfmNode.Small -> {
                    println("Small: ")
                    traverse(spr.children, level + 1)
                }

                is MfmNode.Italic -> {
                    println("Italic: ")
                    traverse(spr.children, level + 1)
                }

                is MfmNode.Strike -> {
                    println("Strike: ")
                    traverse(spr.children, level + 1)
                }

                is MfmNode.Function -> {
                    println("Function: ${spr.name} ${spr.args.map { "${it.key}=${it.value}" }}")
                    traverse(spr.children, level + 1)
                }

                is MfmNode.InlineCode -> {
                    println("InlineCode: ")
                    traverse(spr.children, level + 1)
                }

                is MfmNode.EmojiCode -> {
                    println("EmojiCode: ${spr.value}")
                }

                is MfmNode.Mention -> {
                    println("Mention: ${spr.value}")
                }

                is MfmNode.Url -> {
                    println("Url: ${spr.value}")
                }

                is MfmNode.UrlWithTitle -> {
                    println("UrlWithTitle: ${spr.url}")
                    traverse(spr.children, level + 1)
                }
            }
        }
    }

    /**
     * バックトラッキング回数制限のテスト
     * 未対応構文により大量のバックトラッキングが発生するケースで
     * MfmParseExceptionがスローされることを確認する
     */
    @Test
    fun parse_backtrack_limit_exceeded() {
        // 大量の未閉じ $[ を含むMFMを生成
        // $[ が20個、] が1個なので、19個の未閉じタグ
        // 最悪のケースで O(2^19) のバックトラッキングが発生する可能性がある
        val manyUnclosedFunctions = "$[a ".repeat(20) + "text]"

        assertFailsWith<MfmParseException> {
            MfmSyntaxParser(MfmTokenParser.tokenize(manyUnclosedFunctions), optionAll).parse()
        }
    }

}