package com.amrstudio.ui.developer

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amrstudio.data.model.ApiKey
import com.amrstudio.data.model.Model
import com.amrstudio.data.model.User
import com.amrstudio.data.repository.ApiKeyRepository
import com.amrstudio.data.repository.ModelRepository
import com.amrstudio.data.repository.UserRepository
import com.amrstudio.utils.IdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class DeveloperViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val modelRepository: ModelRepository,
    application: Application
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _apiKeys = MutableStateFlow<List<ApiKey>>(emptyList())
    val apiKeys = _apiKeys.asStateFlow()

    private val _models = MutableStateFlow<List<Model>>(emptyList())
    val models = _models.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _selectedTab = MutableStateFlow<DevTab>(DevTab.USERS)
    val selectedTab = _selectedTab.asStateFlow()

    enum class DevTab { USERS, API_KEYS, MODELS }

    init {
        loadAll()
    }

    fun selectTab(tab: DevTab) {
        _selectedTab.value = tab
    }

    private fun loadAll() {
        viewModelScope.launch {
            userRepository.allUsers.collect { _users.value = it }
        }
        viewModelScope.launch {
            apiKeyRepository.allKeys.collect { _apiKeys.value = it }
        }
        viewModelScope.launch {
            modelRepository.allModels.collect { _models.value = it }
        }
    }

    // User operations
    fun toggleUserActive(user: User) {
        viewModelScope.launch {
            val newActive = !user.isActive
            userRepository.toggleUserActive(user.id, newActive)
        }
    }

    fun extendUserDuration(user: User, hours: Long) {
        viewModelScope.launch {
            userRepository.extendUserExpiry(user.id, hours)
        }
    }

    fun setUserAllowedModels(user: User, modelIds: List<String>) {
        viewModelScope.launch {
            userRepository.setAllowedModels(user.id, modelIds)
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            userRepository.deleteUser(user.id)
        }
    }

    // API Key operations
    fun saveApiKey(apiKey: ApiKey) {
        viewModelScope.launch {
            _isLoading.value = true
            apiKeyRepository.saveApiKey(apiKey)
            _isLoading.value = false
        }
    }

    fun updateApiKey(apiKey: ApiKey) {
        viewModelScope.launch {
            _isLoading.value = true
            apiKeyRepository.updateApiKey(apiKey)
            _isLoading.value = false
        }
    }

    fun deleteApiKey(id: String) {
        viewModelScope.launch {
            apiKeyRepository.deleteApiKey(id)
        }
    }

    fun testApiKeyConnection(apiKey: ApiKey): Boolean {
        // Placeholder for actual connection test
        return true
    }

    // Model operations
    fun saveModel(model: Model) {
        viewModelScope.launch {
            _isLoading.value = true
            modelRepository.saveModel(model)
            _isLoading.value = false
        }
    }

    fun updateModel(model: Model) {
        viewModelScope.launch {
            modelRepository.updateModel(model)
        }
    }

    fun toggleModelActive(model: Model) {
        viewModelScope.launch {
            modelRepository.setModelActive(model.id, !model.isActive)
        }
    }

    fun setModelOrder(model: Model, order: Int) {
        viewModelScope.launch {
            modelRepository.setModelOrder(model.id, order)
        }
    }

    fun deleteModel(model: Model) {
        viewModelScope.launch {
            modelRepository.deleteModel(model.id)
        }
    }

    fun createNewApiKey(providerName: String, baseUrl: String, apiKey: String, defaultModel: String): ApiKey {
        return ApiKey(
            id = IdGenerator.generate(),
            providerName = providerName,
            baseUrl = baseUrl,
            apiKeyEncrypted = apiKey, // Should be encrypted in production
            defaultModel = defaultModel,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    fun createNewModel(name: String, providerId: String, type: Model.ModelType, isFree: Boolean): Model {
        return Model(
            id = IdGenerator.generate(),
            name = name,
            providerId = providerId,
            type = type,
            isFree = isFree,
            isActive = true,
            displayOrder = _models.value.size
        )
    }
}