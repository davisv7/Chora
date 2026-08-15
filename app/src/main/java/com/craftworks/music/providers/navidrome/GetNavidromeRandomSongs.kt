package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.media3.common.MediaItem
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.toMediaItem
import kotlinx.serialization.Serializable

@Serializable
data class RandomSongs(
    val song: List<MediaData.Song>? = null
)

fun parseNavidromeRandomSongsJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
): List<MediaItem> {
    val subsonicResponse = parseSubsonicResponse(response)

    val passwordSaltMedia = NavidromeDataSource.generateSalt(8)
    val passwordHashMedia = NavidromeDataSource.md5Hash(navidromePassword + passwordSaltMedia)

    val songs = subsonicResponse.randomSongs?.song?.map {
        it.copy(
            media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora",
            imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora&size=128"
        ).toMediaItem()
    } ?: emptyList()

    Log.d("NAVIDROME", "Got random songs. Total: ${songs.size}")

    return songs
}
