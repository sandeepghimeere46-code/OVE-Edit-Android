package com.ove.edit.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.ove.edit.models.Timeline
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VideoEditorEngine(private val context: Context) {

    private val transformer: Transformer = Transformer.Builder(context)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .setAudioMimeType(MimeTypes.AUDIO_AAC)
        .build()

    /**
     * Executes the actual render/export using Media3 Transformer.
     * Takes the internal multi-track Timeline model and maps it to a Media3 Composition.
     */
    suspend fun exportProject(timeline: Timeline, outputPath: String): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            
            // Build the sequences for each video track
            val sequences = mutableListOf<EditedMediaItemSequence>()

            for (videoTrack in timeline.videoTracks) {
                val editedMediaItems = mutableListOf<EditedMediaItem>()

                for (clip in videoTrack.clips) {
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(clip.uri))
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(clip.startTimeMs)
                                .setEndPositionMs(clip.endTimeMs)
                                .build()
                        )
                        .build()

                    val videoEffects = mutableListOf<androidx.media3.common.Effect>()
                    
                    // Add effects based on clip properties (e.g., Color Grade LUTs would go here)
                    // if (clip.colorGradeLutUri != null) { ... apply SingleColorLut ... }

                    val editedItem = EditedMediaItem.Builder(mediaItem)
                        .setEffects(Effects(emptyList(), videoEffects))
                        .setRemoveAudio(false) // Keep native audio unless ducked/removed
                        .build()

                    editedMediaItems.add(editedItem)
                }

                if (editedMediaItems.isNotEmpty()) {
                    sequences.add(EditedMediaItemSequence(editedMediaItems))
                }
            }

            if (sequences.isEmpty()) {
                continuation.resume(Result.failure(Exception("Timeline is empty. Nothing to export.")))
                return@suspendCancellableCoroutine
            }

            // Combine all sequences into a final Composition (handling multi-track overlaps)
            val composition = Composition.Builder(sequences)
                .experimentalSetForceAudioTrack(true) // Ensure audio track exists even if some clips are silent
                .build()

            val listener = object : Transformer.Listener {
                override fun onCompleted(comp: Composition, exportResult: ExportResult) {
                    if (continuation.isActive) {
                        continuation.resume(Result.success(outputPath))
                    }
                }

                override fun onError(
                    comp: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(exportException)
                    }
                }
            }

            val exportTransformer = transformer.buildUpon().addListener(listener).build()
            
            exportTransformer.start(composition, outputPath)

            continuation.invokeOnCancellation {
                exportTransformer.cancel()
            }
        }
    }
}
