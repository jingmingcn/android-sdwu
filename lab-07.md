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

# 实验七 多页面导航与 ViewModel

###### “把一个屏幕的应用，变成一个真正的应用。”

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
- [任务一：搭建导航图](#0)
- [任务二：参数传递与返回](#0)
- [任务三：底部导航](#0)
- [任务四：笔记应用](#0)
- [验收、思考与提交](#0)

---

## 实验目的与准备

- 实验目的
- 项目准备

---

### 实验目的

1. 掌握 Navigation Compose 的 `NavHost`、路由定义与导航操作
2. 会用路径参数与查询参数传递数据，并在 ViewModel 中通过 `SavedStateHandle` 取参
3. 实现"下一页返回结果给上一页"
4. 搭建底部导航栏，正确处理标签页的状态保存与恢复
5. 理解并实现"页面组件不依赖 NavController"的设计
6. 掌握一次性事件（导航、提示）的正确处理方式

**课时**：4 学时

---

### 项目准备

新建项目 `NoteApp`（Empty Activity，minSdk 24）。

```kotlin
dependencies {
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
```

---

## 任务一：搭建导航图

- 定义路由
- 创建页面
- 实现 NavHost

---

### 步骤 1.1：定义路由

```kotlin
sealed class Screen(val route: String) {
    data object NoteList : Screen("note_list")
    data object Search : Screen("search?keyword={keyword}") {
        fun createRoute(keyword: String = "") = "search?keyword=$keyword"
    }
    data object Settings : Screen("settings")
    data object NoteDetail : Screen("note/{noteId}") {
        fun createRoute(noteId: Long) = "note/$noteId"
    }
    data object NoteEdit : Screen("note/edit?noteId={noteId}") {
        fun createRoute(noteId: Long? = null) =
            if (noteId == null) "note/edit" else "note/edit?noteId=$noteId"
    }
}
```

> ⚠️ **禁止**在跳转处直接写字符串字面量，必须通过 `createRoute`。

---

### 步骤 1.2：创建五个页面骨架

先让每个页面只显示标题和跳转按钮，跑通导航后再填内容：

```kotlin
@Composable
fun NoteListScreen(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) { /* ... */ }
```

要求（**重要**）：五个 Screen 组件的参数中**都不能出现 `NavController`**。

---

### 步骤 1.3：实现 NavHost

```kotlin
@Composable
fun NoteNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.NoteList.route,
        modifier = modifier
    ) {
        composable(Screen.NoteList.route) {
            NoteListRoute(
                onNoteClick = { id -> navController.navigate(Screen.NoteDetail.createRoute(id)) },
                onAddClick = { navController.navigate(Screen.NoteEdit.createRoute()) }
            )
        }

        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) {
            NoteDetailRoute(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.NoteEdit.createRoute(id)) }
            )
        }
        // 其余页面...
    }
}
```

> ✅ **检查点 1**：五个页面能互相跳转，返回键行为正确

---

### 步骤 1.4：导航行为实验

在列表页放置三个按钮，分别用不同方式跳到详情页，观察返回栈：

```kotlin
// A：普通跳转
navController.navigate(route)

// B：单顶模式
navController.navigate(route) { launchSingleTop = true }

// C：跳转并清空到列表页
navController.navigate(route) {
    popUpTo(Screen.NoteList.route) { inclusive = false }
}
```

**实验**：连续点击同一个按钮 3 次，然后连按返回键，记录需要按几次才回到列表页。

| 方式 | 返回次数 | 说明 |
| --- | --- | --- |
| A | | |
| B | | |
| C | | |

---

## 任务二：参数传递与返回

- 路径参数
- SavedStateHandle 取参
- 返回结果

---

### 步骤 2.1：在 ViewModel 中取参数

```kotlin
class NoteDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository = NoteRepository
) : ViewModel() {

    private val noteId: Long = checkNotNull(savedStateHandle["noteId"]) {
        "缺少 noteId 参数"
    }

    val uiState: StateFlow<NoteDetailUiState> = repository.observeNote(noteId)
        .map { note -> NoteDetailUiState(note = note, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
                 NoteDetailUiState(isLoading = true))
}
```

> ✅ **检查点 2**：详情页组件的参数中**没有** `noteId`，它由 ViewModel 自己从导航参数取得

---

### 步骤 2.2：可选参数

搜索页接收可选的关键字：

```kotlin
composable(
    route = Screen.Search.route,
    arguments = listOf(navArgument("keyword") {
        type = NavType.StringType
        defaultValue = ""
    })
) { entry ->
    val keyword = entry.arguments?.getString("keyword").orEmpty()
    SearchRoute(initialKeyword = keyword, onBack = { navController.popBackStack() })
}
```

编辑页接收可选的 `noteId`（有则编辑，无则新建）：

```kotlin
arguments = listOf(navArgument("noteId") {
    type = NavType.LongType
    defaultValue = -1L
})
```

---

### 步骤 2.3：返回结果给上一页

需求：列表页点击"选择分类"→ 进入分类选择页 → 选中后返回并显示。

```kotlin
// 分类选择页：设置结果后返回
fun onCategorySelected(category: String) {
    navController.previousBackStackEntry
        ?.savedStateHandle
        ?.set("selected_category", category)
    navController.popBackStack()
}
```

```kotlin
// 列表页：接收结果
val category by navController.currentBackStackEntry
    ?.savedStateHandle
    ?.getStateFlow("selected_category", "")
    ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf("") }

LaunchedEffect(category) {
    if (category.isNotEmpty()) viewModel.applyCategory(category)
}
```

> ✅ **检查点 3**：演示"选择分类→返回→列表已按分类过滤"

---

### 步骤 2.4：保存后返回

编辑页保存成功后，应该返回并在列表页显示 Snackbar：

```kotlin
// EditViewModel
private val _events = Channel<EditEvent>()
val events = _events.receiveAsFlow()

fun save() = viewModelScope.launch {
    repository.save(currentNote)
    _events.send(EditEvent.SavedAndExit("已保存"))
}
```

```kotlin
// EditRoute
LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
        when (event) {
            is EditEvent.SavedAndExit -> onSaved(event.message)   // 由 NavHost 决定怎么导航
        }
    }
}
```

> ⚠️ 用 `Channel` 而不是 `StateFlow`，否则旋转屏幕后会重复触发导航。

---

## 任务三：底部导航

- NavigationBar
- 状态保存与恢复
- 嵌套导航图

---

### 步骤 3.1：定义标签页

```kotlin
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    NOTES("note_list", "笔记", Icons.Default.Description),
    SEARCH("search_tab", "搜索", Icons.Default.Search),
    SETTINGS("settings", "设置", Icons.Default.Settings)
}
```

```kotlin
Scaffold(
    bottomBar = {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        NavigationBar {
            TopLevelDestination.entries.forEach { dest ->
                NavigationBarItem(
                    selected = currentRoute == dest.route,
                    onClick = { navController.navigateToTab(dest.route) },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) }
                )
            }
        }
    }
) { padding -> NoteNavHost(navController, Modifier.padding(padding)) }
```

---

### 步骤 3.2：正确的标签切换

```kotlin
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

**对比实验**：先写成 `navigate(route)`（不加任何配置），执行以下操作：

1. 在"笔记"页滚动到第 20 项
2. 切到"设置"页
3. 切回"笔记"页 → **观察滚动位置**
4. 连按返回键 → **观察需要按几次才退出**

然后改为上面的完整写法，重复实验。

> ✅ **检查点 4**：填写对比表，说明 `saveState`/`restoreState`/`popUpTo` 各自解决了什么问题

---

### 步骤 3.3：详情页隐藏底部栏

```kotlin
val showBottomBar = currentRoute in TopLevelDestination.entries.map { it.route }

Scaffold(
    bottomBar = {
        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) { NavigationBar { /* ... */ } }
    }
) { /* ... */ }
```

> ✅ **检查点 5**：进入详情页时底部栏滑出，返回时滑入

---

## 任务四：笔记应用

- 数据层
- 五个页面
- 完整功能

---

### 数据模型与临时仓库

本实验暂不接数据库（下次实验替换为 Room），先用内存仓库：

```kotlin
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val category: String = "默认",
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

object NoteRepository {
    private val _notes = MutableStateFlow(sampleNotes())
    fun observeAll(): Flow<List<Note>> = _notes.asStateFlow()
    fun observeNote(id: Long): Flow<Note?> = _notes.map { list -> list.find { it.id == id } }
    fun search(keyword: String): Flow<List<Note>> = _notes.map { list ->
        list.filter { it.title.contains(keyword, true) || it.content.contains(keyword, true) }
    }
    fun save(note: Note) { /* 新增或更新 */ }
    fun delete(id: Long) { /* ... */ }
    fun togglePin(id: Long) { /* ... */ }
}
```

---

### 功能清单

| 页面 | 功能 |
| --- | --- |
| **列表页** | 显示所有笔记（置顶在前）；卡片显示标题、摘要、时间、分类；点击进详情；长按弹菜单（置顶/删除）；FAB 新建 |
| **详情页** | 显示完整内容；顶栏"编辑"和"删除"按钮；删除后返回列表 |
| **编辑页** | 标题+内容输入；分类选择；保存/取消；有未保存改动时按返回弹确认框 |
| **搜索页** | 输入框实时过滤；显示匹配数量；点击结果进详情；空结果提示 |
| **设置页** | 深色模式开关（暂用内存状态）；排序方式选择；关于信息 |

---

### 未保存改动拦截

```kotlin
@Composable
fun NoteEditScreen(
    uiState: EditUiState,
    onBack: () -> Unit,
    /* ... */
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.hasUnsavedChanges) {
        showConfirmDialog = true
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("放弃编辑？") },
            text = { Text("你有未保存的修改，确定要离开吗？") },
            confirmButton = { TextButton(onClick = onBack) { Text("放弃") } },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("继续编辑") }
            }
        )
    }
}
```

---

### 深链接（加分项）

```kotlin
composable(
    route = Screen.NoteDetail.route,
    arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
    deepLinks = listOf(navDeepLink { uriPattern = "noteapp://note/{noteId}" })
) { /* ... */ }
```

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="noteapp" android:host="note" />
</intent-filter>
```

```bash
adb shell am start -a android.intent.action.VIEW -d "noteapp://note/2"
```

---

## 验收、思考与提交

- 验收标准
- 思考题
- 提交要求

---

### 验收标准

| 任务 | 分值 | 要点 |
| --- | --- | --- |
| 任务一 导航图 | 20 | 五页互通、Screen 不依赖 NavController、导航行为实验表 |
| 任务二 参数传递 | 25 | 路径/可选参数、SavedStateHandle 取参、返回结果 |
| 任务三 底部导航 | 20 | 状态保存恢复正确、对比实验表完整 |
| 任务四 笔记应用 | 25 | 五个页面功能完整 |
| 代码质量 | 10 | Route/Screen 拆分、无重复导航逻辑 |

**加分项**：深链接可用（+5）；页面切换动画（+5）；嵌套导航图分组（+5）

---

### 常见问题

| 问题 | 原因与处理 |
| --- | --- |
| `IllegalArgumentException: navigation destination not found` | 路由字符串拼错；用 `createRoute` 统一生成 |
| 参数取到 null | `navArgument` 的类型或名称与路由中不一致 |
| 返回后数据没更新 | 上一页 ViewModel 没有观察数据源；应用 `Flow` 而非一次性读取 |
| 旋转后重复导航 | 一次性事件用了 `StateFlow`，改用 `Channel` |
| 底部栏切换后列表回到顶部 | 缺少 `saveState` / `restoreState` |
| 返回栈无限增长 | 标签切换缺少 `popUpTo` |

---

### 思考题

1. 为什么页面组件不应该直接接收 `NavController` 参数？这样设计带来了哪些好处？
2. 导航参数是如何进入 `SavedStateHandle` 的？这与"进程被杀后恢复"有什么关系？
3. 为什么路由中只应传递 ID 而不是整个对象？如果传对象会遇到哪些问题？
4. `popUpTo` 的 `inclusive = true` 与 `false` 有什么区别？登录成功后跳首页应该用哪个？
5. 标签切换时 `saveState` / `restoreState` / `launchSingleTop` 三个参数各自解决了什么问题？
6. 如果用 `StateFlow` 承载"保存成功后返回上一页"这个事件，旋转屏幕会发生什么？为什么？

---

### 提交要求

**提交物**（打包为 `学号_姓名_实验七.zip`）：

1. **项目源码**（删除 `build/`、`.gradle/`、`.idea/`）
2. **演示视频**（2 分钟内，必交）
   - 五个页面的完整导航流程
   - 参数传递与返回结果演示
   - 标签切换后滚动位置保留的验证
   - 未保存改动时按返回键弹确认框
3. **实验报告**（PDF/Word）
   - **导航图**：画出所有页面与跳转关系（含参数）
   - 任务一、任务三的两张对比实验表
   - 思考题解答

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

- [Navigation Compose](https://developer.android.google.cn/develop/ui/compose/navigation)
- [ViewModel 与 SavedStateHandle](https://developer.android.google.cn/topic/libraries/architecture/viewmodel/viewmodel-savedstate)
- 理论课件：[ch-07.md](./ch-07.md)
