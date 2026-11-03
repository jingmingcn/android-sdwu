package edu.sdwu.teaching

import android.app.Application
import edu.sdwu.teaching.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TeachingApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Room runs its queries on its own background executor, so this is safe to launch here.
        appScope.launch { container.lessonRepository.seedIfEmpty() }
    }
}
