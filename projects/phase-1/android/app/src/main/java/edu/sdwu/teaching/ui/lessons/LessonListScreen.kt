package edu.sdwu.teaching.ui.lessons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    LessonListContent(uiState = uiState, onLessonClick = onLessonClick)
}

/** Stateless UI: easy to preview and to test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListContent(
    uiState: LessonListUiState,
    onLessonClick: (String) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TopAppBar(title = { Text("Lessons") }, scrollBehavior = scrollBehavior) },
    ) { padding ->
        when (uiState) {
            LessonListUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is LessonListUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.lessons, key = { it.id }) { lesson ->
                    LessonCard(lesson = lesson, onClick = { onLessonClick(lesson.id) })
                }
            }
        }
    }
}

@Preview
@Composable
private fun LessonListPreview() {
    TeachingTheme(darkTheme = false) {
        LessonListContent(LessonListUiState.Success(SeedData.lessons), onLessonClick = {})
    }
}
