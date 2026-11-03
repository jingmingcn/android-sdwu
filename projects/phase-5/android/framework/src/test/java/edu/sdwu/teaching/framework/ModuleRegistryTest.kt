package edu.sdwu.teaching.framework

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class MapStore : ModuleStateStore {
    val rows = mutableMapOf<String, StoredModuleState>()
    override suspend fun loadAll() = rows.values.toList()
    override suspend fun save(state: StoredModuleState) {
        rows[state.id] = state
    }
}

private class TestFunction(override val id: String) : CustomFunction {
    override val title = id
    override val description = ""
    override val icon = Icons.Filled.Star

    @Composable
    override fun Content(host: FunctionHost) = Unit
}

private class TestRenderer(override val id: String, override val priority: Int, private val accepts: Boolean) : LessonRenderer {
    override val name = id
    override fun canRender(lesson: LessonContent) = accepts

    @Composable
    override fun Render(lesson: LessonContent, modifier: Modifier) = Unit
}

private fun module(
    id: String,
    functionIds: List<String> = listOf("$id-fn"),
    renderers: List<LessonRenderer> = emptyList(),
    minVersion: Int = FRAMEWORK_VERSION,
) = object : ModuleProvider {
    override val info = ModuleInfo(id, "Module $id", "test", "Tester", "1.0.0", Icons.Filled.Star, minFrameworkVersion = minVersion)
    override val functions = functionIds.map { TestFunction(it) }
    override val lessonRenderers = renderers
}

class ModuleRegistryTest {
    private val store = MapStore()
    private var now = 100L
    private val registry = DefaultModuleRegistry(store) { now }

    @Test
    fun `valid modules are registered, enabled and persisted`() = runTest {
        assertTrue(registry.register(module("timer"), ModuleSource.BUILT_IN))
        val registered = registry.modules.value.single()
        assertTrue(registered.enabled)
        assertEquals(ModuleSource.BUILT_IN, registered.source)
        assertEquals(100L, store.rows["timer"]!!.firstSeenAt)
        assertEquals(listOf("timer-fn"), registry.enabledFunctions.first().map { it.item.id })
    }

    @Test
    fun `invalid modules are rejected with a readable problem`() = runTest {
        registry.register(module("timer"))
        assertFalse(registry.register(module("timer", functionIds = listOf("other-fn")))) // duplicate module id
        assertFalse(registry.register(module("Bad Id")))
        assertFalse(registry.register(module("dup", functionIds = listOf("same-fn", "same-fn"))))
        assertFalse(registry.register(module("steal", functionIds = listOf("timer-fn")))) // taken by "timer"
        assertFalse(registry.register(module("empty", functionIds = emptyList())))
        assertFalse(registry.register(module("future", minVersion = FRAMEWORK_VERSION + 1)))

        assertEquals(1, registry.modules.value.size)
        val problems = registry.problems.value.associate { it.moduleId to it.message }
        assertTrue(problems.getValue("timer").contains("already registered"))
        assertTrue(problems.getValue("Bad Id").contains("lowercase"))
        assertTrue(problems.getValue("dup").contains("used twice"))
        assertTrue(problems.getValue("steal").contains("another module"))
        assertTrue(problems.getValue("empty").contains("contributes nothing"))
        assertTrue(problems.getValue("future").contains("framework v2"))
    }

    @Test
    fun `disabled state survives a restart`() = runTest {
        registry.register(module("timer"))
        registry.setEnabled("timer", false)
        assertTrue(registry.enabledFunctions.first().isEmpty())

        // "Restart": a new registry over the same store.
        now = 999
        val restarted = DefaultModuleRegistry(store) { now }
        restarted.register(module("timer"))
        val module = restarted.modules.value.single()
        assertFalse(module.enabled)
        assertEquals(100L, module.firstSeenAt) // first-seen date is kept
        assertEquals(999L, store.rows["timer"]!!.lastSeenAt)
    }

    @Test
    fun `renderer with the highest priority that accepts the lesson wins`() = runTest {
        registry.register(
            module(
                "render",
                functionIds = emptyList(),
                renderers = listOf(
                    TestRenderer("low", priority = 1, accepts = true),
                    TestRenderer("high-no", priority = 9, accepts = false),
                    TestRenderer("high", priority = 5, accepts = true),
                ),
            ),
        )
        val chosen = pickRenderer(registry.enabledRenderers.first(), SampleLessons.first())
        assertEquals("high", chosen?.item?.id)

        registry.setEnabled("render", false)
        assertEquals(null, pickRenderer(registry.enabledRenderers.first(), SampleLessons.first()))
    }

    @Test
    fun `contribution summary reads naturally`() {
        assertEquals("2 functions", module("x", functionIds = listOf("a-fn", "b-fn")).contributionSummary())
    }
}
