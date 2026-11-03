package edu.sdwu.teaching.data.repository

import dagger.Lazy
import edu.sdwu.teaching.data.local.LessonDao
import edu.sdwu.teaching.data.local.LessonEntity
import edu.sdwu.teaching.data.local.ProgressDao
import edu.sdwu.teaching.data.local.ProgressEntity
import edu.sdwu.teaching.data.local.QuizQuestionEntity
import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.data.local.toEntity
import edu.sdwu.teaching.data.prefs.SessionDataSource
import edu.sdwu.teaching.data.remote.TeachingApi
import edu.sdwu.teaching.data.remote.dto.LessonDto
import edu.sdwu.teaching.data.remote.dto.LoginRequestDto
import edu.sdwu.teaching.data.remote.dto.ProgressDto
import edu.sdwu.teaching.data.remote.dto.QuizQuestionDto
import edu.sdwu.teaching.data.remote.dto.RegisterRequestDto
import edu.sdwu.teaching.data.remote.dto.TokenResponseDto
import edu.sdwu.teaching.data.remote.dto.UserDto
import edu.sdwu.teaching.domain.model.ProgressStatus
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.model.User
import edu.sdwu.teaching.testing.DemoUser
import edu.sdwu.teaching.testing.FakeAuthRepository
import edu.sdwu.teaching.testing.FakeSyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

// ---------- fakes for the data layer ----------

private class FakeProgressDao : ProgressDao {
    val rows = MutableStateFlow<Map<String, ProgressEntity>>(emptyMap())
    override fun observeAll(): Flow<List<ProgressEntity>> = rows.map { it.values.toList() }
    override fun observe(lessonId: String): Flow<ProgressEntity?> = rows.map { it[lessonId] }
    override suspend fun get(lessonId: String) = rows.value[lessonId]
    override suspend fun pending() = rows.value.values.filter { it.pendingSync }
    override suspend fun upsert(row: ProgressEntity) = rows.update { it + (row.lessonId to row) }
    override suspend fun upsertAll(rows: List<ProgressEntity>) = rows.forEach { upsert(it) }
    override suspend fun deleteAll() = rows.update { emptyMap() }
}

private class FakeSession(token: String?) : SessionDataSource {
    private val state = MutableStateFlow<SessionState>(
        if (token != null) SessionState.SignedIn(DemoUser, token) else SessionState.Guest,
    )
    override val session: Flow<SessionState> = state
    override suspend fun token() = (state.value as? SessionState.SignedIn)?.token
    override suspend fun saveSignedIn(user: User, token: String) { state.value = SessionState.SignedIn(user, token) }
    override suspend fun updateUser(user: User) = Unit
    override suspend fun saveGuest() { state.value = SessionState.Guest }
    override suspend fun clear() { state.value = SessionState.SignedOut }
}

/** Only the endpoints a test needs are implemented; the rest fail loudly. */
private open class FakeApi : TeachingApi {
    val pushed = mutableListOf<List<ProgressDto>>()
    var syncResponse: (List<ProgressDto>) -> List<ProgressDto> = { it }
    var lessonsResponse: () -> List<LessonDto> = { error("not stubbed") }
    var quizzesResponse: () -> List<QuizQuestionDto> = { emptyList() }

    override suspend fun syncProgress(items: List<ProgressDto>): List<ProgressDto> {
        pushed += items
        return syncResponse(items)
    }
    override suspend fun lessons() = lessonsResponse()
    override suspend fun quizzes() = quizzesResponse()
    override suspend fun login(body: LoginRequestDto): TokenResponseDto = error("unused")
    override suspend fun register(body: RegisterRequestDto): TokenResponseDto = error("unused")
    override suspend fun updateMe(body: JsonObject): UserDto = error("unused")
}

private fun http(code: Int) = HttpException(Response.error<Any>(code, "{}".toResponseBody()))

// ---------- tests ----------

class OfflineFirstProgressRepositoryTest {
    private val dao = FakeProgressDao()
    private val api = FakeApi()
    private val scheduler = FakeSyncScheduler()
    private val auth = FakeAuthRepository()
    private var now = 1_000L

    private fun repository(token: String? = "token") = OfflineFirstProgressRepository(
        dao = dao,
        api = api,
        session = FakeSession(token),
        syncScheduler = scheduler,
        authRepository = Lazy { auth },
        clock = { now },
    )

    @Test
    fun `local writes are saved immediately as pending and schedule a sync`() = runTest {
        val repo = repository()
        repo.markOpened("l1")
        now = 2_000
        repo.recordQuizResult("l1", score = 3, total = 3)

        val row = dao.get("l1")!!
        assertEquals(ProgressStatus.COMPLETED.name, row.status) // passing a quiz completes the lesson
        assertEquals(3, row.quizBestScore)
        assertEquals(2_000, row.updatedAt)
        assertTrue(row.pendingSync)
        assertEquals(2, scheduler.requests)
    }

    @Test
    fun `a lower quiz score never replaces the best score`() = runTest {
        val repo = repository()
        repo.recordQuizResult("l1", 3, 3)
        repo.recordQuizResult("l1", 1, 3)
        assertEquals(3, dao.get("l1")!!.quizBestScore)
    }

    @Test
    fun `guests never call the network`() = runTest {
        val repo = repository(token = null)
        repo.setCompleted("l1", true)
        assertTrue(repo.sync().isSuccess)
        assertTrue(api.pushed.isEmpty())
        assertEquals(0, scheduler.requests)
        assertTrue(dao.get("l1")!!.pendingSync) // kept for upload after sign-in
    }

    @Test
    fun `sync pushes pending rows and stores the merged result`() = runTest {
        val repo = repository()
        repo.setCompleted("l1", true)
        api.syncResponse = { pushed -> pushed + ProgressDto("l2", "IN_PROGRESS", updatedAt = 5) }

        assertTrue(repo.sync().isSuccess)
        assertEquals(listOf("l1"), api.pushed.single().map { it.lessonId })
        assertFalse(dao.get("l1")!!.pendingSync)
        assertEquals("IN_PROGRESS", dao.get("l2")!!.status) // pulled from "another device"
    }

    @Test
    fun `expired token signs the user out`() = runTest {
        val repo = repository()
        api.syncResponse = { throw http(401) }
        assertTrue(repo.sync().isFailure)
        assertEquals(1, auth.signOutCalls)
    }

    @Test
    fun `network failure keeps rows pending`() = runTest {
        val repo = repository()
        repo.setCompleted("l1", true)
        api.syncResponse = { throw IOException("offline") }
        assertTrue(repo.sync().isFailure)
        assertTrue(dao.get("l1")!!.pendingSync)
        assertEquals(0, auth.signOutCalls)
    }
}

/** LessonDao is an abstract class (it has a @Transaction method), so the fake extends it. */
private class FakeLessonDao : LessonDao() {
    val lessons = MutableStateFlow(SeedData.lessons.map { it.toEntity() })
    val questions = MutableStateFlow<List<QuizQuestionEntity>>(emptyList())

    override fun observeAll() = lessons
    override fun observeById(id: String) = lessons.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun count() = lessons.value.size
    override suspend fun upsertAll(lessons: List<LessonEntity>) =
        this.lessons.update { old -> (old.associateBy { it.id } + lessons.associateBy { it.id }).values.toList() }
    override suspend fun deleteAllExcept(keepIds: List<String>) = lessons.update { list -> list.filter { it.id in keepIds } }
    override fun observeQuiz(lessonId: String) = questions.map { list -> list.filter { it.lessonId == lessonId } }
    override suspend fun quizQuestionCount() = questions.value.size
    override suspend fun upsertQuestions(questions: List<QuizQuestionEntity>) = this.questions.update { it + questions }
    override suspend fun deleteAllQuestions() = questions.update { emptyList() }
}

class OfflineFirstLessonRepositoryTest {
    private val dao = FakeLessonDao()
    private val api = FakeApi()
    private val repository = OfflineFirstLessonRepository(dao, api)

    @Test
    fun `refresh failure keeps the cached lessons`() = runTest {
        api.lessonsResponse = { throw IOException("offline") }
        assertTrue(repository.refresh().isFailure)
        assertEquals(8, repository.observeLessons().first().size)
    }

    @Test
    fun `refresh replaces the catalogue, including deletions`() = runTest {
        api.lessonsResponse = {
            listOf(LessonDto("only-one", 1, "Only", "s", "c", "Kotlin", "BEGINNER", 10))
        }
        assertTrue(repository.refresh().isSuccess)
        assertEquals(listOf("only-one"), repository.observeLessons().first().map { it.id })
    }

    @Test
    fun `seeding fills quizzes even when lessons already exist (Phase 1 upgrade)`() = runTest {
        repository.seedIfEmpty()
        assertEquals(SeedData.quizQuestions.size, dao.quizQuestionCount())
    }
}
