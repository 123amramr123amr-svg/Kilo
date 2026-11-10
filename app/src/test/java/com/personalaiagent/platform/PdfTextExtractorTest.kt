package com.personalaiagent.platform

import java.io.File
import java.util.zip.Deflater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The extractor is the reason a 200-page typed PDF is readable: the tests build
 * real (tiny) PDF-shaped files, one plain and one Flate-compressed, and check the
 * two decisions it makes — "this has text" and "this is a scan".
 */
class PdfTextExtractorTest {

    private val tmp = File.createTempFile("pdf-test", ".pdf").apply { deleteOnExit() }

    private fun pdf(vararg streams: ByteArray): File {
        val out = StringBuilder()
        out.append("%PDF-1.4\n")
        streams.forEachIndexed { index, body ->
            out.append("$index 0 obj\n<< /Length ${body.size} >>\nstream\n")
            out.append(String(body, Charsets.ISO_8859_1))
            out.append("\nendstream\nendobj\n")
        }
        out.append("%%EOF")
        tmp.writeText(out.toString(), Charsets.ISO_8859_1)
        return tmp
    }

    private fun content(vararg lines: String): ByteArray = buildString {
        append("BT\n/F1 12 Tf\n")
        lines.forEach { line ->
            append("72 720 Td\n(").append(line).append(") Tj\n")
        }
        append("ET")
    }.toByteArray(Charsets.ISO_8859_1)

    private fun deflate(bytes: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(bytes)
        deflater.finish()
        val buffer = ByteArray(bytes.size * 2 + 64)
        val size = deflater.deflate(buffer)
        deflater.end()
        return buffer.copyOf(size)
    }

    @Test
    fun `plain content stream is read and marked usable`() {
        val file = pdf(content("Hello AURA", "second line about the workspace and its files"))
        val result = PdfTextExtractor.extract(file)

        assertTrue(result.usable)
        assertTrue(result.blocks.joinToString("").contains("Hello AURA"))
        assertTrue(result.asDocumentText().contains("--- page 1 ---"))
    }

    @Test
    fun `flate compressed stream is inflated before decoding`() {
        val file = pdf(deflate(content("compressed text survives the inflate step intact")))
        val result = PdfTextExtractor.extract(file)

        assertTrue(result.usable)
        assertTrue(result.blocks.first().contains("compressed text"))
    }

    @Test
    fun `scanned pdf without a text layer is reported as not usable`() {
        val drawingOnly = "0 0 612 792 re f\n1 0 0 RG\n10 10 m 100 100 l S".toByteArray(Charsets.ISO_8859_1)
        val result = PdfTextExtractor.extract(pdf(drawingOnly))

        assertFalse(result.usable)
        assertEquals(1, result.streams)
    }

    @Test
    fun `several pages keep their order`() {
        val file = pdf(content("page one text"), content("page two text"))
        val text = PdfTextExtractor.extract(file).asDocumentText()

        assertTrue(text.indexOf("page one") < text.indexOf("page two"))
        assertTrue(text.contains("--- page 2 ---"))
    }

    @Test
    fun `document text respects the character budget`() {
        val long = "x".repeat(500)
        val file = pdf(content(long), content(long))
        val text = PdfTextExtractor.extract(file).asDocumentText(maxChars = 600)

        assertTrue(text.length <= 620)
    }

    @Test
    fun `a missing file is not an exception`() {
        val result = PdfTextExtractor.extract(File("/definitely/not/here.pdf"))

        assertFalse(result.usable)
        assertEquals(0, result.streams)
    }
}
