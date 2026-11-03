package edu.sdwu.teaching.framework

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ModuleSource { BUILT_IN, STUDENT }

/** A module the registry accepted, plus its state. */
data class RegisteredModule(
    val provider: ModuleProvider,
    val source: ModuleSource,
    val enabled: Boolean,
    /** When this module was first seen on this device (epoch millis). */
    val firstSeenAt: Long,
) {
    val info: ModuleInfo get() = provider.info
}

/** Why a module was rejected. Shown in the Modules tab so students can fix their module. */
data class ModuleProblem(val moduleId: String, val message: String)

/** A contribution together with the module it came from. */
data class Contributed<T>(val module: RegisteredModule, val item: T)

/** What the registry remembers between app starts. */
data class StoredModuleState(
    val id: String,
    val name: String,
    val version: String,
    val source: ModuleSource,
    val enabled: Boolean,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
)

/** Persistence for the registry. The app implements it with Room; tests use a map. */
interface ModuleStateStore {
    suspend fun loadAll(): List<StoredModuleState>
    suspend fun save(state: StoredModuleState)
}

/**
 * The central list of modules. In-memory for the code (providers) and backed by a
 * [ModuleStateStore] for state that must survive restarts (enabled flag, first seen date).
 */
interface ModuleRegistry {
    /** Accepted modules in registration order. */
    val modules: StateFlow<List<RegisteredModule>>

    /** Modules that were rejected, with the reason. */
    val problems: StateFlow<List<ModuleProblem>>

    /**
     * Validates and adds a module. Returns false (and records a [ModuleProblem]) when the
     * module is invalid, e.g. a duplicate id or a malformed contribution id.
     */
    suspend fun register(provider: ModuleProvider, source: ModuleSource = ModuleSource.STUDENT): Boolean

    suspend fun setEnabled(moduleId: String, enabled: Boolean)
}

// ----- Convenience views over enabled modules (used by the app's screens) -----

val ModuleRegistry.enabledFunctions: Flow<List<Contributed<CustomFunction>>>
    get() = modules.map { list -> list.filter { it.enabled }.flatMap { m -> m.provider.functions.map { Contributed(m, it) } } }

val ModuleRegistry.enabledTools: Flow<List<Contributed<ToolEntry>>>
    get() = modules.map { list -> list.filter { it.enabled }.flatMap { m -> m.provider.tools.map { Contributed(m, it) } } }

val ModuleRegistry.enabledRenderers: Flow<List<Contributed<LessonRenderer>>>
    get() = modules.map { list -> list.filter { it.enabled }.flatMap { m -> m.provider.lessonRenderers.map { Contributed(m, it) } } }

fun ModuleRegistry.findFunction(functionId: String): Contributed<CustomFunction>? =
    modules.value.firstNotNullOfOrNull { m -> m.provider.functions.firstOrNull { it.id == functionId }?.let { Contributed(m, it) } }

/**
 * Picks the renderer for a lesson: the highest priority among those that say [LessonRenderer.canRender];
 * on a tie, the one registered first. A renderer that throws in canRender is skipped.
 */
fun pickRenderer(renderers: List<Contributed<LessonRenderer>>, lesson: LessonContent): Contributed<LessonRenderer>? =
    renderers
        .filter { runCatching { it.item.canRender(lesson) }.getOrDefault(false) }
        .maxByOrNull { it.item.priority } // maxBy keeps the first of equal elements

/** Number of contributions, e.g. "2 functions · 1 tool". */
fun ModuleProvider.contributionSummary(): String = listOfNotNull(
    functions.size.takeIf { it > 0 }?.let { "$it ${if (it == 1) "function" else "functions"}" },
    tools.size.takeIf { it > 0 }?.let { "$it ${if (it == 1) "tool" else "tools"}" },
    lessonRenderers.size.takeIf { it > 0 }?.let { "$it ${if (it == 1) "renderer" else "renderers"}" },
).joinToString(" · ").ifEmpty { "No contributions" }

/** The framework's registry implementation. Pure Kotlin: no Android, fully unit tested. */
class DefaultModuleRegistry(
    private val store: ModuleStateStore,
    private val clock: () -> Long = System::currentTimeMillis,
) : ModuleRegistry {

    private val _modules = MutableStateFlow<List<RegisteredModule>>(emptyList())
    override val modules: StateFlow<List<RegisteredModule>> = _modules.asStateFlow()

    private val _problems = MutableStateFlow<List<ModuleProblem>>(emptyList())
    override val problems: StateFlow<List<ModuleProblem>> = _problems.asStateFlow()

    private val mutex = Mutex()
    private var stored: Map<String, StoredModuleState>? = null

    override suspend fun register(provider: ModuleProvider, source: ModuleSource): Boolean = mutex.withLock {
        val info = runCatching { provider.info }.getOrElse {
            return reject("?", "info threw ${it::class.simpleName}: ${it.message}")
        }
        validate(provider)?.let { return reject(info.id, it) }

        val states = stored ?: store.loadAll().associateBy { it.id }.also { stored = it }
        val previous = states[info.id]
        val now = clock()
        val state = StoredModuleState(
            id = info.id,
            name = info.name,
            version = info.version,
            source = source,
            enabled = previous?.enabled ?: true, // new modules start enabled
            firstSeenAt = previous?.firstSeenAt ?: now,
            lastSeenAt = now,
        )
        store.save(state)
        stored = states + (info.id to state)
        _modules.update { it + RegisteredModule(provider, source, state.enabled, state.firstSeenAt) }
        true
    }

    override suspend fun setEnabled(moduleId: String, enabled: Boolean) = mutex.withLock {
        val module = _modules.value.firstOrNull { it.info.id == moduleId } ?: return@withLock
        val state = stored?.get(moduleId) ?: return@withLock
        val updated = state.copy(enabled = enabled)
        store.save(updated)
        stored = stored.orEmpty() + (moduleId to updated)
        _modules.update { list -> list.map { if (it === module) it.copy(enabled = enabled) else it } }
    }

    /** Returns a human-readable problem, or null when the module is fine. */
    private fun validate(provider: ModuleProvider): String? {
        val info = provider.info
        val existing = _modules.value
        val contributionIds = runCatching {
            provider.functions.map { it.id } + provider.tools.map { it.id } + provider.lessonRenderers.map { it.id }
        }.getOrElse { return "listing contributions threw ${it::class.simpleName}: ${it.message}" }
        val takenIds = existing.flatMap { m ->
            m.provider.functions.map { it.id } + m.provider.tools.map { it.id } + m.provider.lessonRenderers.map { it.id }
        }.toSet()

        return when {
            !VALID_ID.matches(info.id) -> "Module id \"${info.id}\" must be 3-40 lowercase letters, digits or dashes"
            info.name.isBlank() -> "Module name is empty"
            existing.any { it.info.id == info.id } -> "A module with id \"${info.id}\" is already registered"
            info.minFrameworkVersion > FRAMEWORK_VERSION ->
                "Needs framework v${info.minFrameworkVersion}, this app has v$FRAMEWORK_VERSION"
            contributionIds.isEmpty() -> "Module contributes nothing (add a function, tool or renderer)"
            else -> contributionIds.firstOrNull { !VALID_ID.matches(it) }
                ?.let { "Contribution id \"$it\" must be 3-40 lowercase letters, digits or dashes" }
                ?: contributionIds.groupBy { it }.entries.firstOrNull { it.value.size > 1 }
                    ?.let { "Contribution id \"${it.key}\" is used twice in this module" }
                ?: contributionIds.firstOrNull { it in takenIds }
                    ?.let { "Contribution id \"$it\" is already used by another module" }
        }
    }

    private fun reject(moduleId: String, message: String): Boolean {
        _problems.update { it + ModuleProblem(moduleId, message) }
        return false
    }
}
