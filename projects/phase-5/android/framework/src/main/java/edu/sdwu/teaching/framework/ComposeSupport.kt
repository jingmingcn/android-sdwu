package edu.sdwu.teaching.framework

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Observes one storage value as Compose state:
 *
 *     val sessions by host.storage.rememberValue("sessions")
 *
 * Why not `host.storage.observe(key).collectAsState(null)`? Because observe() returns a NEW
 * Flow on every call. collectAsState restarts whenever it gets a different Flow, so every
 * recomposition would restart the collection, emit again and recompose again: an endless
 * loop that freezes the app (we hit exactly this while building the Study Timer module).
 * This helper remembers the Flow, so it is created once per key.
 */
@Composable
fun ModuleStorage.rememberValue(key: String): State<String?> {
    val flow = remember(this, key) { observe(key) }
    return flow.collectAsState(initial = null)
}

/** Returns a function that writes a storage value from a click handler: `save("count", "3")`. */
@Composable
fun ModuleStorage.rememberSetter(): (key: String, value: String?) -> Unit {
    val scope = rememberCoroutineScope()
    return remember(this, scope) { { key, value -> scope.launch { put(key, value) } } }
}
