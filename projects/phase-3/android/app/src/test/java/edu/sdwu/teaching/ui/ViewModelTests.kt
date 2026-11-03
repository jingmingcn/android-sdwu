package edu.sdwu.teaching.ui

import app.cash.turbine.test
import edu.sdwu.teaching.domain.model.Difficulty
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.testing.FakeAuthRepository
import edu.sdwu.teaching.testing.FakeBookmarkRepository
import edu.sdwu.teaching.testing.FakeLessonRepository
import edu.sdwu.teaching.testing.FakeNoteRepository
import edu.sdwu.teaching.testing.FakePreferencesRepository
import edu.sdwu.teaching.testing.FakeProgressRepository
import edu.sdwu.teaching.testing.FakeReminderScheduler
import edu.sdwu.teaching.testing.FakeSyncScheduler
import edu.sdwu.teaching.testing.MainDispatcherRule
import edu.sdwu.teaching.ui.auth.AuthMode
import edu.sdwu.teaching.ui.auth.AuthViewModel
import edu.sdwu.teaching.ui.lessons.LessonDetailUiState
import edu.sdwu.teaching.ui.lessons.LessonDetailViewModel
import edu.sdwu.teaching.ui.lessons.LessonListUiState
import edu.sdwu.teaching.ui.lessons.LessonListViewModel
import edu.sdwu.teaching.ui.profile.EditProfileViewModel
import edu.sdwu.teaching.ui.profile.ProfileViewModel
import edu.sdwu.teaching.ui.quiz.QuizUiState
import edu.sdwu.teaching.ui.quiz.QuizViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * stateIn(WhileSubscribed) only runs while someone collects. In tests we start a collector
 * in backgroundScope (cancelled automatically at the end of runTest) and then read `.value`.
 */
private fun <T> TestScope.observe(flow: StateFlow<T>): StateFlow<T> {
    // Unconfined: the collector starts right away instead of waiting for the test to suspend.
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    return flow
}

class LessonListViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val lessons = FakeLessonRepository()
    private val progress = FakeProgressRepository()
    private val bookmarks = FakeBookmarkRepository()
    private fun viewModel() = LessonListViewModel(lessons, progress, bookmarks)

    private val StateFlow<LessonListUiState>.success get() = value as LessonListUiState.Success

    @Test
    fun `shows all lessons and their categories`() = runTest {
        val state = observe(viewModel().uiState).success
        assertEquals(8, state.lessons.size)
        assertEquals(listOf("Kotlin", "Compose", "Architecture", "Data"), state.categories)
    }

    @Test
    fun `search and difficulty filter combine`() = runTest {
        val vm = viewModel()
        val state = observe(vm.uiState)
        vm.onQueryChange("compose")
        vm.onDifficultySelected(Difficulty.INTERMEDIATE)
        // "compose" also matches the ViewModel lesson, whose text says "collect it in Compose".
        assertEquals(listOf("compose-state", "navigation-compose", "viewmodel-lifecycle"), state.success.lessons.map { it.id })

        vm.onDifficultySelected(Difficulty.INTERMEDIATE) // tapping again clears it
        assertNull(state.success.filter.difficulty)
    }

    @Test
    fun `bookmarked only shows bookmarked lessons`() = runTest {
        val vm = viewModel()
        val state = observe(vm.uiState)
        bookmarks.toggle("room-database")
        vm.onBookmarkedOnlyToggle()
        assertEquals(listOf("room-database"), state.success.lessons.map { it.id })
    }

    @Test
    fun `failed refresh shows an offline message and keeps lessons`() = runTest {
        lessons.refreshResult = Result.failure(IOException("no network"))
        val vm = viewModel()
        val state = observe(vm.uiState)
        vm.refresh()
        assertTrue(state.success.message!!.startsWith("Offline"))
        assertFalse(state.success.isRefreshing)
        assertEquals(8, state.success.lessons.size)
        assertEquals(0, progress.syncCalls)

        vm.onMessageShown()
        assertNull(state.success.message)
    }

    @Test
    fun `successful refresh also syncs progress`() = runTest {
        viewModel().refresh()
        assertEquals(1, lessons.refreshCalls)
        assertEquals(1, progress.syncCalls)
    }
}

class LessonDetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val progress = FakeProgressRepository()
    private val bookmarks = FakeBookmarkRepository()
    private val notes = FakeNoteRepository()

    private fun viewModel(lessonId: String) = LessonDetailViewModel(
        lessonId,
        FakeLessonRepository(),
        progress,
        notes,
        bookmarks,
        FakePreferencesRepository(),
    )

    @Test
    fun `opening a lesson marks it as started`() = runTest {
        val state = observe(viewModel("compose-state").uiState)
        assertEquals(listOf("compose-state"), progress.opened)
        val success = state.value as LessonDetailUiState.Success
        assertEquals(3, success.quizQuestionCount)
        // Relative /media paths are resolved against the configured server address.
        assertEquals(
            "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4",
            success.videoUrl,
        )
    }

    @Test
    fun `bookmark and notes appear in the state`() = runTest {
        val vm = viewModel("compose-state")
        val state = observe(vm.uiState)
        vm.toggleBookmark()
        vm.saveNote("  hoist state up  ")
        vm.saveNote("   ") // blank notes are ignored

        val success = state.value as LessonDetailUiState.Success
        assertTrue(success.isBookmarked)
        assertEquals(listOf("hoist state up"), success.notes.map { it.text })

        vm.saveNote("edited", noteId = success.notes.single().id)
        assertEquals("edited", (state.value as LessonDetailUiState.Success).notes.single().text)
    }

    @Test
    fun `unknown lesson is NotFound`() = runTest {
        assertEquals(LessonDetailUiState.NotFound, observe(viewModel("nope").uiState).value)
    }
}

class QuizViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `answering all questions and submitting records the score`() = runTest {
        val progress = FakeProgressRepository()
        val vm = QuizViewModel("kotlin-basics", FakeLessonRepository(), progress)

        // Turbine: assert each emitted state in order.
        vm.uiState.test {
            // A StateFlow only replays its latest value; with an unconfined Main dispatcher the
            // questions are already loaded when we subscribe, so the first item is Answering.
            var state = awaitItem() as QuizUiState.Answering
            repeat(state.questions.size) {
                vm.selectAnswer(state.current.id, state.current.correctIndex)
                state = awaitItem() as QuizUiState.Answering
                if (!state.isLast) {
                    vm.next()
                    state = awaitItem() as QuizUiState.Answering
                }
            }
            vm.submit()
            val finished = awaitItem() as QuizUiState.Finished
            assertEquals(3, finished.result.score)
            assertTrue(finished.result.passed)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(Triple("kotlin-basics", 3, 3)), progress.quizResults)
    }
}

class AuthViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val auth = FakeAuthRepository(SessionState.SignedOut)
    private val sync = FakeSyncScheduler()
    private fun viewModel() = AuthViewModel(auth, sync, FakePreferencesRepository())

    @Test
    fun `invalid email is rejected before calling the server`() {
        val vm = viewModel()
        vm.onEmailChange("not-an-email")
        vm.onPasswordChange("secret1")
        vm.submit()
        assertEquals("Enter a valid email address", vm.uiState.value.error)
        assertEquals(0, auth.loginCalls)
    }

    @Test
    fun `register enforces the password rule`() {
        val vm = viewModel()
        vm.toggleMode()
        assertEquals(AuthMode.REGISTER, vm.uiState.value.mode)
        vm.onDisplayNameChange("New")
        vm.onEmailChange("new@example.com")
        vm.onPasswordChange("abcdefg")
        vm.submit()
        assertEquals("Use at least one letter and one digit", vm.uiState.value.error)
    }

    @Test
    fun `server error is shown and loading stops`() {
        auth.loginResult = Result.failure(IOException("down"))
        val vm = viewModel()
        vm.onEmailChange("student@example.com")
        vm.onPasswordChange("student123")
        vm.submit()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.error!!.startsWith("Can't reach the server"))
    }

    @Test
    fun `successful login asks for a background sync`() {
        val vm = viewModel()
        vm.onEmailChange("student@example.com")
        vm.onPasswordChange("student123")
        vm.submit()
        assertEquals(1, sync.requests)
        assertTrue(auth.session.value is SessionState.SignedIn)
    }
}

class ProfileViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `reminder settings are saved and test reminder is sent`() = runTest {
        val prefs = FakePreferencesRepository()
        val reminders = FakeReminderScheduler()
        val vm = ProfileViewModel(prefs, FakeAuthRepository(), FakeLessonRepository(), FakeProgressRepository(), reminders)

        vm.onReminderChange(enabled = true, hour = 8)
        assertTrue(prefs.preferences.value.reminderEnabled)
        assertEquals(8, prefs.preferences.value.reminderHour)

        vm.onReminderChange(enabled = false) // keeps the hour
        assertEquals(8, prefs.preferences.value.reminderHour)

        vm.sendTestReminder()
        assertEquals(1, reminders.testReminders)
    }

    @Test
    fun `sync now reports the result`() = runTest {
        val lessons = FakeLessonRepository().apply { refreshResult = Result.failure(IOException()) }
        val vm = ProfileViewModel(FakePreferencesRepository(), FakeAuthRepository(), lessons, FakeProgressRepository(), FakeReminderScheduler())
        val state = observe(vm.uiState)
        vm.syncNow()
        assertTrue(state.value.syncMessage!!.startsWith("Can't reach the server"))
        assertFalse(state.value.isSyncing)
    }
}

class EditProfileViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `form starts with the current user and validates the avatar URL`() {
        val auth = FakeAuthRepository()
        val vm = EditProfileViewModel(auth)
        assertEquals("Demo Student", vm.uiState.value.displayName)

        vm.onAvatarUrlChange("ftp://nope")
        vm.save()
        assertNotNull(vm.uiState.value.avatarError)
        assertNull(auth.lastUpdate)
    }

    @Test
    fun `saving sends trimmed values and blank avatar as null`() {
        val auth = FakeAuthRepository()
        val vm = EditProfileViewModel(auth)
        vm.onDisplayNameChange("Renamed")
        vm.onAvatarUrlChange("   ")
        vm.save()
        assertEquals("Renamed" to null, auth.lastUpdate)
        assertTrue(vm.uiState.value.saved)
    }
}
