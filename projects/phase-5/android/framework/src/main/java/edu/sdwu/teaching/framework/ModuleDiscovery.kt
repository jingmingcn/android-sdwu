package edu.sdwu.teaching.framework

import java.util.ServiceConfigurationError
import java.util.ServiceLoader

/**
 * Marker for modules shipped by course staff. Student modules simply implement [ModuleProvider];
 * the app shows "Built-in" or "Student" based on this marker.
 */
interface BuiltInModule : ModuleProvider

/** What [ModuleDiscovery] found: working providers, and entries that could not be loaded. */
data class DiscoveryResult(
    val providers: List<ModuleProvider>,
    val failures: List<ModuleProblem>,
)

/**
 * Finds every module on the classpath, without a central list.
 *
 * How: each module ships one text file,
 *     src/main/resources/META-INF/services/edu.sdwu.teaching.framework.ModuleProvider
 * containing the fully-qualified class name of its ModuleProvider. Gradle merges these files into
 * the APK, and java.util.ServiceLoader instantiates each listed class (it needs a public
 * no-argument constructor). This is the same mechanism the JDK uses for JDBC drivers.
 */
object ModuleDiscovery {

    fun discover(classLoader: ClassLoader? = ModuleProvider::class.java.classLoader): DiscoveryResult {
        val providers = mutableListOf<ModuleProvider>()
        val failures = mutableListOf<ModuleProblem>()
        val iterator = ServiceLoader.load(ModuleProvider::class.java, classLoader).iterator()
        // Iterate by hand: one broken entry (typo in the class name, missing no-arg constructor,
        // exception in the constructor) must not hide the other modules.
        while (true) {
            try {
                if (!iterator.hasNext()) break
                providers += iterator.next()
            } catch (e: ServiceConfigurationError) {
                failures += ModuleProblem(moduleId = "?", message = "Could not load a module: ${e.message}")
            }
        }
        return DiscoveryResult(providers.sortedBy { it.info.id }, failures)
    }
}
