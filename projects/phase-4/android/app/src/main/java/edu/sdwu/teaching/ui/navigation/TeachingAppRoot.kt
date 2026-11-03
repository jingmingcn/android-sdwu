package edu.sdwu.teaching.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import edu.sdwu.teaching.data.modules.HostNavigator
import edu.sdwu.teaching.data.modules.ModuleHostFactory
import edu.sdwu.teaching.framework.FunctionHost
import edu.sdwu.teaching.ui.home.HomeScreen
import edu.sdwu.teaching.ui.modules.FunctionScreen
import edu.sdwu.teaching.ui.modules.LocalModuleHosts
import edu.sdwu.teaching.ui.modules.ModuleDetailScreen
import edu.sdwu.teaching.ui.modules.ModuleHosts
import edu.sdwu.teaching.ui.modules.ModulesScreen
import edu.sdwu.teaching.ui.lessons.LessonDetailScreen
import edu.sdwu.teaching.ui.lessons.LessonListScreen
import edu.sdwu.teaching.ui.notes.NotesScreen
import edu.sdwu.teaching.ui.profile.EditProfileScreen
import edu.sdwu.teaching.ui.profile.ProfileScreen
import edu.sdwu.teaching.ui.quiz.QuizScreen

/**
 * App shell: a Scaffold with the bottom bar and the NavHost that swaps screens.
 * It also provides [LocalModuleHosts], because it owns what modules may trigger:
 * navigation (openLesson / openFunction) and the snackbar (showMessage).
 */
@Composable
fun TeachingAppRoot(
    hostFactory: ModuleHostFactory,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { currentDestination.isOn(it.route) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val moduleHosts = remember(hostFactory, navController) {
        val navigator = object : HostNavigator {
            override fun openLesson(lessonId: String) = navController.navigate(LessonDetailRoute(lessonId))
            override fun openFunction(functionId: String) = navController.navigate(FunctionRoute(functionId))
            override fun showMessage(text: String) {
                scope.launch { snackbarHostState.showSnackbar(text) }
            }
        }
        val cache = mutableMapOf<String, FunctionHost>()
        ModuleHosts { moduleId -> cache.getOrPut(moduleId) { hostFactory.create(moduleId, navigator) } }
    }

    CompositionLocalProvider(LocalModuleHosts provides moduleHosts) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // Each screen draws its own top bar and handles its own system bar insets.
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        TopLevelDestination.entries.forEach { destination ->
                            val selected = currentDestination.isOn(destination.route)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.navigateToTopLevel(destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = null, // The label below already describes the item.
                                    )
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        onLessonClick = { navController.navigate(LessonDetailRoute(it)) },
                        onBrowseAll = { navController.navigateToTopLevel(TopLevelDestination.LESSONS) },
                    )
                }
                composable<LessonsRoute> {
                    LessonListScreen(onLessonClick = { navController.navigate(LessonDetailRoute(it)) })
                }
                composable<LessonDetailRoute> { entry ->
                    // toRoute() decodes the type-safe arguments; the id is handed to the ViewModel factory.
                    LessonDetailScreen(
                        lessonId = entry.toRoute<LessonDetailRoute>().lessonId,
                        onBack = navController::popBackStack,
                        onTakeQuiz = { navController.navigate(QuizRoute(it)) },
                    )
                }
                composable<QuizRoute> { entry ->
                    QuizScreen(lessonId = entry.toRoute<QuizRoute>().lessonId, onClose = navController::popBackStack)
                }
                composable<ProfileRoute> {
                    ProfileScreen(
                        onOpenNotes = { navController.navigate(NotesRoute) },
                        onEditProfile = { navController.navigate(EditProfileRoute) },
                    )
                }
                composable<NotesRoute> {
                    NotesScreen(
                        onBack = navController::popBackStack,
                        onLessonClick = { navController.navigate(LessonDetailRoute(it)) },
                    )
                }
                composable<EditProfileRoute> {
                    EditProfileScreen(onDone = navController::popBackStack)
                }

                // ----- Module framework -----
                composable<ModulesRoute> {
                    ModulesScreen(onModuleClick = { navController.navigate(ModuleDetailRoute(it)) })
                }
                composable<ModuleDetailRoute> { entry ->
                    ModuleDetailScreen(
                        moduleId = entry.toRoute<ModuleDetailRoute>().moduleId,
                        onBack = navController::popBackStack,
                        onOpenFunction = { navController.navigate(FunctionRoute(it)) },
                    )
                }
                composable<FunctionRoute> { entry ->
                    FunctionScreen(functionId = entry.toRoute<FunctionRoute>().functionId, onBack = navController::popBackStack)
                }
            }
        }
    }
}

private fun NavDestination?.isOn(route: Any): Boolean =
    this?.hierarchy?.any { it.hasRoute(route::class) } == true

/** Standard bottom-bar navigation: one copy of each tab, with saved/restored state. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
