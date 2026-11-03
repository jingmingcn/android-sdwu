package edu.sdwu.teaching.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.sdwu.teaching.TeachingApp
import edu.sdwu.teaching.di.AppContainer
import edu.sdwu.teaching.ui.auth.AuthViewModel
import edu.sdwu.teaching.ui.home.HomeViewModel
import edu.sdwu.teaching.ui.lessons.LessonDetailViewModel
import edu.sdwu.teaching.ui.lessons.LessonListViewModel
import edu.sdwu.teaching.ui.profile.ProfileViewModel
import edu.sdwu.teaching.ui.quiz.QuizViewModel

/**
 * One factory that knows how to build every ViewModel in the app.
 * Usage in a screen: `viewModel<LessonListViewModel>(factory = AppViewModelProvider.Factory)`.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { MainViewModel(container().preferencesRepository, container().authRepository) }
        initializer { AuthViewModel(container().authRepository, container().lessonRepository, container().preferencesRepository) }
        initializer { HomeViewModel(container().lessonRepository, container().progressRepository, container().authRepository) }
        initializer { LessonListViewModel(container().lessonRepository, container().progressRepository) }
        initializer {
            LessonDetailViewModel(createSavedStateHandle(), container().lessonRepository, container().progressRepository)
        }
        initializer { QuizViewModel(createSavedStateHandle(), container().lessonRepository, container().progressRepository) }
        initializer {
            ProfileViewModel(
                container().preferencesRepository,
                container().authRepository,
                container().lessonRepository,
                container().progressRepository,
            )
        }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[APPLICATION_KEY] as TeachingApp).container
}
