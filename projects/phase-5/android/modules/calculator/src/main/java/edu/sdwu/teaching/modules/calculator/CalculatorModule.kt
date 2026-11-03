package edu.sdwu.teaching.modules.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.sdwu.teaching.framework.CustomFunction
import edu.sdwu.teaching.framework.FunctionHost
import edu.sdwu.teaching.framework.ModuleCategory
import edu.sdwu.teaching.framework.ModuleInfo
import edu.sdwu.teaching.framework.ModuleProvider
import edu.sdwu.teaching.framework.PreviewFunctionHost
import edu.sdwu.teaching.framework.rememberSetter
import edu.sdwu.teaching.framework.rememberValue

/**
 * Student example #2: Simple Calculator.
 *
 * Created with `./gradlew newModule -Pid=calculator -Pname="Simple Calculator"`, then filled in.
 * It shows how to keep logic out of the UI: [evaluate] (Expression.kt) is plain Kotlin with its
 * own unit tests; this file only draws the keypad and keeps the history in module storage.
 */
class CalculatorModule : ModuleProvider {
    override val info = ModuleInfo(
        id = "calculator",
        name = "Simple Calculator",
        description = "A calculator with brackets, percentages and a history that survives restarts.",
        author = "Example Student",
        version = "1.0.0",
        icon = Icons.Filled.Add,
        category = ModuleCategory.UTILITY,
    )

    override val functions: List<CustomFunction> = listOf(CalculatorFunction)
}

internal object CalculatorFunction : CustomFunction {
    override val id = "calculator-screen"
    override val title = "Calculator"
    override val description = "+ − × ÷ %, brackets and history"
    override val icon = Icons.Filled.Add

    @Composable
    override fun Content(host: FunctionHost) = CalculatorScreen(host)
}

private const val HISTORY_KEY = "history"
private const val HISTORY_SIZE = 5

/** Keypad rows. "C" clears, "⌫" deletes one character, "=" evaluates. */
private val KEYS = listOf(
    listOf("C", "(", ")", "÷"),
    listOf("7", "8", "9", "×"),
    listOf("4", "5", "6", "−"),
    listOf("1", "2", "3", "+"),
    listOf("0", ".", "⌫", "="),
)

@Composable
internal fun CalculatorScreen(host: FunctionHost) {
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }
    val historyText by host.storage.rememberValue(HISTORY_KEY)
    val save = host.storage.rememberSetter()
    val history = historyText?.lines()?.filter { it.isNotBlank() }.orEmpty()

    fun press(key: String) {
        when (key) {
            "C" -> { input = ""; output = "" }
            "⌫" -> input = input.dropLast(1)
            "=" -> when (val result = evaluate(input)) {
                is CalcResult.Value -> {
                    val line = "$input = ${result.text}"
                    save(HISTORY_KEY, (listOf(line) + history).take(HISTORY_SIZE).joinToString("\n"))
                    output = result.text
                    input = result.text
                }
                is CalcResult.Error -> output = result.message
            }
            else -> input += key
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.End) {
                Text(
                    input.ifEmpty { "0" },
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.semantics { contentDescription = "Expression ${input.ifEmpty { "empty" }}" },
                )
                Text(
                    output,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }

        KEYS.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key -> Key(key, Modifier.weight(1f)) { press(key) } }
            }
        }

        if (history.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { save(HISTORY_KEY, null) }) { Text("Clear") }
            }
            history.forEach { line ->
                // Tapping a history line puts its result back into the calculator.
                TextButton(onClick = { input = line.substringAfter("= "); output = "" }) {
                    Text(line, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier, onClick: () -> Unit) {
    val keyModifier = modifier.aspectRatio(1.4f)
    val description = when (label) {
        "⌫" -> "Delete"
        "C" -> "Clear"
        "÷" -> "Divide"
        "×" -> "Multiply"
        "−" -> "Minus"
        "+" -> "Plus"
        "=" -> "Equals"
        else -> label
    }
    val a11y = Modifier.semantics { contentDescription = description }
    val text: @Composable () -> Unit = { Text(label, style = MaterialTheme.typography.titleLarge) }
    when (label) {
        "=" -> Button(onClick = onClick, modifier = keyModifier.then(a11y)) { text() }
        "÷", "×", "−", "+" -> FilledTonalButton(onClick = onClick, modifier = keyModifier.then(a11y)) { text() }
        "C", "⌫" -> OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = keyModifier.then(a11y),
        ) { text() }
        else -> OutlinedButton(onClick = onClick, modifier = keyModifier.then(a11y)) { text() }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalculatorPreview() {
    MaterialTheme { CalculatorScreen(remember { PreviewFunctionHost(moduleId = "calculator") }) }
}
