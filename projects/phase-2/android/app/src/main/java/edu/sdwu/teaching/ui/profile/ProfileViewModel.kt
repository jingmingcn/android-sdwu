package edu.sdwu.teaching.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.sdwu.teaching.data.remote.toUserMessage
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.model.ThemeMode
import edu.sdwu.teaching.domain.model.UserPreferences
import edu.sdwu.teaching.domain.repository.AuthRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.PreferencesRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val preferences: UserPreferences? = null,
    val session: SessionState = SessionState.Guest,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
)

class ProfileViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val isSyncing = MutableStateFlow(false)
    private val syncMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        preferencesRepository.preferences,
        authRepository.session,
        isSyncing,
        syncMessage,
        ::ProfileUiState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
    }

    fun saveServerUrl(url: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(preferencesRepository.setServerUrl(url)) }
    }

    /** Refresh lessons, then push/pull progress. */
    fun syncNow() {
        viewModelScope.launch {
            isSyncing.value = true
            val result = lessonRepository.refresh().mapCatching { progressRepository.sync().getOrThrow() }
            syncMessage.value = result.fold(
                onSuccess = { "Up to date" },
                onFailure = { it.toUserMessage() },
            )
            isSyncing.value = false
        }
    }

    /** Signed in: sign out and clear progress. Guest: go to the login screen and keep progress. */
    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
