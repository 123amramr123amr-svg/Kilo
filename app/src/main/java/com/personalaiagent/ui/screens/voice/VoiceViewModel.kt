package com.personalaiagent.ui.screens.voice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class VoiceError { PERMISSION_REQUIRED, UNAVAILABLE, NO_MATCH, FAILED }

data class VoiceUiState(
    val isAvailable: Boolean = true,
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val partialText: String = "",
    val finalText: String = "",
    val error: VoiceError? = null,
) {
    val transcript: String get() = finalText.ifBlank { partialText }
    val canSend: Boolean get() = finalText.isNotBlank()
}

/**
 * Owns the platform [SpeechRecognizer]. Audio never leaves the device through this
 * app: recognition is performed by the system recogniser the user has installed.
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VoiceUiState(isAvailable = SpeechRecognizer.isRecognitionAvailable(context)),
    )
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    fun hasMicrophonePermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    fun onPermissionDenied() {
        _uiState.update { it.copy(error = VoiceError.PERMISSION_REQUIRED, isListening = false) }
    }

    fun start(languageTag: String) {
        if (!_uiState.value.isAvailable) {
            _uiState.update { it.copy(error = VoiceError.UNAVAILABLE) }
            return
        }
        if (!hasMicrophonePermission()) {
            _uiState.update { it.copy(error = VoiceError.PERMISSION_REQUIRED) }
            return
        }
        ensureRecognizer()
        _uiState.update {
            it.copy(isListening = true, isProcessing = false, error = null, finalText = "", partialText = "")
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        runCatching { recognizer?.startListening(intent) }
            .onFailure { error ->
                _uiState.update { it.copy(isListening = false, error = VoiceError.FAILED) }
            }
    }

    fun stopListening() {
        runCatching { recognizer?.stopListening() }
        _uiState.update { it.copy(isListening = false, isProcessing = true) }
    }

    fun reset() {
        runCatching { recognizer?.cancel() }
        _uiState.update {
            it.copy(isListening = false, isProcessing = false, finalText = "", partialText = "", error = null)
        }
    }

    private fun ensureRecognizer() {
        if (recognizer != null) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _uiState.update { it.copy(isListening = true, error = null) }
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _uiState.update { it.copy(isListening = false, isProcessing = true) }
                }

                override fun onError(error: Int) {
                    val mapped = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            VoiceError.NO_MATCH
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.PERMISSION_REQUIRED
                        else -> VoiceError.FAILED
                    }
                    _uiState.update {
                        it.copy(isListening = false, isProcessing = false, error = mapped)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    _uiState.update {
                        it.copy(
                            isListening = false,
                            isProcessing = false,
                            finalText = text,
                            partialText = "",
                            error = if (text.isBlank()) VoiceError.NO_MATCH else null,
                        )
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    _uiState.update { it.copy(partialText = text) }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { recognizer?.destroy() }
        recognizer = null
    }
}
