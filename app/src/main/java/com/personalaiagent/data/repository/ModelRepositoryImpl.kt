package com.personalaiagent.data.repository

import androidx.room.withTransaction
import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.core.TimeProvider
import com.personalaiagent.data.local.AppDatabase
import com.personalaiagent.data.local.ModelDao
import com.personalaiagent.data.local.ModelEntity
import com.personalaiagent.data.local.ProviderDao
import com.personalaiagent.data.local.toDomain
import com.personalaiagent.data.remote.ModelCatalog
import com.personalaiagent.data.remote.ProviderClientRouter
import com.personalaiagent.domain.model.AiModel
import com.personalaiagent.domain.model.ModelParams
import com.personalaiagent.domain.model.ProviderStatus
import com.personalaiagent.domain.repository.CatalogSyncResult
import com.personalaiagent.domain.repository.CredentialStore
import com.personalaiagent.domain.repository.ModelRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ModelRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val modelDao: ModelDao,
    private val providerDao: ProviderDao,
    private val credentialStore: CredentialStore,
    private val clients: ProviderClientRouter,
    private val time: TimeProvider,
) : ModelRepository {

    override fun observeModels(): Flow<List<AiModel>> =
        combine(modelDao.observeAll(), providerDao.observeAll()) { models, providers ->
            val names = providers.associate { it.id to it.name }
            models.map { entity -> entity.toDomain(names[entity.providerId] ?: "") }
        }

    override fun observeEnabledModels(): Flow<List<AiModel>> =
        combine(modelDao.observeEnabled(), providerDao.observeAll()) { models, providers ->
            val names = providers.associate { it.id to it.name }
            models.map { entity -> entity.toDomain(names[entity.providerId] ?: "") }
        }

    override fun observeModel(id: String): Flow<AiModel?> =
        combine(modelDao.observeById(id), providerDao.observeAll()) { entity, providers ->
            entity?.toDomain(providers.firstOrNull { it.id == entity.providerId }?.name ?: "")
        }

    override suspend fun getModel(id: String): AiModel? {
        val entity = modelDao.getById(id) ?: return null
        return entity.toDomain(providerDao.getById(entity.providerId)?.name ?: "")
    }

    override suspend fun getDefaultModel(): AiModel? =
        modelDao.getDefault()?.let { toDomain(it) }

    override suspend fun firstEnabledModel(): AiModel? =
        modelDao.firstEnabled()?.let { toDomain(it) }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        database.withTransaction {
            val model = modelDao.getById(id) ?: return@withTransaction
            modelDao.setEnabled(id, enabled)
            if (!enabled && model.isDefault) {
                modelDao.clearDefaultFlags()
                modelDao.firstEnabled()?.let { modelDao.markDefault(it.id) }
            }
        }
    }

    override suspend fun setDefault(id: String) {
        database.withTransaction {
            if (modelDao.getById(id) == null) return@withTransaction
            modelDao.clearDefaultFlags()
            modelDao.markDefault(id)
            modelDao.setEnabled(id, true)
        }
    }

    override suspend fun updateParams(id: String, params: ModelParams) {
        modelDao.updateParams(
            id = id,
            temperature = params.temperature.coerceIn(ModelParams.MIN_TEMPERATURE, ModelParams.MAX_TEMPERATURE),
            maxTokens = params.maxTokens.coerceIn(ModelParams.MIN_MAX_TOKENS, ModelParams.MAX_MAX_TOKENS),
            topP = params.topP.coerceIn(0f, 1f),
            systemPrompt = params.systemPrompt.take(ModelParams.MAX_SYSTEM_PROMPT_LENGTH),
            streaming = params.streaming,
        )
    }

    override suspend fun refreshCatalog(providerId: String): Outcome<CatalogSyncResult> {
        val providerEntity = providerDao.getById(providerId)
            ?: return Outcome.Failure(AppError.Unknown("provider not found"))
        val providerName = providerEntity.name
        val key = credentialStore.read(providerId)
            ?: return Outcome.Failure(AppError.MissingKey(providerName))
        val provider = providerEntity.toDomain(hasKey = true)

        val result = clients.forKind(provider.kind).fetchModels(provider, key)
        return when (result) {
            is Outcome.Success -> {
                if (result.value.isEmpty()) {
                    Outcome.Failure(AppError.NoModel)
                } else {
                    val count = store(
                        providerId,
                        result.value.map { Triple(it.remoteId, it.displayName, it.contextWindow) },
                    )
                    providerDao.updateStatus(providerId, ProviderStatus.CONNECTED.name, time.now())
                    Outcome.Success(CatalogSyncResult(count, fromRemote = true))
                }
            }

            is Outcome.Failure -> {
                if (modelDao.countByProvider(providerId) == 0) {
                    // Offline fallback so the user can still pick a model and retry later.
                    val bundled = ModelCatalog.bundled(provider.kind)
                    val count = store(
                        providerId,
                        bundled.map { Triple(it.remoteId, it.displayName, it.contextWindow) },
                    )
                    Outcome.Success(CatalogSyncResult(count, fromRemote = false))
                } else {
                    providerDao.updateStatus(providerId, ProviderStatus.ERROR.name, time.now())
                    Outcome.Failure(result.error)
                }
            }
        }
    }

    private suspend fun store(
        providerId: String,
        models: List<Triple<String, String, Int?>>,
    ): Int {
        val existing = modelDao.getByProvider(providerId).associateBy { it.remoteId }
        val entities = models.mapIndexed { index, (remoteId, displayName, contextWindow) ->
            val old = existing[remoteId]
            ModelEntity(
                id = modelId(providerId, remoteId),
                providerId = providerId,
                remoteId = remoteId,
                displayName = displayName,
                capabilities = old?.capabilities
                    ?: ModelCatalog.inferCapabilities(remoteId).joinToString(",") { it.name },
                contextWindow = contextWindow ?: old?.contextWindow ?: ModelCatalog.defaultContextWindow(remoteId),
                enabled = old?.enabled ?: true,
                isDefault = old?.isDefault ?: false,
                temperature = old?.temperature ?: 0.7f,
                maxTokens = old?.maxTokens ?: ModelParams.DEFAULT_MAX_TOKENS,
                topP = old?.topP ?: 1f,
                systemPrompt = old?.systemPrompt ?: "",
                streaming = old?.streaming ?: true,
                sortOrder = index,
            )
        }
        database.withTransaction {
            modelDao.deleteByProvider(providerId)
            modelDao.upsertAll(entities)
            if (modelDao.getDefault() == null) {
                modelDao.firstEnabled()?.let { modelDao.markDefault(it.id) }
            }
        }
        return entities.size
    }

    override suspend fun applySelection(providerId: String, enabledRemoteIds: Set<String>) {
        database.withTransaction {
            val models = modelDao.getByProvider(providerId)
            models.forEach { entity ->
                val shouldEnable = entity.remoteId in enabledRemoteIds
                if (entity.enabled != shouldEnable) modelDao.setEnabled(entity.id, shouldEnable)
            }
            // A default model that the user just unticked must not stay in use, and
            // the app always needs at least one usable model.
            val current = modelDao.getDefault()
            if (current != null && !current.enabled) modelDao.clearDefaultFlags()
            if (modelDao.getDefault() == null) {
                val fallback = modelDao.firstEnabled()
                if (fallback != null) {
                    modelDao.markDefault(fallback.id)
                } else {
                    models.firstOrNull()?.let {
                        modelDao.setEnabled(it.id, true)
                        modelDao.markDefault(it.id)
                    }
                }
            }
        }
    }

    override suspend fun clearAll() {
        modelDao.clear()
    }

    private suspend fun toDomain(entity: ModelEntity): AiModel =
        entity.toDomain(providerDao.getById(entity.providerId)?.name ?: "")

    companion object {
        fun modelId(providerId: String, remoteId: String): String = "$providerId::$remoteId"
    }
}
