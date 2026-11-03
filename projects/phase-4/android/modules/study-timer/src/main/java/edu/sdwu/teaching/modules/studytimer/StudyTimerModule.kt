package edu.sdwu.teaching.modules.studytimer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.sdwu.teaching.framework.CustomFunction
import edu.sdwu.teaching.framework.FunctionHost
import edu.sdwu.teaching.framework.ModuleCategory
import edu.sdwu.teaching.framework.ModuleInfo
import edu.sdwu.teaching.framework.ModuleProvider
import edu.sdwu.teaching.framework.ToolEntry

/**
 * Built-in example module #1: a focus timer (Pomodoro technique).
 *
 * It shows two extension points working together:
 *  - [StudyTimerFunction]: a full screen (CustomFunction) with the timer;
 *  - [StudyTimerTool]: a Home widget (ToolEntry) with today's count and a shortcut to the screen.
 * Both share data through the module's private [FunctionHost.storage].
 */
class StudyTimerModule : ModuleProvider {
    override val info = ModuleInfo(
        id = "study-timer",
        name = "Study Timer",
        description = "Focus for 25 minutes, then take a break. Counts your focus sessions.",
        author = "Course staff",
        version = "1.0.0",
        icon = Icons.Filled.DateRange,
        category = ModuleCategory.STUDY_TOOL,
    )

    override val functions: List<CustomFunction> = listOf(StudyTimerFunction)
    override val tools: List<ToolEntry> = listOf(StudyTimerTool)
}

internal object StudyTimerFunction : CustomFunction {
    const val ID = "study-timer-screen"
    override val id = ID
    override val title = "Study Timer"
    override val description = "25-minute focus sessions with short breaks"
    override val icon = Icons.Filled.DateRange

    @Composable
    override fun Content(host: FunctionHost) = StudyTimerScreen(host)
}

internal object StudyTimerTool : ToolEntry {
    override val id = "study-timer-widget"
    override val title = "Focus sessions"

    @Composable
    override fun Widget(host: FunctionHost, modifier: Modifier) = StudyTimerWidget(host, modifier)
}

/** Storage keys, shared by the screen and the widget. */
internal object TimerKeys {
    const val SESSIONS = "sessions_completed"
}
