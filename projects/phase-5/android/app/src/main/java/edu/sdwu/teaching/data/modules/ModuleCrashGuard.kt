package edu.sdwu.teaching.data.modules

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.sdwu.teaching.framework.ModuleProblem
import edu.sdwu.teaching.framework.ModuleRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps one broken student module from bricking the app.
 *
 * Compose cannot catch an exception thrown while a module's composable runs, so the app
 * crashes. Before it dies, this guard checks the stack trace: if the crash happened inside a
 * module's package, the module id is saved. On the next start the module is disabled and a
 * problem is shown in the Modules tab. The student fixes the bug and switches it back on.
 */
@Singleton
class ModuleCrashGuard @Inject constructor(
    @ApplicationContext context: Context,
    private val registry: ModuleRegistry,
) {
    // SharedPreferences (not DataStore): the process is dying, so the write must be synchronous.
    private val prefs = context.getSharedPreferences("module_crash_guard", Context.MODE_PRIVATE)

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { recordIfModuleCrash(error) }
            previous?.uncaughtException(thread, error) // Let Android show its crash dialog as usual.
        }
    }

    private fun recordIfModuleCrash(error: Throwable) {
        val packages = registry.modules.value.associate { it.info.id to it.provider.javaClass.`package`?.name.orEmpty() }
        val moduleId = findCrashingModule(error, packages) ?: return
        prefs.edit(commit = true) {
            putString(KEY_MODULE, moduleId)
            putString(KEY_MESSAGE, "${error::class.simpleName}: ${error.message}".take(300))
        }
    }

    /** Call after the modules are registered: disables the module that crashed last time. */
    suspend fun applyPendingCrash() {
        val moduleId = prefs.getString(KEY_MODULE, null) ?: return
        val message = prefs.getString(KEY_MESSAGE, null).orEmpty()
        prefs.edit(commit = true) { clear() }
        registry.setEnabled(moduleId, false)
        registry.reportProblem(
            ModuleProblem(moduleId, "Disabled because it crashed the app ($message). Fix it, then switch it back on."),
        )
    }

    private companion object {
        const val KEY_MODULE = "module_id"
        const val KEY_MESSAGE = "message"
    }
}

/**
 * Returns the id of the module whose code appears in the stack trace (including causes), or
 * null if the crash was not in module code. Pure function: unit tested in ModuleCrashGuardTest.
 *
 * @param packagesByModuleId module id -> Kotlin package of its ModuleProvider
 */
fun findCrashingModule(error: Throwable, packagesByModuleId: Map<String, String>): String? {
    val frames = generateSequence(error) { it.cause }.take(10).flatMap { it.stackTrace.asSequence() }
    for (frame in frames) {
        for ((moduleId, pkg) in packagesByModuleId) {
            if (pkg.isNotEmpty() && frame.className.startsWith("$pkg.")) return moduleId
        }
    }
    return null
}
