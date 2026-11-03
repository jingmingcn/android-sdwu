package edu.sdwu.teaching.modules

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import edu.sdwu.teaching.framework.ModuleProvider
import edu.sdwu.teaching.framework.ModuleSource

/** A module registered by hand, and where it came from. */
data class InstalledModule(val provider: ModuleProvider, val source: ModuleSource)

/**
 * ====================================================================================
 *  HOW MODULES GET INTO THE APP
 * ====================================================================================
 *
 * A) Automatic discovery (the normal way, nothing to edit here):
 *    every folder in modules/ is a Gradle module (settings.gradle.kts), the app depends on all
 *    of them (app/build.gradle.kts), and each module lists its ModuleProvider in
 *    src/main/resources/META-INF/services/edu.sdwu.teaching.framework.ModuleProvider.
 *    TeachingApp calls ModuleDiscovery.discover() at startup.
 *
 * B) Manual registration (optional): for a provider that needs constructor arguments, or that
 *    lives inside :app itself. Add a companion object to this class:
 *
 *        companion object {
 *            @Provides @IntoSet
 *            fun myModule() = InstalledModule(MyModule(apiKey = "..."), ModuleSource.STUDENT)
 *        }
 *
 *    (Or call moduleRegistry.register(MyModule()) from code that has the registry injected.)
 * ====================================================================================
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InstalledModules {
    /** Declares the set, so it exists (empty) even when no module is registered by hand. */
    @Multibinds
    abstract fun manualModules(): Set<InstalledModule>
}
