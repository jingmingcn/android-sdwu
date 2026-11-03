package edu.sdwu.teaching.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.sdwu.teaching.domain.model.UserPreferences
import edu.sdwu.teaching.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Activity-level state: only the theme preferences for now. */
class MainViewModel(preferencesRepository: PreferencesRepository) : ViewModel() {

    /** `null` until DataStore has been read once, so we never flash the wrong theme. */
    val preferences: StateFlow<UserPreferences?> = preferencesRepository.preferences
        .map<UserPreferences, UserPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
