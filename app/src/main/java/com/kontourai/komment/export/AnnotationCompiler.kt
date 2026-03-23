package com.kontourai.komment.export

import com.kontourai.komment.data.Annotation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AnnotationCompiler {

    fun compileToMarkdown(
        annotations: List<Annotation>,
        dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    ): String {
        if (annotations.isEmpty()) return ""

        val sb = StringBuilder()
        annotations.forEachIndexed { index, ann ->
            sb.appendLine("## ${index + 1}.")
            sb.appendLine("*${dateFormat.format(Date(ann.timestamp))}*")
            ann.sourceApp?.let { sb.appendLine("Source: $it") }
            sb.appendLine()

            ann.selectedText?.let {
                sb.appendLine("> $it")
                sb.appendLine()
            }

            if (ann.comment.isNotBlank()) {
                sb.appendLine(ann.comment)
                sb.appendLine()
            }

            ann.screenshotPath?.let {
                sb.appendLine("[Screenshot attached]")
                sb.appendLine()
            }

            sb.appendLine("---")
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }
}
