package edu.sdwu.teaching.modules.codesnippets

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.sdwu.teaching.framework.CustomFunction
import edu.sdwu.teaching.framework.FunctionHost
import edu.sdwu.teaching.framework.ModuleCategory
import edu.sdwu.teaching.framework.ModuleInfo
import edu.sdwu.teaching.framework.ModuleProvider
import edu.sdwu.teaching.framework.PreviewFunctionHost
import edu.sdwu.teaching.framework.ToolEntry
import edu.sdwu.teaching.framework.rememberSetter
import edu.sdwu.teaching.framework.rememberValue
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Student example #1: Code Snippet Viewer.
 *
 * Created with `./gradlew newModule -Pid=code-snippets -Pname="Code Snippet Viewer"`, then filled in.
 * Two contributions:
 *  - a screen (CustomFunction) to search, read, copy and favourite snippets;
 *  - a Home widget (ToolEntry) with the "snippet of the day".
 * Favourites are saved in the module's private storage.
 */
class CodeSnippetsModule : ModuleProvider {
    override val info = ModuleInfo(
        id = "code-snippets",
        name = "Code Snippet Viewer",
        description = "Searchable Kotlin and Compose snippets with highlighting, copy and favourites.",
        author = "Example Student",
        version = "1.0.0",
        icon = Icons.Filled.Edit,
        category = ModuleCategory.STUDY_TOOL,
    )

    override val functions: List<CustomFunction> = listOf(SnippetsFunction)
    override val tools: List<ToolEntry> = listOf(SnippetOfTheDayTool)
}

internal object SnippetsFunction : CustomFunction {
    const val ID = "code-snippets-screen"
    override val id = ID
    override val title = "Code Snippets"
    override val description = "Search, copy and favourite code examples"
    override val icon = Icons.Filled.Edit

    @Composable
    override fun Content(host: FunctionHost) = SnippetsScreen(host)
}

internal object SnippetOfTheDayTool : ToolEntry {
    override val id = "snippet-of-the-day"
    override val title = "Snippet of the day"

    @Composable
    override fun Widget(host: FunctionHost, modifier: Modifier) {
        val snippet = remember { snippetOfTheDay(LocalDate.now().toEpochDay()) }
        Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Snippet of the day", style = MaterialTheme.typography.labelLarge)
                Text(snippet.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    snippet.code.lines().first(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 1,
                )
                TextButton(onClick = { host.openFunction(SnippetsFunction.ID) }, contentPadding = PaddingValues(0.dp)) {
                    Text("Open snippets")
                }
            }
        }
    }
}

private const val FAVORITES_KEY = "favorites"

@Composable
internal fun SnippetsScreen(host: FunctionHost) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }

    // Favourites are stored as "id1,id2,..." in module storage.
    val favoritesText by host.storage.rememberValue(FAVORITES_KEY)
    val save = host.storage.rememberSetter()
    val favorites = favoritesText?.split(',')?.filter { it.isNotBlank() }?.toSet().orEmpty()

    val visible = filterSnippets(query, category).filter { !favoritesOnly || it.id in favorites }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search snippets") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("Favourites") })
                SNIPPETS.map { it.category }.distinct().forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = if (category == c) null else c }, label = { Text(c) })
                }
            }
        }
        if (visible.isEmpty()) {
            item { Text("No snippets match.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(visible, key = { it.id }) { snippet ->
            SnippetCard(
                snippet = snippet,
                isFavorite = snippet.id in favorites,
                onToggleFavorite = {
                    val updated = if (snippet.id in favorites) favorites - snippet.id else favorites + snippet.id
                    save(FAVORITES_KEY, updated.joinToString(",").ifEmpty { null })
                },
                onCopied = { host.showMessage("Copied \"${snippet.title}\"") },
            )
        }
    }
}

@Composable
private fun SnippetCard(snippet: Snippet, isFavorite: Boolean, onToggleFavorite: () -> Unit, onCopied: () -> Unit) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(snippet.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(snippet.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Code block: dark background, monospace, scrolls sideways instead of wrapping.
            Text(
                highlight(snippet.code),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFFE6E6E6),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1F22), MaterialTheme.shapes.small)
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            )
            Text(snippet.explanation, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = {
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(snippet.title, snippet.code)))
                    onCopied()
                }
            }) { Text("Copy code") }
        }
    }
}

/** Colours from a dark IDE theme, applied to the tokens from [tokenizeKotlin]. */
private fun highlight(code: String): AnnotatedString = buildAnnotatedString {
    tokenizeKotlin(code).forEach { token ->
        val style = when (token.type) {
            TokenType.KEYWORD -> SpanStyle(color = Color(0xFFCF8E6D), fontWeight = FontWeight.Bold)
            TokenType.STRING -> SpanStyle(color = Color(0xFF6AAB73))
            TokenType.COMMENT -> SpanStyle(color = Color(0xFF7A7E85), fontStyle = FontStyle.Italic)
            TokenType.NUMBER -> SpanStyle(color = Color(0xFF2AACB8))
            TokenType.ANNOTATION -> SpanStyle(color = Color(0xFFB3AE60))
            TokenType.PLAIN -> null
        }
        if (style == null) append(token.text) else withStyle(style) { append(token.text) }
    }
}

@Preview(showBackground = true)
@Composable
private fun SnippetsPreview() {
    MaterialTheme { SnippetsScreen(remember { PreviewFunctionHost(moduleId = "code-snippets") }) }
}
