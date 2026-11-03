---
marp: true
size: 16:9
theme: am_purple
paginate: true
# headingDivider: [2,3,4]
footer: \ *张丽，井明（数据科学与计算机学院）* *移动应用开发（Android+Kotlin）实验* *2026年秋*
---

<!-- _class: cover_e -->
<!-- _paginate: "" -->
<!-- _footer: ![](./images/logo_transparent.png) -->
<!-- _header: ![](./images/logo-1_transparent.png) -->

# 实验十 架构重构与依赖注入

###### “重构不是重写，是让代码变得可改。”

张丽，井明
数据科学与计算机学院
jingming@sdu.edu.cn
2026年秋

---

<!-- _header: 目录<br>CONTENTS<br>![](images/logo-1_transparent.png)-->
<!-- _class: toc_b -->
<!-- _footer: "" -->
<!-- _paginate: "" -->

- [实验目的与准备](#0)
- [任务一：分层重构](#0)
- [任务二：接口与可测试性](#0)
- [任务三：手动依赖注入](#0)
- [任务四：改用 Hilt](#0)
- [验收、思考与提交](#0)

---

## 实验目的与准备

- 实验目的
- 起点代码

---

### 实验目的

1. 把前面实验的代码重构为 UI / Domain / Data 三层架构
2. 为 Repository 抽出接口，编写 Fake 实现并写出第一个 ViewModel 单元测试
3. 用手动依赖容器（`AppContainer`）完成依赖注入
4. 改用 Hilt 实现依赖注入，比较两种方式的差异
5. 按功能组织包结构，理解模块边界

**课时**：4 学时

---

### 起点代码

在 **实验九的 `GitHubExplorer`** 或 **实验八的 `NoteApp`** 基础上重构（二选一，报告中说明）。

**重构前先做两件事**：

1. `git init` 并提交当前代码，打上 tag `before-refactor`
2. 手工记录当前的完整功能清单（重构后要逐条验证功能没坏）

> ⚠️ 重构的铁律：**先保证有回退点，再动手改**。

---

### 重构前自查

在报告中记录当前代码的问题（至少 5 条）：

| # | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| 1 | ViewModel 直接 new 了 Repository | `XxxViewModel.kt:12` | 无法替换实现、无法测试 |
| 2 | Repository 是 `object` 单例 | | 无法注入 Fake |
| 3 | Screen 组件直接引用了 ViewModel | | 无法 Preview |
| 4 | DTO 直接当做 UI 模型使用 | | 接口变更会波及全应用 |
| 5 | | | |

---

## 任务一：分层重构

- 目标包结构
- 领域模型
- 逐层迁移

---

### 步骤 1.1：目标包结构

```text
com.example.app/
├── data/
│   ├── local/         RoomDatabase、Entity、Dao、DataStore
│   ├── remote/        ApiService、Dto
│   ├── mapper/        Dto/Entity ↔ Domain 的映射函数
│   └── repository/    XxxRepositoryImpl
├── domain/
│   ├── model/         纯 Kotlin 数据类，不依赖任何 Android API
│   ├── repository/    XxxRepository 接口
│   └── usecase/       （可选）
├── ui/
│   ├── list/          ListRoute / ListScreen / ListViewModel / ListUiState
│   ├── detail/
│   ├── component/     跨页面复用的组件
│   ├── navigation/
│   └── theme/
└── di/                AppContainer 或 Hilt Module
```

---

### 步骤 1.2：先建 domain 层

```kotlin
// domain/model/Note.kt —— 纯 Kotlin，不 import 任何 android.* 或 androidx.*
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val category: Category,
    val isPinned: Boolean,
    val updatedAt: Long
)

// domain/repository/NoteRepository.kt
interface NoteRepository {
    fun observeAll(): Flow<List<Note>>
    fun observeNote(id: Long): Flow<Note?>
    suspend fun save(note: Note): Long
    suspend fun delete(id: Long)
    suspend fun togglePin(id: Long)
}
```

> ✅ **检查点 1**：`domain` 包中**不允许**出现任何 `android.` / `androidx.` 的 import。用 IDE 搜索验证并截图。

---

### 步骤 1.3：data 层实现接口

```kotlin
// data/repository/NoteRepositoryImpl.kt
class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NoteRepository {

    override fun observeAll(): Flow<List<Note>> =
        noteDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun save(note: Note): Long = withContext(ioDispatcher) {
        noteDao.insert(note.toEntity())
    }
}
```

```kotlin
// data/mapper/NoteMapper.kt
fun NoteEntity.toDomain(): Note = Note(...)
fun Note.toEntity(): NoteEntity = NoteEntity(...)
fun NoteDto.toEntity(): NoteEntity = NoteEntity(...)
```

> 💡 注入 `CoroutineDispatcher` 而非硬编码 `Dispatchers.IO`，测试时可换成 `TestDispatcher`。

---

### 步骤 1.4：ui 层拆分

每个页面**必须**拆成四个文件：

```text
ui/list/
├── NoteListRoute.kt      有状态：连 ViewModel，收集状态，处理事件
├── NoteListScreen.kt     无状态：纯 UI，可 @Preview
├── NoteListViewModel.kt  状态持有与业务调度
└── NoteListUiState.kt    UI 状态数据类
```

```kotlin
// NoteListScreen.kt —— 检查：这个文件中不能出现 ViewModel、NavController、Context
@Composable
fun NoteListScreen(
    uiState: NoteListUiState,
    onNoteClick: (Long) -> Unit,
    onPinToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) { /* ... */ }
```

> ✅ **检查点 2**：为每个 Screen 写至少 2 个 `@Preview`（正常数据、空数据）

---

### 步骤 1.5：验证功能没坏

对照重构前记录的功能清单，逐条验证：

| # | 功能 | 重构前 | 重构后 |
| --- | --- | --- | --- |
| 1 | 列表显示 | ✓ | |
| 2 | 新建笔记 | ✓ | |
| 3 | 编辑保存 | ✓ | |
| ... | | | |

> ✅ **检查点 3**：所有功能全部正常后，`git commit` 并打 tag `after-layering`

---

## 任务二：接口与可测试性

- Fake 实现
- 第一个单元测试
- 测试覆盖

---

### 步骤 2.1：编写 Fake Repository

```kotlin
// src/test/java/.../FakeNoteRepository.kt
class FakeNoteRepository(
    initialNotes: List<Note> = emptyList()
) : NoteRepository {

    private val notes = MutableStateFlow(initialNotes)
    var shouldFail = false
    var saveCallCount = 0                       // 记录调用次数，用于断言

    override fun observeAll(): Flow<List<Note>> = notes.asStateFlow()

    override fun observeNote(id: Long): Flow<Note?> =
        notes.map { list -> list.find { it.id == id } }

    override suspend fun save(note: Note): Long {
        saveCallCount++
        if (shouldFail) throw IOException("模拟保存失败")
        notes.update { list ->
            if (list.any { it.id == note.id }) list.map { if (it.id == note.id) note else it }
            else list + note
        }
        return note.id
    }

    override suspend fun delete(id: Long) {
        notes.update { list -> list.filterNot { it.id == id } }
    }
}
```

---

### 步骤 2.2：测试基础设施

```kotlin
// src/test/java/.../MainDispatcherRule.kt
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}
```

```kotlin
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.google.truth:truth:1.4.2")
}
```

---

### 步骤 2.3：编写 5 个单元测试（必做）

```kotlin
class NoteListViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeNoteRepository
    private lateinit var viewModel: NoteListViewModel

    @Before fun setup() {
        repository = FakeNoteRepository(listOf(note(1, "笔记A"), note(2, "笔记B")))
        viewModel = NoteListViewModel(repository)
    }

    @Test fun `初始状态加载仓库中的笔记`() = runTest {
        assertThat(viewModel.uiState.value.notes).hasSize(2)
    }

    @Test fun `删除笔记后列表减少一项`() = runTest { /* ... */ }

    @Test fun `置顶的笔记排在最前面`() = runTest { /* ... */ }

    @Test fun `保存失败时 uiState 包含错误信息`() = runTest {
        repository.shouldFail = true
        viewModel.save(note(3, "新笔记"))
        assertThat(viewModel.uiState.value.error).isNotNull()
    }

    @Test fun `搜索关键字过滤出正确结果`() = runTest { /* ... */ }
}
```

> ✅ **检查点 4**：5 个测试全部通过，截图测试运行结果

---

### 步骤 2.4：体会可测试性的价值

在报告中回答：

1. 如果 `NoteListViewModel` 内部直接 `new NoteRepositoryImpl(...)`，上面这 5 个测试还能写吗？为什么？
2. `FakeNoteRepository` 相比用 Mockk 框架 mock 有什么优势？
3. 运行这 5 个测试需要模拟器吗？耗时多少毫秒？

```bash
./gradlew testDebugUnitTest
```

---

## 任务三：手动依赖注入

- AppContainer
- ViewModel Factory
- 依赖图

---

### 步骤 3.1：AppContainer

```kotlin
interface AppContainer {
    val noteRepository: NoteRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: NoteDatabase by lazy { NoteDatabase.getInstance(context) }

    private val api: GitHubApi by lazy { NetworkModule.api }

    override val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(database.noteDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(context)
    }
}
```

```kotlin
class NoteApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
```

---

### 步骤 3.2：ViewModel Factory

```kotlin
class NoteListViewModel(private val repository: NoteRepository) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as NoteApplication)
                NoteListViewModel(app.container.noteRepository)
            }
        }
    }
}
```

```kotlin
@Composable
fun NoteListRoute(
    viewModel: NoteListViewModel = viewModel(factory = NoteListViewModel.Factory)
) { /* ... */ }
```

> ✅ **检查点 5**：全项目搜索 `NoteRepositoryImpl(`，应当**只在 `DefaultAppContainer` 中出现一次**

---

### 步骤 3.3：画出依赖图

在报告中画出（手绘或工具均可）：

```text
NoteApplication
    └── DefaultAppContainer
            ├── NoteDatabase ──→ NoteDao ──┐
            │                              ├──→ NoteRepositoryImpl ──→ NoteListViewModel
            ├── GitHubApi ─────────────────┘                      └──→ NoteDetailViewModel
            └── DataStore ──→ SettingsRepositoryImpl ──→ SettingsViewModel
```

要求标注：哪些是单例、哪些每次新建、生命周期各是多久。

---

## 任务四：改用 Hilt

- 配置
- Module
- 对比

---

### 步骤 4.1：配置 Hilt

```kotlin
// 项目级 build.gradle.kts
plugins { id("com.google.dagger.hilt.android") version "2.51" apply false }

// 模块级
plugins {
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}
dependencies {
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
}
```

```kotlin
@HiltAndroidApp
class NoteApplication : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity()
```

---

### 步骤 4.2：编写 Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): NoteDatabase =
        Room.databaseBuilder(ctx, NoteDatabase::class.java, "note.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideNoteDao(db: NoteDatabase): NoteDao = db.noteDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
}
```

```kotlin
// Impl 的构造函数加 @Inject
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository
```

---

### 步骤 4.3：注入 ViewModel

```kotlin
@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val repository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() { /* 内容完全不用改 */ }
```

```kotlin
@Composable
fun NoteListRoute(viewModel: NoteListViewModel = hiltViewModel()) { /* ... */ }
```

> ✅ **检查点 6**：删除 `AppContainer` 与所有 `Factory` 后应用仍能正常运行

---

### 步骤 4.4：故意制造一个错误

**实验**：注释掉 `RepositoryModule` 中的 `@Binds` 方法，然后编译。

记录：

1. 错误发生在**编译期**还是**运行期**？
2. 完整的错误信息是什么？
3. 如果是手动依赖注入，同样的错误会在什么时候暴露？

> ✅ **检查点 7**：截图保存编译错误信息

---

### 步骤 4.5：两种方式对比

在报告中填写：

| 维度 | 手动 AppContainer | Hilt |
| --- | --- | --- |
| 上手难度 | | |
| 样板代码量 | | |
| 编译时间影响 | | |
| 错误暴露时机 | | |
| 作用域管理 | | |
| 适合的项目规模 | | |
| 你的选择及理由 | | |

---

## 验收、思考与提交

- 验收标准
- 思考题
- 提交要求

---

### 验收标准

| 任务 | 分值 | 要点 |
| --- | --- | --- |
| 任务一 分层重构 | 30 | 三层包结构、domain 无 Android 依赖、功能对照表全通过 |
| 任务二 可测试性 | 25 | Fake 实现完整、5 个测试全部通过 |
| 任务三 手动 DI | 20 | 依赖只在容器中创建、依赖图正确 |
| 任务四 Hilt | 20 | 迁移成功、对比表填写有分析 |
| Git 记录 | 5 | 有 `before-refactor` / `after-layering` 等阶段性提交 |

**加分项**：引入 UseCase 层并说明必要性（+5）；拆分为多模块（+10）

---

### 常见问题

| 问题 | 原因与处理 |
| --- | --- |
| 重构后功能丢失 | 未逐条对照功能清单；回退到 tag 重来 |
| `domain` 层出现 Android 依赖 | 用了 `Color`、`Context`、`@DrawableRes` 等；改为纯 Kotlin 类型 |
| 单元测试报 `Module with the Main dispatcher had failed to initialize` | 缺少 `MainDispatcherRule` |
| Hilt 报 `cannot be provided without an @Provides-annotated method` | 缺少对应的 Module 或 `@Inject constructor` |
| `@HiltViewModel` 注入失败 | Activity 未加 `@AndroidEntryPoint` |
| KSP 与 Hilt 版本冲突 | 对齐 Kotlin / KSP / Hilt 三者版本 |

---

### 思考题

1. 为什么 `domain` 层不能依赖 Android SDK？这个约束带来了什么好处？
2. Repository 用**接口**声明而不是直接用实现类，具体解决了哪些问题？（至少 3 条）
3. 为什么把 `CoroutineDispatcher` 作为构造参数注入，而不是在方法内写死 `Dispatchers.IO`？
4. `Route` / `Screen` 拆分的价值是什么？如果不拆，`@Preview` 会遇到什么困难？
5. Hilt 相比手动依赖注入的核心优势是什么？什么规模的项目值得引入？
6. 什么时候需要引入 `UseCase` 层？你的项目需要吗？说明理由。
7. 重构过程中你是如何保证"功能没有被改坏"的？如果有完整的单元测试，重构会更容易还是更难？

---

### 提交要求

**提交物**（打包为 `学号_姓名_实验十.zip`）：

1. **项目源码**（**保留 `.git` 目录**，需要看到重构过程的提交记录）
2. **实验报告**（PDF/Word）
   - 重构前的问题清单（≥5 条）
   - 重构前后的**包结构对比**（目录树截图）
   - 功能验证对照表
   - 单元测试代码与运行结果截图
   - **依赖关系图**（手绘或工具绘制）
   - 手动 DI 与 Hilt 的对比表
   - 思考题解答
3. **git log 截图**：展示阶段性提交记录

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

- [应用架构指南](https://developer.android.google.cn/topic/architecture)
- [Now in Android 示例应用](https://github.com/android/nowinandroid)
- [Hilt 依赖注入](https://developer.android.google.cn/training/dependency-injection/hilt-android)
- 理论课件：[ch-10.md](./ch-10.md)
