package edu.sdwu.teaching.domain.model

import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.data.work.delayUntilHour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class LessonFilterTest {
    private val lessons = SeedData.lessons

    private fun ids(filter: LessonFilter, progress: Map<String, LessonProgress> = emptyMap(), bookmarks: Set<String> = emptySet()) =
        filter.apply(lessons, progress, bookmarks).map { it.id }

    @Test
    fun `empty filter returns everything`() {
        assertEquals(lessons.map { it.id }, ids(LessonFilter()))
        assertFalse(LessonFilter().isActive)
    }

    @Test
    fun `query matches every word, case-insensitively, in any field`() {
        assertEquals(listOf("room-database"), ids(LessonFilter(query = "ROOM entity")))
        assertEquals(emptyList<String>(), ids(LessonFilter(query = "room swiftui")))
    }

    @Test
    fun `status filter treats missing progress as not started`() {
        val progress = mapOf("kotlin-basics" to LessonProgress("kotlin-basics", ProgressStatus.COMPLETED, updatedAt = 1))
        assertEquals(listOf("kotlin-basics"), ids(LessonFilter(status = ProgressStatus.COMPLETED), progress))
        assertEquals(7, ids(LessonFilter(status = ProgressStatus.NOT_STARTED), progress).size)
    }

    @Test
    fun `filters combine with AND`() {
        val filter = LessonFilter(category = "Compose", difficulty = Difficulty.BEGINNER, bookmarkedOnly = true)
        assertTrue(filter.isActive)
        assertEquals(listOf("compose-layouts"), ids(filter, bookmarks = setOf("compose-layouts", "room-database")))
    }
}

class ValidationTest {
    @Test
    fun `email and password rules mirror the backend`() {
        assertTrue(Validation.isValidEmail(" student@example.com "))
        assertFalse(Validation.isValidEmail("student@example"))
        assertEquals(null, Validation.passwordProblem("student123"))
        assertEquals("Use at least one letter and one digit", Validation.passwordProblem("12345678"))
        assertTrue(Validation.isValidHttpUrl("https://example.com/a.png"))
        assertFalse(Validation.isValidHttpUrl("example.com/a.png"))
    }
}

class MediaUrlTest {
    @Test
    fun `relative media paths use the server address, full URLs are kept`() {
        assertEquals("http://10.0.2.2:8000/media/a.mp4", resolveMediaUrl("/media/a.mp4", "http://10.0.2.2:8000/"))
        assertEquals("https://cdn.example.com/a.mp4", resolveMediaUrl("https://cdn.example.com/a.mp4", "http://x/"))
        assertEquals(null, resolveMediaUrl(null, "http://x/"))
    }
}

class ReminderDelayTest {
    @Test
    fun `reminder fires later today or tomorrow`() {
        val morning = LocalDateTime.of(2026, 11, 2, 8, 30)
        assertEquals(Duration.ofMinutes(10 * 60 + 30), delayUntilHour(19, morning))
        val evening = LocalDateTime.of(2026, 11, 2, 19, 0)
        assertEquals(Duration.ofHours(24), delayUntilHour(19, evening)) // exactly at the hour -> tomorrow
    }
}
