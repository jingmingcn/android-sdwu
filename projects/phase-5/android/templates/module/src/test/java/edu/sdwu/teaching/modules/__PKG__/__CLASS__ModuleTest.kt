package edu.sdwu.teaching.modules.__PKG__

import edu.sdwu.teaching.framework.DefaultModuleRegistry
import edu.sdwu.teaching.framework.ModuleDiscovery
import edu.sdwu.teaching.framework.ModuleStateStore
import edu.sdwu.teaching.framework.StoredModuleState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/** Run with: ./gradlew :modules:__ID__:testDebugUnitTest */
class __CLASS__ModuleTest {

    /** The framework accepts the module (valid ids, at least one contribution, ...). */
    @Test
    fun `module passes framework validation`() = runTest {
        val store = object : ModuleStateStore {
            override suspend fun loadAll() = emptyList<StoredModuleState>()
            override suspend fun save(state: StoredModuleState) = Unit
        }
        val registry = DefaultModuleRegistry(store)
        assertTrue(registry.problems.value.toString(), registry.register(__CLASS__Module()))
    }

    /** The META-INF/services file names this module, so the app will find it. */
    @Test
    fun `module is discoverable`() {
        assertTrue(ModuleDiscovery.discover().providers.any { it is __CLASS__Module })
    }

    // TODO(student): test your own logic here. Keep calculations in plain Kotlin functions
    // (outside @Composable code) so they are easy to test, like evaluate() in the calculator module.
}
