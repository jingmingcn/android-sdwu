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

# 实验五 Compose UI 布局与组件

###### “先把界面画对，再让它动起来。”

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
- [任务一：基础组件练习](#0)
- [任务二：Modifier 顺序实验](#0)
- [任务三：个人名片页](#0)
- [任务四：登录注册页](#0)
- [验收、思考与提交](#0)

---

## 实验目的与准备

- 实验目的
- 项目准备

---

### 实验目的

1. 熟练使用 `Text`、`Button`、`TextField`、`Image`、`Icon` 等基础组件
2. 掌握 `Column` / `Row` / `Box` 三大布局与对齐、排列、权重
3. 理解 `Modifier` 的链式顺序对渲染结果的影响
4. 会用 `Scaffold` 搭建页面骨架
5. 使用 Material 3 主题与 `@Preview` 完成界面开发

**课时**：2 学时

---

### 项目准备

新建项目 `ComposeUILab`（Empty Activity，minSdk 24）。

确认依赖：

```kotlin
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

准备一张自己的头像图片放入 `res/drawable/avatar.png`。

---

### 目录组织要求

```text
ui/
├── task1/  BasicComponentsScreen.kt
├── task2/  ModifierLabScreen.kt
├── task3/  ProfileCardScreen.kt
├── task4/  LoginScreen.kt
└── theme/
```

`MainActivity` 中用一个简单的按钮列表切换四个任务界面（用 `when` + 状态即可，不需要 Navigation）。

---

## 任务一：基础组件练习

- 组件展示页
- 每种组件至少一个变体

---

### 步骤 1.1：文本组件

在 `BasicComponentsScreen` 中依次展示：

```kotlin
Text("默认文本")
Text("大标题", style = MaterialTheme.typography.headlineMedium)
Text("加粗红色", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
Text("斜体带下划线", fontStyle = FontStyle.Italic,
     textDecoration = TextDecoration.Underline)
Text("这是一段很长很长的文字，用来演示最多显示两行并以省略号结尾的效果……",
     maxLines = 2, overflow = TextOverflow.Ellipsis)

// 富文本
Text(buildAnnotatedString {
    append("学号：")
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold)) {
        append("202312345678")
    }
})
```

---

### 步骤 1.2：按钮与交互组件

```kotlin
var checked by remember { mutableStateOf(false) }
var switchOn by remember { mutableStateOf(true) }
var selected by remember { mutableStateOf("Kotlin") }
var sliderValue by remember { mutableFloatStateOf(0.5f) }
var clickLog by remember { mutableStateOf("") }
```

要求展示：`Button`、`OutlinedButton`、`TextButton`、`FilledTonalButton`、`IconButton`、`FloatingActionButton`、`Checkbox`、`Switch`、`RadioButton`（三选一）、`Slider`。

每个组件被操作时，把操作记录追加到 `clickLog` 并显示在页面底部：

```text
[14:23:01] 点击了 Button
[14:23:04] Switch 切换为 false
[14:23:07] Slider 调整为 0.72
```

---

### 步骤 1.3：输入框

```kotlin
var username by remember { mutableStateOf("") }

OutlinedTextField(
    value = username,
    onValueChange = { if (it.length <= 20) username = it },
    label = { Text("用户名") },
    placeholder = { Text("请输入 3-20 个字符") },
    leadingIcon = { Icon(Icons.Default.Person, null) },
    trailingIcon = {
        if (username.isNotEmpty()) {
            IconButton(onClick = { username = "" }) { Icon(Icons.Default.Clear, "清空") }
        }
    },
    supportingText = { Text("${username.length}/20") },
    isError = username.isNotEmpty() && username.length < 3,
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
```

> ✅ **检查点 1**：截图保存组件展示页（需滚动展示全部组件）

---

## 任务二：Modifier 顺序实验

- 对比实验
- 记录差异

---

### 步骤 2.1：padding 与 background

在同一页面中并排展示以下四个组合，用文字标注每个的 Modifier 链：

```kotlin
@Composable
fun ModifierDemo() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // A
        Text("A", Modifier.padding(16.dp).background(Color.Cyan))
        // B
        Text("B", Modifier.background(Color.Cyan).padding(16.dp))
        // C
        Text("C", Modifier.padding(16.dp).background(Color.Cyan).padding(8.dp))
        // D
        Text("D", Modifier.size(80.dp).background(Color.Cyan).padding(16.dp))
    }
}
```

> ✅ **检查点 2**：截图并在报告中画出 A、B 的盒模型示意图，解释差异

---

### 步骤 2.2：clickable 与 padding

```kotlin
var log by remember { mutableStateOf("") }

// E：点击区域不含 padding
Box(Modifier
    .clickable { log = "E 被点击" }
    .padding(24.dp)
    .background(Color.Yellow)) { Text("E") }

// F：点击区域含 padding
Box(Modifier
    .padding(24.dp)
    .clickable { log = "F 被点击" }
    .background(Color.Yellow)) { Text("F") }
```

**实验**：分别点击 E、F 的**内边距区域**（黄色块之外、组件之内），记录哪个响应了点击。

> ✅ **检查点 3**：在报告中说明结论及其原因

---

### 步骤 2.3：size 与 clip

```kotlin
// G：先 clip 再 background
Box(Modifier.size(100.dp).clip(CircleShape).background(Color.Magenta))

// H：先 background 再 clip
Box(Modifier.size(100.dp).background(Color.Magenta).clip(CircleShape))

// I：border 与 clip 的顺序
Box(Modifier.size(100.dp).border(4.dp, Color.Blue, CircleShape).clip(CircleShape)
    .background(Color.Magenta))
```

> 💡 观察哪个是圆形、哪个是方形，并总结规律："Modifier 从左到右，由外向内生效"。

---

### 步骤 2.4：布局对齐实验

用 `Row` + `weight` 实现下面三种布局并截图：

```kotlin
// 1. 三等分
Row { repeat(3) { Box(Modifier.weight(1f).height(60.dp).background(colors[it])) } }

// 2. 1:2:1
Row {
    Box(Modifier.weight(1f)...); Box(Modifier.weight(2f)...); Box(Modifier.weight(1f)...)
}

// 3. 左侧固定 80dp，右侧填满
Row {
    Box(Modifier.width(80.dp)...)
    Box(Modifier.weight(1f)...)
}
```

再用 `Box` 实现"图片背景 + 左上角标签 + 居中标题 + 右下角按钮"的层叠布局。

---

## 任务三：个人名片页

- 界面要求
- 组件拆分
- 预览要求

---

### 界面要求

实现一个个人名片页，包含：

```text
┌──────────────────────────────────┐
│  ╭────╮                          │
│  │头像│   张三                    │
│  ╰────╯   计算机科学与技术         │
│           202312345678            │
├──────────────────────────────────┤
│  📧  zhangsan@mail.sdu.edu.cn     │
│  📱  138****5678                  │
│  📍  山东大学（青岛校区）           │
├──────────────────────────────────┤
│    [ 发消息 ]     [ 打电话 ]       │
├──────────────────────────────────┤
│   12        48         256        │
│  作品      关注       粉丝         │
└──────────────────────────────────┘
```

---

### 组件拆分要求

必须拆成**至少 4 个**无状态可组合函数：

```kotlin
@Composable
fun ProfileCard(profile: Profile, modifier: Modifier = Modifier)

@Composable
private fun ProfileHeader(avatar: Int, name: String, major: String, id: String)

@Composable
private fun InfoRow(icon: ImageVector, text: String)

@Composable
private fun ActionButtons(onMessage: () -> Unit, onCall: () -> Unit)

@Composable
private fun StatItem(count: Int, label: String, modifier: Modifier = Modifier)
```

数据模型：

```kotlin
data class Profile(
    @DrawableRes val avatar: Int, val name: String, val major: String,
    val studentId: String, val email: String, val phone: String,
    val location: String, val works: Int, val following: Int, val followers: Int
)
```

---

### 技术要求

| 要求 | 说明 |
| --- | --- |
| 头像 | 圆形裁剪，带 2dp 边框 |
| 卡片 | 使用 `Card`，圆角 16dp，有阴影 |
| 颜色 | **全部**来自 `MaterialTheme.colorScheme`，禁止硬编码 |
| 字体 | 使用 `MaterialTheme.typography` |
| 间距 | 统一使用 8dp 的倍数 |
| 分隔线 | 使用 `HorizontalDivider` |
| 统计区 | 三个 `StatItem` 用 `weight(1f)` 等分 |
| 手机号 | 用实验三写的 `maskPhone()` 扩展函数脱敏 |

---

### 预览要求

```kotlin
@Preview(name = "浅色", showBackground = true)
@Preview(name = "深色", showBackground = true,
         uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "大字体", showBackground = true, fontScale = 1.5f)
@Preview(name = "小屏", showBackground = true, widthDp = 320)
@Composable
fun ProfileCardPreview() {
    ComposeUILabTheme {
        ProfileCard(profile = sampleProfile)
    }
}
```

> ✅ **检查点 4**：四种预览效果全部正常显示，无内容溢出或截断

---

## 任务四：登录注册页

- 界面要求
- 校验逻辑
- Scaffold 骨架

---

### 界面要求

```text
        [ Logo 图标 ]
         欢迎登录

  ┌────────────────────────┐
  │ 👤 用户名               │
  └────────────────────────┘
  ┌────────────────────────┐
  │ 🔒 密码            👁   │
  └────────────────────────┘
  ☑ 记住我            忘记密码？

  ┌────────────────────────┐
  │        登  录           │
  └────────────────────────┘
  ──────── 其他方式 ────────
      [微信]  [QQ]  [手机]

     还没有账号？立即注册
```

---

### 功能要求

1. **密码可见性切换**：点击眼睛图标在明文/密文之间切换

```kotlin
var passwordVisible by remember { mutableStateOf(false) }

OutlinedTextField(
    value = password,
    onValueChange = { password = it },
    visualTransformation = if (passwordVisible) VisualTransformation.None
                           else PasswordVisualTransformation(),
    trailingIcon = {
        IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
                if (passwordVisible) Icons.Default.Visibility
                else Icons.Default.VisibilityOff,
                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
            )
        }
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
)
```

---

### 校验要求

| 字段 | 规则 | 错误提示 |
| --- | --- | --- |
| 用户名 | 3~20 个字符，字母数字下划线 | 用户名格式不正确 |
| 密码 | 至少 6 位，含字母和数字 | 密码至少 6 位且包含字母和数字 |

要求：

1. 失去焦点或输入时实时校验，用 `isError` + `supportingText` 显示错误
2. **两项都合法**时"登录"按钮才可点击（`enabled` 控制）
3. 点击登录后：显示 `CircularProgressIndicator` 2 秒（用 `LaunchedEffect` + `delay`），然后弹 Snackbar 提示"登录成功"

---

### Scaffold 骨架

```kotlin
@Composable
fun LoginScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录") },
                navigationIcon = {
                    IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { /* 表单内容 */ }
    }
}
```

> ⚠️ `padding(padding)` 必须用上，否则内容会被顶栏遮挡。

---

### 键盘处理

```kotlin
val focusManager = LocalFocusManager.current
val keyboard = LocalSoftwareKeyboardController.current

OutlinedTextField(
    // ...
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    keyboardActions = KeyboardActions(
        onNext = { focusManager.moveFocus(FocusDirection.Down) }
    )
)

// 密码框：完成后收起键盘并提交
keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); onLogin() })
```

> ✅ **检查点 5**：验证键盘弹出时页面可滚动，输入框不被遮挡

---

## 验收、思考与提交

- 验收标准
- 思考题
- 提交要求

---

### 验收标准

| 任务 | 分值 | 要点 |
| --- | --- | --- |
| 任务一 基础组件 | 20 | 组件齐全，操作日志正确记录 |
| 任务二 Modifier 实验 | 20 | 9 组对比全部完成，结论正确 |
| 任务三 名片页 | 30 | 拆分合理、4 种预览正常、无硬编码颜色 |
| 任务四 登录页 | 25 | 校验、密码切换、加载态、Snackbar 全部可用 |
| 代码规范 | 5 | 组件命名、参数顺序、`modifier` 透传 |

**加分项**：

- 名片页支持横屏时改为左右布局（`BoxWithConstraints`）（+5）
- 登录页增加"注册"页面与切换动画（+5）

---

### 常见问题

| 问题 | 处理办法 |
| --- | --- |
| Preview 不显示 | 确认有 `ui-tooling` 依赖；Build → Rebuild Project |
| 图标找不到 | 添加 `material-icons-extended` 依赖 |
| 内容被顶栏遮挡 | `Scaffold` 的 `paddingValues` 必须用到内容上 |
| 键盘遮挡输入框 | 内容加 `verticalScroll`；或 Activity 设 `adjustResize` |
| 深色模式颜色错乱 | 检查是否硬编码了 `Color.Black`/`Color.White` |
| 文字溢出屏幕 | 加 `maxLines` + `overflow`，或用 `weight` 约束宽度 |

---

### 思考题

1. 为什么可组合函数推荐把 `modifier: Modifier = Modifier` 作为第一个可选参数？如果不透传会有什么问题？
2. `.padding(16.dp).background(Color.Cyan)` 与 `.background(Color.Cyan).padding(16.dp)` 的渲染结果为什么不同？用"从外到内"的模型解释。
3. `Column` 中什么时候用 `Arrangement.spacedBy(8.dp)`，什么时候用给每个子项加 `padding`？各有什么优缺点？
4. 什么情况下必须用 `LazyColumn` 而不能用 `Column + verticalScroll`？
5. 为什么 Material 3 推荐用 `colorScheme.primary` 这类**语义化**颜色角色，而不是直接写色值？
6. `@Preview` 中为什么要包一层 `MyAppTheme {}`？不包会怎样？

---

### 提交要求

**提交物**（打包为 `学号_姓名_实验五.zip`）：

1. **项目源码**（删除 `build/`、`.gradle/`、`.idea/`）
2. **界面截图**
   - 任务一：组件展示页（可分 2~3 张）
   - 任务二：Modifier 对比截图 + 盒模型手绘图
   - 任务三：名片页四种预览（浅色/深色/大字体/小屏）
   - 任务四：登录页初始态、错误态、加载态、成功提示 四张
3. **实验报告**（PDF/Word）：步骤记录、任务二结论分析、思考题解答

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

- [Compose 布局基础](https://developer.android.google.cn/develop/ui/compose/layouts/basics)
- [Compose Modifier 列表](https://developer.android.google.cn/develop/ui/compose/modifiers-list)
- [Material 3 组件](https://m3.material.io/components)
- 理论课件：[ch-05.md](./ch-05.md)
