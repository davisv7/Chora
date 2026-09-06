package com.craftworks.music.ui.elements

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R

@OptIn(ExperimentalFoundationApi::class)
@Stable
@Composable
fun AlbumCard(
    album: MediaItem,
    onClick: () -> Unit = { },
    onPlay: (album: MediaItem) -> Unit = { },
    onAddToQueue: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (album.mediaMetadata.mediaType != MediaMetadata.MEDIA_TYPE_ALBUM) return
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(128.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (onAddToQueue != null || onPlayNext != null) expanded = true
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(album.mediaMetadata.artworkUri)
                    .crossfade(true)
                    .diskCacheKey(
                        album.mediaMetadata.extras?.getString("navidromeID") ?: album.mediaId
                    )
                    .build(),
                contentDescription = "Album Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            IconButton(
                onClick = { onPlay(album) },
                modifier = Modifier
                    .padding(6.dp)
                    .background(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                        shape = CircleShape
                    )
                    .height(36.dp)
                    .size(36.dp)
                    .align(Alignment.BottomStart)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Play Album",
                    modifier = Modifier.fillMaxSize()
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                onAddToQueue?.let {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.Action_Add_To_Queue)) },
                        onClick = { it(); expanded = false },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.outline_queue_add_24),
                                contentDescription = null
                            )
                        }
                    )
                }
                onPlayNext?.let {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.Action_Play_Next)) },
                        onClick = { it(); expanded = false },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.media3_notification_seek_to_next),
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = album.mediaMetadata.albumTitle.toString(),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Text(
            text = album.mediaMetadata.albumArtist.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
