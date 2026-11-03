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

# 实验八 数据持久化：DataStore 与 Room

###### “关掉再打开，数据还在。”

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
- [任务一：DataStore 设置项](#0)
- [任务二：Room 数据库搭建](#0)
- [任务三：DAO 与响应式查询](#0)
- [任务四：接入笔记应用](#0)
- [验收、思考与提交](#0)

---

## 实验目的与准备

- 实验目的
- 项目准备

---

### 实验目的

1. 用 DataStore 保存用户设置，并以 `Flow` 形式观察
2. 掌握 Room 三件套：`@Entity`、`@Dao`、`@Database`
3. 编写增删改查与复杂查询，理解返回 `Flow` 带来的自动刷新
4. 实现一对多关系查询与事务
5. 编写数据库迁移并验证升级后老数据不丢失
6. 把上次实验的内存仓库替换为 Room，实现真正的持久化

**课时**：4 学时

---

### 项目准备

在 **实验七的 `NoteApp`** 基础上继续开发（保留导航结构）。

```kotlin
// 模块 build.gradle.kts
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
```

```kotlin
// gradle/libs.versions.toml
ksp = { id = "com.google.devtools.ksp", version = "2.2.0-1.0.24" }
```

---

### 导出数据库 Schema

```kotlin
android {
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}
```

作用：把每个版本的表结构导出为 JSON，用于迁移测试与代码审查。**必须开启**。

---

## 任务一：DataStore 设置项

- 定义 Key
- Repository
- 设置页接入

---

### 步骤 1.1：定义 DataStore

```kotlin
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

object SettingsKeys {
    val DARK_MODE = stringPreferencesKey("dark_mode")        // system / light / dark
    val SORT_ORDER = stringPreferencesKey("sort_order")      // time / title / category
    val FONT_SCALE = floatPreferencesKey("font_scale")
    val SHOW_PREVIEW = booleanPreferencesKey("show_preview")
    val USER_NAME = stringPreferencesKey("user_name")
    val LAUNCH_COUNT = intPreferencesKey("launch_count")
}
```

```kotlin
enum class DarkModeOption { SYSTEM, LIGHT, DARK }
enum class SortOrder { BY_TIME, BY_TITLE, BY_CATEGORY }
```

---

### 步骤 1.2：SettingsRepository

```kotlin
data class UserSettings(
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,
    val sortOrder: SortOrder = SortOrder.BY_TIME,
    val fontScale: Float = 1.0f,
    val showPreview: Boolean = true,
    val userName: String = "",
    val launchCount: Int = 0
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<UserSettings> = context.settingsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            UserSettings(
                darkMode = DarkModeOption.valueOf(
                    p[SettingsKeys.DARK_MODE] ?: DarkModeOption.SYSTEM.name),
                sortOrder = SortOrder.valueOf(
                    p[SettingsKeys.SORT_ORDER] ?: SortOrder.BY_TIME.name),
                fontScale = p[SettingsKeys.FONT_SCALE] ?: 1.0f,
                showPreview = p[SettingsKeys.SHOW_PREVIEW] ?: true,
                userName = p[SettingsKeys.USER_NAME].orEmpty(),
                launchCount = p[SettingsKeys.LAUNCH_COUNT] ?: 0
            )
        }

    suspend fun setDarkMode(v: DarkModeOption) =
        context.settingsDataStore.edit { it[SettingsKeys.DARK_MODE] = v.name }

    suspend fun incrementLaunchCount() = context.settingsDataStore.edit { p ->
        p[SettingsKeys.LAUNCH_COUNT] = (p[SettingsKeys.LAUNCH_COUNT] ?: 0) + 1
    }
}
```

---

### 步骤 1.3：设置页接入

要求实现：

| 设置项 | 控件 |
| --- | --- |
| 深色模式 | 三选一 `RadioButton`（跟随系统/浅色/深色） |
| 排序方式 | 三选一 |
| 字体缩放 | `Slider`（0.8 ~ 1.5） |
| 显示内容预览 | `Switch` |
| 用户昵称 | `TextField` |
| 启动次数 | 只读显示 |

**验证**：修改任一设置 → **完全退出应用** → 重新打开 → 设置仍然生效。

> ✅ **检查点 1**：深色模式设置立即生效并在重启后保留；启动次数每次启动 +1

---

### 步骤 1.4：让主题响应设置

```kotlin
@Composable
fun NoteAppTheme(
    darkMode: DarkModeOption,
    fontScale: Float,
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkMode) {
        DarkModeOption.SYSTEM -> isSystemInDarkTheme()
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = scaledTypography(fontScale),
        content = content
    )
}
```

> ✅ **检查点 2**：在设置页切换深色模式，**整个应用**立即变色（不需要重启）

---

## 任务二：Room 数据库搭建

- Entity
- Database
- 类型转换器

---

### 步骤 2.1：定义 Entity

```kotlin
@Entity(
    tableName = "notes",
    indices = [Index("category_id"), Index("updated_at")],
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.SET_NULL
    )]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
)

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int = 0xFF2196F3.toInt()
)
```

---

### 步骤 2.2：类型转换器

```kotlin
class Converters {
    @TypeConverter
    fun fromTagList(tags: List<String>): String = tags.joinToString("|")

    @TypeConverter
    fun toTagList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|")
}
```

> 💡 **思考**：用 `|` 分隔存字符串有什么隐患？如果标签中本身含 `|` 会怎样？更好的方案是什么？（写进报告）

---

### 步骤 2.3：Database

```kotlin
@Database(
    entities = [NoteEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var INSTANCE: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, NoteDatabase::class.java, "note.db"
                )
                    .setQueryCallback({ sql, args -> Log.d("RoomSQL", "$sql | $args") },
                                      Executors.newSingleThreadExecutor())
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
```

> ✅ **检查点 3**：应用启动后，在 App Inspection → Database Inspector 中能看到两张表

---

## 任务三：DAO 与响应式查询

- 基本 CRUD
- 复杂查询
- 关系查询与事务

---

### 步骤 3.1：基本 CRUD

```kotlin
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY is_pinned DESC, updated_at DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}
```

---

### 步骤 3.2：复杂查询（必做 8 条）

```kotlin
// 1. 关键字搜索（标题或内容）
@Query("""SELECT * FROM notes
          WHERE title LIKE '%' || :kw || '%' OR content LIKE '%' || :kw || '%'
          ORDER BY updated_at DESC""")
fun search(kw: String): Flow<List<NoteEntity>>

// 2. 按分类筛选
@Query("SELECT * FROM notes WHERE category_id = :categoryId")
fun observeByCategory(categoryId: Long): Flow<List<NoteEntity>>

// 3. 统计总数
@Query("SELECT COUNT(*) FROM notes")
fun countAll(): Flow<Int>

// 4. 按时间范围
@Query("SELECT * FROM notes WHERE created_at BETWEEN :start AND :end")
suspend fun getByDateRange(start: Long, end: Long): List<NoteEntity>

// 5. 分页
@Query("SELECT * FROM notes ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
suspend fun getPage(limit: Int, offset: Int): List<NoteEntity>

// 6. 批量 ID 查询
@Query("SELECT * FROM notes WHERE id IN (:ids)")
suspend fun getByIds(ids: List<Long>): List<NoteEntity>

// 7. 切换置顶
@Query("UPDATE notes SET is_pinned = NOT is_pinned WHERE id = :id")
suspend fun togglePin(id: Long)

// 8. 每个分类的笔记数
@Query("""SELECT c.name AS name, COUNT(n.id) AS count FROM categories c
          LEFT JOIN notes n ON n.category_id = c.id GROUP BY c.id""")
fun countByCategory(): Flow<List<CategoryCount>>
```

---

### 步骤 3.3：关系查询

```kotlin
data class CategoryWithNotes(
    @Embedded val category: CategoryEntity,
    @Relation(parentColumn = "id", entityColumn = "category_id")
    val notes: List<NoteEntity>
)

@Dao
interface CategoryDao {
    @Transaction
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAllWithNotes(): Flow<List<CategoryWithNotes>>

    @Transaction
    suspend fun deleteCategoryAndMoveNotes(categoryId: Long, targetId: Long?) {
        moveNotes(categoryId, targetId)
        deleteCategory(categoryId)
    }
}
```

> ✅ **检查点 4**：在 Database Inspector 中插入一条数据，观察界面**自动刷新**（不需要手动触发）

---

### 步骤 3.4：Repository 与模型映射

```kotlin
class NoteRepository(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao
) {
    fun observeAll(): Flow<List<Note>> =
        noteDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeNote(id: Long): Flow<Note?> =
        noteDao.observeById(id).map { it?.toDomain() }

    suspend fun save(note: Note): Long =
        noteDao.insert(note.toEntity().copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(id: Long) = noteDao.deleteById(id)
}

fun NoteEntity.toDomain() = Note(id, title, content, categoryId, isPinned, updatedAt, tags)
fun Note.toEntity() = NoteEntity(id, title, content, categoryId, isPinned, tags = tags)
```

---

## 任务四：接入笔记应用

- 替换内存仓库
- 数据库迁移
- 预置数据

---

### 步骤 4.1：替换内存仓库

把实验七的 `object NoteRepository` 换成 Room 版本：

```kotlin
// 简易依赖容器
class AppContainer(context: Context) {
    private val db = NoteDatabase.getInstance(context)
    val noteRepository = NoteRepository(db.noteDao(), db.categoryDao())
    val settingsRepository = SettingsRepository(context)
}

class NoteApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() { super.onCreate(); container = AppContainer(this) }
}
```

```kotlin
// ViewModel Factory
class NoteListViewModel(private val repo: NoteRepository) : ViewModel() {
    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NoteApplication
                NoteListViewModel(app.container.noteRepository)
            }
        }
    }
}
```

> ✅ **检查点 5**：新增笔记 → 完全退出应用 → 重新打开 → 笔记仍在

---

### 步骤 4.2：预置数据

```kotlin
Room.databaseBuilder(context, NoteDatabase::class.java, "note.db")
    .addCallback(object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 首次创建数据库时插入默认分类
            db.execSQL("INSERT INTO categories (name, color) VALUES ('默认', -16776961)")
            db.execSQL("INSERT INTO categories (name, color) VALUES ('工作', -65536)")
            db.execSQL("INSERT INTO categories (name, color) VALUES ('学习', -16711936)")
        }
    })
    .build()
```

---

### 步骤 4.3：数据库迁移（必做）

**需求**：给笔记增加"提醒时间"字段。

1. 修改 Entity：

```kotlin
@ColumnInfo(name = "remind_at") val remindAt: Long? = null
```

2. 版本号改为 2，编写迁移：

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN remind_at INTEGER DEFAULT NULL")
    }
}
```

3. 注册：`.addMigrations(MIGRATION_1_2)`

**验证步骤**：先用 v1 安装并添加 3 条笔记 → 升级到 v2 覆盖安装 → 确认 3 条笔记**没有丢失**

> ✅ **检查点 6**：截图证明升级后老数据仍在，并展示 `schemas/2.json`

---

### 步骤 4.4：数据统计页（加分项）

在设置页增加"数据统计"，展示：

```text
笔记总数：24
已置顶：3
各分类数量：默认 10 / 工作 8 / 学习 6
最近 7 天新增：5
最长的笔记：《期末复习计划》（1280 字）
数据库文件大小：128 KB
```

```kotlin
// 数据库文件大小
val dbFile = context.getDatabasePath("note.db")
val sizeKb = dbFile.length() / 1024
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
| 任务一 DataStore | 20 | 6 个设置项持久化、主题实时响应 |
| 任务二 Room 搭建 | 20 | Entity/Database/Converter 正确、schema 已导出 |
| 任务三 DAO 查询 | 25 | 8 条复杂查询全部可用、关系查询正确 |
| 任务四 应用接入 | 25 | 完全退出后数据仍在、迁移验证成功 |
| 代码质量 | 10 | Repository 分层、Entity 与领域模型分离 |

**加分项**：数据统计页（+5）；导入/导出 JSON 备份（+10）

---

### 常见问题

| 问题 | 原因与处理 |
| --- | --- |
| `Cannot access database on the main thread` | DAO 方法要加 `suspend` 或返回 `Flow` |
| 编译报错 "Not sure how to convert a Cursor" | 缺少 `@TypeConverter` |
| `Room cannot verify the data integrity` | 改了表结构没升 `version`；开发期可临时用 `fallbackToDestructiveMigration()` |
| 界面不自动刷新 | DAO 返回的不是 `Flow`；或 ViewModel 用了一次性 `suspend` 查询 |
| KSP 报错找不到符号 | 确认用的是 `ksp` 而不是 `kapt`；Rebuild Project |
| DataStore 读到默认值 | `Flow` 是异步的，首次发射前 UI 会看到初始值，属正常现象 |

---

### 思考题

1. DataStore 相比 SharedPreferences 解决了哪三个问题？为什么它的读接口返回 `Flow` 而不是直接返回值？
2. Room 的 DAO 方法返回 `Flow<List<T>>` 时，数据变化是如何自动推送到界面的？（描述其机制）
3. 为什么要把 `NoteEntity`（数据层）和 `Note`（领域模型）分开？合成一个会有什么问题？
4. `@Transaction` 注解在什么情况下**必须**加？关系查询为什么需要它？
5. 如果修改了 Entity 但忘记升级 `version`，运行时会发生什么？如果升级了 `version` 但没写 `Migration` 呢？
6. `fallbackToDestructiveMigration()` 为什么绝对不能出现在正式发布的版本中？
7. 用 `|` 分隔存储标签列表有什么隐患？请给出至少两种更好的方案。

---

### 提交要求

**提交物**（打包为 `学号_姓名_实验八.zip`）：

1. **项目源码**（含 `schemas/` 目录，删除 `build/`、`.gradle/`、`.idea/`）
2. **截图**
   - Database Inspector 中的表结构与数据
   - 迁移前后数据对比（证明老数据未丢失）
   - 设置项持久化验证（修改→退出→重开）
3. **实验报告**（PDF/Word）
   - **数据库设计**：ER 图（两张表及外键关系）
   - 8 条复杂查询的 SQL 与运行结果
   - 迁移过程记录与 `schemas/1.json`、`schemas/2.json` 对比
   - 思考题解答

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

- [Room 持久性库](https://developer.android.google.cn/training/data-storage/room)
- [Room 数据库迁移](https://developer.android.google.cn/training/data-storage/room/migrating-db-versions)
- [DataStore](https://developer.android.google.cn/topic/libraries/architecture/datastore)
- 理论课件：[ch-08.md](./ch-08.md)
