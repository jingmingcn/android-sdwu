package edu.sdwu.teaching

import android.app.Application
import edu.sdwu.teaching.di.AppContainer
import edu.sdwu.teaching.domain.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class TeachingApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, appScope)

        appScope.launch {
            // 1. Offline: make sure there is always something to show.
            container.lessonRepository.seedIfEmpty()
            // 2. Online: try to get the latest catalogue. Failure is fine, the cache stays.
            container.lessonRepository.refresh()
        }

        // 3. Sync progress whenever someone signs in (and once at start if already signed in).
        appScope.launch {
            container.authRepository.session
                .distinctUntilChangedBy { (it as? SessionState.SignedIn)?.user?.id }
                .collect { if (it is SessionState.SignedIn) container.progressRepository.sync() }
        }
    }
}
