package edu.sdwu.teaching.ui.lessons

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.domain.model.Difficulty
import edu.sdwu.teaching.domain.model.LessonFilter
import edu.sdwu.teaching.domain.model.ProgressStatus
import edu.sdwu.teaching.ui.components.Bookmark
import edu.sdwu.teaching.ui.components.EmptyState
import edu.sdwu.teaching.ui.components.LessonCard
import edu.sdwu.teaching.ui.components.LoadingState
import edu.sdwu.teaching.ui.theme.TeachingTheme

/** Stateful entry point: gets its state from the ViewModel. */
@Composable
fun LessonListScreen(
    onLessonClick: (String) -> Unit,
    viewModel: LessonListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LessonListContent(
        uiState = uiState,
        actions = LessonListActions(
            onLessonClick = onLessonClick,
            onRefresh = viewModel::refresh,
            onMessageShown = viewModel::onMessageShown,
            onQueryChange = viewModel::onQueryChange,
            onCategory = viewModel::onCategorySelected,
            onDifficulty = viewModel::onDifficultySelected,
            onStatus = viewModel::onStatusSelected,
            onBookmarkedOnly = viewModel::onBookmarkedOnlyToggle,
            onClearFilters = viewModel::clearFilters,
        ),
    )
}

/** Grouping the callbacks keeps the stateless composable's signature readable. */
data class LessonListActions(
    val onLessonClick: (String) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onCategory: (String) -> Unit = {},
    val onDifficulty: (Difficulty) -> Unit = {},
    val onStatus: (ProgressStatus) -> Unit = {},
    val onBookmarkedOnly: () -> Unit = {},
    val onClearFilters: () -> Unit = {},
)

/** Stateless UI: easy to preview and to test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListContent(uiState: LessonListUiState, actions: LessonListActions) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = (uiState as? LessonListUiState.Success)?.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            actions.onMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Lessons") }) },
    ) { padding ->
        when (uiState) {
            LessonListUiState.Loading -> LoadingState(Modifier.padding(padding))
            is LessonListUiState.Success -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = actions.onRefresh,
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { SearchAndFilters(uiState, actions) }
                    if (uiState.lessons.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No matching lessons",
                                message = "Try other words or clear the filters.",
                                icon = Icons.Filled.Search,
                                actionLabel = "Clear filters",
                                onAction = actions.onClearFilters,
                            )
                        }
                    }
                    items(uiState.lessons, key = { it.id }) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            progress = uiState.progress[lesson.id],
                            isBookmarked = lesson.id in uiState.bookmarks,
                            onClick = { actions.onLessonClick(lesson.id) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilters(state: LessonListUiState.Success, actions: LessonListActions) {
    val focusManager = LocalFocusManager.current
    val filter = state.filter
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = filter.query,
            onValueChange = actions.onQueryChange,
            placeholder = { Text("Search lessons") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (filter.query.isNotEmpty()) {
                    IconButton(onClick = { actions.onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        // Each row scrolls sideways so chips never wrap or overflow on small phones.
        ChipRow {
            FilterChip(
                selected = filter.bookmarkedOnly,
                onClick = actions.onBookmarkedOnly,
                label = { Text("Bookmarked") },
                leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null) },
            )
            ProgressStatus.entries.forEach { status ->
                FilterChip(
                    selected = filter.status == status,
                    onClick = { actions.onStatus(status) },
                    label = { Text(status.label) },
                )
            }
        }
        ChipRow {
            state.categories.forEach { category ->
                FilterChip(selected = filter.category == category, onClick = { actions.onCategory(category) }, label = { Text(category) })
            }
            Difficulty.entries.forEach { difficulty ->
                FilterChip(
                    selected = filter.difficulty == difficulty,
                    onClick = { actions.onDifficulty(difficulty) },
                    label = { Text(difficulty.label) },
                )
            }
        }
        if (filter.isActive) {
            Text(
                "Showing ${state.lessons.size} of ${state.totalCount} lessons",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // TalkBack announces the new count as the student types.
                modifier = Modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

private val ProgressStatus.label: String
    get() = when (this) {
        ProgressStatus.NOT_STARTED -> "Not started"
        ProgressStatus.IN_PROGRESS -> "In progress"
        ProgressStatus.COMPLETED -> "Completed"
    }

@Preview
@Composable
private fun LessonListPreview() {
    TeachingTheme(darkTheme = false) {
        LessonListContent(
            LessonListUiState.Success(
                lessons = SeedData.lessons.take(3),
                totalCount = 8,
                categories = listOf("Kotlin", "Compose", "Architecture", "Data"),
                filter = LessonFilter(query = "compose"),
                progress = emptyMap(),
                bookmarks = setOf("compose-fundamentals"),
                isRefreshing = false,
                message = null,
            ),
            LessonListActions(),
        )
    }
}
