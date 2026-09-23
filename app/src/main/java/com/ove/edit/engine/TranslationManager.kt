package com.ove.edit.engine

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.ove.edit.models.TextTrack
import kotlinx.coroutines.tasks.await

/**
 * Handles offline caption translation using Google ML Kit.
 */
class TranslationManager {

    suspend fun translateTextTrack(
        sourceTrack: TextTrack,
        targetLanguageCode: String = TranslateLanguage.SPANISH
    ): TextTrack {
        
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLanguageCode)
            .build()
            
        val translator = Translation.getClient(options)
        
        // Ensure language pack is downloaded before translating (offline capable)
        val conditions = DownloadConditions.Builder().build() // No wifi requirement for OVE
        
        try {
            translator.downloadModelIfNeeded(conditions).await()
            
            val translatedItems = sourceTrack.items.map { caption ->
                val translatedText = translator.translate(caption.text).await()
                caption.copy(text = translatedText)
            }.toMutableList()
            
            return sourceTrack.copy(id = "translated_track", items = translatedItems)
            
        } catch (e: Exception) {
            e.printStackTrace()
            return sourceTrack // Return original on failure
        } finally {
            translator.close()
        }
    }
}
