package com.personalaiagent.data.repository

import androidx.room.withTransaction
import com.personalaiagent.core.AppError
import com.personalaiagent.core.IdGenerator
import com.personalaiagent.core.Outcome
import com.personalaiagent.core.TimeProvider
import com.personalaiagent.data.local.AppDatabase
import com.personalaiagent.data.local.ProviderDao
import com.personalaiagent.data.local.ProviderEntity
import com.personalaiagent.data.local.toDomain
import com.personalaiagent.data.remote.ProviderClientRouter
import com.personalaiagent.data.secure.SecureStorageException
import com.personalaiagent.domain.model.ConnectionTestResult
import com.personalaiagent.domain.model.Provider
import com.personalaiagent.domain.model.ProviderDraft
import com.personalaiagent.domain.model.ProviderStatus
import com.personalaiagent.domain.repository.CredentialStore
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.usecase.ProviderValidator
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProviderRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val providerDao: ProviderDao,
    private val credentialStore: CredentialStore,
    private val clients: ProviderClientRouter,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : ProviderRepository {

    override fun observeProviders(): Flow<List<Provider>> =
        providerDao.observeAll().map { entities ->
            entities.map { entity -> entity.toDomain(hasKey = credentialStore.exists(entity.id)) }
        }

    override fun observeProvider(id: String): Flow<Provider?> =
        providerDao.observeById(id).map { entity ->
            entity?.toDomain(hasKey = credentialStore.exists(entity.id))
        }

    override suspend fun getProvider(id: String): Provider? =
        providerDao.getById(id)?.toDomain(hasKey = credentialStore.exists(id))

    override suspend fun getDefaultProvider(): Provider? {
        val entity = providerDao.getDefault() ?: providerDao.getFirst() ?: return null
        return entity.toDomain(hasKey = credentialStore.exists(entity.id))
    }

    override suspend fun addProvider(draft: ProviderDraft, apiKey: String): Outcome<String> {
        ProviderValidator.validate(draft)?.let { return Outcome.Failure(it) }
        if (apiKey.isBlank()) return Outcome.Failure(AppError.InvalidInput("key"))

        val id = ids.newId()
        try {
            credentialStore.save(id, apiKey)
        } catch (_: SecureStorageException) {
            return Outcome.Failure(AppError.SecureStorage)
        }

        val isFirst = providerDao.count() == 0
        providerDao.insert(
            ProviderEntity(
                id = id,
                name = draft.name.trim(),
                kind = draft.kind.name,
                baseUrl = ProviderValidator.normalizeBaseUrl(draft.baseUrl),
                isDefault = isFirst,
                status = ProviderStatus.UNKNOWN.name,
                createdAt = time.now(),
            ),
        )
        return Outcome.Success(id)
    }

    override suspend fun updateProvider(id: String, draft: ProviderDraft, apiKey: String?): Outcome<Unit> {
        val existing = providerDao.getById(id)
            ?: return Outcome.Failure(AppError.Unknown("provider not found"))
        ProviderValidator.validate(draft)?.let { return Outcome.Failure(it) }

        if (!apiKey.isNullOrBlank()) {
            try {
                credentialStore.save(id, apiKey)
            } catch (_: SecureStorageException) {
                return Outcome.Failure(AppError.SecureStorage)
            }
        }

        val baseUrl = ProviderValidator.normalizeBaseUrl(draft.baseUrl)
        val endpointChanged = baseUrl != existing.baseUrl || draft.kind.name != existing.kind
        database.withTransaction {
            providerDao.update(
                existing.copy(
                    name = draft.name.trim(),
                    kind = draft.kind.name,
                    baseUrl = baseUrl,
                    status = if (endpointChanged) ProviderStatus.UNKNOWN.name else existing.status,
                ),
            )
            // A different endpoint may expose a different catalog: drop cached models.
            if (endpointChanged) database.modelDao().deleteByProvider(id)
        }
        return Outcome.Success(Unit)
    }

    override suspend fun deleteProvider(id: String): Outcome<Unit> {
        val existing = providerDao.getById(id) ?: return Outcome.Success(Unit)
        // The key is removed first: even if the row deletion fails no orphan secret remains.
        runCatching { credentialStore.delete(id) }
        database.withTransaction {
            providerDao.delete(id)
            if (existing.isDefault || providerDao.getDefault() == null) {
                providerDao.getFirst()?.let { next ->
                    providerDao.clearDefaultFlags()
                    providerDao.markDefault(next.id)
                }
            }
        }
        return Outcome.Success(Unit)
    }

    override suspend fun setDefaultProvider(id: String): Outcome<Unit> {
        val existing = providerDao.getById(id) ?: return Outcome.Failure(AppError.Unknown("provider not found"))
        database.withTransaction {
            providerDao.clearDefaultFlags()
            providerDao.markDefault(existing.id)
        }
        return Outcome.Success(Unit)
    }

    override suspend fun testConnection(
        providerId: String?,
        draft: ProviderDraft,
        apiKey: String?,
    ): Outcome<ConnectionTestResult> {
        ProviderValidator.validate(draft)?.let { return Outcome.Failure(it) }

        val stored = providerId?.let { providerDao.getById(it) }
        val kind = draft.kind
        val baseUrl = ProviderValidator.normalizeBaseUrl(draft.baseUrl)
        val key = apiKey?.takeIf { it.isNotBlank() }
            ?: providerId?.let { credentialStore.read(it) }
            ?: return Outcome.Failure(AppError.MissingKey(draft.name.ifBlank { "provider" }))

        val provider = Provider(
            id = providerId ?: "draft",
            name = draft.name.ifBlank { "draft" },
            kind = kind,
            baseUrl = baseUrl,
            isDefault = false,
            hasKey = true,
            status = ProviderStatus.UNKNOWN,
            lastCheckedAt = null,
            createdAt = stored?.createdAt ?: time.now(),
        )

        val result = clients.forKind(kind).fetchModels(provider, key, baseUrl)
        providerId?.let { id ->
            providerDao.updateStatus(
                id = id,
                status = if (result is Outcome.Success) ProviderStatus.CONNECTED.name else ProviderStatus.ERROR.name,
                checkedAt = time.now(),
            )
        }
        return when (result) {
            is Outcome.Success -> Outcome.Success(
                ConnectionTestResult(
                    modelsFound = result.value.size,
                    providerName = draft.name.ifBlank { provider.name },
                ),
            )
            is Outcome.Failure -> Outcome.Failure(result.error)
        }
    }

    override suspend fun clearAll() {
        database.withTransaction {
            database.modelDao().clear()
            providerDao.clear()
        }
        runCatching { credentialStore.clear() }
    }
}
