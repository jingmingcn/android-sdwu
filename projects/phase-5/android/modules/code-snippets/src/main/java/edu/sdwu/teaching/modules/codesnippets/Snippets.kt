package edu.sdwu.teaching.modules.codesnippets

/** One code example. */
data class Snippet(
    val id: String,
    val title: String,
    val category: String,
    val code: String,
    val explanation: String,
)

/** The snippets shown by the module. Add your own: they appear in the list automatically. */
val SNIPPETS: List<Snippet> = listOf(
    Snippet(
        id = "null-safety",
        title = "Null safety with ?. and ?:",
        category = "Kotlin",
        code = """
            val nickname: String? = null
            // Safe call returns null instead of crashing; Elvis gives a fallback.
            val shown = nickname?.uppercase() ?: "GUEST"
        """.trimIndent(),
        explanation = "Prefer ?. and ?: over !! - the double bang throws when the value is null.",
    ),
    Snippet(
        id = "data-class",
        title = "Data class with copy()",
        category = "Kotlin",
        code = """
            data class Student(val name: String, val year: Int = 1)

            val ada = Student("Ada")
            val older = ada.copy(year = 2) // a new object; ada is unchanged
        """.trimIndent(),
        explanation = "Data classes generate equals, hashCode, toString and copy for you.",
    ),
    Snippet(
        id = "remember-state",
        title = "Counter with remember",
        category = "Compose",
        code = """
            @Composable
            fun Counter() {
                var count by remember { mutableStateOf(0) }
                Button(onClick = { count++ }) { Text("Clicked ${'$'}count times") }
            }
        """.trimIndent(),
        explanation = "remember keeps the value across recompositions; use rememberSaveable to survive rotation.",
    ),
    Snippet(
        id = "state-hoisting",
        title = "State hoisting",
        category = "Compose",
        code = """
            @Composable
            fun NameField(name: String, onNameChange: (String) -> Unit) {
                OutlinedTextField(value = name, onValueChange = onNameChange)
            }
        """.trimIndent(),
        explanation = "The caller owns the state; the composable only displays it and reports events.",
    ),
    Snippet(
        id = "lazy-column",
        title = "LazyColumn with keys",
        category = "Compose",
        code = """
            LazyColumn {
                items(lessons, key = { it.id }) { lesson ->
                    Text(lesson.title)
                }
            }
        """.trimIndent(),
        explanation = "Stable keys let Compose keep item state when the list changes.",
    ),
    Snippet(
        id = "stateflow-vm",
        title = "ViewModel with StateFlow",
        category = "Architecture",
        code = """
            class CounterViewModel : ViewModel() {
                private val _count = MutableStateFlow(0)
                val count: StateFlow<Int> = _count.asStateFlow()

                fun increment() = _count.update { it + 1 }
            }
        """.trimIndent(),
        explanation = "Expose read-only state; keep the mutable version private.",
    ),
    Snippet(
        id = "room-dao",
        title = "Room DAO with Flow",
        category = "Data",
        code = """
            @Dao
            interface NoteDao {
                @Query("SELECT * FROM notes ORDER BY updated_at DESC")
                fun observeAll(): Flow<List<NoteEntity>>

                @Upsert
                suspend fun save(note: NoteEntity)
            }
        """.trimIndent(),
        explanation = "Flow queries re-emit whenever the table changes; writes are suspend functions.",
    ),
    Snippet(
        id = "launched-effect",
        title = "LaunchedEffect keys",
        category = "Compose",
        code = """
            LaunchedEffect(running) {
                while (running) {
                    delay(1_000)
                    seconds++
                }
            }
        """.trimIndent(),
        explanation = "Changing a key cancels the running effect and starts a new one. Change keys last!",
    ),
)

/** Deterministic "snippet of the day": the same snippet all day, a different one tomorrow. */
fun snippetOfTheDay(epochDay: Long, snippets: List<Snippet> = SNIPPETS): Snippet =
    snippets[Math.floorMod(epochDay, snippets.size.toLong()).toInt()]

/** Case-insensitive search over title, code and explanation, plus an optional category. */
fun filterSnippets(query: String, category: String?, snippets: List<Snippet> = SNIPPETS): List<Snippet> {
    val q = query.trim().lowercase()
    return snippets.filter { s ->
        (category == null || s.category == category) &&
            (q.isEmpty() || q in s.title.lowercase() || q in s.code.lowercase() || q in s.explanation.lowercase())
    }
}

// ----- Minimal Kotlin syntax highlighting -----

enum class TokenType { KEYWORD, STRING, COMMENT, NUMBER, ANNOTATION, PLAIN }

data class Token(val text: String, val type: TokenType)

private val KEYWORDS = setOf(
    "val", "var", "fun", "class", "data", "interface", "object", "return", "if", "else", "when",
    "while", "for", "in", "is", "null", "true", "false", "private", "override", "suspend", "by",
)

private val TOKEN_REGEX = Regex(
    """(?<comment>//[^\n]*)|(?<string>"(?:\\.|[^"\\])*")|(?<annotation>@\w+)|(?<number>\b\d[\d_]*\b)|(?<word>\b[A-Za-z_]\w*\b)""",
)

/**
 * Splits code into coloured tokens. Not a real compiler lexer - just good enough for snippets.
 * Pure function, so it is unit tested (SnippetsTest).
 */
fun tokenizeKotlin(code: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var last = 0
    for (match in TOKEN_REGEX.findAll(code)) {
        if (match.range.first > last) tokens += Token(code.substring(last, match.range.first), TokenType.PLAIN)
        val groups = match.groups
        val type = when {
            groups["comment"] != null -> TokenType.COMMENT
            groups["string"] != null -> TokenType.STRING
            groups["annotation"] != null -> TokenType.ANNOTATION
            groups["number"] != null -> TokenType.NUMBER
            match.value in KEYWORDS -> TokenType.KEYWORD
            else -> TokenType.PLAIN
        }
        tokens += Token(match.value, type)
        last = match.range.last + 1
    }
    if (last < code.length) tokens += Token(code.substring(last), TokenType.PLAIN)
    return tokens
}
