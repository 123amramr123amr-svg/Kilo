package com.amrstudio.ui.connectors

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amrstudio.data.model.Connector
import com.amrstudio.data.repository.ConnectorRepository
import com.amrstudio.utils.IdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ConnectorsViewModel @Inject constructor(
    private val connectorRepository: ConnectorRepository,
    application: Application
) : ViewModel() {

    private val _connectors = MutableStateFlow<List<Connector>>(emptyList())
    val connectors = _connectors.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadConnectors()
    }

    private fun loadConnectors() {
        viewModelScope.launch {
            connectorRepository.allConnectors.collect { _connectors.value = it }
        }
    }

    fun saveConnector(connector: Connector) {
        viewModelScope.launch {
            _isLoading.value = true
            connectorRepository.saveConnector(connector)
            _isLoading.value = false
        }
    }

    fun updateConnector(connector: Connector) {
        viewModelScope.launch {
            connectorRepository.updateConnector(connector)
        }
    }

    fun deleteConnector(id: String) {
        viewModelScope.launch {
            connectorRepository.deleteConnector(id)
        }
    }

    fun testConnection(connector: Connector): Boolean {
        // Placeholder for actual connection test
        return true
    }

    fun getDefaultConnectors(): List<Connector> {
        return Connector.ConnectorType.values().map { type ->
            Connector(
                id = IdGenerator.generate(),
                name = getConnectorDisplayName(type),
                type = type,
                configEncrypted = "{}",
                isConnected = false,
                createdAt = LocalDateTime.now()
            )
        }
    }

    private fun getConnectorDisplayName(type: Connector.ConnectorType): String = when (type) {
        Connector.ConnectorType.GITHUB -> "GitHub"
        Connector.ConnectorType.GMAIL -> "Gmail"
        Connector.ConnectorType.N8N -> "n8n"
        Connector.ConnectorType.OPENROUTER -> "OpenRouter"
        Connector.ConnectorType.NVIDIA -> "NVIDIA NIM"
        Connector.ConnectorType.OLLAMA -> "Ollama"
        Connector.ConnectorType.NINE_ROUTER -> "9Router"
        Connector.ConnectorType.HERMES -> "Hermes Gateway"
    }
}