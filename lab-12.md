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

# 实验十二 测试、优化与发布（课程设计）

###### “把作业变成一个能装进别人手机的应用。”

张丽，井明
数据科学与计算机学院
jingming@sdu.edu.cn
2026年秋

---

<!-- _header: 目录<br>CONTENTS<br>![](images/logo-1_transparent.png)-->
<!-- _class: toc_b -->
<!-- _footer: "" -->
<!-- _paginate: "" -->

- [实验目的与说明](#0)
- [任务一：单元测试](#0)
- [任务二：UI 测试](#0)
- [任务三：性能优化](#0)
- [任务四：打包发布](#0)
- [课程设计要求](#0)

---

## 实验目的与说明

- 实验目的
- 与课程设计的关系

---

### 实验目的

1. 为 ViewModel 与 Repository 编写单元测试，达到关键逻辑覆盖
2. 编写 Compose UI 测试，验证关键交互路径
3. 用 Profiler 定位并解决至少一处性能问题
4. 开启 R8 混淆并编写正确的保留规则
5. 生成签名密钥，打出可安装的 release 包
6. 完成课程设计作品与答辩

**课时**：4 学时 + 课外

---

### 与课程设计的关系

本实验的对象是**你自己的课程设计作品**：

- 可以是前面实验逐步演进的 NoteApp / TodoApp
- 也可以是自选主题的新应用（需在第 10 周前提交选题）

本次实验的产物**同时也是**课程设计的交付物：

```text
源码 + 测试报告 + 性能分析报告 + 签名 APK + 设计文档 + 演示视频
```

---

## 任务一：单元测试

- 测试范围
- 必做用例
- 覆盖率

---

### 步骤 1.1：测试依赖

```kotlin
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("app.cash.turbine:turbine:1.1.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

### 步骤 1.2：ViewModel 测试（至少 8 个）

```kotlin
class NoteListViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeNoteRepository
    private lateinit var viewModel: NoteListViewModel

    @Before fun setup() {
        repository = FakeNoteRepository(sampleNotes())
        viewModel = NoteListViewModel(repository)
    }

    @Test fun `初始状态为加载中`() = runTest { }
    @Test fun `加载成功后显示全部笔记`() = runTest { }
    @Test fun `置顶笔记排在列表最前`() = runTest { }
    @Test fun `搜索关键字过滤出匹配项`() = runTest { }
    @Test fun `搜索无结果时 isEmpty 为 true`() = runTest { }
    @Test fun `删除后列表中不再包含该项`() = runTest { }
    @Test fun `保存失败时 uiState 包含错误信息`() = runTest { }
    @Test fun `重试后错误信息被清空`() = runTest { }
}
```

> ✅ **检查点 1**：8 个测试全部通过，截图保存运行结果

---

### 步骤 1.3：用 Turbine 测试 Flow

```kotlin
@Test
fun `搜索防抖只发出最后一次请求`() = runTest {
    viewModel.uiState.test {
        assertThat(awaitItem().query).isEmpty()

        viewModel.onQueryChange("k")
        viewModel.onQueryChange("ko")
        viewModel.onQueryChange("kotlin")

        advanceTimeBy(500)

        val state = expectMostRecentItem()
        assertThat(state.query).isEqualTo("kotlin")
        assertThat(repository.searchCallCount).isEqualTo(1)   // 只调用了一次
        cancelAndIgnoreRemainingEvents()
    }
}
```

---

### 步骤 1.4：Room DAO 测试（至少 4 个）

```kotlin
@RunWith(AndroidJUnit4::class)
class NoteDaoTest {
    private lateinit var db: NoteDatabase
    private lateinit var dao: NoteDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), NoteDatabase::class.java
        ).build()
        dao = db.noteDao()
    }

    @After fun close() = db.close()

    @Test fun `插入后可查询到`() = runTest { }
    @Test fun `更新后内容改变`() = runTest { }
    @Test fun `删除后查询返回 null`() = runTest { }
    @Test fun `关键字搜索返回匹配项`() = runTest { }
    @Test fun `置顶项排在前面`() = runTest { }
}
```

---

### 步骤 1.5：迁移测试（加分项）

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NoteDatabase::class.java
    )

    @Test
    fun migrate1To2() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO notes (id, title, content) VALUES (1, '旧笔记', '内容')")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        val cursor = db.query("SELECT * FROM notes WHERE id = 1")
        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("title"))).isEqualTo("旧笔记")
    }
}
```

---

## 任务二：UI 测试

- 测试规则
- 关键路径
- testTag 规范

---

### 步骤 2.1：加上 testTag

```kotlin
object TestTags {
    const val NOTE_LIST = "note_list"
    const val NOTE_ITEM = "note_item"
    const val ADD_FAB = "add_fab"
    const val TITLE_INPUT = "title_input"
    const val CONTENT_INPUT = "content_input"
    const val SAVE_BUTTON = "save_button"
    const val EMPTY_VIEW = "empty_view"
}
```

```kotlin
FloatingActionButton(
    onClick = onAddClick,
    modifier = Modifier.testTag(TestTags.ADD_FAB)
) { Icon(Icons.Default.Add, "新建笔记") }
```

> 💡 优先用 `onNodeWithText` / `onNodeWithContentDescription`（同时验证了无障碍）；文案易变时才用 `testTag`。

---

### 步骤 2.2：UI 测试（至少 5 个）

```kotlin
class NoteListScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `有数据时显示列表`() {
        composeTestRule.setContent {
            AppTheme {
                NoteListScreen(
                    uiState = NoteListUiState(notes = listOf(note(1, "测试笔记"))),
                    onNoteClick = {}, onAddClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("测试笔记").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EMPTY_VIEW).assertDoesNotExist()
    }

    @Test fun `无数据时显示空状态`() { }
    @Test fun `加载中显示进度条`() { }
    @Test fun `点击笔记触发回调`() { }
    @Test fun `点击 FAB 触发新建回调`() { }
}
```

---

### 步骤 2.3：端到端交互测试

```kotlin
@Test
fun `新建笔记完整流程`() {
    var savedNote: Note? = null

    composeTestRule.setContent {
        AppTheme {
            NoteEditScreen(
                uiState = EditUiState(),
                onTitleChange = {}, onContentChange = {},
                onSave = { savedNote = it }
            )
        }
    }

    composeTestRule.onNodeWithTag(TestTags.TITLE_INPUT).performTextInput("期末复习")
    composeTestRule.onNodeWithTag(TestTags.CONTENT_INPUT).performTextInput("第一章到第十二章")
    composeTestRule.onNodeWithTag(TestTags.SAVE_BUTTON).performClick()

    assertThat(savedNote?.title).isEqualTo("期末复习")
}
```

> ✅ **检查点 2**：运行 `./gradlew connectedDebugAndroidTest`，截图保存结果

---

## 任务三：性能优化

- 问题定位
- 优化实施
- 数据对比

---

### 步骤 3.1：开启检测工具

```kotlin
// Application.onCreate
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads().detectDiskWrites().detectNetwork()
            .penaltyLog()
            .build()
    )
    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects().detectActivityLeaks()
            .penaltyLog().build()
    )
}
```

```kotlin
debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
```

> ✅ **检查点 3**：记录 StrictMode 报出的所有违规日志

---

### 步骤 3.2：必做优化项（至少 3 项）

| # | 优化项 | 测量方法 | 记录 |
| --- | --- | --- | --- |
| 1 | 冷启动耗时 | `adb shell am start -W` 的 `TotalTime` | 优化前 __ ms → 优化后 __ ms |
| 2 | 列表滚动重组次数 | Layout Inspector 的 Recomposition counts | 优化前 __ → 优化后 __ |
| 3 | 主线程 IO | StrictMode 日志条数 | 优化前 __ 条 → 优化后 0 条 |
| 4 | 内存泄漏 | LeakCanary 报告数 | 优化前 __ → 优化后 0 |
| 5 | APK 体积 | Analyze APK | 优化前 __ MB → 优化后 __ MB |

---

### 步骤 3.3：测量冷启动

```bash
# 冷启动（先杀进程）
adb shell am force-stop com.example.noteapp
adb shell am start -W -n com.example.noteapp/.MainActivity
```

输出中的关键指标：

```text
TotalTime: 856        # 从启动到界面完全绘制
WaitTime: 872
```

**要求**：连续测 5 次取平均值，优化前后各测一轮。

常见优化点：`Application` 中的同步初始化改为延迟/后台；避免在 `onCreate` 中读数据库；使用启动画面主题避免白屏。

---

### 步骤 3.4：重组优化

```kotlin
// 优化前：每次重组都重新计算
@Composable
fun NoteList(notes: List<Note>, query: String) {
    val filtered = notes.filter { it.title.contains(query) }     // ✗
    LazyColumn { items(filtered) { NoteRow(it) } }
}

// 优化后
@Composable
fun NoteList(notes: List<Note>, query: String) {
    val filtered = remember(notes, query) {                       // ✓
        notes.filter { it.title.contains(query) }
    }
    LazyColumn { items(filtered, key = { it.id }) { NoteRow(it) } }
}
```

**测量方法**：Layout Inspector → 打开 Recomposition Counts → 滚动列表 → 记录数字。

> ✅ **检查点 4**：截图对比优化前后的重组计数

---

### 步骤 3.5：编写优化报告

在报告中，每一项优化写清四件事：

1. **现象**：什么操作下感觉到问题，或哪个工具报了什么
2. **定位**：用什么工具、看到什么数据，确认根因是什么
3. **措施**：改了什么代码（贴关键 diff）
4. **验证**：优化前后的**具体数字**对比

> ⚠️ 只写"我加了 remember 所以变快了"不得分，必须有测量数据。

---

## 任务四：打包发布

- 混淆配置
- 签名
- 打包验证

---

### 步骤 4.1：开启 R8

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

```proguard
# proguard-rules.pro
-keep class com.example.noteapp.data.remote.dto.** { *; }

-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers class **$$serializer { *; }
-keep,allowobfuscation interface com.example.noteapp.data.remote.*Api

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

> ✅ **检查点 5**：混淆后**完整回归**所有功能，特别是网络请求与 JSON 解析

---

### 步骤 4.2：生成签名密钥

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 \
  -validity 10000 -alias noteapp
```

```properties
# gradle.properties（务必加入 .gitignore）
RELEASE_STORE_FILE=../release.jks
RELEASE_STORE_PASSWORD=你的口令
RELEASE_KEY_ALIAS=noteapp
RELEASE_KEY_PASSWORD=你的口令
```

> ⚠️ 密钥文件与口令**不得提交到 Git**。提交作业时只交 APK，不交 `.jks`。

---

### 步骤 4.3：配置签名并打包

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(project.property("RELEASE_STORE_FILE") as String)
            storePassword = project.property("RELEASE_STORE_PASSWORD") as String
            keyAlias = project.property("RELEASE_KEY_ALIAS") as String
            keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
        }
    }
    buildTypes {
        release { signingConfig = signingConfigs.getByName("release") }
    }
}
```

```bash
./gradlew clean
./gradlew assembleRelease
./gradlew bundleRelease
```

---

### 步骤 4.4：验证 release 包

```bash
# 查看签名信息
keytool -printcert -jarfile app-release.apk

# 卸载 debug 包后安装 release 包
adb uninstall com.example.noteapp
adb install app/build/outputs/apk/release/app-release.apk
```

**回归检查清单**（逐项打勾）：

- [ ] 应用能正常启动，图标与名称正确
- [ ] 列表加载、新建、编辑、删除全部正常
- [ ] 网络请求成功，JSON 解析无 null 字段
- [ ] 数据库读写正常，重启后数据仍在
- [ ] 通知发送与点击跳转正常
- [ ] 深色模式、多语言正常
- [ ] 无崩溃（连续操作 5 分钟）

> ✅ **检查点 6**：把 APK 发给同学安装，在**别人的手机**上验证

---

### 步骤 4.5：APK 分析

Android Studio → Build → **Analyze APK**

记录：

| 组成部分 | 大小 | 占比 |
| --- | --- | --- |
| classes.dex | | |
| res/ | | |
| lib/ | | |
| resources.arsc | | |
| assets/ | | |
| **总计** | | |

对比 debug 包与 release 包的体积差异并解释原因。

---

## 课程设计要求

- 交付物
- 评分标准
- 答辩安排

---

### 课程设计交付物

| # | 交付物 | 要求 |
| --- | --- | --- |
| 1 | **源码** | 含 `.git`，提交记录能反映开发过程 |
| 2 | **签名 APK** | 可在真机安装运行 |
| 3 | **设计文档** | 见下页 |
| 4 | **测试报告** | 单元测试 + UI 测试代码与结果 |
| 5 | **性能报告** | ≥3 项优化，含前后数据对比 |
| 6 | **演示视频** | 3~5 分钟，讲解功能与技术要点 |

打包命名：`学号_姓名_课程设计.zip`

---

### 设计文档结构

```text
1. 选题背景与需求分析
   1.1 应用定位与目标用户
   1.2 功能需求清单（用例图或功能列表）
2. 系统设计
   2.1 整体架构图（UI / Domain / Data 三层）
   2.2 数据库设计（ER 图 + 表结构说明）
   2.3 接口设计（如有网络请求）
   2.4 页面导航图
3. 关键技术实现
   3.1 至少 3 个技术难点的实现方案与代码说明
4. 测试
   4.1 测试用例与结果
5. 性能优化
   5.1 三项优化的现象-定位-措施-验证
6. 总结
   6.1 已完成与未完成的功能
   6.2 遇到的主要问题与解决过程
   6.3 收获与改进方向
```

---

### 作品功能要求

**必须包含**（缺一项扣 10 分）：

| # | 要求 |
| --- | --- |
| 1 | 至少 4 个功能页面，使用 Navigation 组织 |
| 2 | 使用 Compose 构建全部界面，无 XML 布局 |
| 3 | 使用 ViewModel + StateFlow 管理状态 |
| 4 | 使用 Room 持久化数据 |
| 5 | 使用 DataStore 保存至少 2 项用户设置 |
| 6 | 包含一次网络请求（或说明为什么不需要） |
| 7 | 三层架构分层清晰 |
| 8 | 处理加载中/成功/失败/空 四种状态 |
| 9 | 适配深色模式 |
| 10 | 有单元测试与 UI 测试 |

---

### 评分标准

| 维度 | 分值 | 说明 |
| --- | --- | --- |
| 功能完整性 | 25 | 10 项必须要求；功能可用无崩溃 |
| 技术运用 | 20 | 架构分层、协程、状态管理运用是否正确 |
| 代码质量 | 15 | 命名、分层、复用、无明显坏味道 |
| 测试 | 10 | 测试数量与质量 |
| 性能优化 | 10 | 三项优化的数据支撑 |
| 界面与体验 | 10 | Material 3 规范、交互流畅、细节完善 |
| 文档 | 10 | 结构完整、图表清晰、分析有深度 |

**加分**：创新功能（+5）；多模块化（+5）；CI 自动构建（+5）

---

### 答辩安排

**时间**：第 16 周实验课
**形式**：每人 8 分钟（演示 5 分钟 + 提问 3 分钟）

**演示内容**：

1. 一句话说清你的应用解决什么问题
2. 真机演示核心流程（不许放录屏，现场操作）
3. 讲解一处你认为最有价值的技术实现
4. 说明一个你遇到并解决的难题

**常见提问方向**：

- 你的应用旋转屏幕后状态为什么不丢失？
- 这个列表为什么用 LazyColumn 而不是 Column？
- 网络请求失败时用户会看到什么？
- 如果数据量涨到 10 万条，哪里会先出问题？

---

### 时间安排建议

| 周次 | 任务 |
| --- | --- |
| 第 10 周 | 提交选题（一页纸：应用名、解决的问题、核心功能） |
| 第 11-13 周 | 功能开发，每周至少 3 次有意义的 Git 提交 |
| 第 14 周 | 完成功能，开始写测试 |
| 第 15 周 | 性能优化、混淆打包、真机测试 |
| 第 16 周 | 提交全部交付物 + 答辩 |

> ⚠️ **不要在最后一周才开始**。Git 提交记录会反映你的开发节奏，也是评分参考。

---

### 思考题

1. 单元测试与 UI 测试各自适合验证什么？为什么单元测试应该占多数？
2. 为什么 `runTest` 中的 `delay(1000)` 会瞬间完成？这对测试有什么意义？
3. 你在性能优化中定位到的问题，如果不用 Profiler 而是靠"感觉"能找到吗？
4. R8 混淆后 JSON 解析变成 null，根本原因是什么？除了 `-keep` 还有什么解决思路？
5. 签名密钥丢失会导致什么后果？团队开发中应该如何管理密钥？
6. 如果你的应用要上架应用商店，除了本次实验做的，还需要准备什么？
7. 回顾整个学期，如果重新开始做这个应用，你会在哪些地方做不同的选择？

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

- [测试应用](https://developer.android.google.cn/training/testing)
- [测试 Compose 布局](https://developer.android.google.cn/develop/ui/compose/testing)
- [应用性能指南](https://developer.android.google.cn/topic/performance)
- [为应用签名](https://developer.android.google.cn/studio/publish/app-signing)
- 理论课件：[ch-12.md](./ch-12.md)
