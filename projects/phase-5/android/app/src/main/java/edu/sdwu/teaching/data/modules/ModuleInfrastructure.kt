package edu.sdwu.teaching.data.modules

import edu.sdwu.teaching.data.local.ModuleDao
import edu.sdwu.teaching.data.local.ModuleDataEntity
import edu.sdwu.teaching.data.local.ModuleStateEntity
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.repository.AuthRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.framework.FunctionHost
import edu.sdwu.teaching.framework.LessonContent
import edu.sdwu.teaching.framework.ModuleSource
import edu.sdwu.teaching.framework.ModuleStateStore
import edu.sdwu.teaching.framework.ModuleStorage
import edu.sdwu.teaching.framework.StoredModuleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// The app side of the framework: Room persistence for the registry and module storage, and the
// FunctionHost that modules talk to. Modules only ever see the framework interfaces.

/** Persists registry state (enabled flag, first-seen date) in the `modules` table. */
class RoomModuleStateStore @Inject constructor(private val dao: ModuleDao) : ModuleStateStore {
    override suspend fun loadAll(): List<StoredModuleState> = dao.loadStates().map {
        StoredModuleState(
            id = it.id,
            name = it.name,
            version = it.version,
            source = ModuleSource.entries.firstOrNull { s -> s.name == it.source } ?: ModuleSource.STUDENT,
            enabled = it.enabled,
            firstSeenAt = it.firstSeenAt,
            lastSeenAt = it.lastSeenAt,
        )
    }

    override suspend fun save(state: StoredModuleState) = dao.saveState(
        ModuleStateEntity(state.id, state.name, state.version, state.source.name, state.enabled, state.firstSeenAt, state.lastSeenAt),
    )
}

/** One module's private key-value storage, rows in `module_data` scoped by module id. */
class RoomModuleStorage(private val moduleId: String, private val dao: ModuleDao) : ModuleStorage {
    override fun observe(key: String): Flow<String?> = dao.observeValue(moduleId, key)
    override suspend fun get(key: String): String? = dao.getValue(moduleId, key)
    override suspend fun put(key: String, value: String?) {
        if (value == null) dao.deleteValue(moduleId, key) else dao.putValue(ModuleDataEntity(moduleId, key, value))
    }
}

/** The UI actions a module may trigger. Implemented by the navigation root. */
interface HostNavigator {
    fun openLesson(lessonId: String)
    fun openFunction(functionId: String)
    fun showMessage(text: String)
}

/** Builds one [FunctionHost] per module. Injected into MainActivity and passed down. */
@Singleton
class ModuleHostFactory @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val authRepository: AuthRepository,
    private val moduleDao: ModuleDao,
) {
    fun create(moduleId: String, navigator: HostNavigator): FunctionHost = object : FunctionHost {
        override val moduleId = moduleId
        override val lessons: Flow<List<LessonContent>> =
            lessonRepository.observeLessons().map { list -> list.map { it.toLessonContent() } }
        override val studentName: Flow<String?> =
            authRepository.session.map { (it as? SessionState.SignedIn)?.user?.displayName }
        override val storage: ModuleStorage = RoomModuleStorage(moduleId, moduleDao)
        override fun openLesson(lessonId: String) = navigator.openLesson(lessonId)
        override fun openFunction(functionId: String) = navigator.openFunction(functionId)
        override fun showMessage(text: String) = navigator.showMessage(text)
    }
}

/** Domain lesson -> the read-only view modules get. */
fun Lesson.toLessonContent() = LessonContent(
    id = id,
    title = title,
    summary = summary,
    category = category,
    difficulty = difficulty.name,
    body = content,
)
