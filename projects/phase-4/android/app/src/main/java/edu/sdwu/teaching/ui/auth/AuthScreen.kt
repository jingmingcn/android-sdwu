package edu.sdwu.teaching.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import edu.sdwu.teaching.ui.components.ServerUrlDialog
import edu.sdwu.teaching.ui.theme.TeachingTheme

@Composable
fun AuthScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    var showServerDialog by rememberSaveable { mutableStateOf(false) }
    var serverError by rememberSaveable { mutableStateOf<String?>(null) }

    AuthContent(
        uiState = uiState,
        serverUrl = serverUrl,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onSubmit = viewModel::submit,
        onToggleMode = viewModel::toggleMode,
        onContinueAsGuest = viewModel::continueAsGuest,
        onChangeServer = { showServerDialog = true },
    )

    if (showServerDialog) {
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

@Composable
fun AuthContent(
    uiState: AuthUiState,
    serverUrl: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onChangeServer: () -> Unit,
) {
    val isRegister = uiState.mode == AuthMode.REGISTER
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Android Teaching Framework",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 32.dp).semantics { heading() },
            )
            Text(
                text = if (isRegister) "Create your student account" else "Sign in to sync your progress",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val fieldModifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
            if (isRegister) {
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = fieldModifier,
                )
            }
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = fieldModifier,
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (uiState.canSubmit) onSubmit() }),
                supportingText = if (isRegister) ({ Text("6+ characters with a letter and a digit") }) else null,
                modifier = fieldModifier,
            )

            uiState.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    // TalkBack announces the error as soon as it appears.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            Button(onClick = onSubmit, enabled = uiState.canSubmit, modifier = fieldModifier) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text(if (isRegister) "Create account" else "Sign in")
                }
            }
            TextButton(onClick = onToggleMode) {
                Text(if (isRegister) "I already have an account" else "New here? Create an account")
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            OutlinedButton(onClick = onContinueAsGuest, modifier = fieldModifier) {
                Text("Continue without account")
            }
            Text(
                text = "Lessons and quizzes work offline. Progress stays on this device until you sign in.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            TextButton(onClick = onChangeServer) {
                Text("Server: $serverUrl", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Preview
@Composable
private fun AuthPreview() {
    TeachingTheme(darkTheme = false) {
        AuthContent(
            uiState = AuthUiState(email = "student@example.com", error = "Wrong email or password"),
            serverUrl = "http://10.0.2.2:8000/",
            onEmailChange = {}, onPasswordChange = {}, onDisplayNameChange = {},
            onSubmit = {}, onToggleMode = {}, onContinueAsGuest = {}, onChangeServer = {},
        )
    }
}
