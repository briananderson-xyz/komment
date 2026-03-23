package com.kontourai.komment

import com.kontourai.komment.data.Annotation
import com.kontourai.komment.export.AnnotationCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AnnotationCompilerTest {

    private val fixedDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    @Test
    fun `empty list returns empty string`() {
        val result = AnnotationCompiler.compileToMarkdown(emptyList(), fixedDateFormat)
        assertEquals("", result)
    }

    @Test
    fun `single text annotation with comment`() {
        val ann = Annotation(
            id = 1,
            sessionId = 1,
            selectedText = "some important text",
            comment = "This is my comment",
            timestamp = 1700000000000L
        )
        val result = AnnotationCompiler.compileToMarkdown(listOf(ann), fixedDateFormat)

        assertTrue(result.contains("## 1."))
        assertTrue(result.contains("> some important text"))
        assertTrue(result.contains("This is my comment"))
        assertTrue(result.contains("---"))
    }

    @Test
    fun `annotation with screenshot shows indicator`() {
        val ann = Annotation(
            id = 1,
            sessionId = 1,
            screenshotPath = "/path/to/screenshot.jpg",
            comment = "See this part",
            timestamp = 1700000000000L
        )
        val result = AnnotationCompiler.compileToMarkdown(listOf(ann), fixedDateFormat)

        assertTrue(result.contains("[Screenshot attached]"))
        assertTrue(result.contains("See this part"))
    }

    @Test
    fun `annotation with source app includes source line`() {
        val ann = Annotation(
            id = 1,
            sessionId = 1,
            selectedText = "text",
            comment = "",
            sourceApp = "com.example.reader",
            timestamp = 1700000000000L
        )
        val result = AnnotationCompiler.compileToMarkdown(listOf(ann), fixedDateFormat)

        assertTrue(result.contains("Source: com.example.reader"))
    }

    @Test
    fun `empty comment is not included in output`() {
        val ann = Annotation(
            id = 1,
            sessionId = 1,
            selectedText = "just text",
            comment = "",
            timestamp = 1700000000000L
        )
        val result = AnnotationCompiler.compileToMarkdown(listOf(ann), fixedDateFormat)
        val lines = result.lines()

        // Should not have a blank line followed by nothing between text and separator
        assertTrue(result.contains("> just text"))
        // Comment line should not appear as empty standalone content
    }

    @Test
    fun `multiple annotations are numbered sequentially`() {
        val annotations = listOf(
            Annotation(id = 1, sessionId = 1, selectedText = "first", comment = "c1", timestamp = 1700000000000L),
            Annotation(id = 2, sessionId = 1, selectedText = "second", comment = "c2", timestamp = 1700000001000L),
            Annotation(id = 3, sessionId = 1, selectedText = "third", comment = "c3", timestamp = 1700000002000L)
        )
        val result = AnnotationCompiler.compileToMarkdown(annotations, fixedDateFormat)

        assertTrue(result.contains("## 1."))
        assertTrue(result.contains("## 2."))
        assertTrue(result.contains("## 3."))
        assertTrue(result.contains("> first"))
        assertTrue(result.contains("> second"))
        assertTrue(result.contains("> third"))
    }

    @Test
    fun `annotation with only screenshot and no text`() {
        val ann = Annotation(
            id = 1,
            sessionId = 1,
            screenshotPath = "/path/screenshot.jpg",
            comment = "look at this",
            timestamp = 1700000000000L
        )
        val result = AnnotationCompiler.compileToMarkdown(listOf(ann), fixedDateFormat)

        assertTrue(result.contains("[Screenshot attached]"))
        assertTrue(result.contains("look at this"))
        assertTrue(!result.contains("> "))
    }

    @Test
    fun `annotation with all fields populated`() {
        val ann = Annotation(
            id = 1,
            sessionId = 1,
            screenshotPath = "/path/screenshot.jpg",
            selectedText = "selected portion",
            comment = "my note",
            sourceApp = "com.reader.app",
            timestamp = 1700000000000L
        )
        val result = AnnotationCompiler.compileToMarkdown(listOf(ann), fixedDateFormat)

        assertTrue(result.contains("## 1."))
        assertTrue(result.contains("Source: com.reader.app"))
        assertTrue(result.contains("> selected portion"))
        assertTrue(result.contains("my note"))
        assertTrue(result.contains("[Screenshot attached]"))
        assertTrue(result.contains("---"))
    }

    @Test
    fun `timestamp is formatted correctly`() {
        // 2023-11-14 22:13:20 UTC
        val ann = Annotation(
            id = 1,
            sessionId = 1,
            comment = "test",
            timestamp = 1700000000000L
        )
        val result = AnnotationCompiler.compileToMarkdown(listOf(ann), fixedDateFormat)

        assertTrue(result.contains("*2023-11-14 22:13:20*"))
    }
}
