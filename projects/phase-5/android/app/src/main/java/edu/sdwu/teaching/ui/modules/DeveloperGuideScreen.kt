package edu.sdwu.teaching.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** One step of the guide: explanation plus an optional code block. */
private data class GuideStep(val title: String, val text: String, val code: String? = null)

private val STEPS = listOf(
    GuideStep(
        "Create the module",
        "Run this in the android/ folder. It copies templates/module into modules/<id> with a working " +
            "screen, a test and the discovery file.",
        "./gradlew newModule -Pid=flash-cards \\\n    -Pname=\"Flash Cards\" -Pauthor=\"Your Name\"",
    ),
    GuideStep(
        "Run the app",
        "Sync Gradle (or just build). Every folder in modules/ is included automatically and found at startup " +
            "through META-INF/services, so there is nothing else to register. Your module appears in this tab.",
    ),
    GuideStep(
        "Build your screen",
        "Open modules/<id>/src/main/java/.../<Name>Module.kt. The app draws the top bar; you draw the rest in " +
            "Content(host). Use the @Preview with PreviewFunctionHost to iterate without running the app.",
        "@Composable\noverride fun Content(host: FunctionHost) {\n    val count by host.storage.rememberValue(\"count\")\n    Text(\"Saved: ${'$'}{count ?: 0}\")\n}",
    ),
    GuideStep(
        "Use the host",
        "FunctionHost is everything your module may use: host.lessons, host.studentName, host.storage, " +
            "host.openLesson(id), host.openFunction(id), host.showMessage(text).",
    ),
    GuideStep(
        "Add more contributions (optional)",
        "A Home widget: implement ToolEntry and add it to tools. A lesson layout: implement LessonRenderer " +
            "(canRender + Render) and add it to lessonRenderers. See the Code Snippet Viewer and Structured Lessons modules.",
        "override val tools = listOf(MyWidget)\noverride val lessonRenderers = listOf(MyRenderer)",
    ),
    GuideStep(
        "Test it",
        "Put logic in plain Kotlin functions and unit test them, like evaluate() in the calculator module.",
        "./gradlew :modules:flash-cards:testDebugUnitTest",
    ),
    GuideStep(
        "Share it",
        "Signed in? Open your module here and tap \"Share with class\". Its name, version and contributions " +
            "(never code) appear in the class catalog.",
    ),
)

/** In-app version of STUDENT_GUIDE.md, so the steps are one tap away while coding. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperGuideScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build your own module") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "A module is a small Gradle project that implements the framework contracts. Seven steps:",
                style = MaterialTheme.typography.bodyLarge,
            )
            STEPS.forEachIndexed { index, step -> StepView(index + 1, step) }
            Text(
                "Stuck? Broken modules are listed under Problems in the Modules tab with the reason. A module that " +
                    "crashes the app is switched off automatically on the next start. Full guide: STUDENT_GUIDE.md.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StepView(number: Int, step: GuideStep) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(number.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(step.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(step.text, style = MaterialTheme.typography.bodyMedium)
            step.code?.let { code ->
                Text(
                    code,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                )
            }
        }
    }
}
