package edu.sdwu.teaching.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.sdwu.teaching.TeachingApp
import edu.sdwu.teaching.di.AppContainer
import edu.sdwu.teaching.ui.home.HomeViewModel
import edu.sdwu.teaching.ui.lessons.LessonDetailViewModel
import edu.sdwu.teaching.ui.lessons.LessonListViewModel
import edu.sdwu.teaching.ui.profile.ProfileViewModel

/**
 * One factory that knows how to build every ViewModel in the app.
 * Usage in a screen: `viewModel<LessonListViewModel>(factory = AppViewModelProvider.Factory)`.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { HomeViewModel(container().lessonRepository) }
        initializer { LessonListViewModel(container().lessonRepository) }
        initializer { LessonDetailViewModel(createSavedStateHandle(), container().lessonRepository) }
        initializer { ProfileViewModel(container().preferencesRepository) }
        initializer { MainViewModel(container().preferencesRepository) }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[APPLICATION_KEY] as TeachingApp).container
}
