package edu.sdwu.teaching.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.model.UserPreferences
import edu.sdwu.teaching.domain.repository.AuthRepository
import edu.sdwu.teaching.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MainUiState(val preferences: UserPreferences, val session: SessionState)

/** Activity-level state: theme preferences and who is signed in. */
class MainViewModel(
    preferencesRepository: PreferencesRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    /** `null` until DataStore has been read once, so we never flash the wrong theme or screen. */
    val uiState: StateFlow<MainUiState?> =
        combine(preferencesRepository.preferences, authRepository.session, ::MainUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
