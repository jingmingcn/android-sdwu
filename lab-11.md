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

# 实验十一 权限、相机与后台任务

###### “调用系统能力之前，先想清楚要不要权限。”

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
- [任务一：运行时权限](#0)
- [任务二：相机与相册](#0)
- [任务三：通知](#0)
- [任务四：WorkManager](#0)
- [验收、思考与提交](#0)

---

## 实验目的与准备

- 实验目的
- 项目准备

---

### 实验目的

1. 掌握运行时权限的完整申请流程，包括"不再询问"的处理
2. 用系统相机 Intent 与 FileProvider 完成拍照并保存
3. 用 Photo Picker 选图，对比其与传统存储权限方案的差异
4. 创建通知渠道并发送可点击跳转的通知
5. 用 WorkManager 实现带约束的周期性后台任务
6. 理解"能不申请权限就不申请"的设计原则

**课时**：4 学时

---

### 项目准备

在前面实验的应用中新增"个人中心"页面，或新建项目 `SystemLab`。

```kotlin
dependencies {
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("io.coil-kt.coil3:coil-compose:3.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.core:core-ktx:1.13.1")
}
```

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

## 任务一：运行时权限

- 权限状态检查
- 完整申请流程
- 三种结果处理

---

### 步骤 1.1：权限状态展示页

做一个"权限中心"页面，列出 4 个权限的当前状态：

```kotlin
data class PermissionItem(
    val permission: String,
    val label: String,
    val description: String,
    val minSdk: Int = 1
)

val permissions = listOf(
    PermissionItem(Manifest.permission.CAMERA, "相机", "用于拍摄头像"),
    PermissionItem(Manifest.permission.ACCESS_FINE_LOCATION, "精确定位", "用于记录笔记位置"),
    PermissionItem(Manifest.permission.POST_NOTIFICATIONS, "通知", "用于任务提醒", 33),
    PermissionItem(Manifest.permission.RECORD_AUDIO, "麦克风", "用于语音笔记")
)
```

每行显示：权限名、用途说明、当前状态（已授权/未授权/已永久拒绝）、操作按钮。

---

### 步骤 1.2：完整申请流程

```kotlin
@Composable
fun PermissionRow(item: PermissionItem) {
    val context = LocalContext.current
    val activity = context as Activity
    var status by remember { mutableStateOf(checkStatus(context, item.permission)) }
    var showRationale by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = when {
            granted -> PermissionStatus.GRANTED
            !activity.shouldShowRequestPermissionRationale(item.permission) ->
                PermissionStatus.PERMANENTLY_DENIED
            else -> PermissionStatus.DENIED
        }
        if (status == PermissionStatus.PERMANENTLY_DENIED) showSettings = true
    }

    Button(onClick = {
        when {
            status == PermissionStatus.GRANTED -> { /* 已有权限 */ }
            activity.shouldShowRequestPermissionRationale(item.permission) ->
                showRationale = true
            else -> launcher.launch(item.permission)
        }
    }) { Text(status.actionLabel) }
}
```

---

### 步骤 1.3：三种对话框

```kotlin
// A. 申请前说明（用户曾拒绝过一次）
if (showRationale) {
    AlertDialog(
        onDismissRequest = { showRationale = false },
        icon = { Icon(Icons.Default.Info, null) },
        title = { Text("需要${item.label}权限") },
        text = { Text(item.description + "\n\n拒绝后该功能将不可用，你可以随时在设置中修改。") },
        confirmButton = {
            TextButton(onClick = { showRationale = false; launcher.launch(item.permission) }) {
                Text("去授权")
            }
        },
        dismissButton = { TextButton(onClick = { showRationale = false }) { Text("暂不") } }
    )
}

// B. 永久拒绝后引导设置
if (showSettings) {
    AlertDialog(
        title = { Text("权限已被拒绝") },
        text = { Text("请在「设置 → 应用 → 权限」中手动开启${item.label}权限") },
        confirmButton = {
            TextButton(onClick = { openAppSettings(context) }) { Text("去设置") }
        },
        /* ... */
    )
}
```

---

### 步骤 1.4：状态转换实验

**实验步骤**（每步后记录 `shouldShowRequestPermissionRationale` 的返回值与界面状态）：

| 步骤 | 操作 | `shouldShow...` | 状态 |
| --- | --- | --- | --- |
| 1 | 全新安装，未申请过 | | |
| 2 | 申请，点"拒绝" | | |
| 3 | 再次申请前检查 | | |
| 4 | 再申请，再点"拒绝" | | |
| 5 | 第三次申请前检查 | | |
| 6 | 从设置中授权后返回 | | |

> ✅ **检查点 1**：填写完整表格，并说明如何区分"首次未申请"与"已永久拒绝"（提示：这两种情况 `shouldShow...` 都返回 false）

---

### 步骤 1.5：多权限申请

```kotlin
val multiLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { results ->
    val granted = results.filterValues { it }.keys
    val denied = results.filterValues { !it }.keys
    log = "已授权：${granted.size} 个，被拒绝：${denied.joinToString()}"
}

Button(onClick = {
    multiLauncher.launch(arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ))
}) { Text("申请定位权限组") }
```

> 💡 **实验**：Android 12+ 在定位权限弹窗中选择"仅大致位置"，观察两个权限的授权结果分别是什么。

---

## 任务二：相机与相册

- 系统相机拍照
- FileProvider
- Photo Picker
- 方案对比

---

### 步骤 2.1：配置 FileProvider

```xml
<!-- AndroidManifest.xml -->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

```xml
<!-- res/xml/file_paths.xml -->
<paths>
    <external-files-path name="images" path="Pictures/" />
    <cache-path name="cache" path="." />
</paths>
```

---

### 步骤 2.2：拍照

```kotlin
fun createImageUri(context: Context): Uri {
    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "").apply {
        if (!exists()) mkdirs()
    }
    val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
```

```kotlin
var photoUri by remember { mutableStateOf<Uri?>(null) }
var savedUri by remember { mutableStateOf<Uri?>(null) }

val takePicture = rememberLauncherForActivityResult(
    ActivityResultContracts.TakePicture()
) { success -> if (success) savedUri = photoUri }

Button(onClick = {
    photoUri = createImageUri(context)
    photoUri?.let { takePicture.launch(it) }
}) { Text("拍照") }

savedUri?.let {
    AsyncImage(model = it, contentDescription = "拍摄的照片",
               modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)))
}
```

> ✅ **检查点 2**：拍照后照片正确显示；**记录是否需要 CAMERA 权限**

---

### 步骤 2.3：Photo Picker 选图

```kotlin
val pickMedia = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
) { uri -> if (uri != null) selectedUri = uri }

Button(onClick = {
    pickMedia.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )
}) { Text("从相册选择") }

// 多选（最多 5 张）
val pickMultiple = rememberLauncherForActivityResult(
    ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
) { uris -> selectedUris = uris }
```

> ✅ **检查点 3**：选图成功；**记录是否需要存储权限**

---

### 步骤 2.4：方案对比（必做）

在报告中填写：

| 方案 | 需要的权限 | 用户体验 | 兼容性 | 适用场景 |
| --- | --- | --- | --- | --- |
| `ACTION_IMAGE_CAPTURE` + FileProvider | | | | |
| CameraX 自建相机预览 | | | | |
| Photo Picker | | | | |
| `READ_EXTERNAL_STORAGE` + MediaStore 查询 | | | | |

> 💡 结论应当是：**优先选择不需要权限的方案**。请在报告中论证这一结论。

---

### 步骤 2.5：保存与压缩（加分项）

```kotlin
suspend fun compressAndSave(context: Context, uri: Uri, maxSize: Int = 1024): File =
    withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)!!
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(input, null, options)
        input.close()

        options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
        options.inJustDecodeBounds = false

        val bitmap = context.contentResolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, options)
        }!!

        val out = File(context.filesDir, "avatar.jpg")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        out
    }
```

> 记录压缩前后的文件大小与像素尺寸。

---

## 任务三：通知

- 创建渠道
- 发送通知
- 点击跳转

---

### 步骤 3.1：创建通知渠道

```kotlin
object NotificationHelper {
    const val CHANNEL_REMINDER = "reminder"
    const val CHANNEL_SYNC = "sync"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "任务提醒",
                                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "笔记到期提醒"
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SYNC, "后台同步",
                                NotificationManager.IMPORTANCE_LOW).apply {
                description = "数据同步状态"
            }
        )
    }
}
```

在 `Application.onCreate` 中调用。

---

### 步骤 3.2：发送通知

```kotlin
fun showReminder(context: Context, noteId: Long, title: String, content: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra("noteId", noteId)
    }
    val pending = PendingIntent.getActivity(
        context, noteId.toInt(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pending)
        .setAutoCancel(true)
        .addAction(R.drawable.ic_done, "标记完成", donePendingIntent)
        .build()

    if (hasNotificationPermission(context)) {
        NotificationManagerCompat.from(context).notify(noteId.toInt(), notification)
    }
}
```

---

### 步骤 3.3：三种通知样式

实现并截图：

```kotlin
// 1. 普通通知
.setContentTitle("标题").setContentText("单行内容")

// 2. 长文本
.setStyle(NotificationCompat.BigTextStyle().bigText(longContent))

// 3. 进度条（同步进度）
.setProgress(100, progress, false)
.setOngoing(true)      // 不可滑动清除
```

**点击跳转验证**：

1. 发送通知后**退到桌面**
2. 下拉通知栏点击通知
3. 验证应用打开并**跳转到对应的详情页**（通过 `intent.getLongExtra("noteId", -1)` 取参）

> ✅ **检查点 4**：录屏演示"发通知→退到桌面→点击通知→跳转到详情页"

---

### 步骤 3.4：Android 13 权限适配

```kotlin
@Composable
fun rememberNotificationPermissionState(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    return granted to {
        if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
```

> 💡 **实验**：在 Android 12 与 Android 13+ 的模拟器上分别测试，记录行为差异。

---

## 任务四：WorkManager

- 定义 Worker
- 约束条件
- 观察状态

---

### 步骤 4.1：定义 Worker

```kotlin
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Log.d("SyncWorker", "开始同步，尝试次数：$runAttemptCount")

        return try {
            setForeground(createForegroundInfo("正在同步..."))   // 可选：前台通知

            val repository = (applicationContext as NoteApplication)
                .container.noteRepository
            repository.syncWithServer()

            val duration = System.currentTimeMillis() - startTime
            Log.d("SyncWorker", "同步完成，耗时 ${duration}ms")

            Result.success(workDataOf("duration" to duration))
        } catch (e: IOException) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "未知错误")))
        }
    }
}
```

---

### 步骤 4.2：提交任务

```kotlin
object SyncScheduler {
    private const val WORK_NAME = "periodic_sync"

    fun scheduleOneTime(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("sync")
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)      // 仅 WiFi
                .setRequiresCharging(true)
                .build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }
}
```

---

### 步骤 4.3：观察任务状态

```kotlin
@Composable
fun SyncStatusCard(context: Context) {
    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosByTagLiveData("sync")
        .observeAsState(emptyList())

    val latest = workInfos.maxByOrNull { it.id.hashCode() }

    Card {
        Column(Modifier.padding(16.dp)) {
            Text("同步状态：${latest?.state?.name ?: "未开始"}")
            when (latest?.state) {
                WorkInfo.State.RUNNING -> LinearProgressIndicator()
                WorkInfo.State.SUCCEEDED ->
                    Text("耗时 ${latest.outputData.getLong("duration", 0)} ms")
                WorkInfo.State.FAILED ->
                    Text("失败：${latest.outputData.getString("error")}")
                WorkInfo.State.ENQUEUED -> Text("等待约束条件满足...")
                else -> Unit
            }
        }
    }
}
```

---

### 步骤 4.4：约束条件实验（必做）

对**周期任务**做以下实验，每次记录任务是否执行：

| 实验 | 网络 | 电量 | 充电 | 是否执行 |
| --- | --- | --- | --- | --- |
| 1 | WiFi | 充足 | 是 | |
| 2 | WiFi | 充足 | 否 | |
| 3 | 移动数据 | 充足 | 是 | |
| 4 | 飞行模式 | 充足 | 是 | |
| 5 | 飞行模式→恢复 WiFi | 充足 | 是 | |

**验证任务持久性**：

```bash
# 提交任务后杀掉应用进程
adb shell am force-stop com.example.systemlab
# 观察任务是否仍会执行
adb shell dumpsys jobscheduler | grep -A 5 systemlab
```

> ✅ **检查点 5**：填写实验表；证明"应用被杀后任务仍会执行"

---

## 验收、思考与提交

- 验收标准
- 思考题
- 提交要求

---

### 验收标准

| 任务 | 分值 | 要点 |
| --- | --- | --- |
| 任务一 权限 | 25 | 完整流程、三种对话框、状态转换表正确 |
| 任务二 相机相册 | 25 | 拍照与选图均可用、FileProvider 正确、方案对比表 |
| 任务三 通知 | 25 | 渠道创建、三种样式、点击跳转正确、13+ 权限适配 |
| 任务四 WorkManager | 20 | 约束条件实验表、状态观察、持久性验证 |
| 代码质量 | 5 | 权限逻辑封装复用、无硬编码 |

**加分项**：图片压缩并记录数据（+5）；传感器数据实时显示（+5）；前台服务实现计时器（+10）

---

### 常见问题

| 问题 | 原因与处理 |
| --- | --- |
| `FileUriExposedException` | 直接用了 `file://`，必须用 FileProvider |
| `IllegalArgumentException: Failed to find configured root` | `file_paths.xml` 中路径与实际保存位置不匹配 |
| 拍照返回但图片为空 | 目录未创建；或 `takePicture.launch(uri)` 的 uri 与 `createImageUri` 不是同一个 |
| 通知不显示 | Android 13+ 未申请 `POST_NOTIFICATIONS`；或渠道未创建；或系统设置中被关闭 |
| `PendingIntent` 崩溃 | Android 12+ 必须显式指定 `FLAG_IMMUTABLE` 或 `FLAG_MUTABLE` |
| WorkManager 任务不执行 | 约束未满足；或设备处于 Doze；或被厂商省电策略限制 |
| 周期任务间隔不准 | 系统最小间隔 15 分钟，且会做批量调度，不保证精确时间 |

---

### 思考题

1. `shouldShowRequestPermissionRationale` 在"首次从未申请"和"已永久拒绝"两种情况下都返回 `false`，实际开发中如何区分这两种状态？
2. 用 `ACTION_IMAGE_CAPTURE` 调用系统相机为什么**不需要** `CAMERA` 权限？什么情况下才必须申请它？
3. Photo Picker 相比 `READ_EXTERNAL_STORAGE` + MediaStore 在**隐私保护**上有什么本质改进？
4. Android 7.0 之后为什么禁止在 Intent 中传递 `file://` URI？FileProvider 是如何解决这个问题的？
5. 为什么 Android 12+ 强制要求 `PendingIntent` 指定可变性标志？不指定会有什么安全风险？
6. 为什么"每天凌晨 2 点同步数据"这类需求推荐用 WorkManager 而不是 `AlarmManager` + `Service`？
7. WorkManager 的周期任务最小间隔为什么是 15 分钟？这个限制是为了解决什么问题？
8. 结合本次实验，总结"权限最小化"的三条具体做法。

---

### 提交要求

**提交物**（打包为 `学号_姓名_实验十一.zip`）：

1. **项目源码**（删除 `build/`、`.gradle/`、`.idea/`）
2. **实验数据表**
   - 任务一：权限状态转换表
   - 任务二：四种方案对比表
   - 任务四：约束条件实验表
3. **演示视频**（2 分钟内，必交）
   - 权限申请的三种结果（允许/拒绝/永久拒绝后引导设置）
   - 拍照与选图
   - 通知发送与点击跳转
4. **实验报告**（PDF/Word）：步骤记录、截图、三张表、思考题解答

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

- [请求运行时权限](https://developer.android.google.cn/training/permissions/requesting)
- [Photo Picker](https://developer.android.google.cn/training/data-storage/shared/photopicker)
- [WorkManager](https://developer.android.google.cn/topic/libraries/architecture/workmanager)
- [创建通知](https://developer.android.google.cn/develop/ui/views/notifications/build-notification)
- 理论课件：[ch-11.md](./ch-11.md)
