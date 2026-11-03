package edu.sdwu.teaching.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizScorerTest {

    private fun question(id: String, correct: Int) =
        QuizQuestion(id, "lesson", 1, "Q$id", listOf("a", "b", "c"), correct, "")

    private val questions = listOf(question("1", 0), question("2", 1), question("3", 2))

    @Test
    fun `all correct answers give full score and pass`() {
        val result = QuizScorer.score(questions, mapOf("1" to 0, "2" to 1, "3" to 2))
        assertEquals(3, result.score)
        assertEquals(100, result.percent)
        assertTrue(result.passed)
    }

    @Test
    fun `unanswered questions count as wrong`() {
        val result = QuizScorer.score(questions, mapOf("1" to 0))
        assertEquals(1, result.score)
        assertEquals(mapOf("1" to true, "2" to false, "3" to false), result.correctByQuestion)
    }

    @Test
    fun `pass mark is 70 percent`() {
        assertFalse(QuizScorer.isPassing(2, 3)) // 66%
        assertTrue(QuizScorer.isPassing(7, 10)) // exactly 70%
        assertFalse(QuizScorer.isPassing(0, 0)) // empty quiz never passes
    }
}
