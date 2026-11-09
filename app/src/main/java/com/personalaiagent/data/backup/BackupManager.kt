package com.personalaiagent.data.backup

import androidx.room.withTransaction
import com.personalaiagent.core.AppConstants
import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.core.TimeProvider
import com.personalaiagent.data.local.AppDatabase
import com.personalaiagent.data.local.ConversationEntity
import com.personalaiagent.data.local.MessageEntity
import com.personalaiagent.data.local.ModelEntity
import com.personalaiagent.data.local.ProviderEntity
import com.personalaiagent.data.local.TaskRunEntity
import com.personalaiagent.data.local.TaskStepEntity
import com.personalaiagent.domain.model.AppLanguage
import com.personalaiagent.domain.model.ThemeMode
import com.personalaiagent.domain.repository.SettingsRepository
import kotlinx.serialization.json.Json

/**
 * Manual, user-initiated backup. Because API keys are Keystore-bound and must
 * never leave the device, they are excluded by construction: after an import the
 * user re-enters them.
 */
class BackupManager(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val time: TimeProvider,
    private val json: Json,
) {

    private val backupJson = Json(json) {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun exportJson(includeTaskLogs: Boolean): String {
        val settings = settingsRepository.current()
        val providers = database.providerDao().getAll()
        val models = database.modelDao().getAll()
        val conversations = database.conversationDao().getAll()
        val messages = database.messageDao().getAll()
        val runs = if (includeTaskLogs) database.taskDao().getAllRuns() else emptyList()
        val steps = if (includeTaskLogs) database.taskDao().getAllSteps() else emptyList()

        val bundle = BackupBundle(
            version = AppConstants.BACKUP_SCHEMA_VERSION,
            exportedAt = time.now(),
            appVersion = com.personalaiagent.BuildConfig.VERSION_NAME,
            keysIncluded = false,
            settings = BackupSettings(
                language = settings.language.tag,
                theme = settings.theme.name,
                ttsEnabled = settings.ttsEnabled,
                hapticsEnabled = settings.hapticsEnabled,
                streamingEnabled = settings.streamingEnabled,
                onboardingCompleted = settings.onboardingCompleted,
                displayName = settings.displayName,
            ),
            providers = providers.map {
                BackupProvider(
                    id = it.id,
                    name = it.name,
                    kind = it.kind,
                    baseUrl = it.baseUrl,
                    isDefault = it.isDefault,
                    status = it.status,
                    lastCheckedAt = it.lastCheckedAt,
                    createdAt = it.createdAt,
                )
            },
            models = models.map {
                BackupModel(
                    id = it.id,
                    providerId = it.providerId,
                    remoteId = it.remoteId,
                    displayName = it.displayName,
                    capabilities = it.capabilities,
                    contextWindow = it.contextWindow,
                    enabled = it.enabled,
                    isDefault = it.isDefault,
                    temperature = it.temperature,
                    maxTokens = it.maxTokens,
                    topP = it.topP,
                    systemPrompt = it.systemPrompt,
                    streaming = it.streaming,
                    sortOrder = it.sortOrder,
                )
            },
            conversations = conversations.map {
                BackupConversation(
                    id = it.id,
                    title = it.title,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    providerId = it.providerId,
                    modelId = it.modelId,
                )
            },
            messages = messages.map {
                BackupMessage(
                    id = it.id,
                    conversationId = it.conversationId,
                    role = it.role,
                    content = it.content,
                    createdAt = it.createdAt,
                    status = it.status,
                    providerId = it.providerId,
                    modelId = it.modelId,
                    tokenIn = it.tokenIn,
                    tokenOut = it.tokenOut,
                )
            },
            tasks = runs.map {
                BackupTask(
                    id = it.id,
                    conversationId = it.conversationId,
                    prompt = it.prompt,
                    status = it.status,
                    startedAt = it.startedAt,
                    finishedAt = it.finishedAt,
                    error = it.error,
                    providerId = it.providerId,
                    modelId = it.modelId,
                    tokensIn = it.tokensIn,
                    tokensOut = it.tokensOut,
                )
            },
            taskSteps = steps.map {
                BackupTaskStep(
                    id = it.id,
                    taskId = it.taskId,
                    name = it.name,
                    status = it.status,
                    startedAt = it.startedAt,
                    finishedAt = it.finishedAt,
                    detail = it.detail,
                )
            },
        )
        return backupJson.encodeToString(BackupBundle.serializer(), bundle)
    }

    suspend fun importJson(raw: String): Outcome<Int> {
        val bundle = try {
            backupJson.decodeFromString(BackupBundle.serializer(), raw)
        } catch (e: Exception) {
            return Outcome.Failure(AppError.InvalidInput("malformed_backup"))
        }

        if (bundle.version > AppConstants.BACKUP_SCHEMA_VERSION) {
            return Outcome.Failure(AppError.InvalidInput("unsupported_version"))
        }

        database.withTransaction {
            // Replace user data. Credential entries are stored separately and kept.
            database.messageDao().clear()
            database.conversationDao().clear()
            database.taskDao().clearSteps()
            database.taskDao().clearRuns()
            database.modelDao().clear()
            database.providerDao().clear()

            database.providerDao().insertAll(
                bundle.providers.map {
                    ProviderEntity(
                        id = it.id,
                        name = it.name,
                        kind = it.kind,
                        baseUrl = it.baseUrl,
                        isDefault = it.isDefault,
                        status = it.status,
                        lastCheckedAt = it.lastCheckedAt,
                        createdAt = it.createdAt,
                    )
                },
            )
            database.modelDao().upsertAll(
                bundle.models.map {
                    ModelEntity(
                        id = it.id,
                        providerId = it.providerId,
                        remoteId = it.remoteId,
                        displayName = it.displayName,
                        capabilities = it.capabilities,
                        contextWindow = it.contextWindow,
                        enabled = it.enabled,
                        isDefault = it.isDefault,
                        temperature = it.temperature,
                        maxTokens = it.maxTokens,
                        topP = it.topP,
                        systemPrompt = it.systemPrompt,
                        streaming = it.streaming,
                        sortOrder = it.sortOrder,
                    )
                },
            )
            database.conversationDao().insertAll(
                bundle.conversations.map {
                    ConversationEntity(
                        id = it.id,
                        title = it.title,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        providerId = it.providerId,
                        modelId = it.modelId,
                    )
                },
            )
            database.messageDao().insertAll(
                bundle.messages.map {
                    MessageEntity(
                        id = it.id,
                        conversationId = it.conversationId,
                        role = it.role,
                        content = it.content,
                        createdAt = it.createdAt,
                        status = it.status,
                        providerId = it.providerId,
                        modelId = it.modelId,
                        tokenIn = it.tokenIn,
                        tokenOut = it.tokenOut,
                    )
                },
            )
            database.taskDao().insertAllRuns(
                bundle.tasks.map {
                    TaskRunEntity(
                        id = it.id,
                        conversationId = it.conversationId,
                        prompt = it.prompt,
                        status = it.status,
                        startedAt = it.startedAt,
                        finishedAt = it.finishedAt,
                        error = it.error,
                        providerId = it.providerId,
                        modelId = it.modelId,
                        tokensIn = it.tokensIn,
                        tokensOut = it.tokensOut,
                    )
                },
            )
            database.taskDao().insertAllSteps(
                bundle.taskSteps.map {
                    TaskStepEntity(
                        id = it.id,
                        taskId = it.taskId,
                        name = it.name,
                        status = it.status,
                        startedAt = it.startedAt,
                        finishedAt = it.finishedAt,
                        detail = it.detail,
                    )
                },
            )
        }

        bundle.settings?.let { settings ->
            settingsRepository.setLanguage(AppLanguage.fromTag(settings.language))
            settingsRepository.setTheme(
                runCatching { ThemeMode.valueOf(settings.theme) }.getOrDefault(ThemeMode.LIGHT),
            )
            settingsRepository.setTtsEnabled(settings.ttsEnabled)
            settingsRepository.setHapticsEnabled(settings.hapticsEnabled)
            settingsRepository.setStreamingEnabled(settings.streamingEnabled)
            settingsRepository.setOnboardingCompleted(settings.onboardingCompleted)
            settingsRepository.setDisplayName(settings.displayName)
        }

        return Outcome.Success(bundle.conversations.size)
    }
}
