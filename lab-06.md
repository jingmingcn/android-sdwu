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

# 实验六 状态管理与待办清单

###### “这一个实验，你会做出第一个真正的 App。”

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
- [任务一：状态基础实验](#0)
- [任务二：待办清单 v1](#0)
- [任务三：状态提升与 ViewModel](#0)
- [任务四：完善功能](#0)
- [验收、思考与提交](#0)

---

## 实验目的与准备

- 实验目的
- 项目准备

---

### 实验目的

1. 理解 `remember`、`rememberSaveable`、普通变量三者的区别并能实测验证
2. 掌握状态提升，能把有状态组件重构为无状态组件
3. 熟练使用 `LazyColumn` 展示列表，理解 `key` 的作用
4. 用 `ViewModel` + `StateFlow` 管理屏幕级 UI 状态
5. 正确使用 `LaunchedEffect`、`rememberCoroutineScope` 等副作用 API
6. 独立完成一个功能完整的待办清单应用

**课时**：4 学时（两次实验课）

---

### 项目准备

新建项目 `TodoApp`（Empty Activity，minSdk 24）。

```kotlin
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## 任务一：状态基础实验

- 三种状态对比
- key 的作用验证

---

### 步骤 1.1：三种状态对比

```kotlin
@Composable
fun StateLab() {
    var plain = 0                                              // A
    var remembered by remember { mutableIntStateOf(0) }        // B
    var saveable by rememberSaveable { mutableIntStateOf(0) }  // C
    val listWrong = remember { mutableListOf<String>() }        // D：普通 List
    val listRight = remember { mutableStateListOf<String>() }   // E：State List

    Column {
        Button(onClick = { plain++ }) { Text("A 普通变量：$plain") }
        Button(onClick = { remembered++ }) { Text("B remember：$remembered") }
        Button(onClick = { saveable++ }) { Text("C saveable：$saveable") }
        Button(onClick = { listWrong += "x" }) { Text("D 普通List：${listWrong.size}") }
        Button(onClick = { listRight += "x" }) { Text("E StateList：${listRight.size}") }
    }
}
```

---

### 步骤 1.2：记录实验结果

依次点击每个按钮 3 次，然后旋转屏幕，填写下表：

| 项 | 点击后数字变化 | 旋转后 | 原因 |
| --- | --- | --- | --- |
| A 普通变量 | | | |
| B `remember` | | | |
| C `rememberSaveable` | | | |
| D 普通 `mutableListOf` | | | |
| E `mutableStateListOf` | | | |

> ✅ **检查点 1**：D 的数字**不会变化**，请在报告中解释原因（提示：数据变了，但没有触发重组）

---

### 步骤 1.3：key 的作用验证

```kotlin
@Composable
fun KeyLab() {
    var items by remember { mutableStateOf(listOf("A", "B", "C")) }
    var useKey by remember { mutableStateOf(true) }

    Column {
        Row {
            Button(onClick = { items = listOf("新增") + items }) { Text("头部插入") }
            Switch(checked = useKey, onCheckedChange = { useKey = it })
            Text(if (useKey) "使用 key" else "不使用 key")
        }
        LazyColumn {
            if (useKey) {
                items(items, key = { it }) { ExpandableItem(it) }
            } else {
                items(items) { ExpandableItem(it) }
            }
        }
    }
}

@Composable
fun ExpandableItem(text: String) {
    var expanded by remember { mutableStateOf(false) }   // 每项自己的状态
    Card(Modifier.fillMaxWidth().padding(4.dp).clickable { expanded = !expanded }) {
        Column(Modifier.padding(12.dp)) {
            Text(text, fontWeight = FontWeight.Bold)
            if (expanded) Text("展开的详细内容")
        }
    }
}
```

---

### 步骤 1.4：验证操作

**实验步骤**：

1. 关闭"使用 key"开关
2. 展开第一项 "A"
3. 点击"头部插入"
4. **观察**：展开状态跑到哪一项去了？
5. 打开"使用 key"开关，重复上述操作
6. **观察**：展开状态是否跟着 "A" 走？

> ✅ **检查点 2**：截图对比两种情况，在报告中解释 `key` 的作用机制

---

## 任务二：待办清单 v1

- 数据模型
- 有状态版本
- 基本功能

---

### 步骤 2.1：数据模型

```kotlin
data class Todo(
    val id: Long,
    val title: String,
    val done: Boolean = false,
    val priority: Priority = Priority.NORMAL,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Priority(val label: String, val color: Color) {
    HIGH("高", Color(0xFFE53935)),
    NORMAL("中", Color(0xFFFB8C00)),
    LOW("低", Color(0xFF43A047))
}

enum class TodoFilter(val label: String) {
    ALL("全部"), ACTIVE("未完成"), COMPLETED("已完成")
}
```

---

### 步骤 2.2：先写一个"能跑"的版本

允许先把状态都放在 `TodoScreen` 里（后面会重构）：

```kotlin
@Composable
fun TodoScreenV1() {
    var todos by remember { mutableStateOf(listOf<Todo>()) }
    var input by remember { mutableStateOf("") }
    var nextId by remember { mutableLongStateOf(1L) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            OutlinedTextField(value = input, onValueChange = { input = it },
                              modifier = Modifier.weight(1f), label = { Text("新待办") })
            Button(onClick = {
                if (input.isNotBlank()) {
                    todos = todos + Todo(nextId++, input.trim())
                    input = ""
                }
            }) { Text("添加") }
        }
        LazyColumn { items(todos, key = { it.id }) { TodoRow(it) } }
    }
}
```

> ✅ **检查点 3**：能添加待办并显示在列表中

---

### 步骤 2.3：列表项组件

```kotlin
@Composable
fun TodoRow(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = todo.title,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = { Text(todo.createdAt.formatTime()) },
        leadingContent = { Checkbox(checked = todo.done, onCheckedChange = { onToggle() }) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriorityTag(todo.priority)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除 ${todo.title}")
                }
            }
        },
        modifier = modifier.clickable { onToggle() }
    )
}
```

---

## 任务三：状态提升与 ViewModel

- UI State 建模
- ViewModel 实现
- 界面重构

---

### 步骤 3.1：定义 UI State

```kotlin
data class TodoUiState(
    val todos: List<Todo> = emptyList(),
    val input: String = "",
    val filter: TodoFilter = TodoFilter.ALL,
    val selectedPriority: Priority = Priority.NORMAL
) {
    val visibleTodos: List<Todo>
        get() = when (filter) {
            TodoFilter.ALL -> todos
            TodoFilter.ACTIVE -> todos.filter { !it.done }
            TodoFilter.COMPLETED -> todos.filter { it.done }
        }.sortedWith(compareBy({ it.done }, { it.priority.ordinal }, { -it.createdAt }))

    val activeCount: Int get() = todos.count { !it.done }
    val completedCount: Int get() = todos.count { it.done }
    val isEmpty: Boolean get() = todos.isEmpty()
    val canAdd: Boolean get() = input.isNotBlank()
}
```

> 💡 派生数据用**计算属性**，不要作为字段存储，避免状态不一致。

---

### 步骤 3.2：ViewModel

```kotlin
class TodoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    private val _events = Channel<String>()
    val events = _events.receiveAsFlow()

    private var nextId = 1L

    fun onInputChange(text: String) = _uiState.update { it.copy(input = text) }

    fun onFilterChange(filter: TodoFilter) = _uiState.update { it.copy(filter = filter) }

    fun addTodo() {
        val state = _uiState.value
        if (state.input.isBlank()) return
        val todo = Todo(nextId++, state.input.trim(), priority = state.selectedPriority)
        _uiState.update { it.copy(todos = it.todos + todo, input = "") }
        viewModelScope.launch { _events.send("已添加：${todo.title}") }
    }

    fun toggle(id: Long) = _uiState.update { s ->
        s.copy(todos = s.todos.map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    fun delete(id: Long) { /* 删除并发送可撤销事件 */ }
    fun clearCompleted() { /* ... */ }
}
```

---

### 步骤 3.3：Route 与 Screen 拆分

```kotlin
// 有状态：连接 ViewModel
@Composable
fun TodoRoute(viewModel: TodoViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    TodoScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onInputChange = viewModel::onInputChange,
        onAdd = viewModel::addTodo,
        onToggle = viewModel::toggle,
        onDelete = viewModel::delete,
        onFilterChange = viewModel::onFilterChange,
        onClearCompleted = viewModel::clearCompleted
    )
}

// 无状态：纯 UI，可预览
@Composable
fun TodoScreen(uiState: TodoUiState, /* 各种回调 */) { /* ... */ }
```

> ✅ **检查点 4**：`TodoScreen` 中**不能出现** `viewModel` 字样；能写出 `@Preview`

---

### 步骤 3.4：验证配置变更

1. 添加 5 条待办，勾选其中 2 条
2. 旋转屏幕
3. **验证**：列表内容与勾选状态全部保留

> ✅ **检查点 5**：录屏或截图证明旋转后数据不丢失
> 💡 在 `TodoViewModel` 的 `init` 中打日志，确认旋转时**没有**重新创建

---

## 任务四：完善功能

- 必做功能
- 空状态与统计
- 交互增强

---

### 必做功能清单

| # | 功能 | 要点 |
| --- | --- | --- |
| 1 | 添加待办 | 输入为空时按钮禁用；添加后清空输入框 |
| 2 | 勾选完成 | 已完成项文字加删除线并置灰 |
| 3 | 删除待办 | 弹确认对话框；Snackbar 支持"撤销" |
| 4 | 优先级 | 添加时可选高/中/低，列表按优先级排序 |
| 5 | 筛选 | `FilterChip` 三选一：全部/未完成/已完成 |
| 6 | 统计 | 顶栏显示"未完成 3 / 共 8" |
| 7 | 清除已完成 | 顶栏菜单，弹确认对话框 |
| 8 | 空状态 | 无数据时显示插图+提示文字 |
| 9 | 编辑 | 点击标题弹对话框修改内容 |
| 10 | 回到顶部 | 滚动超过 5 项时显示 FAB |

---

### 页面骨架

```kotlin
Scaffold(
    topBar = {
        TopAppBar(
            title = { Text("待办清单") },
            actions = {
                Text("${uiState.activeCount}/${uiState.todos.size}",
                     modifier = Modifier.padding(end = 8.dp))
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, "更多")
                }
                DropdownMenu(menuExpanded, { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("清除已完成") }, onClick = onClearCompleted)
                }
            }
        )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    floatingActionButton = { if (showFab) ScrollTopFab(onClick = ::scrollToTop) }
) { padding -> /* 内容 */ }
```

---

### 撤销删除

```kotlin
// ViewModel
fun delete(id: Long) {
    val removed = _uiState.value.todos.find { it.id == id } ?: return
    _uiState.update { s -> s.copy(todos = s.todos.filterNot { it.id == id }) }
    viewModelScope.launch { _deleteEvents.send(removed) }
}

fun undoDelete(todo: Todo) = _uiState.update { it.copy(todos = it.todos + todo) }
```

```kotlin
// UI
LaunchedEffect(Unit) {
    viewModel.deleteEvents.collect { todo ->
        val result = snackbarHostState.showSnackbar(
            message = "已删除「${todo.title}」", actionLabel = "撤销"
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(todo)
    }
}
```

---

### 回到顶部 FAB

```kotlin
val listState = rememberLazyListState()
val scope = rememberCoroutineScope()

val showFab by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 5 }
}

AnimatedVisibility(visible = showFab, enter = fadeIn(), exit = fadeOut()) {
    FloatingActionButton(onClick = {
        scope.launch { listState.animateScrollToItem(0) }
    }) { Icon(Icons.Default.KeyboardArrowUp, "回到顶部") }
}
```

> 💡 用 `derivedStateOf` 而不是直接读 `firstVisibleItemIndex`，可避免每滚动一格就重组。

---

### 空状态

```kotlin
@Composable
fun EmptyView(filter: TodoFilter, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Checklist, contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = when (filter) {
                TodoFilter.ALL -> "还没有待办，点击上方添加一条吧"
                TodoFilter.ACTIVE -> "太棒了，全部完成！"
                TodoFilter.COMPLETED -> "还没有已完成的待办"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

---

### 动画（加分项）

```kotlin
// 列表项增删动画
LazyColumn(state = listState) {
    items(uiState.visibleTodos, key = { it.id }) { todo ->
        TodoRow(
            todo = todo,
            modifier = Modifier.animateItem()      // 位置变化自动过渡
        )
    }
}

// 完成状态切换时的颜色动画
val bgColor by animateColorAsState(
    targetValue = if (todo.done) MaterialTheme.colorScheme.surfaceVariant
                  else MaterialTheme.colorScheme.surface,
    label = "bg"
)
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
| 任务一 状态实验 | 15 | 5 项对比表完整，key 实验有对比截图 |
| 任务二 v1 版本 | 10 | 能添加、显示列表 |
| 任务三 重构 | 30 | `TodoScreen` 完全无状态、可 Preview；旋转不丢数据 |
| 任务四 功能 | 35 | 10 项必做功能，每项 3.5 分 |
| 代码质量 | 10 | 组件拆分合理、命名规范、无重复代码 |

**加分项**（总分不超过 100）：

- 列表项增删动画流畅（+5）
- 支持长按拖拽排序（+10）
- 添加搜索框，支持按标题过滤（+5）

---

### 常见问题

| 问题 | 原因与处理 |
| --- | --- |
| 添加后列表不刷新 | 用了 `mutableListOf` 而非 `mutableStateListOf`；或直接 `list.add()` 而未替换整个 List |
| 旋转后数据丢失 | 状态还在 `remember` 里，应移到 ViewModel |
| Snackbar 旋转后重复弹出 | 一次性事件用了 `StateFlow`，应改用 `Channel` |
| 展开状态错位 | `items()` 缺少 `key` |
| 滚动卡顿 | 每次重组都重新计算列表；用 `remember(key)` 缓存 |
| `Preview` 报错找不到 ViewModel | `Preview` 必须预览无状态的 `TodoScreen` |

---

### 思考题

1. `remember` 与 `rememberSaveable` 的区别是什么？什么类型的数据能被 `rememberSaveable` 保存？
2. 为什么用 `mutableListOf` 存进 `remember` 后，`add()` 不会触发界面刷新？有几种正确做法？
3. 什么是状态提升？把 `TodoRow` 改成无状态组件带来了哪些具体好处？
4. `LazyColumn` 中 `key` 的作用是什么？不传 `key` 会出现哪些问题？（结合你的实验现象回答）
5. 为什么"已删除，可撤销"这类一次性事件不能用 `StateFlow` 承载？用 `StateFlow` 会出现什么现象？
6. `derivedStateOf` 解决了什么性能问题？如果直接写 `if (listState.firstVisibleItemIndex > 5)` 会怎样？
7. `TodoUiState` 中的 `visibleTodos` 为什么设计成计算属性而不是字段？

---

### 提交要求

**提交物**（打包为 `学号_姓名_实验六.zip`）：

1. **项目源码**（删除 `build/`、`.gradle/`、`.idea/`）
2. **演示视频**（1~2 分钟，必交）
   - 依次演示 10 项功能
   - 包含旋转屏幕验证数据不丢失
3. **实验报告**（PDF/Word）
   - 任务一的两张对比表与截图
   - 架构说明：画出 `TodoRoute` / `TodoScreen` / `TodoViewModel` / `TodoUiState` 的关系图
   - 单向数据流示意图（事件向上、状态向下）
   - 思考题解答

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

- [Compose 中的状态](https://developer.android.google.cn/develop/ui/compose/state)
- [状态提升](https://developer.android.google.cn/develop/ui/compose/state-hoisting)
- [列表和网格](https://developer.android.google.cn/develop/ui/compose/lists)
- 理论课件：[ch-06.md](./ch-06.md)
