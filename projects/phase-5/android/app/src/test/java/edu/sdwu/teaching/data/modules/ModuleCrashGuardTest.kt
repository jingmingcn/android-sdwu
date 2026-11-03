package edu.sdwu.teaching.data.modules

import edu.sdwu.teaching.framework.BuiltInModule
import edu.sdwu.teaching.framework.ModuleDiscovery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleCrashGuardTest {

    private val packages = mapOf(
        "calc" to "edu.sdwu.teaching.modules.calc",
        "calculator" to "edu.sdwu.teaching.modules.calculator",
    )

    private fun crashAt(vararg classNames: String) = RuntimeException("boom").apply {
        stackTrace = classNames.map { StackTraceElement(it, "run", "File.kt", 1) }.toTypedArray()
    }

    @Test
    fun `crash inside a module package is attributed to that module`() {
        val error = crashAt("androidx.compose.runtime.Composer", "edu.sdwu.teaching.modules.calculator.CalculatorScreenKt")
        assertEquals("calculator", findCrashingModule(error, packages)) // not "calc": prefix needs the dot
    }

    @Test
    fun `module code found in the cause chain`() {
        val cause = crashAt("edu.sdwu.teaching.modules.calc.Engine")
        val wrapper = IllegalStateException("wrapped", cause).apply { stackTrace = crashAt("android.os.Looper").stackTrace }
        assertEquals("calc", findCrashingModule(wrapper, packages))
    }

    @Test
    fun `crash in app code is not blamed on a module`() {
        assertNull(findCrashingModule(crashAt("edu.sdwu.teaching.ui.home.HomeScreenKt"), packages))
    }
}

class ModuleDiscoveryTest {

    /** The app's unit-test classpath contains every module in modules/, so discovery must find them all. */
    @Test
    fun `all modules in the build are discovered`() {
        val result = ModuleDiscovery.discover()
        assertEquals(emptyList<Any>(), result.failures)
        // containsAll, not equals: this test must keep passing when students add their own modules.
        val ids = result.providers.map { it.info.id }
        assertTrue(ids.toString(), ids.containsAll(listOf("calculator", "code-snippets", "lesson-formatter", "study-timer")))
        assertEquals(ids.sorted(), ids) // stable order
        assertEquals(setOf("lesson-formatter", "study-timer"), result.providers.filter { it is BuiltInModule }.map { it.info.id }.toSet())
    }
}
