package edu.sdwu.teaching.domain.repository

import edu.sdwu.teaching.framework.ModuleProvider

/** A module some student published to the class catalog (metadata only). */
data class CatalogModule(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val category: String,
    /** e.g. "function: Snippets", "tool: Daily tip". */
    val contributions: List<String>,
    val ownerName: String,
)

/** The backend's class catalog. Online only: it is a showcase, not something the app needs offline. */
interface ModuleCatalogRepository {
    suspend fun fetch(): Result<List<CatalogModule>>

    /** Sends the module's metadata (never its code). Requires a signed-in account. */
    suspend fun publish(provider: ModuleProvider): Result<Unit>
}
