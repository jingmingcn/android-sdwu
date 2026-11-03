package edu.sdwu.teaching.domain.model

/** What the student picked in the search bar and filter chips. `null` means "any". */
data class LessonFilter(
    val query: String = "",
    val category: String? = null,
    val difficulty: Difficulty? = null,
    val status: ProgressStatus? = null,
    val bookmarkedOnly: Boolean = false,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || category != null || difficulty != null || status != null || bookmarkedOnly

    /**
     * Pure filtering: no Android, no Room, so it runs in plain JUnit tests and works offline.
     * A lesson without a progress row counts as NOT_STARTED.
     */
    fun apply(
        lessons: List<Lesson>,
        progress: Map<String, LessonProgress>,
        bookmarks: Set<String>,
    ): List<Lesson> {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return lessons.filter { lesson ->
            val text = "${lesson.title} ${lesson.summary} ${lesson.content} ${lesson.category}".lowercase()
            val lessonStatus = progress[lesson.id]?.status ?: ProgressStatus.NOT_STARTED
            words.all { it in text } &&
                (category == null || lesson.category == category) &&
                (difficulty == null || lesson.difficulty == difficulty) &&
                (status == null || lessonStatus == status) &&
                (!bookmarkedOnly || lesson.id in bookmarks)
        }
    }
}
