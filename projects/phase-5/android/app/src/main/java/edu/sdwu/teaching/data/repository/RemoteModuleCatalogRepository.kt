package edu.sdwu.teaching.data.repository

import edu.sdwu.teaching.data.remote.TeachingApi
import edu.sdwu.teaching.data.remote.dto.ContributionDto
import edu.sdwu.teaching.data.remote.dto.PublishModuleDto
import edu.sdwu.teaching.domain.repository.CatalogModule
import edu.sdwu.teaching.domain.repository.ModuleCatalogRepository
import edu.sdwu.teaching.framework.FRAMEWORK_VERSION
import edu.sdwu.teaching.framework.ModuleProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteModuleCatalogRepository @Inject constructor(
    private val api: TeachingApi,
) : ModuleCatalogRepository {

    override suspend fun fetch(): Result<List<CatalogModule>> = runCatchingApi {
        api.moduleCatalog().map { dto ->
            CatalogModule(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                author = dto.author,
                version = dto.version,
                category = dto.category,
                contributions = dto.contributions.map { "${it.type}: ${it.title}" },
                ownerName = dto.ownerName,
                sourceUrl = dto.sourceUrl,
            )
        }
    }

    override suspend fun publish(provider: ModuleProvider): Result<Unit> = runCatchingApi {
        api.publishModule(provider.info.id, provider.toPublishDto())
        Unit
    }
}

/** Framework metadata -> the JSON the backend expects. Pure function, unit tested. */
fun ModuleProvider.toPublishDto() = PublishModuleDto(
    id = info.id,
    name = info.name,
    description = info.description,
    author = info.author,
    version = info.version,
    category = info.category.name,
    contributions = functions.map { ContributionDto("function", it.id, it.title) } +
        tools.map { ContributionDto("tool", it.id, it.title) } +
        lessonRenderers.map { ContributionDto("renderer", it.id, it.name) },
    sourceUrl = info.sourceUrl,
    frameworkVersion = FRAMEWORK_VERSION,
)
