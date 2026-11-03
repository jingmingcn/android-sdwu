package edu.sdwu.teaching.modules.studytimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.sdwu.teaching.framework.FunctionHost
import edu.sdwu.teaching.framework.PreviewFunctionHost
import edu.sdwu.teaching.framework.rememberSetter
import edu.sdwu.teaching.framework.rememberValue
import kotlinx.coroutines.delay

/** Timer modes. "Demo" is one minute, handy for showing the module in class. */
internal enum class TimerMode(val label: String, val minutes: Int) {
    FOCUS("Focus 25", 25),
    BREAK("Break 5", 5),
    DEMO("Demo 1", 1),
}

@Composable
internal fun StudyTimerScreen(host: FunctionHost) {
    val setValue = host.storage.rememberSetter()
    val sessions by host.storage.rememberValue(TimerKeys.SESSIONS)

    // rememberSaveable: the timer survives rotation.
    var mode by rememberSaveable { mutableStateOf(TimerMode.FOCUS) }
    var remaining by rememberSaveable { mutableIntStateOf(mode.minutes * 60) }
    var running by rememberSaveable { mutableStateOf(false) }

    // Ticks once per second while running. Restarts whenever `running` changes.
    LaunchedEffect(running) {
        while (running && remaining > 0) {
            delay(1_000)
            remaining--
        }
        if (running && remaining == 0) {
            if (mode != TimerMode.BREAK) {
                val count = (host.storage.get(TimerKeys.SESSIONS)?.toIntOrNull() ?: 0) + 1
                host.storage.put(TimerKeys.SESSIONS, count.toString())
                host.showMessage("Focus session complete! Time for a break.")
            } else {
                host.showMessage("Break over. Ready for another session?")
            }
            // Last! `running` is this effect's key: changing it cancels the effect, so any
            // suspend call after this line (like the storage writes above) would never run.
            running = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimerMode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = {
                        mode = m
                        remaining = m.minutes * 60
                        running = false
                    },
                    label = { Text(m.label) },
                )
            }
        }

        val total = mode.minutes * 60
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { remaining.toFloat() / total },
                strokeWidth = 12.dp,
                modifier = Modifier.size(240.dp),
            )
            Text(
                text = formatTime(remaining),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics {
                    contentDescription = "${remaining / 60} minutes ${remaining % 60} seconds left"
                },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { running = !running }, enabled = remaining > 0) {
                Text(if (running) "Pause" else "Start")
            }
            OutlinedButton(onClick = {
                running = false
                remaining = total
            }) { Text("Reset") }
        }

        Text(
            text = "Focus sessions completed: ${sessions ?: "0"}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        TextButton(onClick = { setValue(TimerKeys.SESSIONS, null) }) {
            Text("Reset counter")
        }
    }
}

@Composable
internal fun StudyTimerWidget(host: FunctionHost, modifier: Modifier) {
    val sessions by host.storage.rememberValue(TimerKeys.SESSIONS)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Study Timer", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${sessions ?: "0"} focus sessions completed",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(onClick = { host.openFunction(StudyTimerFunction.ID) }) { Text("Focus") }
        }
    }
}

/** 90 -> "01:30". Pure function, unit tested. */
internal fun formatTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

@Preview(showBackground = true)
@Composable
private fun StudyTimerPreview() {
    MaterialTheme { StudyTimerScreen(remember { PreviewFunctionHost(moduleId = "study-timer") }) }
}

@Preview(showBackground = true)
@Composable
private fun StudyTimerWidgetPreview() {
    MaterialTheme { StudyTimerWidget(remember { PreviewFunctionHost() }, Modifier.fillMaxWidth()) }
}
