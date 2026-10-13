package com.amrstudio.ui.main

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amrstudio.data.model.User
import com.amrstudio.data.repository.UserRepository
import com.amrstudio.utils.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    application: Application
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _remainingTime = MutableStateFlow<String>("")
    val remainingTime = _remainingTime.asStateFlow()

    private val _isExpired = MutableStateFlow<Boolean>(false)
    val isExpired = _isExpired.asStateFlow()

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
        updateRemainingTime(user)
    }

    private fun updateRemainingTime(user: User?) {
        user?.let {
            if (it.isExpired) {
                _isExpired.value = true
                _remainingTime.value = "انتهت الصلاحية"
            } else {
                _isExpired.value = false
                _remainingTime.value = DateTimeUtils.formatRemaining(it.remainingHours)
            }
        } ?: run {
            _remainingTime.value = ""
            _isExpired.value = false
        }
    }

    fun startCountdown() {
        viewModelScope.launch {
            while (true) {
                val user = _currentUser.value
                updateRemainingTime(user)
                kotlinx.coroutines.delay(60000) // Update every minute
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }
}