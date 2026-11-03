package edu.sdwu.teaching.data.repository

import edu.sdwu.teaching.data.local.ProgressEntity
import edu.sdwu.teaching.data.remote.dto.ProgressDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressMergeTest {

    private val remote = ProgressDto("l1", "COMPLETED", quizBestScore = 3, quizTotal = 3, updatedAt = 500)

    private fun local(updatedAt: Long, pending: Boolean) =
        ProgressEntity("l1", "IN_PROGRESS", null, null, updatedAt, pending)

    @Test
    fun `new row from server is saved as synced`() {
        val saved = resolveSyncedProgress(local = null, pushedAt = null, remote = remote)
        assertEquals(ProgressEntity("l1", "COMPLETED", 3, 3, 500, pendingSync = false), saved)
    }

    @Test
    fun `row we just pushed is replaced by the merged server row`() {
        val saved = resolveSyncedProgress(local(updatedAt = 400, pending = true), pushedAt = 400, remote = remote)
        assertEquals(false, saved?.pendingSync)
        assertEquals("COMPLETED", saved?.status)
    }

    @Test
    fun `row changed while the request was in flight is kept for the next sync`() {
        val saved = resolveSyncedProgress(local(updatedAt = 600, pending = true), pushedAt = 400, remote = remote)
        assertNull(saved)
    }

    @Test
    fun `synced local row is overwritten by newer server data from another device`() {
        val saved = resolveSyncedProgress(local(updatedAt = 100, pending = false), pushedAt = null, remote = remote)
        assertEquals(500L, saved?.updatedAt)
    }
}
