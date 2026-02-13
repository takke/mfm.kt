package jp.takke.mfm_kt.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import jp.takke.mfm_kt.syntax_parser.MfmSyntaxParser
import jp.takke.mfm_kt.syntax_parser.MfmNode
import jp.takke.mfm_kt.token_parser.MfmTokenParser
import jp.takke.mfm_kt.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        binding.inputText.setText("**Hello**, *World*!")
        binding.inputText.setText(
            """> abc
>abc

>>nest
**Hello**, *World*!

$[x2 なにか]
</center>←先に閉じてみる
<center>**はろー**, *World*!</center>
<small>ほげ</small>`${'$'}abc <- 1` ほげほげ
"""
        )
//        binding.inputText.setText(
//            """
//                |**Hello**, *World*!
//                |**これはBold*ここはBold+Italic*ここもBold**になるかな？
//            """.trimMargin()
//        )

        binding.runButton.setOnClickListener {
            executeParser()
        }

        lifecycleScope.launch {
            delay(1000)

            binding.runButton.performClick()
        }
    }

    private fun executeParser() {

        val text = binding.inputText.text.toString()

        //--------------------------------------------------
        // 字句解析
        //--------------------------------------------------
        val startTick = System.currentTimeMillis()
        val tokenizedResult = MfmTokenParser.tokenize(text)
        val elapsed = System.currentTimeMillis() - startTick

        val ssb = SpannableStringBuilder()
        ssb.append("▼字句解析結果:(${elapsed}ms)\n")
        ssb.append(
            tokenizedResult.holder.tokenList.joinToString("\n") {
                val extractedValue = it.extractedValue.replace("\n", "\\n")
                val wholeText = it.wholeText.replace("\n", "\\n")
                if (extractedValue == wholeText) {
                    "${it.type}: [${extractedValue}]"
                } else {
                    "${it.type}: [${extractedValue}] [${wholeText}]"
                }
            }
        )

        ssb.append("\n----\n")

        //--------------------------------------------------
        // 構文解析
        //--------------------------------------------------
        val startTick2 = System.currentTimeMillis()
        val parsedResult = MfmSyntaxParser(tokenizedResult, MfmSyntaxParser.Option()).parse()
        val elapsed2 = System.currentTimeMillis() - startTick2
        ssb.append("▼構文解析結果:(${elapsed2}ms)\n")

        traverse(parsedResult, 0, ssb)

        //--------------------------------------------------
        // 構文解析(Boldのみ)
        //--------------------------------------------------
        val startTick3 = System.currentTimeMillis()
        val parsedResult2 = MfmSyntaxParser(
            tokenizedResult, MfmSyntaxParser.Option(
                enableBold = true,
                enableItalic = false,
                enableCenter = false,
                enableSmall = false,
                enableQuote = false,
                enableFunction = false
            )
        ).parse()
        val elapsed3 = System.currentTimeMillis() - startTick3
        ssb.append("▼構文解析結果(Boldのみ):(${elapsed3}ms)\n")

        traverse(parsedResult2, 0, ssb)

        binding.resultText.text = ssb
    }

    private fun traverse(parsedResult: List<MfmNode>, level: Int, ssb: SpannableStringBuilder) {

        parsedResult.forEach { spr ->

            ssb.append("   ".repeat(level))
            when (spr) {
                is MfmNode.Text -> {
                    ssb.append("Text: \"${spr.value.replace("\n", "\\n")}\"")
                    ssb.append("\n")
                }
                is MfmNode.Bold -> {
                    ssb.append("Bold: \n")
                    traverse(spr.children, level + 1, ssb)
                }
                is MfmNode.Italic -> {
                    ssb.append("Italic: \n")
                    traverse(spr.children, level + 1, ssb)
                }
                is MfmNode.Center -> {
                    ssb.append("Center: \n")
                    traverse(spr.children, level + 1, ssb)
                }
                is MfmNode.Small -> {
                    ssb.append("Small: \n")
                    traverse(spr.children, level + 1, ssb)
                }
                is MfmNode.Function -> {
                    ssb.append("Function: ${spr.name} ${spr.args.map { it.key + "=" + it.value }}")
                    traverse(spr.children, level + 1, ssb)
                }
                is MfmNode.Quote -> {
                    ssb.append("Quote: (${spr.level})\n")
                    traverse(spr.children, level + 1, ssb)
                }
                is MfmNode.Big -> {
                    ssb.append("Big: \n")
                    traverse(spr.children, level + 1, ssb)
                }
                is MfmNode.EmojiCode -> {
                    ssb.append("EmojiCode: ${spr.value}\n")
                }
                is MfmNode.InlineCode -> {
                    ssb.append("InlineCode: \n")
                    traverse(spr.children, level + 1, ssb)
                }
                is MfmNode.Strike -> {
                    ssb.append("Strike: \n")
                    traverse(spr.children, level + 1, ssb)
                }
                is MfmNode.Mention -> {
                    ssb.append("Mention: ${spr.value}\n")
                }
                is MfmNode.Url -> {
                    ssb.append("Url: ${spr.value}\n")
                }
                is MfmNode.UrlWithTitle -> {
                    ssb.append("UrlWithTitle: ${spr.url}\n")
                    traverse(spr.children, level + 1, ssb)
                }
                is MfmNode.SilentLink -> {
                    ssb.append("SilentLink: ${spr.url}\n")
                    traverse(spr.children, level + 1, ssb)
                }
            }
        }
    }
}