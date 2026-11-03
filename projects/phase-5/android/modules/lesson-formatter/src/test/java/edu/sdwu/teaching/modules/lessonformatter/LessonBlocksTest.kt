package edu.sdwu.teaching.modules.lessonformatter

import edu.sdwu.teaching.framework.SampleLessons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonBlocksTest {

    @Test
    fun `paragraphs, key points and exercises are recognised`() {
        val blocks = parseLessonBlocks("Intro text.\n\n1. First point\n\n12. Twelfth\n\nExercise: do it")
        assertEquals(
            listOf(
                LessonBlock.Paragraph("Intro text."),
                LessonBlock.KeyPoint(1, "First point"),
                LessonBlock.KeyPoint(12, "Twelfth"),
                LessonBlock.Exercise("do it"),
            ),
            blocks,
        )
    }

    @Test
    fun `renderer only volunteers for structured lessons`() {
        assertTrue(StructuredLessonRenderer.canRender(SampleLessons.first()))
        val plain = SampleLessons.first().copy(body = "Just text.\n\nMore text.")
        assertFalse(StructuredLessonRenderer.canRender(plain))
    }
}
