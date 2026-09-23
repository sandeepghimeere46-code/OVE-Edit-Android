package com.ove.edit.models

import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val timeline: Timeline = Timeline(),
    val createdAt: Long = System.currentTimeMillis(),
    var lastSavedAt: Long = System.currentTimeMillis()
)

data class Timeline(
    val videoTracks: List<VideoTrack> = listOf(VideoTrack()),
    val audioTracks: List<AudioTrack> = emptyList(),
    val textTracks: List<TextTrack> = emptyList()
)

data class VideoTrack(
    val id: String = UUID.randomUUID().toString(),
    val clips: MutableList<VideoClip> = mutableListOf()
)

data class AudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val clips: MutableList<AudioClip> = mutableListOf()
)

data class TextTrack(
    val id: String = UUID.randomUUID().toString(),
    val items: MutableList<Caption> = mutableListOf()
)

data class VideoClip(
    val uri: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val sourceDurationMs: Long,
    val speed: Float = 1.0f,
    val colorGradeLutUri: String? = null,
    val backgroundRemovalEnabled: Boolean = false
)

data class AudioClip(
    val uri: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val volume: Float = 1.0f,
    val isDuckable: Boolean = true
)

data class Caption(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val style: CaptionStyle = CaptionStyle()
)

data class CaptionStyle(
    val fontName: String = "Default",
    val fontSize: Int = 16,
    val colorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#00000000"
)

enum class EditActionType {
    TRIM,
    ADD_MUSIC,
    ADD_CAPTION,
    SPEED,
    COLOR_GRADE,
    BACKGROUND_REMOVE
}

data class ParsedEditCommand(
    val actionType: EditActionType,
    val parameters: Map<String, String>,
    val rawText: String
)
