package edu.sdwu.teaching.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.domain.model.QuizQuestion
import edu.sdwu.teaching.domain.model.QuizScorer
import edu.sdwu.teaching.ui.AppViewModelProvider
import edu.sdwu.teaching.ui.theme.TeachingTheme

@Composable
fun QuizScreen(
    onClose: () -> Unit,
    viewModel: QuizViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    QuizContent(
        uiState = uiState,
        onClose = onClose,
        onSelect = viewModel::selectAnswer,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onSubmit = viewModel::submit,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizContent(
    uiState: QuizUiState,
    onClose: () -> Unit,
    onSelect: (questionId: String, option: Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close quiz") }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            QuizUiState.Loading -> Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            QuizUiState.Empty -> Box(modifier, contentAlignment = Alignment.Center) { Text("This lesson has no quiz yet.") }
            is QuizUiState.Answering -> AnsweringBody(uiState, onSelect, onPrevious, onNext, onSubmit, modifier)
            is QuizUiState.Finished -> ResultBody(uiState, onRetry, onClose, modifier)
        }
    }
}

@Composable
private fun AnsweringBody(
    state: QuizUiState.Answering,
    onSelect: (String, Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier,
) {
    val question = state.current
    val selected = state.answers[question.id]
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LinearProgressIndicator(
            progress = { (state.index + 1f) / state.questions.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Question ${state.index + 1} of ${state.questions.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(question.question, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })

        // selectableGroup + selectable(role = RadioButton) makes TalkBack read "1 of 4, selected".
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.options.forEachIndexed { index, option ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected == index,
                            onClick = { onSelect(question.id, index) },
                            role = Role.RadioButton,
                        ),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == index, onClick = null)
                        Text(option, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPrevious, enabled = state.index > 0, modifier = Modifier.weight(1f)) {
                Text("Previous")
            }
            Button(
                onClick = if (state.isLast) onSubmit else onNext,
                enabled = selected != null,
                modifier = Modifier.weight(1f),
            ) { Text(if (state.isLast) "Submit" else "Next") }
        }
    }
}

@Composable
private fun ResultBody(state: QuizUiState.Finished, onRetry: () -> Unit, onClose: () -> Unit, modifier: Modifier) {
    val result = state.result
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (result.passed) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.tertiaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp).semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${result.score} / ${result.total}", style = MaterialTheme.typography.displaySmall)
                Text(
                    if (result.passed) "Passed! Lesson marked as completed."
                    else "Score ${QuizScorer.PASS_PERCENT}% or more to pass. Review and try again.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        state.questions.forEach { q -> ReviewItem(q, state.answers[q.id], result.correctByQuestion[q.id] == true) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("Try again") }
            Button(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Done") }
        }
    }
}

@Composable
private fun ReviewItem(question: QuizQuestion, chosen: Int?, correct: Boolean) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (correct) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = if (correct) "Correct" else "Wrong",
                    tint = if (correct) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                )
                Text(question.question, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
            }
            if (!correct) {
                Text("Your answer: ${chosen?.let { question.options[it] } ?: "-"}", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Answer: ${question.options[question.correctIndex]}", style = MaterialTheme.typography.bodyMedium)
            Text(question.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview
@Composable
private fun QuizPreview() {
    val questions = SeedData.quizQuestions.take(3)
    TeachingTheme(darkTheme = false) {
        QuizContent(
            QuizUiState.Answering(questions, index = 0, answers = mapOf(questions[0].id to 1)),
            onClose = {}, onSelect = { _, _ -> }, onPrevious = {}, onNext = {}, onSubmit = {}, onRetry = {},
        )
    }
}
