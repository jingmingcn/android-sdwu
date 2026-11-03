package edu.sdwu.teaching.domain.model

data class QuizQuestion(
    val id: String,
    val lessonId: String,
    val order: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

data class QuizResult(
    val score: Int,
    val total: Int,
    /** questionId -> whether the chosen answer was right. */
    val correctByQuestion: Map<String, Boolean>,
) {
    val percent: Int get() = if (total == 0) 0 else score * 100 / total
    val passed: Boolean get() = QuizScorer.isPassing(score, total)
}

/** Scores a quiz on the device, so quizzes work offline. Pure Kotlin: easy to unit test. */
object QuizScorer {
    /** Passing a quiz with at least this score marks the lesson as completed. */
    const val PASS_PERCENT = 70

    fun isPassing(score: Int, total: Int): Boolean = total > 0 && score * 100 >= total * PASS_PERCENT

    /** @param answers questionId -> chosen option index. Unanswered questions count as wrong. */
    fun score(questions: List<QuizQuestion>, answers: Map<String, Int>): QuizResult {
        val correct = questions.associate { it.id to (answers[it.id] == it.correctIndex) }
        return QuizResult(score = correct.count { it.value }, total = questions.size, correctByQuestion = correct)
    }
}
