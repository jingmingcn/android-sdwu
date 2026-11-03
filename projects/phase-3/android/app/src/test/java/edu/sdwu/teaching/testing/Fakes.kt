package edu.sdwu.teaching.testing

import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.data.work.ReminderScheduler
import edu.sdwu.teaching.data.work.SyncScheduler
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.model.LessonProgress
import edu.sdwu.teaching.domain.model.Note
import edu.sdwu.teaching.domain.model.NoteWithLesson
import edu.sdwu.teaching.domain.model.ProgressStatus
import edu.sdwu.teaching.domain.model.QuizQuestion
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.model.ThemeMode
import edu.sdwu.teaching.domain.model.User
import edu.sdwu.teaching.domain.model.UserPreferences
import edu.sdwu.teaching.domain.repository.AuthRepository
import edu.sdwu.teaching.domain.repository.BookmarkRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.NoteRepository
import edu.sdwu.teaching.domain.repository.PreferencesRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// In-memory fakes of every domain interface. ViewModel tests use these instead of Room,
// Retrofit or DataStore. This is the payoff of depending on interfaces, not implementations.

class FakeLessonRepository(
    lessons: List<Lesson> = SeedData.lessons,
    private val questions: List<QuizQuestion> = SeedData.quizQuestions,
) : LessonRepository {
    val lessons = MutableStateFlow(lessons)
    var refreshResult: Result<Unit> = Result.success(Unit)
    var refreshCalls = 0

    override fun observeLessons(): Flow<List<Lesson>> = lessons
    override fun observeLesson(id: String): Flow<Lesson?> = lessons.map { list -> list.firstOrNull { it.id == id } }
    override fun observeQuiz(lessonId: String): Flow<List<QuizQuestion>> =
        MutableStateFlow(questions.filter { it.lessonId == lessonId })

    override suspend fun seedIfEmpty() = Unit
    override suspend fun refresh(): Result<Unit> {
        refreshCalls++
        return refreshResult
    }
}

class FakeProgressRepository : ProgressRepository {
    val progress = MutableStateFlow<Map<String, LessonProgress>>(emptyMap())
    val opened = mutableListOf<String>()
    val quizResults = mutableListOf<Triple<String, Int, Int>>()
    var syncCalls = 0

    override fun observeAll(): Flow<Map<String, LessonProgress>> = progress
    override fun observe(lessonId: String): Flow<LessonProgress?> = progress.map { it[lessonId] }

    override suspend fun markOpened(lessonId: String) {
        opened += lessonId
        progress.update { it + (lessonId to (it[lessonId] ?: LessonProgress(lessonId, ProgressStatus.IN_PROGRESS, updatedAt = 1))) }
    }

    override suspend fun setCompleted(lessonId: String, completed: Boolean) {
        val status = if (completed) ProgressStatus.COMPLETED else ProgressStatus.IN_PROGRESS
        progress.update { it + (lessonId to LessonProgress(lessonId, status, updatedAt = 2)) }
    }

    override suspend fun recordQuizResult(lessonId: String, score: Int, total: Int) {
        quizResults += Triple(lessonId, score, total)
    }

    override suspend fun sync(): Result<Unit> {
        syncCalls++
        return Result.success(Unit)
    }

    override suspend fun clearLocal() = progress.update { emptyMap() }
}

class FakeBookmarkRepository : BookmarkRepository {
    val ids = MutableStateFlow<Set<String>>(emptySet())
    override fun observeBookmarkedIds(): Flow<Set<String>> = ids
    override suspend fun toggle(lessonId: String) = ids.update { if (lessonId in it) it - lessonId else it + lessonId }
}

class FakeNoteRepository : NoteRepository {
    private val notes = MutableStateFlow<List<Note>>(emptyList())
    private var nextId = 1L

    override fun observeNotes(lessonId: String): Flow<List<Note>> = notes.map { list -> list.filter { it.lessonId == lessonId } }
    override fun observeAllNotes(): Flow<List<NoteWithLesson>> = notes.map { list -> list.map { NoteWithLesson(it, it.lessonId) } }

    override suspend fun saveNote(lessonId: String, text: String, id: Long?) {
        if (text.isBlank()) return
        notes.update { list ->
            if (id == null) list + Note(nextId++, lessonId, text.trim(), 0, 0)
            else list.map { if (it.id == id) it.copy(text = text.trim()) else it }
        }
    }

    override suspend fun deleteNote(id: Long) = notes.update { list -> list.filterNot { it.id == id } }
}

val DemoUser = User(id = 1, email = "student@example.com", displayName = "Demo Student", isInstructor = false)

class FakeAuthRepository(initial: SessionState = SessionState.SignedIn(DemoUser, "token")) : AuthRepository {
    override val session = MutableStateFlow(initial)
    var loginResult: Result<User> = Result.success(DemoUser)
    var updateResult: Result<User>? = null
    var loginCalls = 0
    var signOutCalls = 0
    var lastUpdate: Pair<String, String?>? = null

    override suspend fun login(email: String, password: String): Result<User> {
        loginCalls++
        loginResult.onSuccess { session.value = SessionState.SignedIn(it, "token") }
        return loginResult
    }

    override suspend fun register(email: String, password: String, displayName: String) = login(email, password)
    override suspend fun continueAsGuest() { session.value = SessionState.Guest }

    override suspend fun updateProfile(displayName: String, avatarUrl: String?): Result<User> {
        lastUpdate = displayName to avatarUrl
        return updateResult ?: Result.success(DemoUser.copy(displayName = displayName, avatarUrl = avatarUrl))
    }

    override suspend fun signOut() {
        signOutCalls++
        session.value = SessionState.SignedOut
    }

    override suspend fun onTokenExpired() = signOut()
}

class FakePreferencesRepository : PreferencesRepository {
    override val preferences = MutableStateFlow(UserPreferences(serverUrl = "http://10.0.2.2:8000/"))
    override suspend fun setThemeMode(mode: ThemeMode) = preferences.update { it.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) = preferences.update { it.copy(dynamicColor = enabled) }
    override suspend fun setReminder(enabled: Boolean, hour: Int) =
        preferences.update { it.copy(reminderEnabled = enabled, reminderHour = hour) }

    override suspend fun setServerUrl(url: String): Boolean {
        preferences.update { it.copy(serverUrl = url) }
        return true
    }
}

class FakeSyncScheduler : SyncScheduler {
    var requests = 0
    override fun requestSync() { requests++ }
    override fun schedulePeriodicSync() = Unit
}

class FakeReminderScheduler : ReminderScheduler {
    var testReminders = 0
    override fun schedule(enabled: Boolean, hour: Int) = Unit
    override fun sendTestReminder() { testReminders++ }
}
