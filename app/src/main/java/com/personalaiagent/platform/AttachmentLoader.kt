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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a picked document into attachments a vision model can read.
 *
 * Images are downscaled to [MAX_IMAGE_SIDE] px and re-encoded as JPEG. PDFs are
 * rasterised with the platform [PdfRenderer] (first [MAX_PDF_PAGES] pages), so no
 * PDF library or server-side parsing is needed.
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
                renderPdf(uri, name, maxPages)

            mime.startsWith("image/") || IMAGE_EXTENSIONS.any { name.endsWith(it, ignoreCase = true) } ->
                listOfNotNull(encodeImage(readBytes(uri), name))

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

    private fun renderPdf(uri: Uri, name: String, maxPages: Int): List<Attachment> {
        val descriptor: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("unreadable")
        return descriptor.use { fd ->
            PdfRenderer(fd).use { renderer ->
                (0 until minOf(renderer.pageCount, maxPages)).mapNotNull { index ->
                    renderer.openPage(index).use { page ->
                        val scale = MAX_IMAGE_SIDE.toFloat() / maxOf(page.width, page.height)
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
                            sourceLabel = "$name · ${index + 1}/${minOf(renderer.pageCount, maxPages)}",
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
        const val MAX_PDF_PAGES = 5
        private const val JPEG_QUALITY = 82
        private val IMAGE_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".webp", ".heic", ".heif")
    }
}
