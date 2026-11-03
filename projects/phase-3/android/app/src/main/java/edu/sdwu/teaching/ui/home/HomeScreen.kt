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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.ui.components.LessonCard
import edu.sdwu.teaching.ui.theme.TeachingTheme

@Composable
fun HomeScreen(
    onLessonClick: (String) -> Unit,
    onBrowseAll: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(uiState, onLessonClick, onBrowseAll, onRefresh = viewModel::refresh)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onLessonClick: (String) -> Unit,
    onBrowseAll: () -> Unit,
    onRefresh: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(uiState.greetingName?.let { "Hi, $it" } ?: "Welcome back") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ProgressCard(uiState) }

                uiState.continueLesson?.let { lesson ->
                    item { SectionHeader("Continue learning", onBrowseAll) }
                    item {
                        LessonCard(lesson = lesson, progress = uiState.progress[lesson.id], onClick = { onLessonClick(lesson.id) })
                    }
                }
                if (uiState.upNext.isNotEmpty()) {
                    item { SectionHeader("Up next", action = null) }
                    items(uiState.upNext, key = { it.id }) { lesson ->
                        LessonCard(lesson = lesson, progress = uiState.progress[lesson.id], onClick = { onLessonClick(lesson.id) })
                    }
                }
                if (!uiState.isLoading && uiState.continueLesson == null) {
                    item {
                        Text(
                            "You finished every lesson. Great work!",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
                item { RoadmapCard() }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (action != null) TextButton(onClick = action) { Text("See all") }
    }
}

@Composable
private fun ProgressCard(uiState: HomeUiState) {
    val fraction = if (uiState.lessonCount == 0) 0f else uiState.completedCount.toFloat() / uiState.lessonCount
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp).semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Your progress", style = MaterialTheme.typography.titleMedium)
            Text(
                "${uiState.completedCount} of ${uiState.lessonCount} lessons completed",
                style = MaterialTheme.typography.headlineSmall,
            )
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            Text("${uiState.totalMinutes} minutes of material in total", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Shows students where the course project is heading. */
@Composable
private fun RoadmapCard() {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Framework roadmap", style = MaterialTheme.typography.titleMedium)
            listOf(
                "Phase 1 - Compose, Navigation, Room, DataStore",
                "Phase 2 - Backend, login, progress, quizzes, offline-first",
                "Phase 3 - Notes, search, video, notifications, Hilt, tests (you are here)",
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
            HomeUiState(
                isLoading = false,
                greetingName = "Demo Student",
                lessonCount = 8,
                completedCount = 3,
                totalMinutes = 280,
                continueLesson = SeedData.lessons[3],
                upNext = SeedData.lessons.drop(4).take(2),
            ),
            onLessonClick = {},
            onBrowseAll = {},
        )
    }
}
