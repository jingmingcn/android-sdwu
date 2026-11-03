package edu.sdwu.teaching.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.sdwu.teaching.data.remote.toUserMessage
import edu.sdwu.teaching.domain.repository.AuthRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean
        get() = !isLoading && email.isNotBlank() && password.isNotEmpty() &&
            (mode == AuthMode.LOGIN || displayName.isNotBlank())
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val lessonRepository: LessonRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val serverUrl: StateFlow<String> = preferencesRepository.preferences
        .map { it.serverUrl }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun onDisplayNameChange(value: String) = _uiState.update { it.copy(displayName = value, error = null) }

    fun toggleMode() = _uiState.update {
        it.copy(mode = if (it.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN, error = null)
    }

    fun submit() {
        val state = _uiState.value
        validate(state)?.let { message -> _uiState.update { it.copy(error = message) }; return }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = when (state.mode) {
                AuthMode.LOGIN -> authRepository.login(state.email, state.password)
                AuthMode.REGISTER -> authRepository.register(state.email, state.password, state.displayName)
            }
            result
                // On success the session Flow changes and MainActivity swaps to the app automatically.
                .onSuccess { lessonRepository.refresh() }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) } }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch { authRepository.continueAsGuest() }
    }

    fun saveServerUrl(url: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(preferencesRepository.setServerUrl(url)) }
    }

    /** Client-side checks give instant feedback; the server validates again. */
    private fun validate(state: AuthUiState): String? = when {
        !Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches() -> "Enter a valid email address"
        state.mode == AuthMode.REGISTER && state.password.length < 6 -> "Password must have at least 6 characters"
        else -> null
    }
}
