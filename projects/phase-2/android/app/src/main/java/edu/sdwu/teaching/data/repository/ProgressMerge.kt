package edu.sdwu.teaching.data.repository

import edu.sdwu.teaching.data.local.ProgressEntity
import edu.sdwu.teaching.data.remote.dto.ProgressDto
import edu.sdwu.teaching.data.remote.dto.toEntity

/**
 * Decides what to store locally after a sync round trip. Pure function: no Room, no network,
 * so it is covered by plain JUnit tests (ProgressMergeTest).
 *
 * @param local     the current local row (may have changed while the request was in flight)
 * @param pushedAt  updatedAt of the row we sent, or null if we did not send this lesson
 * @param remote    the merged row returned by the server
 * @return the row to save, or null to keep [local] untouched
 */
fun resolveSyncedProgress(local: ProgressEntity?, pushedAt: Long?, remote: ProgressDto): ProgressEntity? {
    val changedDuringSync = local != null && local.pendingSync && local.updatedAt != pushedAt
    return if (changedDuringSync) null else remote.toEntity(pendingSync = false)
}
