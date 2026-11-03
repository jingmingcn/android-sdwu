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

# 实验二 Kotlin 语言基础编程

###### “把语法练成肌肉记忆。”

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
- [任务一：变量与类型](#0)
- [任务二：控制流](#0)
- [任务三：函数与 Lambda](#0)
- [任务四：综合题——成绩统计](#0)
- [验收、思考与提交](#0)

---

## 实验目的与准备

- 实验目的
- 实验环境
- 两种运行方式

---

### 实验目的

1. 熟练使用 `val`/`var`、类型推断与显式类型转换
2. 掌握字符串模板与常用字符串操作
3. 熟练运用 `if` / `when` 表达式与各类循环
4. 掌握函数定义、默认参数、具名参数、可变参数
5. 会写 Lambda 表达式并使用集合高阶函数完成数据处理

**课时**：2 学时

---

### 两种运行方式

**方式 A：Kotlin Playground（推荐用于前三个任务）**

- 打开 `https://play.kotlinlang.org/`
- 直接编写 `main` 函数并运行，免安装

**方式 B：Android Studio 中的 Kotlin 文件**

1. 在项目中新建 `Lab02.kt`
2. 写 `fun main() { ... }`
3. 点击行号旁的绿色三角运行（会以 JVM 程序方式运行，不启动 App）

---

### 提交规范

每个任务在同一个文件中用独立函数组织：

```kotlin
fun main() {
    println("===== 任务 1.1 =====")
    task1_1()
    println("===== 任务 1.2 =====")
    task1_2()
    // ...
}

fun task1_1() { /* 你的代码 */ }
```

> 所有输出必须带任务编号标记，便于批改。

---

## 任务一：变量与类型

- 1.1 变量声明
- 1.2 类型转换
- 1.3 字符串处理

---

### 任务 1.1：变量声明与个人信息

编写函数 `task1_1()`，要求：

1. 用 `val` 声明姓名、学号（不可变）
2. 用 `var` 声明年龄、GPA（可变）
3. 用字符串模板输出一段自我介绍
4. 把年龄加 1、GPA 改为 3.8 后再输出一次

**期望输出示例**：

```text
我是张三，学号 202312345678，今年 20 岁，GPA 3.65
一年后：我是张三，今年 21 岁，GPA 3.80
```

> 💡 提示：格式化保留两位小数用 `"%.2f".format(gpa)`

---

### 任务 1.2：类型转换与安全解析

编写函数 `task1_2()`，要求：

```kotlin
fun task1_2() {
    val inputs = listOf("123", "45.6", "abc", "", "-99")

    for (s in inputs) {
        // 1. 尝试转为 Int，失败输出"无法解析为整数"
        // 2. 尝试转为 Double，失败输出"无法解析为小数"
        // 要求：不允许程序崩溃
    }
}
```

**期望输出**：

```text
"123" → Int: 123, Double: 123.0
"45.6" → Int: 无法解析为整数, Double: 45.6
"abc" → Int: 无法解析为整数, Double: 无法解析为小数
```

> 💡 使用 `toIntOrNull()` / `toDoubleOrNull()` 与 Elvis 运算符

---

### 任务 1.3：字符串处理

编写函数 `task1_3()`，对字符串 `"  Kotlin,Android,Compose,Coroutine  "` 完成：

1. 去掉首尾空格
2. 按逗号拆分成列表
3. 统计元素个数
4. 输出每个元素及其长度
5. 把所有元素用 `" | "` 连接后输出
6. 找出最长的元素

**期望输出**：

```text
共 4 个技术：
Kotlin(6) Android(7) Compose(7) Coroutine(9)
Kotlin | Android | Compose | Coroutine
最长：Coroutine
```

---

## 任务二：控制流

- 2.1 成绩等级
- 2.2 简易计算器
- 2.3 循环练习

---

### 任务 2.1：成绩等级（when）

编写函数 `gradeOf(score: Int): String`，用 `when` 实现：

| 分数区间 | 等级 |
| --- | --- |
| 90 ~ 100 | 优秀 |
| 80 ~ 89 | 良好 |
| 70 ~ 79 | 中等 |
| 60 ~ 69 | 及格 |
| 0 ~ 59 | 不及格 |
| 其他 | 无效分数 |

```kotlin
fun task2_1() {
    listOf(95, 83, 71, 60, 45, 105, -3).forEach {
        println("$it 分 → ${gradeOf(it)}")
    }
}
```

> ⚠️ 要求：必须用 `when` 表达式（有返回值），不允许用 `if-else` 链

---

### 任务 2.2：简易计算器

编写函数 `calculate(a: Double, b: Double, op: Char): Double?`：

- 支持 `+ - * /` 四种运算
- 除数为 0 时返回 `null`
- 不支持的运算符返回 `null`

```kotlin
fun task2_2() {
    val cases = listOf(
        Triple(6.0, 3.0, '+'), Triple(6.0, 3.0, '-'),
        Triple(6.0, 3.0, '*'), Triple(6.0, 3.0, '/'),
        Triple(6.0, 0.0, '/'), Triple(6.0, 3.0, '%')
    )
    cases.forEach { (a, b, op) ->
        val r = calculate(a, b, op)
        println("$a $op $b = ${r ?: "无法计算"}")
    }
}
```

---

### 任务 2.3：循环练习

编写函数 `task2_3()` 完成以下四小题：

1. 打印 1~100 中所有能被 3 整除但不能被 5 整除的数（每行 10 个）
2. 用 `while` 计算 1+2+...+100 的和
3. 打印 9×9 乘法表（下三角）
4. 找出 100~200 之间的所有质数

**乘法表期望输出**：

```text
1*1=1
1*2=2  2*2=4
1*3=3  2*3=6  3*3=9
...
```

---

## 任务三：函数与 Lambda

- 3.1 函数参数
- 3.2 高阶函数
- 3.3 集合操作

---

### 任务 3.1：函数参数

编写函数：

```kotlin
fun buildProfile(
    name: String,
    age: Int = 18,
    city: String = "济南",
    vararg hobbies: String
): String {
    // 返回形如：张三（18 岁，济南）爱好：篮球、编程
    // 无爱好时输出：张三（18 岁，济南）暂无爱好
}
```

分别用以下四种方式调用并输出结果：

```kotlin
buildProfile("张三")
buildProfile("李四", 20)
buildProfile("王五", city = "青岛")
buildProfile("赵六", 22, "北京", "篮球", "编程", "摄影")
```

---

### 任务 3.2：高阶函数

编写以下三个高阶函数并测试：

```kotlin
// 1. 对 1..n 的每个数执行 action
fun repeatTimes(n: Int, action: (Int) -> Unit)

// 2. 对两个整数执行传入的运算，返回结果
fun operate(a: Int, b: Int, op: (Int, Int) -> Int): Int

// 3. 返回一个函数：接收 x，返回 x 的 n 次方
fun powerOf(n: Int): (Int) -> Int
```

```kotlin
fun task3_2() {
    repeatTimes(5) { print("$it ") }              // 1 2 3 4 5
    println(operate(6, 3) { a, b -> a * b })      // 18
    val square = powerOf(2)
    println(square(7))                            // 49
}
```

---

### 任务 3.3：集合操作

给定：

```kotlin
val nums = listOf(15, 8, 23, 4, 42, 16, 7, 30, 11)
```

用**链式调用**（每题一行）完成：

1. 所有偶数
2. 所有偶数的平方
3. 大于 10 的数的个数
4. 所有数的平均值
5. 从大到小排序后取前 3 个
6. 所有偶数的平方和
7. 按奇偶分组，输出 `{false=[...], true=[...]}`
8. 判断是否存在大于 40 的数
9. 转换成 "数字:是否偶数" 的字符串列表

---

## 任务四：综合题——成绩统计

- 需求说明
- 数据
- 输出要求

---

### 需求说明

给定一组学生成绩数据，输出一份统计报告。

```kotlin
data class Student(
    val name: String,
    val className: String,
    val chinese: Int,
    val math: Int,
    val english: Int
) {
    val total: Int get() = chinese + math + english
    val average: Double get() = total / 3.0
}
```

> 提示：`data class` 的完整用法在第三章讲，这里先照抄使用。

---

### 测试数据

```kotlin
val students = listOf(
    Student("张三", "计科1班", 88, 95, 76),
    Student("李四", "计科1班", 92, 68, 85),
    Student("王五", "计科2班", 75, 82, 90),
    Student("赵六", "计科2班", 60, 55, 72),
    Student("孙七", "计科1班", 95, 91, 89),
    Student("周八", "计科2班", 83, 79, 68)
)
```

---

### 输出要求

编写 `fun printReport(students: List<Student>)`，输出：

```text
========== 成绩统计报告 ==========
【总分排行榜】
1. 孙七  计科1班  总分 275  平均 91.67  优秀
2. 张三  计科1班  总分 259  平均 86.33  良好
...

【班级平均分】
计科1班：85.67
计科2班：76.11

【单科最高分】
语文：孙七 95    数学：张三 95    英语：王五 90

【不及格记录】
赵六 数学 55

【统计概览】
总人数 6，平均分 80.89，最高 275，最低 187
```

---

### 实现提示

```kotlin
// 排序
students.sortedByDescending { it.total }

// 分组求平均
students.groupBy { it.className }
        .mapValues { (_, list) -> list.map { it.average }.average() }

// 单科最高
students.maxByOrNull { it.chinese }

// 收集不及格记录（每人可能多科不及格）
students.flatMap { s ->
    buildList {
        if (s.chinese < 60) add("${s.name} 语文 ${s.chinese}")
        // ...
    }
}

// 格式化对齐
"%-6s %-8s 总分 %3d".format(name, className, total)
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
| 任务一 变量与类型 | 15 | 正确使用 val/var、安全转换不崩溃 |
| 任务二 控制流 | 20 | when 用作表达式、循环逻辑正确 |
| 任务三 函数与 Lambda | 25 | 默认/具名/可变参数、高阶函数写法正确 |
| 任务四 综合统计 | 30 | 输出完整、使用链式集合操作而非手写循环 |
| 代码规范 | 10 | 命名规范、缩进一致、有必要的注释 |

**加分项**（每项 +5，总分不超过 100）：

- 任务四中使用 `Sequence` 并说明理由
- 输出使用表格对齐，格式美观
- 为任务四增加"按班级筛选"的交互式输入

---

### 思考题

1. `val` 与 `var` 的区别是什么？为什么 `val list = mutableListOf(1,2)` 之后仍能 `list.add(3)`？
2. Kotlin 为什么不提供隐式数值类型转换？这样设计避免了什么问题？
3. `if` 和 `when` 作为**表达式**使用时，与作为**语句**使用时有什么区别？各自适合什么场景？
4. 对比下面两段代码，哪种写法更好？为什么？

```kotlin
// A
var sum = 0
for (n in nums) if (n % 2 == 0) sum += n * n

// B
val sum = nums.filter { it % 2 == 0 }.sumOf { it * it }
```

5. 查阅资料说明：`vararg` 参数在编译后对应 Java 的什么结构？为什么传数组时需要 `*`？

---

### 提交要求

**提交物**（打包为 `学号_姓名_实验二.zip`）：

1. **源代码**：`Lab02.kt`（包含全部任务函数）
2. **运行结果**：完整控制台输出的截图或文本文件
3. **实验报告**（PDF/Word）
   - 每个任务的核心代码片段与运行截图
   - 调试过程中遇到的编译错误及原因分析（至少 3 个）
   - 思考题解答

**评分说明**：代码必须能直接运行；无法编译的任务不计分。

---

<!-- _class: lastpage -->
<!-- _footer: "" -->

###### 参考资料

- [Kotlin Playground](https://play.kotlinlang.org/)
- [Kotlin 官方教程 - Basic syntax](https://kotlinlang.org/docs/basic-syntax.html)
- [Kotlin 集合操作](https://kotlinlang.org/docs/collection-operations.html)
- 理论课件：[ch-02.md](./ch-02.md)
