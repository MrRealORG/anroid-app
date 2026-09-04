package com.fbr.ntn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fbr.ntn.data.FbrRepository
import com.fbr.ntn.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

enum class AppScreen { SPLASH, NTN, MOBILE, OTP, WEB_LOGIN, HOME }

data class AppUiState(
    val screen: AppScreen = AppScreen.SPLASH,
    val account: AccountContext? = null,
    val ntnLoading: Boolean = false,
    val ntnError: String? = null,
    val otpSending: Boolean = false,
    val challengeId: String? = null,
    val resendSeconds: Int = 0,
    val otpVerifying: Boolean = false,
    val otpError: String? = null,
    val otpVerified: Boolean = false,
    val loginUrl: String? = null,
    val webError: Boolean = false,
    val pendingLoading: Boolean = false,
    val pendingRefreshing: Boolean = false,
    val pendingItems: List<PendingItem> = emptyList(),
    val pendingError: String? = null
)

class AppViewModel(private val repository: FbrRepository) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    private var countdownJob: Job? = null
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init { checkSession() }

    private fun checkSession() = viewModelScope.launch {
        val session = repository.validSession()
        delay(1100)
        if (session != null) {
            val savedAccount = if (session.ntn != null && session.displayName != null) AccountContext(
                session.ntn, session.displayName, session.maskedMobile.orEmpty(), ""
            ) else null
            _state.value = _state.value.copy(screen = AppScreen.HOME, account = savedAccount)
            loadPending()
        } else _state.value = _state.value.copy(screen = AppScreen.NTN)
    }

    fun checkNtn(ntn: String) = viewModelScope.launch {
        _state.value = _state.value.copy(ntnLoading = true, ntnError = null)
        when (val result = repository.checkNtn(ntn)) {
            is AppResult.Success -> _state.value = _state.value.copy(screen = AppScreen.MOBILE, account = result.value, ntnLoading = false)
            is AppResult.Error -> _state.value = _state.value.copy(ntnLoading = false, ntnError = if (result.kind == ErrorKind.NOT_FOUND) "We couldn't find that NTN" else friendlyError(result.kind))
        }
    }

    fun sendOtp() = viewModelScope.launch {
        val mobile = state.value.account?.mobileToken ?: return@launch
        _state.value = _state.value.copy(otpSending = true, otpError = null)
        when (val result = repository.sendOtp(mobile)) {
            is AppResult.Success -> {
                _state.value = _state.value.copy(screen = AppScreen.OTP, otpSending = false, challengeId = result.value.first, resendSeconds = result.value.second)
                startCountdown(result.value.second)
            }
            is AppResult.Error -> _state.value = _state.value.copy(otpSending = false, otpError = friendlyError(result.kind))
        }
    }

    fun verifyOtp(code: String) = viewModelScope.launch {
        if (state.value.otpVerifying) return@launch
        val challenge = state.value.challengeId ?: return@launch
        _state.value = _state.value.copy(otpVerifying = true, otpError = null)
        when (val verified = repository.verifyOtp(challenge, code)) {
            is AppResult.Success -> {
                _state.value = _state.value.copy(otpVerifying = false, otpVerified = true)
                delay(800)
                when (val link = repository.getLoginLink(verified.value)) {
                    is AppResult.Success -> _state.value = _state.value.copy(screen = AppScreen.WEB_LOGIN, loginUrl = link.value, otpVerified = false)
                    is AppResult.Error -> _state.value = _state.value.copy(otpVerified = false, otpError = friendlyError(link.kind))
                }
            }
            is AppResult.Error -> _state.value = _state.value.copy(otpVerifying = false, otpError = if (verified.kind == ErrorKind.INVALID_OTP) "That code didn't match — try again" else friendlyError(verified.kind))
        }
    }

    fun resendOtp() { if (state.value.resendSeconds == 0) sendOtp() }

    private fun startCountdown(from: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
        repeat(from) {
            delay(1000)
            _state.value = _state.value.copy(resendSeconds = (_state.value.resendSeconds - 1).coerceAtLeast(0))
        }
    }
    }

    fun webFailed() { _state.value = _state.value.copy(webError = true) }
    fun retryWeb() { _state.value = _state.value.copy(webError = false) }

    fun completeLogin(token: String) {
        repository.completeWebLogin(token, state.value.account)
        _state.value = _state.value.copy(screen = AppScreen.HOME, webError = false)
        loadPending()
    }

    fun loadPending(refresh: Boolean = false) = viewModelScope.launch {
        _state.value = _state.value.copy(pendingLoading = !refresh, pendingRefreshing = refresh, pendingError = null)
        when (val result = repository.getPending()) {
            is AppResult.Success -> _state.value = _state.value.copy(pendingLoading = false, pendingRefreshing = false, pendingItems = result.value)
            is AppResult.Error -> _state.value = _state.value.copy(pendingLoading = false, pendingRefreshing = false, pendingError = friendlyError(result.kind))
        }
    }

    fun back() {
        _state.value = when (state.value.screen) {
            AppScreen.MOBILE -> state.value.copy(screen = AppScreen.NTN)
            AppScreen.OTP -> state.value.copy(screen = AppScreen.MOBILE, otpError = null)
            AppScreen.WEB_LOGIN -> state.value.copy(screen = AppScreen.OTP)
            else -> state.value
        }
    }

    private fun friendlyError(kind: ErrorKind) = when (kind) {
        ErrorKind.NETWORK -> "Couldn't reach the server — check your connection and try again"
        ErrorKind.UNAUTHORIZED -> "Your session has expired. Please verify again"
        else -> "Something went wrong. Please try again"
    }
}
