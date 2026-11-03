package edu.sdwu.teaching.data.local

import edu.sdwu.teaching.domain.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonMappingTest {

    @Test
    fun `entity round trip keeps every field`() {
        SeedData.lessons.forEach { lesson ->
            assertEquals(lesson, lesson.toEntity().toDomain())
        }
    }

    @Test
    fun `unknown difficulty falls back to beginner`() {
        val entity = SeedData.lessons.first().toEntity().copy(difficulty = "EXPERT")
        assertEquals(Difficulty.BEGINNER, entity.toDomain().difficulty)
    }

    @Test
    fun `seed ids are unique and orders are sequential`() {
        val lessons = SeedData.lessons
        assertEquals(lessons.size, lessons.map { it.id }.toSet().size)
        assertTrue(lessons.map { it.order } == (1..lessons.size).toList())
    }
}
