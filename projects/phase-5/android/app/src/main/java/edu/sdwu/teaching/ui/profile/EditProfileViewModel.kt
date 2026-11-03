package edu.sdwu.teaching.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.sdwu.teaching.data.remote.toUserMessage
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.model.Validation
import edu.sdwu.teaching.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val avatarError: String? = null,
    val error: String? = null,
    /** True once the server accepted the change; the screen then navigates back. */
    val saved: Boolean = false,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        // Start from the current values once; the form owns the text after that.
        viewModelScope.launch {
            val user = (authRepository.session.first() as? SessionState.SignedIn)?.user
            _uiState.update {
                it.copy(
                    isLoading = false,
                    email = user?.email.orEmpty(),
                    displayName = user?.displayName.orEmpty(),
                    avatarUrl = user?.avatarUrl.orEmpty(),
                )
            }
        }
    }

    fun onDisplayNameChange(value: String) = _uiState.update { it.copy(displayName = value, nameError = null, error = null) }
    fun onAvatarUrlChange(value: String) = _uiState.update { it.copy(avatarUrl = value, avatarError = null, error = null) }

    fun save() {
        val state = _uiState.value
        val nameError = if (state.displayName.isBlank()) "Name cannot be empty" else null
        val avatarError = if (state.avatarUrl.isNotBlank() && !Validation.isValidHttpUrl(state.avatarUrl)) {
            "Use an http(s) image link, or leave empty"
        } else {
            null
        }
        if (nameError != null || avatarError != null) {
            _uiState.update { it.copy(nameError = nameError, avatarError = avatarError) }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            authRepository.updateProfile(state.displayName, state.avatarUrl.ifBlank { null })
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.toUserMessage()) } }
        }
    }
}
