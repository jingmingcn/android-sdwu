package edu.sdwu.teaching.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes. A route is any @Serializable object/class;
// its constructor parameters become the navigation arguments.

@Serializable data object HomeRoute
@Serializable data object LessonsRoute
@Serializable data object ProfileRoute
@Serializable data class LessonDetailRoute(val lessonId: String)
@Serializable data class QuizRoute(val lessonId: String)
@Serializable data object NotesRoute
@Serializable data object EditProfileRoute
@Serializable data object ModulesRoute
@Serializable data class ModuleDetailRoute(val moduleId: String)
@Serializable data class FunctionRoute(val functionId: String)

/** The tabs of the bottom navigation bar. */
enum class TopLevelDestination(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(HomeRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    LESSONS(LessonsRoute, "Lessons", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
    MODULES(ModulesRoute, "Modules", Icons.Filled.Build, Icons.Outlined.Build),
    PROFILE(ProfileRoute, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
}
