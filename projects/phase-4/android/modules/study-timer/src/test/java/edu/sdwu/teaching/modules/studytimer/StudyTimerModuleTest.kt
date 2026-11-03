package edu.sdwu.teaching.modules.studytimer

import edu.sdwu.teaching.framework.DefaultModuleRegistry
import edu.sdwu.teaching.framework.ModuleStateStore
import edu.sdwu.teaching.framework.StoredModuleState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyTimerModuleTest {

    @Test
    fun `time is formatted as mm ss`() {
        assertEquals("25:00", formatTime(25 * 60))
        assertEquals("01:05", formatTime(65))
        assertEquals("00:00", formatTime(0))
    }

    /** Every module should have this test: the framework accepts it. */
    @Test
    fun `module passes framework validation`() = runTest {
        val store = object : ModuleStateStore {
            override suspend fun loadAll() = emptyList<StoredModuleState>()
            override suspend fun save(state: StoredModuleState) = Unit
        }
        val registry = DefaultModuleRegistry(store)
        assertTrue(registry.problems.value.toString(), registry.register(StudyTimerModule()))
    }
}
