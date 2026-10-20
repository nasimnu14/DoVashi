package com.example.dovashiapp.presentation.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dovashiapp.shared.generated.resources.ic_stop
import dovashiapp.shared.generated.resources.ic_mic
import kotlinx.coroutines.delay
import kotlin.time.Clock
import androidx.compose.runtime.remember
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dovashiapp.shared.generated.resources.Res
import dovashiapp.shared.generated.resources.ic_arrow_back
import dovashiapp.shared.generated.resources.ic_pause
import dovashiapp.shared.generated.resources.ic_play
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPlaybackClick: (Long) -> Unit,
    onMicClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (state as? ChatUiState.Content)?.title.orEmpty()
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = { (state as? ChatUiState.Content)?.let { MicBar(it.mic, it.micMessage, onMicClick) } },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                ChatUiState.Loading -> Unit
                ChatUiState.NotFound -> CenteredText("Conversation not found")
                is ChatUiState.Content ->
                    if (state.bubbles.isEmpty()) {
                        CenteredText("No messages yet")
                    } else {
                        MessageList(state.bubbles, onPlaybackClick)
                    }
            }
        }
    }
}

@Composable
private fun MessageList(bubbles: List<MessageBubbleUi>, onPlaybackClick: (Long) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxBubbleWidth = maxWidth * 0.85f
        val listState = rememberLazyListState()
        val newestId = bubbles.firstOrNull()?.id
        // Survives rotation, so a restored scroll position isn't overridden on recomposition.
        var lastNewestId by rememberSaveable { mutableStateOf(newestId) }
        // A new Message keeps the old anchor by key; follow it if the user was already at the newest.
        LaunchedEffect(newestId) {
            if (newestId != lastNewestId) {
                lastNewestId = newestId
                if (listState.firstVisibleItemIndex <= 1) listState.animateScrollToItem(0)
            }
        }
        // Newest first + reversed layout: opens at the newest Message, older ones are above.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        ) {
            items(bubbles, key = { it.id }) { bubble ->
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = if (bubble.side == BubbleSide.END) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    MessageBubble(bubble, Modifier.widthIn(max = maxBubbleWidth), onPlaybackClick = { onPlaybackClick(bubble.id) })
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(bubble: MessageBubbleUi, modifier: Modifier, onPlaybackClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (bubble.side == BubbleSide.END) colors.primaryContainer else colors.secondaryContainer,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val header = listOfNotNull("Voice", bubble.sourceLabel).joinToString(" · ")
            Text(header, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            bubble.originalText?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            if (bubble.translatedText != null) {
                Spacer(Modifier.size(4.dp))
                bubble.targetLabel?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant) }
                Text(bubble.translatedText, style = MaterialTheme.typography.bodyLarge)
                bubble.reading?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = colors.onSurfaceVariant)
                }
            }
            bubble.statusLabel?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bubble.isStatusError) colors.error else colors.onSurfaceVariant,
                )
            }
            PlaybackControl(bubble.playback, onPlaybackClick)
        }
    }
}

@Composable
private fun PlaybackControl(playback: BubblePlayback, onClick: () -> Unit) {
    when (playback) {
        BubblePlayback.NONE -> Unit
        BubblePlayback.UNAVAILABLE -> Text(
            "Recording unavailable",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            // Replaces the focused Play button, so announce it.
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        BubblePlayback.PLAY, BubblePlayback.PAUSE -> {
            val isPause = playback == BubblePlayback.PAUSE
            TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                // Decorative: the label carries the meaning for screen readers.
                Icon(painterResource(if (isPause) Res.drawable.ic_pause else Res.drawable.ic_play), contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (isPause) "Pause" else "Play")
            }
        }
    }
}

@Composable
private fun CenteredText(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MicBar(mic: MicUi, message: String?, onMicClick: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            val recording = mic as? MicUi.Recording
            FilledIconButton(
                onClick = onMicClick,
                enabled = mic != MicUi.Unavailable,
                modifier = Modifier.size(64.dp),
                colors = if (recording != null) {
                    IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    IconButtonDefaults.filledIconButtonColors()
                },
            ) {
                Icon(
                    painterResource(if (recording != null) Res.drawable.ic_stop else Res.drawable.ic_mic),
                    contentDescription = if (recording != null) "Stop recording" else "Start recording",
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                when {
                    recording != null -> "Recording ${elapsedLabel(recording.startedAtMillis)} · tap to stop"
                    mic == MicUi.Unavailable -> "Another conversation is recording"
                    else -> "Tap to speak"
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** "m:ss" since [startedAtMillis], ticking every second while shown. */
@Composable
private fun elapsedLabel(startedAtMillis: Long): String {
    var now by remember(startedAtMillis) { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            delay(1_000)
            now = Clock.System.now().toEpochMilliseconds()
        }
    }
    val seconds = ((now - startedAtMillis).coerceAtLeast(0) / 1_000).toInt()
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
