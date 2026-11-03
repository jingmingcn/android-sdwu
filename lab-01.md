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

# 实验一 开发环境搭建与第一个应用

###### “Hello, World 是所有旅程的起点。”

张丽，井明
数据科学与计算机学院
jingming@sdu.edu.cn
2026年秋

---

<!-- _header: 目录<br>CONTENTS<br>![](images/logo-1_transparent.png)-->
<!-- _class: toc_b -->
<!-- _footer: "" -->
<!-- _paginate: "" -->

- [实验目的与环境](#0)
- [任务一：安装与配置](#0)
- [任务二：创建第一个项目](#0)
- [任务三：运行与调试](#0)
- [任务四：修改界面](#0)
- [验收、思考与提交](#0)

---

## 实验目的与环境

- 实验目的
- 实验环境
- 课时与考核

---

### 实验目的

完成本实验后，你应当能够：

1. 独立完成 JDK 与 Android Studio 的安装与国内镜像配置
2. 说明 Android 项目的目录结构与各 Gradle 文件的作用
3. 创建并运行一个 Jetpack Compose 项目
4. 使用模拟器与真机运行应用，使用 Logcat 查看日志
5. 修改可组合函数并通过 Preview 预览效果

---

### 实验环境

| 项目 | 要求 |
| --- | --- |
| 操作系统 | Windows 10/11 x64、macOS 12+ |
| 内存 | 建议 16 GB 及以上（最低 8 GB） |
| 磁盘 | 至少 20 GB 可用空间 |
| JDK | 17 或 21 |
| Android Studio | 最新稳定版 |
| SDK | API 24（最低）～ API 36（编译） |

**课时**：2 学时　**考核**：实验报告 + 现场演示

---

## 任务一：安装与配置

- 安装 JDK
- 安装 Android Studio
- 配置国内镜像

---

### 步骤 1.1：安装 JDK

1. 从华为云 OpenJDK 镜像下载 JDK 17
   `https://mirrors.huaweicloud.com/openjdk/`
2. 安装或解压到**无中文、无空格**的路径
3. 配置环境变量 `JAVA_HOME` 与 `Path`
4. 验证

```bash
java -version
javac -version
```

> ✅ **检查点 1**：命令输出显示 JDK 17.x 版本号，截图保存

---

### 步骤 1.2：安装 Android Studio

1. 下载：`https://developer.android.google.cn/studio`
2. 安装选项
   - "Do not import settings"
   - 安装路径：`D:\Android\AndroidStudio`（避开系统盘）
   - 组件：默认（含 Android Virtual Device）
3. 首次启动 Setup Wizard
   - SDK 路径：`D:\Android\Sdk`
   - **先取消向导中的自动下载**，配置好镜像后再下载

---

### 步骤 1.3：配置 Gradle 镜像

在用户目录创建 `.gradle/init.gradle`：

- Windows：`C:\Users\<用户名>\.gradle\init.gradle`
- macOS：`~/.gradle/init.gradle`

```groovy
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/central' }
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
        maven { url 'https://mirrors.huaweicloud.com/repository/maven' }
        google()
        mavenCentral()
    }
}
```

> ✅ **检查点 2**：文件已保存，编码为 UTF-8

---

### 步骤 1.4：SDK Manager

打开 Settings → Languages & Frameworks → Android SDK

**SDK Platforms** 勾选：

- Android API 36（或当前最新）
- Android 7.0 (API 24)

**SDK Tools** 勾选：

- Android SDK Build-Tools
- Android SDK Platform-Tools
- Android Emulator
- Android SDK Command-line Tools

> ✅ **检查点 3**：SDK 下载完成，无红色错误提示

---

## 任务二：创建第一个项目

- 新建项目
- 熟悉目录结构
- 阅读 Gradle 配置

---

### 步骤 2.1：新建项目

File → New → New Project → **Empty Activity**

| 配置项 | 值 |
| --- | --- |
| Name | `FirstApp` |
| Package name | `com.example.firstapp` |
| Save location | 无中文路径 |
| Minimum SDK | API 24 (Android 7.0) |
| Build configuration language | Kotlin DSL (build.gradle.kts) |

点击 Finish，等待首次 Gradle Sync（首次可能需要 5～15 分钟）。

---

### 步骤 2.2：对照目录结构

在 Project 视图切换到 **Android** 与 **Project** 两种模式，找到并记录：

```text
app/src/main/java/com/example/firstapp/MainActivity.kt
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml
app/build.gradle.kts
build.gradle.kts
settings.gradle.kts
gradle/libs.versions.toml
```

> ✅ **检查点 4**：在实验报告中填表说明上述 7 个文件各自的作用

---

### 步骤 2.3：阅读构建脚本

打开 `app/build.gradle.kts`，找到并记录以下值：

```kotlin
android {
    namespace = "________"
    compileSdk = ____
    defaultConfig {
        applicationId = "________"
        minSdk = ____
        targetSdk = ____
        versionCode = ____
        versionName = "____"
    }
}
```

> 💡 **思考**：`namespace` 与 `applicationId` 有什么区别？为什么可以不一样？

---

## 任务三：运行与调试

- 创建模拟器
- 运行应用
- 真机调试
- 使用 Logcat

---

### 步骤 3.1：创建模拟器

Tools → Device Manager → Create Virtual Device

1. 设备：**Pixel 6**
2. 系统镜像：API 34 或 35，带 **Google APIs**
   - Intel/AMD 电脑选 x86_64，Apple Silicon 选 arm64-v8a
3. AVD Name：`Pixel6_API34`
4. Finish

> ⚠️ 若下载缓慢，检查步骤 1.3 的镜像是否生效；也可先用真机完成实验。

---

### 步骤 3.2：运行应用

1. 工具栏选择设备 → 点击 **Run**（绿色三角，Shift+F10）
2. 观察 Build 输出窗口的编译过程
3. 应用启动后显示 "Hello Android!"

> ✅ **检查点 5**：截图保存模拟器中运行的应用界面

---

### 步骤 3.3：真机调试

1. 手机：设置 → 关于手机 → 连点"版本号"7 次
2. 开发者选项 → 打开 **USB 调试**
3. USB 连接电脑，手机端点击"允许"
4. 验证并安装

```bash
adb devices
```

> ✅ **检查点 6**：`adb devices` 输出中出现你的设备序列号并显示 `device`

---

### 步骤 3.4：使用 Logcat

在 `MainActivity.kt` 中添加日志：

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FirstApp", "应用启动，学号：202312345678")
        setContent { /* ... */ }
    }
}
```

在 Logcat 窗口的过滤框输入：

```text
tag:FirstApp
```

> ✅ **检查点 7**：Logcat 中能看到你的学号日志，截图保存

---

### 步骤 3.5：断点调试

1. 在 `Log.d(...)` 那一行左侧点击设置断点（红点）
2. 点击 **Debug**（Shift+F9）
3. 程序在断点处暂停，在 Variables 面板查看 `savedInstanceState` 的值
4. 按 F8 单步执行，F9 继续运行

> ✅ **检查点 8**：截图保存断点暂停时的调试面板

---

## 任务四：修改界面

- 修改文本与样式
- 使用 Preview
- 增加交互

---

### 步骤 4.1：修改问候语

打开 `MainActivity.kt`，把 `Greeting` 改成显示你的个人信息：

```kotlin
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(24.dp)) {
        Text(text = "姓名：$name", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(text = "学号：202312345678", fontSize = 18.sp)
        Text(text = "专业：计算机科学与技术", fontSize = 18.sp)
    }
}
```

在 `setContent` 中调用：`Greeting(name = "张三")`

---

### 步骤 4.2：使用 Preview

```kotlin
@Preview(showBackground = true, name = "浅色")
@Preview(showBackground = true, name = "深色",
         uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GreetingPreview() {
    FirstAppTheme {
        Greeting("张三")
    }
}
```

点击编辑器右上角 **Split** 或 **Design** 查看预览。

> ✅ **检查点 9**：截图保存浅色与深色两种预览效果

---

### 步骤 4.3：增加一个按钮

```kotlin
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var clickCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("姓名：$name", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("学号：202312345678", fontSize = 18.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { clickCount++ }) {
            Text("点我：已点击 $clickCount 次")
        }
    }
}
```

> ✅ **检查点 10**：在真机或模拟器上点击按钮，数字正确递增

---

## 验收、思考与提交

- 验收标准
- 常见问题
- 思考题
- 提交要求

---

### 验收标准

| 项目 | 分值 | 标准 |
| --- | --- | --- |
| 环境搭建 | 20 | JDK、Studio、SDK、镜像配置正确 |
| 项目创建 | 15 | 项目能正常 Sync 与编译 |
| 运行调试 | 25 | 模拟器/真机运行成功，Logcat 与断点会用 |
| 界面修改 | 25 | 显示个人信息，按钮交互正常，Preview 可用 |
| 实验报告 | 15 | 结构完整，截图齐全，思考题有分析 |

---

### 常见问题排查

| 问题 | 处理办法 |
| --- | --- |
| Gradle Sync 一直转圈 | 检查 `init.gradle`；File → Invalidate Caches → Restart |
| `SDK location not found` | 在项目根目录建 `local.properties`，写 `sdk.dir=D:\\Android\\Sdk` |
| 模拟器启动黑屏/很慢 | 换镜像架构；Windows 开启 WHPX/HAXM；关闭其他虚拟机软件 |
| 真机 `adb devices` 无设备 | 换数据线（有些线只能充电）；安装厂商 USB 驱动 |
| 中文显示乱码 | File Encoding 全部设为 UTF-8 |
| 编译报错 Unsupported class file | JDK 版本与 AGP 不匹配，改用 JDK 17 |

---

### 思考题

1. `compileSdk`、`minSdk`、`targetSdk` 三者的含义分别是什么？如果把 `minSdk` 从 24 改成 21，会带来什么影响？
2. `namespace` 与 `applicationId` 有什么区别？分别在什么时候起作用？
3. Gradle Wrapper（`gradlew`）的作用是什么？为什么不建议在本机全局安装 Gradle？
4. `setContent {}` 与传统的 `setContentView(R.layout.xxx)` 有什么本质区别？
5. 查阅资料说明：为什么 debug 包可以直接安装，而 release 包必须签名？

---

### 提交要求

**提交物**（打包为 `学号_姓名_实验一.zip`）：

1. **实验报告**（PDF 或 Word）
   - 实验目的、环境、步骤记录
   - 10 个检查点的截图（每张需包含你的学号）
   - 遇到的问题及解决过程
   - 思考题解答
2. **项目源码**（删除 `build/`、`.gradle/`、`.idea/` 目录后打包）

**截止时间**：下次实验课前

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

- [Android Studio 用户指南](https://developer.android.google.cn/studio/intro)
- [创建您的第一个应用](https://developer.android.google.cn/get-started/overview)
- 理论课件：[ch-01.md](./ch-01.md)
