package edu.sdwu.teaching

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import edu.sdwu.teaching.data.modules.ModuleCrashGuard
import edu.sdwu.teaching.data.work.ReminderScheduler
import edu.sdwu.teaching.data.work.StudyNotifier
import edu.sdwu.teaching.data.work.SyncScheduler
import edu.sdwu.teaching.di.ApplicationScope
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.repository.AuthRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.PreferencesRepository
import edu.sdwu.teaching.framework.BuiltInModule
import edu.sdwu.teaching.framework.ModuleDiscovery
import edu.sdwu.teaching.framework.ModuleRegistry
import edu.sdwu.teaching.framework.ModuleSource
import edu.sdwu.teaching.modules.InstalledModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * @HiltAndroidApp generates the dependency graph. Fields marked @Inject are filled in
 * before onCreate() runs, so there is no AppContainer anymore.
 */
@HiltAndroidApp
class TeachingApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var lessonRepository: LessonRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var notifier: StudyNotifier
    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope
    @Inject lateinit var moduleRegistry: ModuleRegistry
    @Inject lateinit var manualModules: Set<@JvmSuppressWildcards InstalledModule>
    @Inject lateinit var crashGuard: ModuleCrashGuard

    /** Lets WorkManager create @HiltWorker classes with injected repositories. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    /** Coil shares the app's OkHttp client (one connection pool, same logging). */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
            .build()

    override fun onCreate() {
        super.onCreate()
        notifier.createChannel()

        // 0. Modules: find them (ServiceLoader), register them (the registry validates each one),
        //    then disable any module that crashed the app last time. See modules/InstalledModules.kt.
        crashGuard.install()
        appScope.launch {
            val discovered = ModuleDiscovery.discover()
            discovered.failures.forEach(moduleRegistry::reportProblem)
            discovered.providers.forEach { provider ->
                moduleRegistry.register(provider, if (provider is BuiltInModule) ModuleSource.BUILT_IN else ModuleSource.STUDENT)
            }
            manualModules.sortedBy { it.provider.info.id }.forEach { moduleRegistry.register(it.provider, it.source) }
            crashGuard.applyPendingCrash()
        }

        // 1. Offline: there is always something to show. Then ask WorkManager to refresh.
        appScope.launch {
            lessonRepository.seedIfEmpty()
            syncScheduler.requestSync()
        }
        syncScheduler.schedulePeriodicSync()

        // 2. Sync progress whenever someone signs in.
        appScope.launch {
            authRepository.session
                .distinctUntilChangedBy { (it as? SessionState.SignedIn)?.user?.id }
                .collect { if (it is SessionState.SignedIn) syncScheduler.requestSync() }
        }

        // 3. Keep the daily reminder in line with the preference (also re-applied after updates).
        appScope.launch {
            preferencesRepository.preferences
                .map { it.reminderEnabled to it.reminderHour }
                .distinctUntilChanged()
                .collect { (enabled, hour) -> reminderScheduler.schedule(enabled, hour) }
        }
    }
}
