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

# 实验三 面向对象与空安全实践

###### “让类型替你检查错误。”

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
- [任务一：类与继承](#0)
- [任务二：特殊类实践](#0)
- [任务三：空安全验证](#0)
- [任务四：图书管理系统](#0)
- [验收、思考与提交](#0)

---

## 实验目的与准备

- 实验目的
- 环境准备

---

### 实验目的

1. 掌握类、构造函数、属性、可见性修饰符的用法
2. 理解 `open`/`override`，能正确使用继承、抽象类与接口
3. 熟练使用 `data class`、`object`、`enum class`、`sealed class`
4. 验证并解释 Kotlin 空安全机制的各种行为
5. 熟练使用扩展函数与作用域函数
6. 综合运用上述知识完成一个小型管理系统

**课时**：2 学时　**运行方式**：Kotlin Playground 或 Android Studio 中的 `main` 函数

---

## 任务一：类与继承

- 1.1 图形类层次
- 1.2 接口与多实现

---

### 任务 1.1：图形类层次

设计如下类层次并测试：

```kotlin
abstract class Shape(val name: String) {
    abstract fun area(): Double
    abstract fun perimeter(): Double
    open fun describe(): String =
        "%s：面积 %.2f，周长 %.2f".format(name, area(), perimeter())
}

class Circle(val radius: Double) : Shape("圆")
class Rectangle(val width: Double, val height: Double) : Shape("矩形")
class Square(val side: Double) : Shape("正方形")       // 考虑：继承 Rectangle 还是 Shape？
class Triangle(val a: Double, val b: Double, val c: Double) : Shape("三角形")
```

要求：

1. 补全所有子类的 `area()` 与 `perimeter()`
2. `Triangle` 用海伦公式求面积；构造时用 `init` 校验三边能否构成三角形
3. `Square` 重写 `describe()` 额外输出"（正方形是特殊的矩形）"

---

### 任务 1.1 测试代码

```kotlin
fun task1_1() {
    val shapes = listOf(
        Circle(3.0),
        Rectangle(4.0, 5.0),
        Square(4.0),
        Triangle(3.0, 4.0, 5.0)
    )

    shapes.forEach { println(it.describe()) }

    println("总面积：%.2f".format(shapes.sumOf { it.area() }))
    println("面积最大：${shapes.maxByOrNull { it.area() }?.name}")
    println("按面积排序：${shapes.sortedBy { it.area() }.map { it.name }}")

    // 验证非法三角形
    runCatching { Triangle(1.0, 2.0, 10.0) }
        .onFailure { println("捕获异常：${it.message}") }
}
```

---

### 任务 1.2：接口与多实现

```kotlin
interface Drawable {
    fun draw(): String
    fun preview(): String = "预览：${draw()}"     // 默认实现
}

interface Resizable {
    var scale: Double
    fun resize(factor: Double) { scale *= factor }
}
```

要求：

1. 让 `Circle` 同时实现 `Drawable` 和 `Resizable`
2. `draw()` 返回形如 `"○(r=3.0)"` 的字符串
3. 调用 `resize(2.0)` 后，`area()` 应反映缩放结果
4. 再定义一个接口 `Printable`，它也有默认方法 `preview()`，让 `Circle` 同时实现两者并解决冲突

> ✅ **检查点**：在报告中说明多接口同名默认方法冲突时编译器为什么强制要求重写

---

## 任务二：特殊类实践

- 2.1 data class 行为验证
- 2.2 enum 与 sealed class
- 2.3 单例

---

### 任务 2.1：data class 行为验证

```kotlin
data class Book(val title: String, val authors: List<String>, val year: Int)
```

编写 `task2_1()` 逐条验证并输出结果：

1. `toString()` 的默认格式
2. `copy(year = 2025)` 生成新对象，原对象不变
3. 解构：`val (t, a, y) = book`
4. 两个内容相同的 `Book` 用 `==` 比较为 `true`，用 `===` 比较为 `false`
5. 把 `Book` 放进 `Set`，加入内容相同的对象后 `size` 仍为 1

---

### 任务 2.1（续）：两个经典陷阱

**陷阱 A：数组字段**

```kotlin
data class D(val arr: IntArray)
val d1 = D(intArrayOf(1, 2)); val d2 = D(intArrayOf(1, 2))
println(d1 == d2)      // 输出什么？为什么？
```

要求：修改 `D`，使内容相同的两个实例相等（两种方案任选，并说明差异）

**陷阱 B：可变主构造属性**

```kotlin
data class P(var name: String)
val p = P("a")
val map = hashMapOf(p to 1)
p.name = "b"
println(map[p])        // 输出什么？为什么？
```

> ✅ **检查点**：在报告中画出陷阱 B 的哈希桶变化示意，解释为什么取不到值

---

### 任务 2.2：enum 与 sealed class

```kotlin
enum class BookStatus(val label: String, val canBorrow: Boolean) {
    AVAILABLE("可借阅", true),
    BORROWED("已借出", false),
    RESERVED("已预约", false),
    LOST("已遗失", false)
}

sealed class BorrowResult {
    data class Success(val dueDate: String) : BorrowResult()
    data class Failure(val reason: String) : BorrowResult()
    data object NeedLogin : BorrowResult()
}
```

要求：

1. 遍历 `BookStatus.entries`，输出每个状态的 `name`、`label`、`canBorrow`
2. 编写 `fun render(result: BorrowResult): String`，用 `when` 处理三种情况（**不允许写 `else`**）
3. 给 `sealed class` 新增一个子类，观察 `when` 是否编译报错，并在报告中解释原因

---

### 任务 2.3：单例与伴生对象

```kotlin
object LibraryConfig {
    const val MAX_BORROW_COUNT = 5
    var openTime: String = "08:00-22:00"
    private val logs = mutableListOf<String>()

    fun log(msg: String) { logs += "[${logs.size + 1}] $msg" }
    fun printLogs() = logs.forEach(::println)
}

class Book private constructor(val isbn: String, val title: String) {
    companion object {
        private var count = 0
        fun create(isbn: String, title: String): Book? {
            // 要求：ISBN 必须是 13 位数字，否则返回 null
            // 每成功创建一本 count++
        }
        fun createdCount() = count
    }
}
```

要求：验证 `LibraryConfig` 在不同函数中访问是同一实例；验证私有构造函数无法直接 `Book(...)`

---

## 任务三：空安全验证

- 3.1 空安全操作符
- 3.2 作用域函数
- 3.3 扩展函数

---

### 任务 3.1：空安全操作符对比

编写 `task3_1()`，对下面的数据结构逐一验证：

```kotlin
data class Address(val city: String?, val street: String?)
data class Person(val name: String, val address: Address?)

val p1 = Person("张三", Address("济南", "山大南路"))
val p2 = Person("李四", Address(null, "未知路"))
val p3 = Person("王五", null)
```

对每个 person 分别输出：

| 表达式 | p1 | p2 | p3 |
| --- | --- | --- | --- |
| `p.address?.city` | ? | ? | ? |
| `p.address?.city ?: "未知城市"` | ? | ? | ? |
| `p.address?.city?.length` | ? | ? | ? |
| `p.address?.let { "有地址" } ?: "无地址"` | ? | ? | ? |

再演示 `p3.address!!.city` 会抛出什么异常（用 try-catch 捕获并打印）。

---

### 任务 3.2：作用域函数

用**五个作用域函数各写一个例子**，并在注释中说明选择理由：

```kotlin
fun task3_2() {
    // let：
    val cityLen = p1.address?.city?.let { it.length }

    // run：
    // with：
    // apply：
    // also：
}
```

要求：

1. 五个例子必须是**不同场景**，不能只是同一段代码换写法
2. 在报告中填写下表

| 函数 | 上下文对象 | 返回值 | 你的使用场景 |
| --- | --- | --- | --- |
| `let` | | | |
| `run` | | | |
| `with` | | | |
| `apply` | | | |
| `also` | | | |

---

### 任务 3.3：扩展函数

编写以下扩展函数并测试：

```kotlin
// 1. 判断字符串是否是合法邮箱
fun String.isValidEmail(): Boolean

// 2. 判断字符串是否是合法手机号（11 位，1 开头）
fun String.isValidPhone(): Boolean

// 3. 手机号脱敏：138****5678
fun String.maskPhone(): String

// 4. 扩展属性：字符串的单词数
val String.wordCount: Int

// 5. 给 List<Int> 加一个求中位数的扩展函数
fun List<Int>.median(): Double

// 6. 给可空字符串加扩展：为空或空白时返回默认值
fun String?.orDefault(default: String): String
```

> 💡 第 6 题的接收者是**可空类型**，这是扩展函数相对成员函数的独特能力。

---

## 任务四：图书管理系统

- 需求说明
- 类设计
- 功能要求

---

### 需求说明

用本章所学知识实现一个**控制台版图书管理系统**（无 UI，纯 `main` 函数演示）。

核心实体：

- **图书**：ISBN、书名、作者列表、出版年、状态、分类
- **读者**：ID、姓名、已借图书列表、最大可借数量
- **借阅记录**：图书、读者、借出日期、应还日期、是否归还

---

### 类设计要求

必须用到以下每一种结构（在报告中标注用在哪里）：

| 结构 | 要求 |
| --- | --- |
| `data class` | 至少 2 个 |
| `enum class` | 图书状态、图书分类 |
| `sealed class` | 操作结果 `OperationResult` |
| `interface` | `Searchable`，定义搜索能力 |
| `abstract class` 或继承 | 读者分为"学生"和"教师"，可借数量不同 |
| `object` | 全局配置或 ID 生成器 |
| `companion object` | 工厂方法或校验逻辑 |
| 扩展函数 | 至少 2 个 |
| 空安全 | 查询不到时返回 `null` 并正确处理 |

---

### 功能要求

```kotlin
class Library {
    fun addBook(book: Book): OperationResult
    fun removeBook(isbn: String): OperationResult
    fun search(keyword: String): List<Book>          // 书名或作者模糊匹配
    fun borrow(isbn: String, readerId: String): OperationResult
    fun giveBack(isbn: String, readerId: String): OperationResult
    fun statistics(): String                          // 统计报告
}
```

**统计报告**需包含：

1. 馆藏总数、可借数、已借出数
2. 按分类统计数量
3. 借阅次数最多的前 3 本书
4. 当前借书数量已达上限的读者名单

---

### 演示脚本

`main` 函数中按顺序演示并打印结果：

```text
1. 添加 8 本图书（含 2 本相同分类）
2. 注册 3 个读者（2 学生 1 教师）
3. 搜索关键字 "Kotlin"                → 显示匹配结果
4. 学生 A 借 3 本书                    → 全部成功
5. 学生 A 再借 1 本（超过学生上限 3）   → 失败并给出原因
6. 教师 C 借同一本已被借出的书          → 失败并给出原因
7. 借一本不存在的 ISBN                 → 失败并给出原因
8. 学生 A 归还 1 本                    → 成功
9. 输出统计报告
```

> ✅ 每一步都要用 `when` 处理 `OperationResult` 并输出友好提示。

---

## 验收、思考与提交

- 验收标准
- 思考题
- 提交要求

---

### 验收标准

| 任务 | 分值 | 要点 |
| --- | --- | --- |
| 任务一 类与继承 | 15 | 抽象类、重写、接口冲突处理正确 |
| 任务二 特殊类 | 20 | 两个陷阱能复现并正确解释 |
| 任务三 空安全 | 20 | 对比表完整、作用域函数场景恰当 |
| 任务四 图书系统 | 35 | 结构齐全、9 步演示全部正确 |
| 代码规范 | 10 | 分文件组织、命名规范、注释恰当 |

**加分项**：

- 用 `sealed interface` 替代 `sealed class` 并说明差异（+5）
- 借阅记录用 `Map` 索引优化查询复杂度并说明（+5）

---

### 思考题

1. Kotlin 的类为什么默认是 `final`？这与 Java 的默认可继承相比有什么利弊？
2. `data class` 的 `equals` 只基于**主构造参数**，这个设计会带来什么问题？举一个实际会出错的场景。
3. `sealed class` 与 `enum class` 都能表达"有限的可能"，它们的区别是什么？各适合什么场景？
4. `object` 单例是线程安全的吗？它在什么时候被初始化？
5. 为什么扩展函数**不能被重写**（没有多态）？下面代码输出什么？

```kotlin
open class A
class B : A()
fun A.name() = "A"
fun B.name() = "B"
val x: A = B()
println(x.name())     // 输出？
```

6. 平台类型（`String!`）是什么？调用 Java 方法返回值时应该如何防御？

---

### 提交要求

**提交物**（打包为 `学号_姓名_实验三.zip`）：

1. **源代码**
   - `Task1.kt`、`Task2.kt`、`Task3.kt`
   - `library/` 目录：`Book.kt`、`Reader.kt`、`Library.kt`、`Main.kt`
2. **运行结果**：任务四 9 步演示的完整控制台输出
3. **实验报告**（PDF/Word）
   - 任务二两个陷阱的原理分析（**必答**）
   - 任务三的对比表与作用域函数表
   - 任务四的类图（可手绘拍照或用工具绘制）
   - 思考题解答

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

- [Kotlin 类与继承](https://kotlinlang.org/docs/classes.html)
- [Kotlin 空安全](https://kotlinlang.org/docs/null-safety.html)
- [Kotlin 作用域函数](https://kotlinlang.org/docs/scope-functions.html)
- 理论课件：[ch-03.md](./ch-03.md)
