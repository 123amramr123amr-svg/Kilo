package com.amrstudio.ui.auth

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amrstudio.data.model.User
import com.amrstudio.data.repository.UserRepository
import com.amrstudio.utils.HashUtils
import com.amrstudio.utils.IdGenerator
import com.amrstudio.utils.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _loginState = MutableStateFlow<AuthResult?>(null)
    val loginState = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthResult?>(null)
    val registerState = _registerState.asStateFlow()

    data class AuthUiState(
        val name: String = "",
        val email: String = "",
        val phone: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val agreePrivacy: Boolean = false,
        val isLoading: Boolean = false,
        val nameError: String? = null,
        val emailError: String? = null,
        val phoneError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null,
        val privacyError: String? = null
    )

    sealed interface AuthResult {
        data class Success(val user: User) : AuthResult
        data class Error(val message: String) : AuthResult
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun updatePhone(phone: String) {
        _uiState.update { it.copy(phone = phone, phoneError = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun updatePrivacyAgreement(agree: Boolean) {
        _uiState.update { it.copy(agreePrivacy = agree, privacyError = null) }
    }

    fun validateAndLogin() {
        val state = _uiState.value
        var hasError = false

        val newState = state.copy(
            emailError = if (state.email.isBlank()) "البريد الإلكتروني مطلوب" else if (!ValidationUtils.isValidEmail(state.email)) "بريد إلكتروني غير صحيح" else null,
            passwordError = if (state.password.isBlank()) "كلمة المرور مطلوبة" else null,
            isLoading = true
        )

        _uiState.value = newState

        if (newState.emailError != null || newState.passwordError != null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            val passwordHash = HashUtils.hashPassword(state.password)
            val result = userRepository.authenticate(state.email, passwordHash)

            _loginState.value = when (result) {
                is com.amrstudio.data.repository.Result.Success -> AuthResult.Success(result.data)
                is com.amrstudio.data.repository.Result.Failure -> AuthResult.Error(result.exception.message ?: "فشل تسجيل الدخول")
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun validateAndRegister() {
        val state = _uiState.value
        var hasError = false

        val nameError = if (state.name.trim().isBlank()) "الاسم مطلوب"
        val emailError = if (state.email.isBlank()) "البريد الإلكتروني مطلوب"
            else if (!ValidationUtils.isValidEmail(state.email)) "بريد إلكتروني غير صحيح"
        val phoneError = if (state.phone.isBlank()) "رقم الهاتف مطلوب"
            else if (!ValidationUtils.isValidPhone(state.phone)) "رقم هاتف غير صحيح"
        val passwordError = if (state.password.isBlank()) "كلمة المرور مطلوبة"
            else if (!ValidationUtils.isStrongPassword(state.password)) "كلمة المرور ضعيفة (8 أحرف على الأقل، حروف كبيرة وصغيرة وأرقام)"
        val confirmError = if (state.confirmPassword.isBlank()) "تأكيد كلمة المرور مطلوب"
            else if (!ValidationUtils.passwordsMatch(state.password, state.confirmPassword)) "كلمة المرور غير متطابقة"
        val privacyError = if (!state.agreePrivacy) "يجب الموافقة على سياسة الخصوصية"

        val newState = state.copy(
            nameError = nameError,
            emailError = emailError,
            phoneError = phoneError,
            passwordError = passwordError,
            confirmPasswordError = confirmError,
            privacyError = privacyError,
            isLoading = true
        )

        _uiState.value = newState

        if (nameError != null || emailError != null || phoneError != null ||
            passwordError != null || confirmError != null || privacyError != null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            val user = User(
                id = IdGenerator.generate(),
                name = state.name.trim(),
                email = state.email.trim().lowercase(),
                phone = state.phone.trim(),
                passwordHash = HashUtils.hashPassword(state.password),
                isDeveloper = false,
                isActive = true,
                createdAt = LocalDateTime.now(),
                expiresAt = LocalDateTime.now().plusHours(24),
                trialDurationHours = 24
            )

            val result = userRepository.createUser(user)

            _registerState.value = when (result) {
                is com.amrstudio.data.repository.Result.Success -> AuthResult.Success(result.data)
                is com.amrstudio.data.repository.Result.Failure -> AuthResult.Error(result.exception.message ?: "فشل إنشاء الحساب")
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearResults() {
        _loginState.value = null
        _registerState.value = null
    }

    fun resetForm() {
        _uiState.value = AuthUiState()
    }
}