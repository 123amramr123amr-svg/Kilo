package com.personalaiagent.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import com.personalaiagent.domain.model.Attachment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a picked file into something a model can actually read.
 *
 * Two paths, chosen by what the file *is*, not by its size:
 *
 * 1. **Text** — a PDF's text layer is extracted on device ([PdfTextExtractor]) and
 *    sent as text. Plain text/markdown/code/CSV/JSON files are read directly.
 *    This is the path that scales: 200 typed pages ≈ 90k characters, no images.
 * 2. **Pixels** — a scanned PDF (no text layer) is rasterised with the platform
 *    [PdfRenderer], and images are downscaled. Capped, because every page is
 *    tokens; the attachment label says how many pages were sent.
 *
 * Nothing is silently dropped: when a cap is hit, the label spells it out.
 */
@Singleton
class AttachmentLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Returns attachments plus an error string when something was skipped. */
    fun load(uri: Uri, maxPages: Int = MAX_PDF_PAGES): Result<List<Attachment>> = runCatching {
        val name = displayName(uri)
        val mime = context.contentResolver.getType(uri).orEmpty()
        when {
            mime == "application/pdf" || name.endsWith(".pdf", ignoreCase = true) ->
                loadPdf(uri, name, maxPages)

            mime.startsWith("image/") || IMAGE_EXTENSIONS.any { name.endsWith(it, ignoreCase = true) } ->
                listOfNotNull(encodeImage(readBytes(uri), name))

            mime.startsWith("text/") || TEXT_EXTENSIONS.any { name.endsWith(it, ignoreCase = true) } ->
                listOf(loadText(uri, name))

            else -> throw IllegalArgumentException("unsupported_type")
        }
    }

    fun displayName(uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()?.let { return it }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "attachment"
    }

    private fun readBytes(uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("unreadable")

    /** Copies the document to a private cache file, streaming, whatever its size. */
    private fun copyToCache(uri: Uri, name: String): File {
        val target = File(context.cacheDir, "uploads").apply { mkdirs() }
            .resolve(name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "document.pdf" })
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output, 128 * 1024) }
        } ?: throw IllegalStateException("unreadable")
        return target
    }

    private fun loadPdf(uri: Uri, name: String, maxPages: Int): List<Attachment> {
        val file = copyToCache(uri, name)
        val extracted = runCatching { PdfTextExtractor.extract(file) }.getOrNull()
        if (extracted != null && extracted.usable) {
            val text = extracted.asDocumentText()
            if (text.isNotBlank()) {
                file.delete()
                return listOf(textAttachment(name, text, extracted.blocks.size, extracted.truncated))
            }
        }
        // No usable text layer: it is a scan (or a broken encoding), so use eyes.
        return try {
            renderPdf(uri, name, maxPages)
        } finally {
            file.delete()
        }
    }

    private fun textAttachment(name: String, text: String, pages: Int, truncated: Boolean): Attachment {
        val label = buildString {
            append(name)
            append(" · ")
            append(if (pages > 0) "$pages صفحة" else "${text.length} حرف")
            if (truncated) append(" · أول جزء من الملف")
        }
        return Attachment(
            name = name,
            mimeType = "text/plain",
            base64 = "",
            sourceLabel = label,
            text = text,
        )
    }

    /** Plain text of any size: read in full (bounded), never cut at the first page. */
    private fun loadText(uri: Uri, name: String): Attachment {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(MAX_TEXT_BYTES)
            var total = 0
            while (total < buffer.size) {
                val read = stream.read(buffer, total, buffer.size - total)
                if (read <= 0) break
                total += read
            }
            String(buffer, 0, total, Charsets.UTF_8)
        } ?: throw IllegalStateException("unreadable")
        val label = "$name · ${text.length} حرف"
        return Attachment(
            name = name,
            mimeType = "text/plain",
            base64 = "",
            sourceLabel = label,
            text = text,
        )
    }

    private fun renderPdf(uri: Uri, name: String, maxPages: Int): List<Attachment> {
        val descriptor: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("unreadable")
        return descriptor.use { fd ->
            PdfRenderer(fd).use { renderer ->
                val pages = minOf(renderer.pageCount, maxPages)
                (0 until pages).mapNotNull { index ->
                    renderer.openPage(index).use { page ->
                        val scale = SCAN_IMAGE_SIDE.toFloat() / maxOf(page.width, page.height)
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val base64 = bitmap.toJpegBase64()
                        bitmap.recycle()
                        Attachment(
                            name = "$name#${index + 1}",
                            mimeType = "image/jpeg",
                            base64 = base64,
                            sourceLabel = if (renderer.pageCount > pages) {
                                "$name · صورة صفحة ${index + 1} من ${renderer.pageCount} (أول $pages)"
                            } else {
                                "$name · ${index + 1}/${renderer.pageCount}"
                            },
                        )
                    }
                }
            }
        }
    }

    private fun encodeImage(bytes: ByteArray, name: String): Attachment? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null

        var sample = 1
        while (longest / sample > MAX_IMAGE_SIDE) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null

        val scaled = if (maxOf(decoded.width, decoded.height) > MAX_IMAGE_SIDE) {
            val ratio = MAX_IMAGE_SIDE.toFloat() / maxOf(decoded.width, decoded.height)
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * ratio).toInt().coerceAtLeast(1),
                (decoded.height * ratio).toInt().coerceAtLeast(1),
                true,
            ).also { decoded.recycle() }
        } else {
            decoded
        }

        val base64 = scaled.toJpegBase64()
        scaled.recycle()
        return Attachment(name = name, mimeType = "image/jpeg", base64 = base64)
    }

    private fun Bitmap.toJpegBase64(): String {
        val out = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        const val MAX_IMAGE_SIDE = 1400

        /** Rasterised pages are capped because each one is paid for in tokens. */
        const val MAX_PDF_PAGES = 10
        const val SCAN_IMAGE_SIDE = 1700

        /** Text files: 2 million characters ≈ 500k tokens of *available* text. */
        const val MAX_TEXT_BYTES = 2_000_000

        private const val JPEG_QUALITY = 82
        private val IMAGE_EXTENSIONS =
            listOf(".png", ".jpg", ".jpeg", ".webp", ".heic", ".heif", ".bmp", ".gif")
        private val TEXT_EXTENSIONS = listOf(
            ".txt", ".md", ".markdown", ".csv", ".tsv", ".json", ".xml", ".html", ".htm",
            ".log", ".yaml", ".yml", ".kt", ".kts", ".java", ".py", ".js", ".ts", ".css",
            ".gradle", ".properties", ".sql", ".sh", ".toml", ".ini",
        )
    }
}
