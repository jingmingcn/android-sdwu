package edu.sdwu.teaching.ui.modules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.sdwu.teaching.framework.Contributed
import edu.sdwu.teaching.framework.CustomFunction
import edu.sdwu.teaching.framework.ModuleRegistry
import edu.sdwu.teaching.framework.enabledFunctions
import edu.sdwu.teaching.ui.components.EmptyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Exposes enabled functions; the screen picks one by id. Observing (not a one-off lookup)
 *  means the screen also works right after process death, before modules are re-registered. */
@HiltViewModel
class FunctionViewModel @Inject constructor(registry: ModuleRegistry) : ViewModel() {
    val functions: StateFlow<List<Contributed<CustomFunction>>?> = registry.enabledFunctions
        .map<List<Contributed<CustomFunction>>, List<Contributed<CustomFunction>>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * Hosts a module's [CustomFunction]: the app draws the top bar, the module draws the body
 * through `Content(host)`. This is the whole contract between a function and the app UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionScreen(
    functionId: String,
    onBack: () -> Unit,
    viewModel: FunctionViewModel = hiltViewModel(),
) {
    val functions by viewModel.functions.collectAsStateWithLifecycle()
    val contributed = functions?.firstOrNull { it.item.id == functionId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(contributed?.item?.title ?: "Module")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    contributed?.let {
                        Text(
                            "by ${it.module.info.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                functions == null -> Unit // registry still loading: show nothing for a moment
                contributed == null -> EmptyState(
                    title = "Function unavailable",
                    message = "\"$functionId\" is not installed or its module is disabled.",
                )
                else -> contributed.item.Content(rememberFunctionHost(contributed.module.info.id))
            }
        }
    }
}
