package edu.sdwu.teaching.ui.lessons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.ui.AppViewModelProvider
import edu.sdwu.teaching.ui.components.LessonCard
import edu.sdwu.teaching.ui.theme.TeachingTheme

/** Stateful entry point: gets its state from the ViewModel. */
@Composable
fun LessonListScreen(
    onLessonClick: (String) -> Unit,
    viewModel: LessonListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LessonListContent(
        uiState = uiState,
        onLessonClick = onLessonClick,
        onRefresh = viewModel::refresh,
        onMessageShown = viewModel::onMessageShown,
    )
}

/** Stateless UI: easy to preview and to test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListContent(
    uiState: LessonListUiState,
    onLessonClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val message = (uiState as? LessonListUiState.Success)?.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Lessons") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh lessons from server")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when (uiState) {
            LessonListUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is LessonListUiState.Success -> Column(Modifier.padding(top = padding.calculateTopPadding())) {
                if (uiState.isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = padding.calculateBottomPadding() + 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.lessons, key = { it.id }) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            progress = uiState.progress[lesson.id],
                            onClick = { onLessonClick(lesson.id) },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LessonListPreview() {
    TeachingTheme(darkTheme = false) {
        LessonListContent(
            LessonListUiState.Success(SeedData.lessons, emptyMap(), isRefreshing = true, message = null),
            onLessonClick = {},
            onRefresh = {},
            onMessageShown = {},
        )
    }
}
