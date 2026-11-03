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

# 实验九 协程与网络请求

###### “断网了，应用也要能用。”

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
- [任务一：协程基础实验](#0)
- [任务二：Retrofit 接入](#0)
- [任务三：搜索与状态处理](#0)
- [任务四：离线优先](#0)
- [验收、思考与提交](#0)

---

## 实验目的与准备

- 实验目的
- 项目准备
- 接口选择

---

### 实验目的

1. 实测协程的串行与并发执行差异，理解调度器的作用
2. 掌握结构化并发：取消传播、超时、异常处理
3. 用 Retrofit + kotlinx.serialization 完成网络请求
4. 实现加载中/成功/失败/空 四种界面状态的完整处理
5. 用 `debounce` + `flatMapLatest` 实现实时搜索
6. 实现离线优先架构：网络写数据库，UI 只读数据库

**课时**：4 学时

---

### 项目准备

新建项目 `GitHubExplorer`（或在 `NoteApp` 中新增模块）。

```kotlin
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("io.coil-kt.coil3:coil-compose:3.0.0")
    implementation("androidx.room:room-runtime:2.6.1")   // 任务四需要
}
```

```kotlin
plugins { alias(libs.plugins.kotlin.serialization) }
```

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

### 接口选择

**主选：GitHub 公开 API**（无需 Token，有频率限制 60 次/小时）

```text
https://api.github.com/search/repositories?q=kotlin&per_page=20
https://api.github.com/users/{login}
```

**备选**（任选其一，需在报告中说明）：

- 和风天气（需注册免费 Key）
- 一言 API：`https://v1.hitokoto.cn/`
- 自建 Mock 服务：用 `json-server` 在本机启动，模拟器访问 `http://10.0.2.2:3000/`

---

## 任务一：协程基础实验

- 串行 vs 并发
- 调度器
- 取消与超时

---

### 步骤 1.1：串行与并发对比

```kotlin
suspend fun fakeApi(name: String, delayMs: Long): String {
    delay(delayMs)
    return "$name 完成"
}

fun task1_1() = runBlocking {
    val serial = measureTimeMillis {
        val a = fakeApi("A", 1000)
        val b = fakeApi("B", 1000)
        val c = fakeApi("C", 1000)
        println("$a $b $c")
    }
    println("串行耗时：$serial ms")

    val parallel = measureTimeMillis {
        coroutineScope {
            val da = async { fakeApi("A", 1000) }
            val db = async { fakeApi("B", 1000) }
            val dc = async { fakeApi("C", 1000) }
            println("${da.await()} ${db.await()} ${dc.await()}")
        }
    }
    println("并发耗时：$parallel ms")
}
```

> ✅ **检查点 1**：记录两个耗时，解释为什么并发≈1000ms 而非 3000ms

---

### 步骤 1.2：调度器与线程

```kotlin
fun task1_2() = runBlocking {
    println("主协程：${Thread.currentThread().name}")

    launch(Dispatchers.Main.immediate) { println("Main: ${Thread.currentThread().name}") }
    launch(Dispatchers.IO)      { println("IO: ${Thread.currentThread().name}") }
    launch(Dispatchers.Default) { println("Default: ${Thread.currentThread().name}") }

    withContext(Dispatchers.IO) {
        println("切换到 IO：${Thread.currentThread().name}")
        withContext(Dispatchers.Default) {
            println("再切到 Default：${Thread.currentThread().name}")
        }
        println("回到 IO：${Thread.currentThread().name}")
    }
}
```

**对比实验**：在 Activity 中分别执行下面两段，观察界面是否卡死 3 秒。

```kotlin
lifecycleScope.launch { Thread.sleep(3000) }   // A
lifecycleScope.launch { delay(3000) }          // B
```

> ✅ **检查点 2**：说明 `Thread.sleep` 与 `delay` 的本质区别

---

### 步骤 1.3：取消与超时

```kotlin
fun task1_3() = runBlocking {
    // 1. 取消传播
    val parent = launch {
        launch { repeat(100) { i -> delay(200); println("子协程1: $i") } }
        launch { repeat(100) { i -> delay(200); println("子协程2: $i") } }
    }
    delay(1000)
    parent.cancel()
    println("父协程取消后，两个子协程是否停止？")

    // 2. 不响应取消的协程
    val stubborn = launch(Dispatchers.Default) {
        var i = 0
        while (i < 5) {                    // 没有检查 isActive
            Thread.sleep(500)              // 阻塞式，无法被取消
            println("顽固协程 ${i++}")
        }
    }
    delay(600); stubborn.cancel()
    println("顽固协程被取消了吗？")

    // 3. 超时
    val r = withTimeoutOrNull(1500) { fakeApi("慢接口", 3000) }
    println("超时结果：${r ?: "超时返回 null"}")
}
```

> ✅ **检查点 3**：修正"顽固协程"，使其能被正确取消（提示：`ensureActive()` / `isActive`）

---

### 步骤 1.4：异常处理

```kotlin
fun task1_4() = runBlocking {
    // A. 普通 Job：一个失败，兄弟全被取消
    runCatching {
        coroutineScope {
            launch { delay(100); throw IOException("接口 A 失败") }
            launch { delay(500); println("接口 B 完成") }   // 会打印吗？
        }
    }.onFailure { println("捕获：${it.message}") }

    // B. SupervisorJob：互相独立
    supervisorScope {
        launch(CoroutineExceptionHandler { _, e -> println("处理：${e.message}") }) {
            delay(100); throw IOException("接口 A 失败")
        }
        launch { delay(500); println("接口 B 完成") }        // 会打印吗？
    }
}
```

> ✅ **检查点 4**：填表说明 A、B 两种情况下"接口 B 完成"是否打印及原因

---

## 任务二：Retrofit 接入

- 定义接口
- DTO 与映射
- 统一结果封装

---

### 步骤 2.1：定义 API 与 DTO

```kotlin
@Serializable
data class RepoDto(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val description: String? = null,
    @SerialName("stargazers_count") val stars: Int = 0,
    @SerialName("forks_count") val forks: Int = 0,
    val language: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val owner: OwnerDto
)

@Serializable
data class OwnerDto(val login: String, @SerialName("avatar_url") val avatarUrl: String)

@Serializable
data class SearchResponse(
    @SerialName("total_count") val totalCount: Int,
    val items: List<RepoDto> = emptyList()
)

interface GitHubApi {
    @GET("search/repositories")
    suspend fun searchRepos(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): SearchResponse
}
```

---

### 步骤 2.2：构建 Retrofit

```kotlin
object NetworkModule {
    private val json = Json {
        ignoreUnknownKeys = true       // 忽略未声明字段，接口新增字段不会崩溃
        coerceInputValues = true
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    val api: GitHubApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GitHubApi::class.java)
}
```

> ✅ **检查点 5**：Logcat 中能看到完整的请求 URL、Header 与响应体

---

### 步骤 2.3：统一结果封装

```kotlin
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val code: Int? = null, val message: String) : NetworkResult<Nothing>
}

suspend fun <T> safeApiCall(block: suspend () -> T): NetworkResult<T> = try {
    NetworkResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    NetworkResult.Error(e.code(), when (e.code()) {
        403 -> "请求过于频繁，请稍后再试"
        404 -> "资源不存在"
        in 500..599 -> "服务器开小差了"
        else -> "请求失败（${e.code()}）"
    })
} catch (e: SocketTimeoutException) {
    NetworkResult.Error(message = "请求超时，请检查网络")
} catch (e: IOException) {
    NetworkResult.Error(message = "网络连接失败，请检查网络设置")
} catch (e: Exception) {
    NetworkResult.Error(message = e.message ?: "未知错误")
}
```

---

### 步骤 2.4：DTO → 领域模型

```kotlin
data class Repo(
    val id: Long, val name: String, val fullName: String,
    val description: String, val stars: Int, val forks: Int,
    val language: String, val ownerName: String, val ownerAvatar: String,
    val htmlUrl: String
)

fun RepoDto.toDomain() = Repo(
    id = id,
    name = name,
    fullName = fullName,
    description = description ?: "暂无描述",
    stars = stars,
    forks = forks,
    language = language ?: "未知",
    ownerName = owner.login,
    ownerAvatar = owner.avatarUrl,
    htmlUrl = htmlUrl
)
```

> ⚠️ **必做**：DTO 中所有可能为空的字段都要声明为可空并给默认值，否则接口返回 null 时会解析失败。

---

## 任务三：搜索与状态处理

- 四种界面状态
- 实时搜索
- 分页加载

---

### 步骤 3.1：UI State 与四种状态

```kotlin
data class SearchUiState(
    val query: String = "",
    val repos: List<Repo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val totalCount: Int = 0,
    val page: Int = 1,
    val hasMore: Boolean = true
) {
    val isEmpty: Boolean get() = repos.isEmpty() && !isLoading && error == null
}
```

```kotlin
when {
    uiState.isLoading -> LoadingView()
    uiState.error != null -> ErrorView(uiState.error, onRetry = viewModel::retry)
    uiState.isEmpty -> EmptyView(uiState.query)
    else -> RepoList(uiState.repos, onLoadMore = viewModel::loadMore)
}
```

> ✅ **检查点 6**：四种状态都要能演示（关掉网络演示错误态，搜索无结果的关键字演示空态）

---

### 步骤 3.2：实时搜索（debounce）

```kotlin
class SearchViewModel(private val repository: RepoRepository) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(400)                       // 停止输入 400ms 后才请求
                .filter { it.trim().length >= 2 }    // 至少 2 个字符
                .distinctUntilChanged()
                .collectLatest { q ->                // 新查询到达时取消旧请求
                    _uiState.update { it.copy(isLoading = true, error = null, page = 1) }
                    when (val r = repository.search(q, page = 1)) {
                        is NetworkResult.Success -> _uiState.update {
                            it.copy(repos = r.data.items, totalCount = r.data.total,
                                    isLoading = false)
                        }
                        is NetworkResult.Error -> _uiState.update {
                            it.copy(error = r.message, isLoading = false)
                        }
                    }
                }
        }
    }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
        queryFlow.value = q
    }
}
```

---

### 步骤 3.3：验证 debounce 效果

**实验**：在搜索框中快速输入 `kotlin`（6 个字符），观察 Logcat 中的请求日志。

| 配置 | 实际发出的请求数 |
| --- | --- |
| 无 `debounce` | |
| `debounce(400)` | |
| `debounce(400)` + `distinctUntilChanged` | |

> ✅ **检查点 7**：截图 Logcat 请求日志，填写上表并解释

---

### 步骤 3.4：分页加载

```kotlin
fun loadMore() {
    val state = _uiState.value
    if (state.isLoadingMore || !state.hasMore) return

    viewModelScope.launch {
        _uiState.update { it.copy(isLoadingMore = true) }
        when (val r = repository.search(state.query, state.page + 1)) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(
                    repos = it.repos + r.data.items,
                    page = it.page + 1,
                    hasMore = r.data.items.isNotEmpty(),
                    isLoadingMore = false
                )
            }
            is NetworkResult.Error -> _uiState.update {
                it.copy(error = r.message, isLoadingMore = false)
            }
        }
    }
}
```

```kotlin
// 触底加载
val shouldLoadMore by remember {
    derivedStateOf {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        last >= uiState.repos.size - 3
    }
}
LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }
```

---

### 步骤 3.5：列表项与图片加载

```kotlin
@Composable
fun RepoCard(repo: Repo, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(16.dp)) {
            AsyncImage(
                model = repo.ownerAvatar,
                contentDescription = "${repo.ownerName} 的头像",
                modifier = Modifier.size(48.dp).clip(CircleShape),
                placeholder = painterResource(R.drawable.placeholder),
                error = painterResource(R.drawable.avatar_error)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(repo.fullName, style = MaterialTheme.typography.titleMedium,
                     maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(repo.description, style = MaterialTheme.typography.bodySmall,
                     maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconText(Icons.Default.Star, "${repo.stars}")
                    IconText(Icons.Default.CallSplit, "${repo.forks}")
                    Text(repo.language, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
```

---

## 任务四：离线优先

- 架构改造
- 缓存策略
- 断网验证

---

### 步骤 4.1：加入本地缓存

```kotlin
@Entity(tableName = "repos")
data class RepoEntity(
    @PrimaryKey val id: Long,
    val name: String, val fullName: String, val description: String,
    val stars: Int, val forks: Int, val language: String,
    val ownerName: String, val ownerAvatar: String, val htmlUrl: String,
    val query: String,                                  // 属于哪次搜索
    val cachedAt: Long = System.currentTimeMillis()
)

@Dao
interface RepoDao {
    @Query("SELECT * FROM repos WHERE query = :q ORDER BY stars DESC")
    fun observeByQuery(q: String): Flow<List<RepoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repos: List<RepoEntity>)

    @Query("DELETE FROM repos WHERE query = :q")
    suspend fun deleteByQuery(q: String)

    @Query("DELETE FROM repos WHERE cachedAt < :expireTime")
    suspend fun deleteExpired(expireTime: Long)
}
```

---

### 步骤 4.2：单一可信来源

```kotlin
class RepoRepository(private val api: GitHubApi, private val dao: RepoDao) {

    // UI 只观察数据库
    fun observeRepos(query: String): Flow<List<Repo>> =
        dao.observeByQuery(query).map { list -> list.map { it.toDomain() } }

    // 刷新：拉网络 → 写数据库 → Flow 自动推送
    suspend fun refresh(query: String): NetworkResult<Unit> = safeApiCall {
        val response = api.searchRepos(query)
        dao.deleteByQuery(query)
        dao.insertAll(response.items.map { it.toEntity(query) })
    }

    // 清理 24 小时前的缓存
    suspend fun cleanExpired() =
        dao.deleteExpired(System.currentTimeMillis() - 24 * 3600_000)
}
```

> ✅ **检查点 8**：改造后 ViewModel 中**不再**直接持有网络返回的列表

---

### 步骤 4.3：断网验证

**实验步骤**：

1. 联网状态下搜索 `kotlin`，等待数据加载完成
2. 打开飞行模式（或 `adb shell svc wifi disable && adb shell svc data disable`）
3. **完全退出应用**并重新打开
4. 再次搜索 `kotlin`

**期望结果**：

- 列表仍能显示上次缓存的数据
- 顶部显示"网络不可用，显示缓存内容"的提示条
- 恢复网络后点击刷新，数据更新

> ✅ **检查点 9**：录屏演示断网后仍可浏览缓存数据

---

### 步骤 4.4：网络状态监听（加分项）

```kotlin
class NetworkMonitor(private val context: Context) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }
        manager.registerDefaultNetworkCallback(callback)
        trySend(manager.activeNetwork != null)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
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
| 任务一 协程实验 | 20 | 四个实验数据完整、结论正确 |
| 任务二 Retrofit | 20 | 请求成功、DTO 映射正确、错误封装完整 |
| 任务三 搜索与状态 | 30 | 四种状态可演示、debounce 生效、分页可用 |
| 任务四 离线优先 | 20 | 断网可浏览缓存、数据来源单一 |
| 代码质量 | 10 | 分层清晰、无主线程 IO、无内存泄漏 |

**加分项**：网络状态实时监听并提示（+5）；下拉刷新（+5）；请求失败自动重试（+5）

---

### 常见问题

| 问题 | 原因与处理 |
| --- | --- |
| `SecurityException: Permission denied` | 忘记加 `INTERNET` 权限 |
| `CLEARTEXT communication not permitted` | Android 9+ 默认禁 HTTP，配置 `networkSecurityConfig` 或改用 HTTPS |
| JSON 解析报 `Field 'x' is required` | DTO 字段没设可空或默认值；加 `ignoreUnknownKeys = true` |
| 403 Forbidden | GitHub 未登录限流 60 次/小时，等待或加 Token |
| 图片不显示 | 检查 Coil 依赖版本与 `AsyncImage` 导入是否匹配 |
| 快速输入触发大量请求 | 缺少 `debounce` / `distinctUntilChanged` |
| 界面卡顿 | 有主线程 IO；开 StrictMode 检查 |

---

### 思考题

1. `launch` 和 `async` 的区别是什么？什么时候必须用 `async`？
2. 为什么并发请求要用 `coroutineScope { async ... }` 而不是分别 `launch`？
3. 什么是"主线程安全"？为什么 Retrofit 的 `suspend` 方法不需要手动 `withContext(Dispatchers.IO)`？
4. 结构化并发中，子协程抛出异常为什么会取消兄弟协程？`supervisorScope` 如何改变这一行为？
5. 为什么 `safeApiCall` 中要单独 `catch (e: CancellationException) { throw e }`？如果吞掉它会发生什么？
6. `collectLatest` 与 `collect` 有什么区别？在搜索场景中为什么要用前者？
7. "离线优先"架构中，为什么说数据库是**单一可信来源**？如果 UI 同时观察网络和数据库会出现什么问题？

---

### 提交要求

**提交物**（打包为 `学号_姓名_实验九.zip`）：

1. **项目源码**（删除 `build/`、`.gradle/`、`.idea/`）
2. **实验数据**
   - 任务一：四个实验的完整控制台输出
   - 任务三：debounce 对比表 + Logcat 请求日志截图
3. **演示视频**（2 分钟内，必交）
   - 搜索、分页、四种状态
   - 断网后仍可浏览缓存数据
4. **实验报告**（PDF/Word）
   - 网络层架构图（API → Repository → ViewModel → UI）
   - 离线优先的数据流向图
   - 思考题解答

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

- [Android 上的 Kotlin 协程](https://developer.android.google.cn/kotlin/coroutines)
- [Retrofit 官方文档](https://square.github.io/retrofit/)
- [GitHub REST API](https://docs.github.com/en/rest)
- 理论课件：[ch-09.md](./ch-09.md)
