package com.personalaiagent.domain.usecase.agent

import com.personalaiagent.domain.model.Attachment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the extracted text of the last documents per conversation, in memory only.
 *
 * Without this, "اقرأ الـ PDF ده" works but the follow-up "وإيه اللي في آخر صفحة؟"
 * does not: attachments belong to one message, and a document is not something you
 * re-pick for every question. Only the extracted **text** is kept (never the base64
 * of an image), the last two documents win, and the vault dies with the process —
 * nothing is written to disk.
 */
@Singleton
class DocumentVault @Inject constructor() {

    private val store = linkedMapOf<String, List<Attachment>>()

    fun remember(conversationId: String, attachments: List<Attachment>) {
        val documents = attachments.filter { it.isDocument }
        if (documents.isEmpty()) return
        val existing = store[conversationId].orEmpty()
            .filterNot { old -> documents.any { it.name == old.name } }
        store[conversationId] = (documents + existing).take(MAX_DOCUMENTS)
    }

    /** Documents still in play for this conversation, newest first. */
    fun recall(conversationId: String?): List<Attachment> =
        conversationId?.let { store[it] }.orEmpty()

    fun forget(conversationId: String) {
        store.remove(conversationId)
    }

    private companion object {
        const val MAX_DOCUMENTS = 2
    }
}
