package com.amrstudio.ui.models

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amrstudio.data.model.ApiKey
import com.amrstudio.data.model.Download
import com.amrstudio.data.model.Model
import com.amrstudio.data.repository.ApiKeyRepository
import com.amrstudio.data.repository.DownloadRepository
import com.amrstudio.data.repository.ModelRepository
import com.amrstudio.utils.IdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject

interface OllamaApi {
    @GET("/api/tags")
    suspend fun getModels(): OllamaModelsResponse

    @POST("/api/pull")
    suspend fun pullModel(@Header("Content-Type") contentType: String, body: PullRequest): retrofit2.Response<okhttp3.ResponseBody>

    @GET("/api/show")
    suspend fun showModel(@Query("name") name: String): OllamaModelInfo
}

data class OllamaModelsResponse(val models: List<OllamaModel>)
data class OllamaModel(val name: String, val size: Long, val digest: String, val details: OllamaModelDetails?)
data class OllamaModelDetails(val format: String, val family: String, val families: List<String>, val parameter_size: String, val quantization_level: String)
data class PullRequest(val name: String, val stream: Boolean = false)
data class OllamaModelInfo(val name: String, val modelfile: String, val template: String, val details: OllamaModelDetails)

interface OpenRouterApi {
    @GET("/api/v1/models")
    suspend fun getModels(@Header("Authorization") auth: String): OpenRouterModelsResponse
}

data class OpenRouterModelsResponse(val data: List<OpenRouterModel>)
data class OpenRouterModel(
    val id: String,
    val name: String,
    val description: String,
    val pricing: OpenRouterPricing,
    val context_length: Int,
    val architecture: OpenRouterArchitecture?
)
data class OpenRouterPricing(val prompt: String, val completion: String, val request: String, val image: String?)
data class OpenRouterArchitecture(val modality: String, val tokenizer: String, val instruct_type: String?)

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val downloadRepository: DownloadRepository,
    application: Application
) : ViewModel() {

    private val _ollamaAddress = MutableStateFlow<String>("http://127.0.0.1:11434")
    val ollamaAddress = _ollamaAddress.asStateFlow()

    private val _ollamaModels = MutableStateFlow<List<OllamaModel>>(emptyList())
    val ollamaModels = _ollamaModels.asStateFlow()

    private val _openRouterModels = MutableStateFlow<List<OpenRouterModel>>(emptyList())
    val openRouterModels = _openRouterModels.asStateFlow()

    private val _hermesAddress = MutableStateFlow<String>("http://127.0.0.1:8642/v1")
    val hermesAddress = _hermesAddress.asStateFlow()

    private val _hermesApiKey = MutableStateFlow<String>("")
    val hermesApiKey = _hermesApiKey.asStateFlow()

    private val _nineRouterAddress = MutableStateFlow<String>("http://localhost:20128/v1")
    val nineRouterAddress = _nineRouterAddress.asStateFlow()

    private val _addedModels = MutableStateFlow<List<Model>>(emptyList())
    val addedModels = _addedModels.asStateFlow()

    private val _downloads = MutableStateFlow<List<Download>>(emptyList())
    val downloads = _downloads.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _selectedTab = MutableStateFlow<ModelTab>(ModelTab.OLLAMA)
    val selectedTab = _selectedTab.asStateFlow()

    enum class ModelTab { OLLAMA, HERMES, NINE_ROUTER, ADDED, DOWNLOADS }

    init {
        loadAddedModels()
        loadDownloads()
    }

    fun selectTab(tab: ModelTab) {
        _selectedTab.value = tab
    }

    fun setOllamaAddress(address: String) {
        _ollamaAddress.value = address
    }

    fun setHermesAddress(address: String) {
        _hermesAddress.value = address
    }

    fun setHermesApiKey(key: String) {
        _hermesApiKey.value = key
    }

    fun setNineRouterAddress(address: String) {
        _nineRouterAddress.value = address
    }

    fun fetchOllamaModels() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(_ollamaAddress.value)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
                val api = retrofit.create(OllamaApi::class.java)
                val response = api.getModels()
                _ollamaModels.value = response.models
            } catch (e: Exception) {
                _error.value = "فشل الاتصال بـ Ollama: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadOllamaModel(model: OllamaModel) {
        val download = Download(
            id = IdGenerator.generate(),
            modelId = model.name,
            name = model.name,
            url = "${_ollamaAddress.value}/api/pull",
            destinationPath = "", // Will be set by Ollama
            totalBytes = model.size,
            status = Download.DownloadStatus.DOWNLOADING,
            createdAt = LocalDateTime.now()
        )
        viewModelScope.launch {
            downloadRepository.createDownload(download)
            _downloads.value = _downloads.value + download
        }

        viewModelScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(_ollamaAddress.value)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
                val api = retrofit.create(OllamaApi::class.java)
                val request = PullRequest(name = model.name)
                val response = api.pullModel("application/json", request)

                if (response.isSuccessful) {
                    // Handle streaming response
                    response.body()?.source()?.use { source ->
                        // Parse streaming JSON updates
                        // For now, mark as complete
                    }
                    updateDownloadStatus(download.id, Download.DownloadStatus.COMPLETED)
                    addModelToLocal(model)
                } else {
                    updateDownloadStatus(download.id, Download.DownloadStatus.FAILED, "فشل التنزيل: ${response.code()}")
                }
            } catch (e: Exception) {
                updateDownloadStatus(download.id, Download.DownloadStatus.FAILED, e.message ?: "خطأ غير معروف")
            }
        }
    }

    private fun updateDownloadStatus(downloadId: String, status: Download.DownloadStatus, error: String? = null) {
        viewModelScope.launch {
            val download = downloadRepository.getDownload(downloadId)?.copy(
                status = status,
                completedAt = if (status == Download.DownloadStatus.COMPLETED) LocalDateTime.now() else null,
                errorMessage = error
            )
            download?.let {
                downloadRepository.updateDownload(it)
                _downloads.value = _downloads.value.map { if (it.id == downloadId) it else it }
            }
        }
    }

    private fun addModelToLocal(ollamaModel: OllamaModel) {
        val model = Model(
            id = IdGenerator.generate(),
            name = ollamaModel.name,
            providerId = "ollama",
            type = Model.ModelType.CHAT,
            isFree = true,
            isActive = true,
            description = "Ollama model: ${ollamaModel.details?.family ?: "unknown"}",
            contextLength = 4096
        )
        viewModelScope.launch {
            modelRepository.saveModel(model)
            _addedModels.value = _addedModels.value + model
        }
    }

    fun fetchOpenRouterModels() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val apiKey = apiKeyRepository.getApiKeyByProvider("OpenRouter")
                if (apiKey == null) {
                    _error.value = "مفتاح OpenRouter غير موجود. أضفه من لوحة المطور."
                    _isLoading.value = false
                    return@launch
                }

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://openrouter.ai")
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
                val api = retrofit.create(OpenRouterApi::class.java)
                val response = api.getModels("Bearer ${apiKey.apiKeyEncrypted}")
                _openRouterModels.value = response.data
            } catch (e: Exception) {
                _error.value = "فشل جلب نماذج OpenRouter: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addOpenRouterModel(model: OpenRouterModel) {
        val newModel = Model(
            id = IdGenerator.generate(),
            name = model.name,
            providerId = "openrouter",
            type = Model.ModelType.CHAT,
            isFree = model.pricing.prompt == "0" && model.pricing.completion == "0",
            isActive = true,
            description = model.description,
            contextLength = model.context_length,
            pricing = "Prompt: \$${model.pricing.prompt}/1K, Completion: \$${model.pricing.completion}/1K"
        )
        viewModelScope.launch {
            modelRepository.saveModel(newModel)
            _addedModels.value = _addedModels.value + newModel
        }
    }

    fun testHermesConnection(): Boolean {
        // Placeholder for Hermes connection test
        return true
    }

    fun testNineRouterConnection(): Boolean {
        // Placeholder for 9Router connection test
        return true
    }

    private fun loadAddedModels() {
        viewModelScope.launch {
            modelRepository.allModels.collect { _addedModels.value = it }
        }
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            downloadRepository.allDownloads.collect { _downloads.value = it }
        }
    }

    fun pauseDownload(download: Download) {
        viewModelScope.launch {
            val updated = download.copy(status = Download.DownloadStatus.PAUSED)
            downloadRepository.updateDownload(updated)
        }
    }

    fun resumeDownload(download: Download) {
        viewModelScope.launch {
            val updated = download.copy(status = Download.DownloadStatus.DOWNLOADING)
            downloadRepository.updateDownload(updated)
            // Resume download logic here
        }
    }

    fun retryDownload(download: Download) {
        viewModelScope.launch {
            val updated = download.copy(
                status = Download.DownloadStatus.DOWNLOADING,
                downloadedBytes = 0,
                errorMessage = null
            )
            downloadRepository.updateDownload(updated)
            // Retry logic here
        }
    }

    fun deleteDownload(download: Download) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(download.id)
            _downloads.value = _downloads.value.filter { it.id != download.id }
        }
    }
}