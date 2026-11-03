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

# 实验四 Activity 生命周期与页面跳转

###### “看不见的生命周期，用日志把它照出来。”

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
- [任务一：生命周期观察](#0)
- [任务二：状态保存对比](#0)
- [任务三：Intent 跳转与传参](#0)
- [任务四：资源与多语言](#0)
- [验收、思考与提交](#0)

---

## 实验目的与准备

- 实验目的
- 项目准备

---

### 实验目的

1. 通过日志**实测**并记录 Activity 生命周期的完整回调序列
2. 理解配置变更导致的重建，掌握三种状态保存机制的差异
3. 掌握显式 Intent、隐式 Intent 与 Activity Result API
4. 会使用资源限定符实现多语言与深色模式适配
5. 能读懂 `AndroidManifest.xml` 并正确注册组件

**课时**：2 学时

---

### 项目准备

新建项目：

| 配置项 | 值 |
| --- | --- |
| Name | `LifecycleLab` |
| Package | `com.example.lifecyclelab` |
| Template | Empty Activity（Compose） |
| Minimum SDK | API 24 |

创建三个 Activity：`MainActivity`（已有）、`SecondActivity`、`DialogActivity`

```kotlin
// New → Activity → Empty Activity，注意勾选 Launcher Activity = false
```

---

## 任务一：生命周期观察

- 建立日志基础设施
- 实测七种场景
- 记录回调序列

---

### 步骤 1.1：抽出日志基类

```kotlin
abstract class LoggingActivity : ComponentActivity() {
    private val tag = "LIFECYCLE"
    private val who: String get() = this::class.simpleName ?: "Unknown"

    private fun log(event: String) = Log.d(tag, "$who → $event")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("onCreate(savedInstanceState=${if (savedInstanceState == null) "null" else "有数据"})")
    }
    override fun onStart()   { super.onStart();   log("onStart") }
    override fun onResume()  { super.onResume();  log("onResume") }
    override fun onPause()   { super.onPause();   log("onPause") }
    override fun onStop()    { super.onStop();    log("onStop") }
    override fun onRestart() { super.onRestart(); log("onRestart") }
    override fun onDestroy() { super.onDestroy(); log("onDestroy") }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState); log("onSaveInstanceState")
    }
}
```

---

### 步骤 1.2：让三个 Activity 继承它

```kotlin
class MainActivity : LoggingActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MyTheme { MainScreen() } }
    }
}
```

```xml
<!-- AndroidManifest.xml：DialogActivity 使用对话框主题 -->
<activity
    android:name=".DialogActivity"
    android:theme="@style/Theme.AppCompat.Dialog"
    android:exported="false" />
```

在 Logcat 过滤框输入：`tag:LIFECYCLE`

---

### 步骤 1.3：实测七种场景

依次操作并**完整记录** Logcat 输出：

| 编号 | 场景 | 操作 |
| --- | --- | --- |
| S1 | 首次启动 | 从桌面启动应用 |
| S2 | 启动第二个页面 | 点击"跳转到 Second" |
| S3 | 从第二页返回 | 按系统返回键 |
| S4 | 按 Home 键 | 回到桌面 |
| S5 | 从后台恢复 | 从任务列表回到应用 |
| S6 | 弹出对话框式 Activity | 点击"打开对话框页" |
| S7 | 旋转屏幕 | 模拟器旋转（Ctrl+F11 / 侧边旋转按钮） |

> ✅ **检查点 1**：把 7 组日志整理成表格填入实验报告

---

### 步骤 1.4：填写对照表

在报告中填写下表并与理论课件对照：

| 场景 | 你实测的回调序列 | 是否与预期一致 |
| --- | --- | --- |
| S1 | `onCreate → onStart → onResume` | |
| S2 | | |
| S3 | | |
| S4 | | |
| S5 | | |
| S6 | | |
| S7 | | |

> 💡 **重点观察**：S6 中 `MainActivity` 有没有走 `onStop`？为什么？

---

## 任务二：状态保存对比

- 三种机制对比实验
- 进程被杀验证

---

### 步骤 2.1：搭建对比界面

在 `MainScreen` 中放置四个计数器，它们各用不同机制保存状态：

```kotlin
@Composable
fun StateComparisonScreen(viewModel: CounterViewModel = viewModel()) {
    var plain = 0                                              // A：普通变量
    var remembered by remember { mutableIntStateOf(0) }        // B：remember
    var saveable by rememberSaveable { mutableIntStateOf(0) }  // C：rememberSaveable
    val fromVm by viewModel.count.collectAsStateWithLifecycle() // D：ViewModel

    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CounterRow("A 普通变量", plain) { plain++ }
        CounterRow("B remember", remembered) { remembered++ }
        CounterRow("C rememberSaveable", saveable) { saveable++ }
        CounterRow("D ViewModel", fromVm) { viewModel.increment() }
    }
}
```

---

### 步骤 2.2：CounterViewModel

```kotlin
class CounterViewModel : ViewModel() {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    init { Log.d("LIFECYCLE", "CounterViewModel 创建") }

    fun increment() { _count.value++ }

    override fun onCleared() {
        super.onCleared()
        Log.d("LIFECYCLE", "CounterViewModel onCleared")
    }
}
```

依赖：

```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
```

---

### 步骤 2.3：三轮验证

**第 1 轮：旋转屏幕**

四个计数器各点 3 次 → 旋转屏幕 → 记录哪些归零

**第 2 轮：模拟进程被杀**

1. 四个计数器各点 3 次
2. 按 Home 键回到桌面
3. 在 Android Studio 的 Logcat 工具栏点击 **Terminate Application**
   （或执行 `adb shell am kill com.example.lifecyclelab`）
4. 从任务列表恢复应用 → 记录哪些值被保留

**第 3 轮：完全退出**

按返回键退出 → 重新打开 → 记录结果

---

### 步骤 2.4：填写结论表

| 机制 | 旋转屏幕 | 进程被杀后恢复 | 完全退出后重开 |
| --- | --- | --- | --- |
| A 普通变量 | | | |
| B `remember` | | | |
| C `rememberSaveable` | | | |
| D `ViewModel` | | | |

> ✅ **检查点 2**：观察 Logcat，`CounterViewModel 创建` 在旋转屏幕时**有没有**再次打印？为什么？

---

<!-- _footer: "" -->

### 步骤 2.5：onSaveInstanceState 实测

在 `MainActivity` 中增加：

```kotlin
private var draft: String = ""

override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString("draft", draft)
    Log.d("LIFECYCLE", "保存草稿：$draft")
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    draft = savedInstanceState?.getString("draft") ?: ""
    Log.d("LIFECYCLE", "恢复草稿：$draft")
    setContent { /* ... */ }
}
```

> ✅ **检查点 3**：验证 `onSaveInstanceState` 在哪些场景会被调用，在哪些场景不会（提示：按返回键退出时会不会调用？）

---

## 任务三：Intent 跳转与传参

- 显式 Intent 传参
- Activity Result 返回值
- 隐式 Intent

---

### 步骤 3.1：显式 Intent 传参

`MainActivity` → `SecondActivity`，传递学生信息：

```kotlin
// 定义可传递对象
@Parcelize
data class Student(val id: String, val name: String, val major: String) : Parcelable
```

```kotlin
// build.gradle.kts
plugins { id("kotlin-parcelize") }
```

```kotlin
// 发送
val intent = Intent(context, SecondActivity::class.java).apply {
    putExtra("student", Student("202312345678", "张三", "计算机科学与技术"))
    putExtra("from", "MainActivity")
}
context.startActivity(intent)
```

```kotlin
// 接收（SecondActivity）
val student = if (Build.VERSION.SDK_INT >= 33)
    intent.getParcelableExtra("student", Student::class.java)
else @Suppress("DEPRECATION") intent.getParcelableExtra<Student>("student")
```

---

### 步骤 3.2：接收返回结果

`SecondActivity` 提供一个输入框，用户输入备注后返回给 `MainActivity`：

```kotlin
// MainActivity
val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        remark = result.data?.getStringExtra("remark") ?: ""
    }
}

Button(onClick = {
    launcher.launch(Intent(context, SecondActivity::class.java))
}) { Text("去填写备注") }

Text("收到备注：$remark")
```

---

```kotlin
// SecondActivity 返回
setResult(Activity.RESULT_OK, Intent().putExtra("remark", inputText))
finish()
```

> ✅ **检查点 4**：录制或截图演示"跳转→输入→返回→主页显示"完整流程

---

### 步骤 3.3：隐式 Intent

实现一个"工具箱"页面，包含 6 个按钮：

```kotlin
// 1. 打开网页
startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.android.google.cn")))

// 2. 拨号（跳到拨号盘，不直接拨出，无需权限）
startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:10086")))

// 3. 发送短信
startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:10086")).apply {
    putExtra("sms_body", "来自 LifecycleLab 的问候")
})

```

---

```

// 4. 分享文本
startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, "我在学 Android 开发")
}, "分享到"))

// 5. 打开系统设置
startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))

// 6. 打开应用详情页
startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", packageName, null)))
```

> ⚠️ 用 `resolveActivity` 或 try-catch 处理"没有应用能处理该 Intent"的情况

---

### 步骤 3.4：启动模式实验

在 `AndroidManifest.xml` 中给 `SecondActivity` 依次设置四种 `launchMode`，每次都执行：

**操作**：Main → Second → Second → Second，然后连按返回键

```xml
<activity android:name=".SecondActivity" android:launchMode="standard" />
```

| launchMode | 需要按几次返回键回到 Main | 是否调用 `onNewIntent` |
| --- | --- | --- |
| `standard` | | |
| `singleTop` | | |
| `singleTask` | | |
| `singleInstance` | | |

> 💡 用 `adb shell dumpsys activity activities | grep -A 20 "Task"` 观察任务栈

---

## 任务四：资源与多语言

- 字符串资源
- 多语言
- 深色模式

---

### 步骤 4.1：抽取字符串资源

把界面上所有硬编码的中文抽到 `res/values/strings.xml`：

```xml
<resources>
    <string name="app_name">生命周期实验</string>
    <string name="btn_go_second">跳转到第二页</string>
    <string name="student_info">学号：%1$s　姓名：%2$s</string>
    <string name="click_count">已点击 %1$d 次</string>
</resources>
```

```kotlin
Text(stringResource(R.string.btn_go_second))
Text(stringResource(R.string.student_info, id, name))
```

> ⚠️ Android Studio 快捷方式：选中中文字符串 → `Alt+Enter` → Extract string resource

---

### 步骤 4.2：添加英文资源

新建 `res/values-en/strings.xml`：

```xml
<resources>
    <string name="app_name">Lifecycle Lab</string>
    <string name="btn_go_second">Go to Second</string>
    <string name="student_info">ID: %1$s  Name: %2$s</string>
    <string name="click_count">Clicked %1$d times</string>
</resources>
```

**验证**：设置 → 系统 → 语言 → 添加 English 并置顶 → 回到应用

> ✅ **检查点 5**：截图保存中英文两种界面
> 💡 切换语言时观察 Logcat，Activity 是否重建？

---

### 步骤 4.3：深色模式适配

```kotlin
// ui/theme/Theme.kt 中确认使用了 isSystemInDarkTheme()
@Composable
fun MyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

**要求**：

1. 界面中**不允许**出现硬编码颜色（如 `Color.Black`），全部改用 `MaterialTheme.colorScheme.*`
2. 在系统设置中切换深色模式，验证界面正确变化
3. 观察 Logcat，切换深色模式时 Activity 是否重建？

> ✅ **检查点 6**：截图保存浅色/深色两种效果

---

## 验收、思考与提交

- 验收标准
- 思考题
- 提交要求

---

### 验收标准

| 任务 | 分值 | 要点 |
| --- | --- | --- |
| 任务一 生命周期 | 25 | 7 种场景日志完整，对照表填写正确 |
| 任务二 状态保存 | 25 | 三轮验证完成，结论表正确且能解释 |
| 任务三 Intent | 25 | 传参、返回结果、6 个隐式 Intent 全部可用 |
| 任务四 资源适配 | 15 | 无硬编码字符串与颜色，中英文/深浅色均正常 |
| 实验报告 | 10 | 截图齐全，分析有据 |

**加分项**：完成启动模式实验表并能解释每种模式的差异（+10）

---

### 常见问题

| 问题 | 处理办法 |
| --- | --- |
| Logcat 看不到日志 | 检查过滤条件；确认选中了正确的设备和进程 |
| `@Parcelize` 报错 | 确认已添加 `kotlin-parcelize` 插件并 Sync |
| 旋转屏幕没反应 | 模拟器需关闭"自动旋转锁定"；扩展控制中开启旋转 |
| 切换语言后应用没变 | 确认 `values-en` 目录名正确（不是 `values-EN`） |
| `startActivity` 崩溃 | 检查 Activity 是否在 Manifest 中注册 |
| 隐式 Intent 找不到应用 | 用 `intent.resolveActivity(packageManager) != null` 判断 |

---

### 思考题

1. 场景 S6（弹出对话框式 Activity）中，`MainActivity` 为什么只走 `onPause` 而不走 `onStop`？
2. 旋转屏幕时 `CounterViewModel` 没有重新创建，但 Activity 重建了。ViewModel 是如何做到"跨重建存活"的？
3. `onSaveInstanceState` 与 `onPause` 哪个先调用？用户主动按返回键退出时 `onSaveInstanceState` 会被调用吗？为什么？
4. 如果在 Manifest 中给 Activity 加上 `android:configChanges="orientation|screenSize"`，旋转屏幕将不再重建。这样做的代价是什么？
5. 为什么 Android 7.0 之后禁止在 Intent 中直接传递 `file://` URI？
6. `Parcelable` 与 `Serializable` 的区别是什么？为什么 Android 推荐前者？

---

### 提交要求

**提交物**（打包为 `学号_姓名_实验四.zip`）：

1. **项目源码**（删除 `build/`、`.gradle/`、`.idea/`）
2. **实验报告**（PDF/Word）
   - 任务一：7 种场景的 Logcat 原始日志 + 对照表
   - 任务二：三轮验证的结论表 + 原理分析
   - 任务三：跳转与返回流程截图；启动模式对照表（选做）
   - 任务四：中英文、深浅色四张界面截图
   - 思考题解答
3. **演示视频**（可选，30 秒内，加分项）

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

---

- [Activity 生命周期](https://developer.android.google.cn/guide/components/activities/activity-lifecycle)
- [Intent 和 Intent 过滤器](https://developer.android.google.cn/guide/components/intents-filters)
- [应用资源概览](https://developer.android.google.cn/guide/topics/resources/providing-resources)
- 理论课件：[ch-04.md](./ch-04.md)
