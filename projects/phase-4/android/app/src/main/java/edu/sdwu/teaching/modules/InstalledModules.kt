package edu.sdwu.teaching.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import edu.sdwu.teaching.framework.ModuleProvider
import edu.sdwu.teaching.framework.ModuleSource
import edu.sdwu.teaching.modules.lessonformatter.LessonFormatterModule
import edu.sdwu.teaching.modules.studytimer.StudyTimerModule

/** A module that is compiled into this app, and where it came from. */
data class InstalledModule(val provider: ModuleProvider, val source: ModuleSource)

/**
 * ====================================================================================
 *  THE PLACE WHERE MODULES ARE PLUGGED IN
 * ====================================================================================
 *
 * Discovery uses a Hilt *multibinding*: every `@Provides @IntoSet` function below adds one
 * element to a `Set<InstalledModule>`. TeachingApp receives the whole set at startup and
 * registers each module with the ModuleRegistry, which validates it.
 *
 * To plug in a new module:
 *   1. include(":modules:your-module") in settings.gradle.kts
 *   2. implementation(project(":modules:your-module")) in app/build.gradle.kts
 *   3. add one @Provides @IntoSet function here
 * ====================================================================================
 */
@Module
@InstallIn(SingletonComponent::class)
object InstalledModules {

    @Provides
    @IntoSet
    fun studyTimer() = InstalledModule(StudyTimerModule(), ModuleSource.BUILT_IN)

    @Provides
    @IntoSet
    fun lessonFormatter() = InstalledModule(LessonFormatterModule(), ModuleSource.BUILT_IN)
}
