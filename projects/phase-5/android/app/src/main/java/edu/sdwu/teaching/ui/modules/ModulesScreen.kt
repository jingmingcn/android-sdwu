package edu.sdwu.teaching.ui.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.sdwu.teaching.domain.repository.CatalogModule
import edu.sdwu.teaching.framework.ModuleProblem
import edu.sdwu.teaching.framework.ModuleSource
import edu.sdwu.teaching.framework.RegisteredModule
import edu.sdwu.teaching.framework.contributionSummary

/** The Modules tab: what is installed, what was rejected, and what classmates published. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesScreen(
    onModuleClick: (String) -> Unit,
    onOpenGuide: () -> Unit,
    viewModel: ModulesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Modules") }) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { BuildYourOwnCard(onOpenGuide) }

            item { SectionTitle("Installed (${state.installed.size})") }
            items(state.installed, key = { "m-" + it.info.id }) { module ->
                InstalledModuleCard(
                    module,
                    onClick = { onModuleClick(module.info.id) },
                    onEnabledChange = { viewModel.setEnabled(module.info.id, it) },
                )
            }

            if (state.problems.isNotEmpty()) {
                item { SectionTitle("Problems (${state.problems.size})") }
                items(state.problems) { ProblemCard(it) }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { SectionTitle("Class catalog") }
                    IconButton(onClick = viewModel::refreshCatalog) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh class catalog")
                    }
                }
            }
            when (val catalog = state.catalog) {
                CatalogState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                is CatalogState.Error -> item { HintText("Catalog unavailable: ${catalog.message}") }
                is CatalogState.Loaded ->
                    if (catalog.modules.isEmpty()) {
                        item { HintText("Nobody has shared a module yet. Open a module and tap \"Share with class\".") }
                    } else {
                        items(catalog.modules, key = { "c-" + it.id }) { entry ->
                            CatalogCard(entry, installed = state.installed.any { it.info.id == entry.id })
                        }
                    }
            }
        }
    }
}

@Composable
private fun InstalledModuleCard(module: RegisteredModule, onClick: () -> Unit, onEnabledChange: (Boolean) -> Unit) {
    val info = module.info
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (module.enabled) CardDefaults.cardColors() else CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModuleIcon(info.icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(info.name, style = MaterialTheme.typography.titleMedium)
                Text(info.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${info.category.label} · v${info.version} · ${info.author}",
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(if (module.source == ModuleSource.BUILT_IN) "Built-in" else "Student")
                    Badge(module.provider.contributionSummary())
                }
            }
            Switch(
                checked = module.enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.semantics { contentDescription = "${info.name} enabled" },
            )
        }
    }
}

@Composable
private fun BuildYourOwnCard(onOpenGuide: () -> Unit) {
    Card(
        onClick = onOpenGuide,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Build your own module", style = MaterialTheme.typography.titleMedium)
            Text(
                "Modules add screens, Home tools and lesson renderers. Create one with a single Gradle " +
                    "command - tap for the step-by-step guide.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ProblemCard(problem: ModuleProblem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Column {
                Text("Module \"${problem.moduleId}\" was not loaded", style = MaterialTheme.typography.titleSmall)
                Text(problem.message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CatalogCard(entry: CatalogModule, installed: Boolean) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium)
            if (entry.description.isNotBlank()) Text(entry.description, style = MaterialTheme.typography.bodyMedium)
            Text("v${entry.version} · by ${entry.author} · shared by ${entry.ownerName}", style = MaterialTheme.typography.labelMedium)
            Text(entry.contributions.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            entry.sourceUrl?.let { Text("Code: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            if (installed) Badge("Installed in this app")
        }
    }
}

@Composable
fun ModuleIcon(icon: ImageVector, size: Int = 48) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(size.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun Badge(text: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
}

@Composable
private fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
