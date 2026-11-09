package com.personalaiagent.ui.screens.providers

import androidx.lifecycle.SavedStateHandle
import com.personalaiagent.core.AppError
import com.personalaiagent.domain.model.RemoteModel
import com.personalaiagent.domain.usecase.AddProviderUseCase
import com.personalaiagent.domain.usecase.FakeModelRepository
import com.personalaiagent.domain.usecase.FakeProviderRepository
import com.personalaiagent.domain.usecase.SampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProviderEditViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val models = listOf(
        RemoteModel("deepseek-ai/DeepSeek-V4-Flash-0731", "DeepSeek V4 Flash", 64_000),
        RemoteModel("MiniMaxAI/MiniMax-M2.7", "MiniMax M2.7", 200_000),
        RemoteModel("zai-org/GLM-5.3-Flash", "GLM 5.3 Flash", 128_000),
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        providerRepository: FakeProviderRepository,
        modelRepository: FakeModelRepository = FakeModelRepository(SampleData.model()),
        providerId: String? = "p1",
    ) = ProviderEditViewModel(
        savedStateHandle = if (providerId == null) {
            SavedStateHandle()
        } else {
            SavedStateHandle(mapOf("providerId" to providerId))
        },
        providerRepository = providerRepository,
        modelRepository = modelRepository,
        addProvider = AddProviderUseCase(providerRepository, modelRepository),
    )

    @Test
    fun `applying fetched models saves the endpoint and stores that catalog`() = runTest(dispatcher) {
        val providerRepository = FakeProviderRepository(SampleData.provider(), availableModels = models)
        val modelRepository = FakeModelRepository(SampleData.model())
        val viewModel = viewModel(providerRepository, modelRepository)
        advanceUntilIdle()

        // The user edits the URL and key, then fetches.
        viewModel.onBaseUrlChange("https://inference.dahl.global/v1")
        viewModel.onApiKeyChange("sk-test")
        viewModel.fetchModels()
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.fetchedModels.size)

        // Untick two models, then apply.
        viewModel.toggleModelSelection(models[1].remoteId)
        viewModel.toggleModelSelection(models[2].remoteId)
        viewModel.applyModelSelection()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.fetchError)
        assertNull(state.formError)
        assertEquals(1, state.appliedModels)
        assertEquals(1, state.selectedModelIds.size)

        // The endpoint on screen is persisted, so chat calls the same server.
        assertEquals("https://inference.dahl.global/v1", providerRepository.updates.single().second.baseUrl)
        assertEquals("sk-test", providerRepository.updates.single().third)

        // The catalog that was already fetched is stored: no second network request
        // that could fail and lose the models (the bug behind "Something went wrong").
        assertEquals(models, modelRepository.storedCatalogs.single().second)
        assertEquals(0, modelRepository.refreshCalls)
    }

    @Test
    fun `a rejected provider update is shown instead of a generic error`() = runTest(dispatcher) {
        val providerRepository = FakeProviderRepository(
            SampleData.provider(),
            availableModels = models,
            failUpdates = true,
        )
        val modelRepository = FakeModelRepository(SampleData.model())
        val viewModel = viewModel(providerRepository, modelRepository)
        advanceUntilIdle()

        viewModel.onApiKeyChange("sk-bad")
        viewModel.fetchModels()
        advanceUntilIdle()
        viewModel.applyModelSelection()
        advanceUntilIdle()

        assertEquals(AppError.Auth, viewModel.uiState.value.formError)
        assertEquals(0, modelRepository.storedCatalogs.size)
    }

    @Test
    fun `fetching models reports the provider error and keeps the form`() = runTest(dispatcher) {
        val providerRepository = FakeProviderRepository(
            SampleData.provider(),
            failFetch = AppError.HttpNotFound,
        )
        val viewModel = viewModel(providerRepository)
        advanceUntilIdle()

        viewModel.onApiKeyChange("sk-test")
        viewModel.fetchModels()
        advanceUntilIdle()

        assertEquals(AppError.HttpNotFound, viewModel.uiState.value.fetchError)
        // The typed key stays in the form so the user can retry without re-entering it.
        assertEquals("sk-test", viewModel.uiState.value.apiKey)
        assertEquals(false, viewModel.uiState.value.isFetchingModels)
    }

    @Test
    fun `a new provider keeps the ticks until it is saved`() = runTest(dispatcher) {
        val providerRepository = FakeProviderRepository(null, availableModels = models)
        val modelRepository = FakeModelRepository(SampleData.model())
        val viewModel = viewModel(providerRepository, modelRepository, providerId = null)
        advanceUntilIdle()

        viewModel.onNameChange("Gateway")
        viewModel.onBaseUrlChange("https://inference.dahl.global/v1")
        viewModel.onApiKeyChange("sk-test")
        viewModel.fetchModels()
        advanceUntilIdle()
        viewModel.applyModelSelection()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.selectedModelIds.size)
        assertEquals(0, modelRepository.storedCatalogs.size)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(models, modelRepository.storedCatalogs.single().second)
        assertEquals(0, modelRepository.refreshCalls)
    }

    @Test
    fun `saving after editing only the URL re-reads the catalog`() = runTest(dispatcher) {
        // A different base URL invalidates the cached models, so a save without a
        // freshly fetched list has to ask the new endpoint.
        val providerRepository = FakeProviderRepository(SampleData.provider())
        val modelRepository = FakeModelRepository(SampleData.model())
        val viewModel = viewModel(providerRepository, modelRepository)
        advanceUntilIdle()

        viewModel.onBaseUrlChange("https://inference.dahl.global/v1")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, modelRepository.refreshCalls)
        assertEquals(0, modelRepository.storedCatalogs.size)
        assertEquals("https://inference.dahl.global/v1", providerRepository.updates.single().second.baseUrl)
        assertNull(viewModel.uiState.value.fetchError)
    }

    @Test
    fun `a failed re-read keeps the user on the screen with the reason`() = runTest(dispatcher) {
        val providerRepository = FakeProviderRepository(SampleData.provider())
        val modelRepository = FakeModelRepository(SampleData.model(), failRefresh = AppError.HttpNotFound)
        val viewModel = viewModel(providerRepository, modelRepository)
        advanceUntilIdle()

        viewModel.onBaseUrlChange("https://wrong.example/v1")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(AppError.HttpNotFound, viewModel.uiState.value.fetchError)
        assertEquals(false, viewModel.uiState.value.isSaving)
    }

    @Test
    fun `an unknown provider id degrades to adding a provider instead of failing every save`() =
        runTest(dispatcher) {
            // Regression: the chat screen used to navigate with the raw route pattern,
            // so providerId arrived as the literal "{providerId}". The screen then sat in
            // edit mode for a row that does not exist and every save returned
            // "provider not found" — the provider was never created.
            val providerRepository = FakeProviderRepository(SampleData.provider(), strictLookup = true)
            val viewModel = viewModel(providerRepository, providerId = "{providerId}")
            advanceUntilIdle()

            val loaded = viewModel.uiState.value
            assertEquals(false, loaded.isLoading)
            assertEquals(false, loaded.isEditing)
            assertNull(loaded.providerId)
            assertEquals(AppError.NotFound, loaded.formError)

            viewModel.onNameChange("test")
            viewModel.onBaseUrlChange("https://inference.dahl.global/v1")
            viewModel.onApiKeyChange("sk-test")
            viewModel.save()
            advanceUntilIdle()

            // The save creates a provider; it never touches the missing row.
            assertEquals(0, providerRepository.updates.size)
            assertEquals("test", providerRepository.addedDrafts.single().first.name)
            assertEquals("https://inference.dahl.global/v1", providerRepository.addedDrafts.single().first.baseUrl)
            assertEquals(false, viewModel.uiState.value.isSaving)
            assertNull(viewModel.uiState.value.formError)
        }

    @Test
    fun `a deleted provider row also falls back to the add form`() = runTest(dispatcher) {
        val providerRepository = FakeProviderRepository(SampleData.provider(), strictLookup = true)
        val viewModel = viewModel(providerRepository, providerId = "missing-id")
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isEditing)
        assertEquals(AppError.NotFound, viewModel.uiState.value.formError)
    }

    @Test
    fun `a created provider whose catalog failed stays on the screen in edit mode`() =
        runTest(dispatcher) {
            // The provider row is created, but the model list could not be pulled (a
            // 404 on /models, wrong key...). Leaving the screen would silently produce a
            // provider you cannot chat with, and retrying in "add" mode would create a
            // duplicate — so the screen switches to editing the provider it just made.
            val providerRepository = FakeProviderRepository(null)
            val modelRepository = FakeModelRepository(null, failRefresh = AppError.HttpNotFound)
            val viewModel = viewModel(providerRepository, modelRepository, providerId = null)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.isEditing)

            viewModel.onNameChange("test")
            viewModel.onBaseUrlChange("https://inference.dahl.global/v1")
            viewModel.onApiKeyChange("sk-test")
            viewModel.save()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, providerRepository.addedDrafts.size)
            assertEquals(AppError.HttpNotFound, state.fetchError)
            assertEquals("p-new", state.providerId)
            assertEquals(true, state.isEditing)
            assertEquals(false, state.isSaving)
        }
}
