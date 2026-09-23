package com.craftworks.music.ui.playing

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.player.ChoraMediaLibraryService
import kotlinx.coroutines.flow.MutableStateFlow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayQueueContent(
    mediaController: MediaController?,
    modifier: Modifier = Modifier
) {
    if (mediaController == null)
        return
    val currentList = remember { mutableStateListOf<MediaItem>() }
    // Timeline index for each entry in currentList — used to translate display
    // positions back to the timeline when shuffle mode is on.
    val timelineIndices = remember { mutableStateListOf<Int>() }

    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragCurrentIndex by remember { mutableIntStateOf(-1) }

    var currentMediaItem by remember { mutableStateOf(mediaController.currentMediaItem) }
    var shuffleModeEnabled by remember { mutableStateOf(mediaController.shuffleModeEnabled) }

    val haptic = LocalHapticFeedback.current

    // Observe the service's shuffle order so the queue reflects actual play
    // order in shuffle mode. Empty list means "no shuffle order available" —
    // either shuffle is off, or the service isn't reachable, in which case
    // we fall back to timeline order.
    val serviceShuffleFlow = remember {
        ChoraMediaLibraryService.getInstance()?.shuffleOrder ?: MutableStateFlow(emptyList())
    }
    val shuffleOrder by serviceShuffleFlow.collectAsStateWithLifecycle()

    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                currentMediaItem = mediaController.currentMediaItem
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaItem = mediaController.currentMediaItem
            }

            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                shuffleModeEnabled = enabled
            }
        }

        currentMediaItem = mediaController.currentMediaItem
        mediaController.addListener(listener)
        onDispose { mediaController.removeListener(listener) }
    }

    LaunchedEffect(mediaController, shuffleModeEnabled, shuffleOrder, currentMediaItem) {
        currentList.clear()
        timelineIndices.clear()
        val useShuffle = shuffleModeEnabled && shuffleOrder.isNotEmpty() &&
            shuffleOrder.size == mediaController.mediaItemCount
        if (useShuffle) {
            // Show the whole shuffle sequence — items above the current one
            // are played history, items below are up next. The service
            // anchors the current item to position 0 whenever shuffle is
            // enabled, so nothing above current is an unplayed orphan.
            shuffleOrder.forEach { timelineIdx ->
                currentList.add(mediaController.getMediaItemAt(timelineIdx))
                timelineIndices.add(timelineIdx)
            }
        } else {
            for (i in 0 until mediaController.mediaItemCount) {
                currentList.add(mediaController.getMediaItemAt(i))
                timelineIndices.add(i)
            }
        }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (currentMediaItem in currentList) {
            lazyListState.scrollToItem(currentList.indexOf(currentMediaItem))
        }
    }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (dragStartIndex == -1) dragStartIndex = from.index
        currentList.add(to.index, currentList.removeAt(from.index))
        // Keep timelineIndices in lockstep so click/seek during a drag uses
        // the right timeline position for the item now at each display index.
        if (from.index in timelineIndices.indices) {
            timelineIndices.add(to.index, timelineIndices.removeAt(from.index))
        }
        dragCurrentIndex = to.index
    }

    // Position of the playing item in the display list; drives the "up next"
    // numbering below so items after current start at 1 in shuffle mode.
    val currentDisplayIndex = currentList.indexOf(currentMediaItem)

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(currentList, key = { _, item -> item.mediaId }) { index, item ->
            ReorderableItem(
                state = reorderableState,
                key = item.mediaId,
                animateItemModifier = Modifier.animateItem(
                    placementSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                )
            ) { draggingThis ->
                val elevation by animateDpAsState(
                    targetValue = if (draggingThis) 6.dp else 0.dp,
                    label = "queue_item_elevation"
                )
                val isCurrentItem = item == currentMediaItem

                Surface(
                    tonalElevation = elevation,
                    shadowElevation = elevation,
                    color = when {
                        draggingThis -> MaterialTheme.colorScheme.surfaceContainerHighest
                        isCurrentItem -> MaterialTheme.colorScheme.secondaryContainer
                        else -> BottomSheetDefaults.ContainerColor
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().clickable {
                        val timelineIdx = timelineIndices.getOrNull(index) ?: index
                        mediaController.seekTo(timelineIdx, 0)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrentItem && !draggingThis) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Now playing",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (shuffleModeEnabled) {
                                // Shuffle mode: items above current are played
                                // history (no number), items below are up next
                                // starting at 1.
                                if (currentDisplayIndex >= 0 && index > currentDisplayIndex) {
                                    Text(
                                        text = "${index - currentDisplayIndex}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                // Shuffle off: timeline position + 1.
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Title + artist
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = item.mediaMetadata.title?.toString() ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrentItem) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            item.mediaMetadata.artist?.toString()?.let { artist ->
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = (
                                            if (isCurrentItem) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                            ).copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Drag handle
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.baseline_drag_handle_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        // Commit to player only if the item actually moved.
                                        if (dragStartIndex != -1 && dragCurrentIndex != -1 &&
                                            dragStartIndex != dragCurrentIndex
                                        ) {
                                            if (shuffleModeEnabled) {
                                                // Display index maps directly to a shuffle
                                                // position since we render the whole shuffle
                                                // order. LaunchedEffect resyncs from the new
                                                // shuffle order after commit, so moving the
                                                // currently-playing song or dropping onto its
                                                // slot works without leaving stale UI.
                                                val service = ChoraMediaLibraryService.getInstance()
                                                if (service != null) {
                                                    service.moveInShuffleOrder(dragStartIndex, dragCurrentIndex)
                                                } else {
                                                    val fromTl = timelineIndices.getOrNull(dragStartIndex) ?: dragStartIndex
                                                    val toTl = timelineIndices.getOrNull(dragCurrentIndex) ?: dragCurrentIndex
                                                    mediaController.moveMediaItem(fromTl, toTl)
                                                }
                                            } else {
                                                mediaController.moveMediaItem(dragStartIndex, dragCurrentIndex)
                                            }
                                        }
                                        dragStartIndex = -1
                                        dragCurrentIndex = -1
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}