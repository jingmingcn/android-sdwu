package edu.sdwu.teaching.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * Profile picture loaded with Coil. While loading, on error, or without a URL it shows the
 * user's initials, so the avatar never looks broken.
 */
@Composable
fun UserAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val initials: @Composable () -> Unit = {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size)) {
            Box(contentAlignment = Alignment.Center) {
                Text(initials(name), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
    if (avatarUrl.isNullOrBlank()) {
        Box(modifier) { initials() }
    } else {
        SubcomposeAsyncImage(
            model = avatarUrl,
            contentDescription = null, // Decorative: the name is shown next to it.
            contentScale = ContentScale.Crop,
            loading = { initials() },
            error = { initials() },
            modifier = modifier.size(size).clip(CircleShape),
        )
    }
}

fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
