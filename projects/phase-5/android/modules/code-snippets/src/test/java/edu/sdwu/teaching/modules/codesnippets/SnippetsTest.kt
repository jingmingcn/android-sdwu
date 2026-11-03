package edu.sdwu.teaching.modules.codesnippets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetsTest {

    @Test
    fun `tokenizer finds keywords, strings, comments, numbers and annotations`() {
        val tokens = tokenizeKotlin("@Composable val x = \"hi\" // note\nvar n = 42")
        fun typeOf(text: String) = tokens.first { it.text == text }.type
        assertEquals(TokenType.ANNOTATION, typeOf("@Composable"))
        assertEquals(TokenType.KEYWORD, typeOf("val"))
        assertEquals(TokenType.STRING, typeOf("\"hi\""))
        assertEquals(TokenType.COMMENT, typeOf("// note"))
        assertEquals(TokenType.NUMBER, typeOf("42"))
        assertEquals(TokenType.PLAIN, typeOf("x"))
    }

    @Test
    fun `tokens put back together give the original code`() {
        SNIPPETS.forEach { s -> assertEquals(s.id, s.code, tokenizeKotlin(s.code).joinToString("") { it.text }) }
    }

    @Test
    fun `search and category filter`() {
        assertEquals(listOf("room-dao"), filterSnippets("upsert", null).map { it.id })
        assertTrue(filterSnippets("", "Compose").all { it.category == "Compose" })
        assertTrue(filterSnippets("no such thing", null).isEmpty())
    }

    @Test
    fun `snippet of the day is stable per day and cycles`() {
        assertEquals(snippetOfTheDay(100), snippetOfTheDay(100))
        assertEquals(snippetOfTheDay(0), snippetOfTheDay(SNIPPETS.size.toLong()))
        assertEquals(SNIPPETS.last(), snippetOfTheDay(-1)) // floorMod handles negative days
    }

    @Test
    fun `snippet ids are unique`() {
        assertEquals(SNIPPETS.size, SNIPPETS.map { it.id }.toSet().size)
    }
}
