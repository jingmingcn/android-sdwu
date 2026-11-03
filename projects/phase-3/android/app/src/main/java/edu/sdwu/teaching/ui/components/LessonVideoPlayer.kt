package edu.sdwu.teaching.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import kotlinx.coroutines.delay

/**
 * A 100% Compose video player built on Media3 ExoPlayer and [PlayerSurface] (no XML PlayerView).
 * Same call site as Phase 1's VideoPlaceholder: the extension point paid off.
 *
 * Lifecycle rules shown here:
 *  - the player is created once per URL and released when the composable leaves the screen;
 *  - playback pauses when the app goes to the background (ON_STOP);
 *  - nothing is downloaded until the student presses play.
 */
@Composable
fun LessonVideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(videoUrl)) }
    }
    var state by remember(videoUrl) { mutableStateOf(PlayerUiState()) }
    var progress by remember(videoUrl) { mutableFloatStateOf(0f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                state = state.copy(
                    isPlaying = player.isPlaying,
                    isBuffering = player.playbackState == Player.STATE_BUFFERING,
                    isEnded = player.playbackState == Player.STATE_ENDED,
                    hasFirstFrame = state.hasFirstFrame || player.playbackState == Player.STATE_READY,
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                state = state.copy(error = "Video unavailable (${error.errorCodeName})")
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Pause when the app is in the background; the student resumes manually.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) player.pause() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ExoPlayer has no position callback, so poll it while playing.
    LaunchedEffect(player, state.isPlaying) {
        while (state.isPlaying) {
            val duration = player.duration
            if (duration > 0) progress = player.currentPosition.toFloat() / duration
            delay(500)
        }
    }

    Column(modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Color.Black)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                // While playing, the whole video area is the pause target (with a TalkBack label).
                .clickable(enabled = state.isPlaying, onClickLabel = "Pause video") { player.pause() },
            contentAlignment = Alignment.Center,
        ) {
            if (state.started) {
                PlayerSurface(player = player, surfaceType = SURFACE_TYPE_SURFACE_VIEW, modifier = Modifier.fillMaxSize())
            }
            when {
                state.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = {
                        state = state.copy(error = null)
                        player.prepare()
                        player.play()
                    }) { Icon(Icons.Filled.Refresh, contentDescription = "Retry video", tint = Color.White) }
                }
                state.isBuffering || (state.started && !state.hasFirstFrame) ->
                    CircularProgressIndicator(color = Color.White)
                !state.isPlaying -> PlayButton(
                    onClick = {
                        if (!state.started) {
                            player.prepare()
                            state = state.copy(started = true)
                        }
                        if (state.isEnded) player.seekTo(0) // Replay from the start.
                        player.play()
                    },
                )
            }
        }
        if (state.started) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f))
                Text(
                    when {
                        state.isPlaying -> "  Playing - tap to pause"
                        state.isBuffering -> "  Loading..."
                        state.isEnded -> "  Finished"
                        else -> "  Paused"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun PlayButton(onClick: () -> Unit) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.9f)) {
        IconButton(onClick = onClick, modifier = Modifier.size(64.dp)) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play video", tint = Color.Black, modifier = Modifier.size(40.dp))
        }
    }
}

private data class PlayerUiState(
    val started: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val hasFirstFrame: Boolean = false,
    val error: String? = null,
)
