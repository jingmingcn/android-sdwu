package edu.sdwu.teaching.ui.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.sdwu.teaching.data.remote.toUserMessage
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.repository.AuthRepository
import edu.sdwu.teaching.domain.repository.CatalogModule
import edu.sdwu.teaching.domain.repository.ModuleCatalogRepository
import edu.sdwu.teaching.framework.ModuleProblem
import edu.sdwu.teaching.framework.ModuleRegistry
import edu.sdwu.teaching.framework.RegisteredModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The class catalog comes from the server, so it has its own loading/error states. */
sealed interface CatalogState {
    data object Loading : CatalogState
    data class Loaded(val modules: List<CatalogModule>) : CatalogState
    data class Error(val message: String) : CatalogState
}

data class ModulesUiState(
    val installed: List<RegisteredModule> = emptyList(),
    val problems: List<ModuleProblem> = emptyList(),
    val catalog: CatalogState = CatalogState.Loading,
)

@HiltViewModel
class ModulesViewModel @Inject constructor(
    registry: ModuleRegistry,
    private val catalogRepository: ModuleCatalogRepository,
) : ViewModel() {

    private val catalog = MutableStateFlow<CatalogState>(CatalogState.Loading)

    val uiState: StateFlow<ModulesUiState> = combine(registry.modules, registry.problems, catalog, ::ModulesUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModulesUiState())

    init {
        refreshCatalog()
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            catalog.value = CatalogState.Loading
            catalog.value = catalogRepository.fetch().fold(
                onSuccess = { CatalogState.Loaded(it) },
                onFailure = { CatalogState.Error(it.toUserMessage()) },
            )
        }
    }
}

data class ModuleDetailUiState(
    /** null while the registry is still loading, or if the id is unknown. */
    val module: RegisteredModule? = null,
    val isLoading: Boolean = true,
    val canPublish: Boolean = false,
    val isPublishing: Boolean = false,
    val message: String? = null,
)

@HiltViewModel(assistedFactory = ModuleDetailViewModel.Factory::class)
class ModuleDetailViewModel @AssistedInject constructor(
    @Assisted private val moduleId: String,
    private val registry: ModuleRegistry,
    private val catalogRepository: ModuleCatalogRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val publishing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ModuleDetailUiState> = combine(
        registry.modules.map { list -> list.firstOrNull { it.info.id == moduleId } },
        authRepository.session.map { it is SessionState.SignedIn },
        publishing,
        message,
    ) { module, signedIn, isPublishing, msg ->
        ModuleDetailUiState(module, isLoading = false, canPublish = signedIn, isPublishing = isPublishing, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModuleDetailUiState())

    /** Shares the module's metadata (not its code) with the class catalog. */
    fun publish() {
        viewModelScope.launch {
            val module = registry.modules.first().firstOrNull { it.info.id == moduleId } ?: return@launch
            publishing.value = true
            message.value = catalogRepository.publish(module.provider).fold(
                onSuccess = { "Shared with your class" },
                onFailure = { it.toUserMessage() },
            )
            publishing.value = false
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(moduleId: String): ModuleDetailViewModel
    }
}
