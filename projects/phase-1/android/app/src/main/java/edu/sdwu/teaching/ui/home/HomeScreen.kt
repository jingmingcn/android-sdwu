package edu.sdwu.teaching.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.ui.AppViewModelProvider
import edu.sdwu.teaching.ui.components.LessonCard
import edu.sdwu.teaching.ui.theme.TeachingTheme

@Composable
fun HomeScreen(
    onLessonClick: (String) -> Unit,
    onBrowseAll: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(uiState, onLessonClick, onBrowseAll)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onLessonClick: (String) -> Unit,
    onBrowseAll: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text("Welcome back") }, scrollBehavior = scrollBehavior)
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { StatsCard(uiState) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Start here",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    TextButton(onClick = onBrowseAll) { Text("See all") }
                }
            }
            items(uiState.featured, key = { it.id }) { lesson ->
                LessonCard(lesson = lesson, onClick = { onLessonClick(lesson.id) })
            }
            item { RoadmapCard() }
        }
    }
}

@Composable
private fun StatsCard(uiState: HomeUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat(value = uiState.lessonCount.toString(), label = "Lessons")
            Stat(value = uiState.categoryCount.toString(), label = "Topics")
            Stat(value = "${uiState.totalMinutes}", label = "Minutes")
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/** Shows students where the course project is heading. */
@Composable
private fun RoadmapCard() {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Framework roadmap", style = MaterialTheme.typography.titleMedium)
            listOf(
                "Phase 1 - Compose, Navigation, Room, DataStore (you are here)",
                "Phase 2 - Backend, login, progress, quizzes, offline-first",
                "Phase 3 - Notes, search, video, notifications, Hilt, tests",
                "Phase 4 - Module system for custom functions",
                "Phase 5 - Build and plug in your own module",
            ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Preview
@Composable
private fun HomePreview() {
    TeachingTheme(darkTheme = false) {
        HomeContent(
            HomeUiState(isLoading = false, lessonCount = 8, totalMinutes = 280, categoryCount = 4, featured = SeedData.lessons.take(3)),
            onLessonClick = {},
            onBrowseAll = {},
        )
    }
}
