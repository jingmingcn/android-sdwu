package edu.sdwu.teaching.ui.modules

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import edu.sdwu.teaching.framework.FunctionHost

/**
 * Gives any composable access to the [FunctionHost] of a module:
 *
 *     val host = rememberFunctionHost("study-timer")
 *     tool.Widget(host, Modifier)
 *
 * Provided once by the navigation root (it owns navigation and the snackbar).
 */
fun interface ModuleHosts {
    fun hostFor(moduleId: String): FunctionHost
}

val LocalModuleHosts = staticCompositionLocalOf<ModuleHosts> {
    error("LocalModuleHosts is not provided. Wrap your UI in TeachingAppRoot.")
}

@Composable
fun rememberFunctionHost(moduleId: String): FunctionHost {
    val hosts = LocalModuleHosts.current
    return remember(moduleId, hosts) { hosts.hostFor(moduleId) }
}
