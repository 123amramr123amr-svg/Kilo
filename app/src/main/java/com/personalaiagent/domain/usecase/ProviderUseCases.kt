package com.personalaiagent.domain.usecase

import com.personalaiagent.core.AppError
import com.personalaiagent.core.Outcome
import com.personalaiagent.core.fold
import com.personalaiagent.domain.repository.CatalogSyncResult
import com.personalaiagent.domain.repository.ChatRepository
import com.personalaiagent.domain.repository.CredentialStore
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.repository.SettingsRepository
import com.personalaiagent.domain.repository.TaskRepository
import com.personalaiagent.domain.model.ProviderDraft
import com.personalaiagent.domain.model.RemoteModel
import javax.inject.Inject

data class AddProviderResult(
    val providerId: String,
    val catalog: CatalogSyncResult?,
    val catalogError: AppError?,
)

/** Creates the provider, stores the key and pulls its model catalog. */
class AddProviderUseCase @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
) {
    /**
     * @param knownModels a catalog the UI already fetched with these exact
     *   credentials. When present it is stored instead of asking the provider again,
     *   so a successful "fetch models" cannot be undone by a failing second call.
     */
    suspend operator fun invoke(
        draft: ProviderDraft,
        apiKey: String,
        knownModels: List<RemoteModel> = emptyList(),
    ): Outcome<AddProviderResult> {
        ProviderValidator.validate(draft)?.let { return Outcome.Failure(it) }
        if (apiKey.isBlank()) return Outcome.Failure(AppError.InvalidInput("key"))

        return when (val created = providerRepository.addProvider(draft, apiKey)) {
            is Outcome.Failure -> created
            is Outcome.Success -> {
                val sync = if (knownModels.isNotEmpty()) {
                    modelRepository.storeCatalog(
                        created.value,
                        knownModels,
                        knownModels.map { it.remoteId }.toSet(),
                    ).fold(
                        onSuccess = { Outcome.Success(com.personalaiagent.domain.repository.CatalogSyncResult(it, fromRemote = true)) },
                        onFailure = { Outcome.Failure(it) },
                    )
                } else {
                    modelRepository.refreshCatalog(created.value)
                }
                if (modelRepository.getDefaultModel() == null) {
                    modelRepository.firstEnabledModel()?.let { modelRepository.setDefault(it.id) }
                }
                Outcome.Success(
                    AddProviderResult(
                        providerId = created.value,
                        catalog = sync.getOrNull(),
                        catalogError = (sync as? Outcome.Failure)?.error,
                    ),
                )
            }
        }
    }
}

/** "Sign out" for a local-only account: removes every trace of user data. */
class ClearLocalDataUseCase @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
    private val chatRepository: ChatRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val credentialStore: CredentialStore,
) {
    suspend operator fun invoke() {
        chatRepository.clearAll()
        taskRepository.clearAll()
        modelRepository.clearAll()
        providerRepository.clearAll()
        credentialStore.clear()
        settingsRepository.clearAll()
    }
}
