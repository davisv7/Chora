@file:OptIn(UnstableApi::class) package com.craftworks.music.player

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongHelper {
    companion object{
        suspend fun play(
            mediaItems: List<MediaItem>,
            index: Int,
            mediaController: MediaController?,
            shuffle: Boolean = false,
        ) {
            if (mediaItems.isEmpty())
                return

            withContext(Dispatchers.Main) {
                // Set shuffle mode BEFORE loading media items so the service's
                // shuffle-anchor logic doesn't scramble a chronological queue.
                mediaController?.shuffleModeEnabled = shuffle
                mediaController?.setMediaItems(mediaItems, index, 0)
                mediaController?.prepare()
                mediaController?.play()
            }
        }

        // Insert items so they play immediately after the current one, even in
        // shuffle mode. Routes through the service so we can rewrite the
        // ExoPlayer ShuffleOrder (MediaController can't do that on its own).
        fun playNext(mediaController: MediaController?, items: List<MediaItem>) {
            if (items.isEmpty()) return

            val service = ChoraMediaLibraryService.getInstance()
            if (service != null) {
                service.playNext(items)
                return
            }

            // Service not reachable — fall back to naive insert (correct only
            // when shuffle is off).
            val controller = mediaController ?: return
            val insertAt = (controller.currentMediaItemIndex + 1).coerceAtLeast(0)
            items.forEachIndexed { i, item -> controller.addMediaItem(insertAt + i, item) }
        }

        fun playNext(mediaController: MediaController?, item: MediaItem) =
            playNext(mediaController, listOf(item))
    }
}