package edu.sdwu.teaching.domain.model

enum class ProgressStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

data class LessonProgress(
    val lessonId: String,
    val status: ProgressStatus,
    val quizBestScore: Int? = null,
    val quizTotal: Int? = null,
    /** Epoch millis of the last local or remote change. Used to resolve sync conflicts. */
    val updatedAt: Long,
) {
    val isCompleted: Boolean get() = status == ProgressStatus.COMPLETED
}
