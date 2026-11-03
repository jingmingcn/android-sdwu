package edu.sdwu.teaching.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.sdwu.teaching.data.local.AppDatabase
import edu.sdwu.teaching.data.local.LessonDao
import edu.sdwu.teaching.data.local.ModuleDao
import edu.sdwu.teaching.data.local.ProgressDao
import edu.sdwu.teaching.data.local.StudyDao
import edu.sdwu.teaching.data.modules.RoomModuleStateStore
import edu.sdwu.teaching.data.prefs.DataStorePreferencesRepository
import edu.sdwu.teaching.data.prefs.SessionDataSource
import edu.sdwu.teaching.data.prefs.SessionStore
import edu.sdwu.teaching.data.remote.TeachingApi
import edu.sdwu.teaching.data.remote.createApiOkHttpClient
import edu.sdwu.teaching.data.remote.createBaseOkHttpClient
import edu.sdwu.teaching.data.remote.createTeachingApi
import edu.sdwu.teaching.data.repository.Clock
import edu.sdwu.teaching.data.repository.LocalBookmarkRepository
import edu.sdwu.teaching.data.repository.LocalNoteRepository
import edu.sdwu.teaching.data.repository.OfflineFirstLessonRepository
import edu.sdwu.teaching.data.repository.OfflineFirstProgressRepository
import edu.sdwu.teaching.data.repository.RemoteAuthRepository
import edu.sdwu.teaching.data.repository.RemoteModuleCatalogRepository
import edu.sdwu.teaching.data.work.ReminderScheduler
import edu.sdwu.teaching.data.work.SyncScheduler
import edu.sdwu.teaching.data.work.WorkManagerReminderScheduler
import edu.sdwu.teaching.data.work.WorkManagerSyncScheduler
import edu.sdwu.teaching.domain.repository.AuthRepository
import edu.sdwu.teaching.domain.repository.BookmarkRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.ModuleCatalogRepository
import edu.sdwu.teaching.domain.repository.NoteRepository
import edu.sdwu.teaching.domain.repository.PreferencesRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import edu.sdwu.teaching.framework.DefaultModuleRegistry
import edu.sdwu.teaching.framework.ModuleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

// Hilt modules replace Phase 2's AppContainer. Rule of thumb:
//  - @Binds  : "when someone asks for this interface, give them that implementation"
//  - @Provides: "here is how to build an object I don't own" (Room, Retrofit, WorkManager)
// Every binding is @Singleton: one instance for the whole app, like the old lazy properties.

/** A CoroutineScope that lives as long as the app (for work that must not stop with a screen). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun lessons(impl: OfflineFirstLessonRepository): LessonRepository
    @Binds abstract fun progress(impl: OfflineFirstProgressRepository): ProgressRepository
    @Binds abstract fun auth(impl: RemoteAuthRepository): AuthRepository
    @Binds abstract fun preferences(impl: DataStorePreferencesRepository): PreferencesRepository
    @Binds abstract fun notes(impl: LocalNoteRepository): NoteRepository
    @Binds abstract fun bookmarks(impl: LocalBookmarkRepository): BookmarkRepository
    @Binds abstract fun moduleCatalog(impl: RemoteModuleCatalogRepository): ModuleCatalogRepository
    @Binds abstract fun session(impl: SessionStore): SessionDataSource
    @Binds abstract fun syncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler
    @Binds abstract fun reminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(*AppDatabase.MIGRATIONS) // Phase 1 and 2 users keep their data.
            .build()

    @Provides fun lessonDao(db: AppDatabase): LessonDao = db.lessonDao()
    @Provides fun progressDao(db: AppDatabase): ProgressDao = db.progressDao()
    @Provides fun studyDao(db: AppDatabase): StudyDao = db.studyDao()
    @Provides fun moduleDao(db: AppDatabase): ModuleDao = db.moduleDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /** Plain client, also used by Coil for images (see TeachingApp.newImageLoader). */
    @Provides
    @Singleton
    fun baseOkHttpClient(): OkHttpClient = createBaseOkHttpClient()

    @Provides
    @Singleton
    fun teachingApi(
        base: OkHttpClient,
        preferences: PreferencesRepository,
        session: SessionDataSource,
    ): TeachingApi = createTeachingApi(
        createApiOkHttpClient(
            base = base,
            serverUrl = { preferences.preferences.first().serverUrl },
            token = { session.token() },
        ),
    )
}

@Module
@InstallIn(SingletonComponent::class)
object FrameworkModule {
    /** One registry for the whole app, persisted in Room (framework/ModuleRegistry.kt). */
    @Provides
    @Singleton
    fun moduleRegistry(store: RoomModuleStateStore): ModuleRegistry = DefaultModuleRegistry(store)
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun workManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    fun clock(): Clock = Clock(System::currentTimeMillis)
}
