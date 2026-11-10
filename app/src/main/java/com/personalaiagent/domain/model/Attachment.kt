package com.personalaiagent.domain.model

/**
 * A file the user attached to a message.
 *
 * Images are sent as base64 data (vision models). PDF pages are rasterised on
 * device and attached as PNG images, so any vision-capable model can review
 * them without a server-side PDF parser.
 */
data class Attachment(
    val name: String,
    val mimeType: String,
    val base64: String,
    val isImage: Boolean = mimeType.startsWith("image/"),
    val sourceLabel: String? = null,
    /**
     * Extracted document text (PDF text layer, .txt/.md/.csv/.json/…).
     *
     * Documents travel as text, not pixels: a 200-page typed PDF then costs a
     * fraction of what 200 images would, and even a non-vision model can read it.
     */
    val text: String? = null,
) {
    val isDocument: Boolean get() = text != null
}

/** An image produced by an image-capable model, saved into the workspace. */
data class GeneratedImage(
    val bytes: ByteArray,
    val mimeType: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is GeneratedImage && bytes.contentEquals(other.bytes) && mimeType == other.mimeType)

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + mimeType.hashCode()
}
