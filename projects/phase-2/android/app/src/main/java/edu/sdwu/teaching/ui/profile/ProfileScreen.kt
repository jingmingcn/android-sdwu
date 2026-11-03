package edu.sdwu.teaching.ui.profile

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.model.ThemeMode
import edu.sdwu.teaching.domain.model.User
import edu.sdwu.teaching.domain.model.UserPreferences
import edu.sdwu.teaching.ui.AppViewModelProvider
import edu.sdwu.teaching.ui.components.ServerUrlDialog
import edu.sdwu.teaching.ui.theme.TeachingTheme

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showServerDialog by rememberSaveable { mutableStateOf(false) }
    var serverError by rememberSaveable { mutableStateOf<String?>(null) }

    ProfileContent(
        uiState = uiState,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDynamicColorChange = viewModel::onDynamicColorChange,
        onSyncNow = viewModel::syncNow,
        onSignOut = viewModel::signOut,
        onChangeServer = { showServerDialog = true },
    )

    val serverUrl = uiState.preferences?.serverUrl
    if (showServerDialog && serverUrl != null) {
        ServerUrlDialog(
            currentUrl = serverUrl,
            error = serverError,
            onDismiss = { showServerDialog = false; serverError = null },
            onSave = { url ->
                viewModel.saveServerUrl(url) { ok ->
                    if (ok) { showServerDialog = false; serverError = null } else serverError = "Not a valid http(s) URL"
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onChangeServer: () -> Unit,
) {
    val preferences = uiState.preferences
    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AccountSection(uiState, onSyncNow, onSignOut)

            if (preferences != null) {
                SectionTitle("Appearance")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Theme", style = MaterialTheme.typography.titleSmall)
                        ThemeModeSelector(preferences.themeMode, onThemeModeChange)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ListItem(
                            headlineContent = { Text("Dynamic color") },
                            colors = TransparentListItem,
                            supportingContent = { Text("Use colors from your wallpaper") },
                            trailingContent = { Switch(checked = preferences.dynamicColor, onCheckedChange = null) },
                            // The whole row is the touch target; the Switch is only a visual.
                            modifier = Modifier.toggleable(
                                value = preferences.dynamicColor,
                                role = Role.Switch,
                                onValueChange = onDynamicColorChange,
                            ),
                        )
                    }
                }

                SectionTitle("Server")
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        headlineContent = { Text("Backend address") },
                        supportingContent = { Text(preferences.serverUrl) },
                        colors = TransparentListItem,
                        modifier = Modifier.clickable(onClickLabel = "Change server address", onClick = onChangeServer),
                    )
                }
            }

            SectionTitle("About")
            Card(Modifier.fillMaxWidth()) {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    headlineContent = { Text("Android Teaching Framework") },
                    colors = TransparentListItem,
                    supportingContent = { Text("Version 0.2.0 - Phase 2 (backend + offline-first)") },
                )
            }
        }
    }
}

/** Lets a ListItem take the color of the Card it sits in. */
private val TransparentListItem
    @Composable get() = ListItemDefaults.colors(containerColor = Color.Transparent)

@Composable
private fun AccountSection(uiState: ProfileUiState, onSyncNow: () -> Unit, onSignOut: () -> Unit) {
    val user = (uiState.session as? SessionState.SignedIn)?.user
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics(mergeDescendants = true) {},
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        initials(user?.displayName ?: "Guest"),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Text(user?.displayName ?: "Guest", style = MaterialTheme.typography.titleLarge)
            Text(
                text = when {
                    user == null -> "Progress is saved on this device only"
                    user.isInstructor -> "${user.email} - instructor"
                    else -> user.email
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            if (user != null) {
                Button(onClick = onSyncNow, enabled = !uiState.isSyncing) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Sync now")
                    }
                }
                OutlinedButton(onClick = onSignOut) { Text("Sign out") }
            } else {
                Button(onClick = onSignOut) { Text("Sign in or create account") }
            }
        }
        uiState.syncMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = ThemeMode.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = {
                    Text(
                        when (mode) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        },
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun ProfilePreview() {
    TeachingTheme(darkTheme = false) {
        ProfileContent(
            ProfileUiState(
                preferences = UserPreferences(serverUrl = "http://10.0.2.2:8000/"),
                session = SessionState.SignedIn(User(1, "student@example.com", "Demo Student", false), "token"),
            ),
            onThemeModeChange = {}, onDynamicColorChange = {}, onSyncNow = {}, onSignOut = {}, onChangeServer = {},
        )
    }
}
