package com.vidora.app.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Data class to hold stream information
 */
data class StreamInfo(
    val streamUrl: String,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val qualities: List<QualityLevel> = emptyList()
)

data class SubtitleTrack(
    val language: String,
    val label: String,
    val url: String
)

data class QualityLevel(
    val resolution: String,
    val bandwidth: Int,
    val url: String
)

/**
 * Parses HLS manifest to extract subtitle tracks and quality levels
 */
class HlsManifestParser {
    
    suspend fun parseManifest(manifestUrl: String): StreamInfo = withContext(Dispatchers.IO) {
        try {
            val manifestContent = URL(manifestUrl).readText()
            
            val subtitles = parseSubtitles(manifestContent, manifestUrl)
            val qualities = parseQualities(manifestContent, manifestUrl)
            
            StreamInfo(
                streamUrl = manifestUrl,
                subtitles = subtitles,
                qualities = qualities
            )
        } catch (e: Exception) {
            // If parsing fails, return basic info
            StreamInfo(streamUrl = manifestUrl)
        }
    }
    
    private fun parseSubtitles(manifest: String, baseUrl: String): List<SubtitleTrack> {
        val subtitles = mutableListOf<SubtitleTrack>()
        val lines = manifest.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            
            // Look for subtitle tracks: #EXT-X-MEDIA:TYPE=SUBTITLES
            if (line.startsWith("#EXT-X-MEDIA:") && line.contains("TYPE=SUBTITLES")) {
                val language = extractAttribute(line, "LANGUAGE") ?: "unknown"
                val name = extractAttribute(line, "NAME") ?: language
                val uri = extractAttribute(line, "URI")
                
                if (uri != null) {
                    val fullUrl = resolveUrl(baseUrl, uri)
                    subtitles.add(SubtitleTrack(language, name, fullUrl))
                }
            }
            i++
        }
        
        return subtitles
    }
    
    private fun parseQualities(manifest: String, baseUrl: String): List<QualityLevel> {
        val qualities = mutableListOf<QualityLevel>()
        val lines = manifest.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            
            // Look for quality variants: #EXT-X-STREAM-INF
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val bandwidth = extractAttribute(line, "BANDWIDTH")?.toIntOrNull() ?: 0
                val resolution = extractAttribute(line, "RESOLUTION") ?: "Auto"
                
                // Next line should be the URL
                if (i + 1 < lines.size) {
                    val url = lines[i + 1].trim()
                    if (url.isNotEmpty() && !url.startsWith("#")) {
                        val fullUrl = resolveUrl(baseUrl, url)
                        qualities.add(QualityLevel(resolution, bandwidth, fullUrl))
                    }
                }
            }
            i++
        }
        
        return qualities.sortedByDescending { it.bandwidth }
    }
    
    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = """$attribute="([^"]+)"""".toRegex()
        val match = pattern.find(line)
        return match?.groupValues?.get(1)
            ?: run {
                // Try without quotes
                val pattern2 = """$attribute=([^,\s]+)""".toRegex()
                pattern2.find(line)?.groupValues?.get(1)
            }
    }
    
    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return if (relativeUrl.startsWith("http")) {
            relativeUrl
        } else {
            val base = baseUrl.substringBeforeLast("/")
            "$base/$relativeUrl"
        }
    }
}
