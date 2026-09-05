package com.fbr.ntn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fbr.ntn.data.FbrRepository
import com.fbr.ntn.data.SettingsStore
import com.fbr.ntn.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen { SPLASH, NTN, PIN, CONNECT, HOME, DETAIL, SETTINGS, LOCK }

data class AppUiState(
    val screen: AppScreen = AppScreen.SPLASH,
    val account: AccountContext? = null,
    val ntn: String = "",
    val ntnLoading: Boolean = false,
    val ntnError: String? = null,
    val pinLoading: Boolean = false,
    val pinError: String? = null,
    val connectLoading: Boolean = false,
    val connectError: String? = null,
    val pendingLoading: Boolean = false,
    val pendingRefreshing: Boolean = false,
    val pendingItems: List<PendingItem> = emptyList(),
    val pendingError: String? = null,
    val selectedId: String? = null,
    val themeMode: String = "system",
    val soundsEnabled: Boolean = false
)

class AppViewModel(
    private val repository: FbrRepository,
    private val settings: SettingsStore
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(
            themeMode = settings.themeMode,
            soundsEnabled = settings.soundsEnabled
        )
        com.fbr.ntn.ui.sound.SoundFx.enabled = settings.soundsEnabled
        checkSession()
    }

    fun setThemeMode(mode: String) {
        settings.themeMode = mode
        _state.value = _state.value.copy(themeMode = mode)
    }

    fun setSoundsEnabled(enabled: Boolean) {
        settings.soundsEnabled = enabled
        com.fbr.ntn.ui.sound.SoundFx.enabled = enabled
        _state.value = _state.value.copy(soundsEnabled = enabled)
    }

    fun openSettings() { _state.value = _state.value.copy(screen = AppScreen.SETTINGS) }
    fun closeSettings() { _state.value = _state.value.copy(screen = AppScreen.HOME) }

    private fun checkSession() = viewModelScope.launch {
        val session = repository.validSession()
        delay(1100)
        if (session != null && !session.apiUrl.isNullOrBlank()) {
            _state.value = _state.value.copy(
                screen = AppScreen.HOME,
                account = AccountContext(session.ntn ?: "", session.displayName ?: "", ""),
                pendingItems = emptyList()
            )
            loadPending()
        } else if (session != null) {
            _state.value = _state.value.copy(screen = AppScreen.CONNECT, ntn = session.ntn ?: "")
        } else {
            _state.value = _state.value.copy(screen = AppScreen.NTN)
        }
    }

    fun checkNtn(ntn: String) = viewModelScope.launch {
        _state.value = _state.value.copy(ntnLoading = true, ntnError = null, ntn = ntn)
        when (val result = repository.checkNtn(ntn)) {
            is AppResult.Success -> _state.value = _state.value.copy(screen = AppScreen.PIN, ntnLoading = false)
            is AppResult.Error -> _state.value = _state.value.copy(ntnLoading = false, ntnError = result.message)
        }
    }

    fun verifyPin(pin: String) = viewModelScope.launch {
        if (_state.value.pinLoading) return@launch
        _state.value = _state.value.copy(pinLoading = true, pinError = null)
        val ntn = _state.value.ntn
        when (val result = repository.verifyPin(ntn, pin)) {
            is AppResult.Success -> {
                val session = result.value
                repository.saveSession(session)
                _state.value = _state.value.copy(
                    screen = AppScreen.CONNECT,
                    pinLoading = false,
                    ntn = session.ntn ?: ntn
                )
            }
            is AppResult.Error -> {
                com.fbr.ntn.ui.sound.SoundFx.error()
                _state.value = _state.value.copy(pinLoading = false, pinError = result.message)
            }
        }
    }

    fun connect(apiUrl: String, username: String, password: String, pin: String) = viewModelScope.launch {
        _state.value = _state.value.copy(connectLoading = true, connectError = null)
        val ntn = _state.value.ntn
        when (val result = repository.connectAndLogin(apiUrl, username, password, pin, ntn)) {
            is AppResult.Success -> {
                val session = result.value
                repository.saveSession(session)
                _state.value = _state.value.copy(
                    screen = AppScreen.HOME,
                    connectLoading = false,
                    account = AccountContext(session.ntn ?: "", session.displayName ?: "", ""),
                    pendingItems = emptyList()
                )
                loadPending()
            }
            is AppResult.Error -> {
                _state.value = _state.value.copy(connectLoading = false, connectError = result.message)
            }
        }
    }

    fun validateInvoice(id: String) = viewModelScope.launch {
        _state.value = _state.value.copy(pendingItems = state.value.pendingItems.map {
            if (it.id == id) it.copy(dueLabel = "Validating…") else it
        })
        when (val result = repository.validateInvoice(id)) {
            is AppResult.Success -> {
                _state.value = _state.value.copy(pendingItems = state.value.pendingItems.map {
                    if (it.id == id) it.copy(status = PendingStatus.POSTED, dueLabel = "Validated") else it
                })
                com.fbr.ntn.ui.sound.SoundFx.success()
                loadPending()
            }
            is AppResult.Error -> {
                _state.value = _state.value.copy(pendingItems = state.value.pendingItems.map {
                    if (it.id == id) it.copy(dueLabel = "Retry") else it
                })
                com.fbr.ntn.ui.sound.SoundFx.error()
            }
        }
    }

    fun postInvoice(id: String) = viewModelScope.launch {
        _state.value = _state.value.copy(pendingItems = state.value.pendingItems.map {
            if (it.id == id) it.copy(dueLabel = "Posting…") else it
        })
        when (val result = repository.postInvoice(id)) {
            is AppResult.Success -> {
                _state.value = _state.value.copy(pendingItems = state.value.pendingItems.filter { it.id != id })
                com.fbr.ntn.ui.sound.SoundFx.success()
            }
            is AppResult.Error -> {
                _state.value = _state.value.copy(pendingItems = state.value.pendingItems.map {
                    if (it.id == id) it.copy(dueLabel = "Retry") else it
                })
                com.fbr.ntn.ui.sound.SoundFx.error()
            }
        }
    }

    fun loadPending(refresh: Boolean = false) = viewModelScope.launch {
        _state.value = _state.value.copy(pendingLoading = !refresh, pendingRefreshing = refresh, pendingError = null)
        when (val result = repository.getPending()) {
            is AppResult.Success -> _state.value = _state.value.copy(pendingLoading = false, pendingRefreshing = false, pendingItems = result.value)
            is AppResult.Error -> _state.value = _state.value.copy(pendingLoading = false, pendingRefreshing = false, pendingError = result.message)
        }
    }

    fun openInvoice(id: String) { _state.value = _state.value.copy(selectedId = id, screen = AppScreen.DETAIL) }
    fun closeDetail() { _state.value = _state.value.copy(selectedId = null, screen = AppScreen.HOME) }
    fun lock() { _state.value = _state.value.copy(screen = AppScreen.LOCK, selectedId = null) }

    fun unlock(username: String, password: String, pin: String) = viewModelScope.launch {
        _state.value = _state.value.copy(connectLoading = true, connectError = null)
        val session = repository.validSession()
        val apiUrl = session?.apiUrl ?: com.fbr.ntn.data.FbrRepository.BASE_URL
        val savedNtn = session?.ntn ?: _state.value.ntn
        when (val result = repository.connectAndLogin(apiUrl, username, password, pin, savedNtn)) {
            is AppResult.Success -> {
                val s = result.value
                repository.saveSession(s)
                _state.value = _state.value.copy(
                    screen = AppScreen.HOME, connectLoading = false,
                    account = AccountContext(s.ntn ?: "", s.displayName ?: "", ""),
                    pendingItems = emptyList()
                )
                loadPending()
            }
            is AppResult.Error -> {
                com.fbr.ntn.ui.sound.SoundFx.error()
                _state.value = _state.value.copy(connectLoading = false, connectError = result.message)
            }
        }
    }

    fun switchAccount() {
        repository.clearSession()
        _state.value = AppUiState(
            screen = AppScreen.NTN,
            themeMode = settings.themeMode,
            soundsEnabled = settings.soundsEnabled
        )
    }
}
