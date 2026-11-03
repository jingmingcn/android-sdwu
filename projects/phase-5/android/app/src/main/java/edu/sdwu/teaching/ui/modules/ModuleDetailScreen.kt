package edu.sdwu.teaching.ui.modules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.sdwu.teaching.framework.ModuleSource
import edu.sdwu.teaching.ui.components.EmptyState
import edu.sdwu.teaching.ui.components.LoadingState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(
    moduleId: String,
    onBack: () -> Unit,
    onOpenFunction: (String) -> Unit,
    viewModel: ModuleDetailViewModel = hiltViewModel<ModuleDetailViewModel, ModuleDetailViewModel.Factory>(
        key = moduleId,
        creationCallback = { it.create(moduleId) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.module?.info?.name ?: "Module") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        val module = state.module
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            module == null -> EmptyState(
                title = "Module not found",
                message = "\"$moduleId\" is not installed in this build.",
                modifier = Modifier.padding(padding),
            )
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val info = module.info
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ModuleIcon(info.icon, size = 64)
                    Column {
                        Text(info.name, style = MaterialTheme.typography.headlineSmall)
                        Text("v${info.version} · ${info.author}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(info.description, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(info.category.label)
                    Badge(if (module.source == ModuleSource.BUILT_IN) "Built-in" else "Student")
                }
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Enabled") },
                        supportingContent = {
                            Text(if (module.enabled) "Its screens, tools and renderers are active" else "Hidden everywhere in the app")
                        },
                        trailingContent = { Switch(checked = module.enabled, onCheckedChange = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.toggleable(value = module.enabled, role = Role.Switch, onValueChange = viewModel::setEnabled),
                    )
                }

                val provider = module.provider
                if (provider.functions.isNotEmpty()) {
                    SectionTitle("Screens")
                    Card(Modifier.fillMaxWidth()) {
                        provider.functions.forEach { function ->
                            ListItem(
                                leadingContent = { Icon(function.icon, contentDescription = null) },
                                headlineContent = { Text(function.title) },
                                supportingContent = { Text(function.description) },
                                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable(enabled = module.enabled, onClickLabel = "Open ${function.title}") {
                                    onOpenFunction(function.id)
                                },
                            )
                        }
                    }
                }
                if (provider.tools.isNotEmpty()) {
                    SectionTitle("Home tools")
                    provider.tools.forEach { ContributionRow(Icons.Filled.Home, it.title, "Shown on the Home screen") }
                }
                if (provider.lessonRenderers.isNotEmpty()) {
                    SectionTitle("Lesson renderers")
                    provider.lessonRenderers.forEach {
                        ContributionRow(Icons.Filled.Info, it.name, "Priority ${it.priority}. Used when a lesson matches.")
                    }
                }

                SectionTitle("Technical details")
                Text(
                    "id: ${info.id}\nfirst installed: ${DateFormat.getDateTimeInstance().format(Date(module.firstSeenAt))}\n" +
                        "contribution ids: " + (provider.functions.map { it.id } + provider.tools.map { it.id } +
                            provider.lessonRenderers.map { it.id }).joinToString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )

                Button(
                    onClick = viewModel::publish,
                    enabled = state.canPublish && !state.isPublishing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isPublishing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Text("  Share with class")
                    }
                }
                Text(
                    state.message ?: if (state.canPublish) "Sends name, version and contributions (never code) to the class catalog."
                    else "Sign in to share modules with your class.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ContributionRow(icon: ImageVector, title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null)
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
}
