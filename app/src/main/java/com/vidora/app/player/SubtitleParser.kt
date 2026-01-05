package com.vidora.app.player

import java.util.*

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

class SubtitleParser {

    fun parse(content: String, isVtt: Boolean): List<SubtitleCue> {
        return if (isVtt) parseVtt(content) else parseSrt(content)
    }

    private fun parseSrt(content: String): List<SubtitleCue> {
        val groups = content.trim().split(Regex("\\n\\s*\\n"))
        return groups.mapNotNull { group ->
            try {
                val lines = group.lines().filter { it.isNotBlank() }
                if (lines.size < 3) return@mapNotNull null
                
                val timeLine = lines[1]
                val times = timeLine.split(" --> ")
                if (times.size != 2) return@mapNotNull null
                
                val start = parseSrtTime(times[0])
                val end = parseSrtTime(times[1])
                val text = lines.drop(2).joinToString("\n")
                
                SubtitleCue(start, end, text)
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.startTimeMs }
    }

    private fun parseVtt(content: String): List<SubtitleCue> {
        val lines = content.lines().toMutableList()
        if (lines.isEmpty() || !lines[0].startsWith("WEBVTT")) return emptyList()
        
        val groups = content.trim().split(Regex("\\n\\s*\\n"))
        return groups.drop(1).mapNotNull { group -> // Skip WEBVTT header
            try {
                val groupLines = group.lines().filter { it.isNotBlank() }
                if (groupLines.size < 2) return@mapNotNull null
                
                // VTT timing might be on first or second line depending on if there's an ID
                var timeLine = groupLines[0]
                var textStart = 1
                if (!timeLine.contains(" --> ")) {
                    if (groupLines.size < 3) return@mapNotNull null
                    timeLine = groupLines[1]
                    textStart = 2
                }
                
                val times = timeLine.split(" --> ")
                if (times.size != 2) return@mapNotNull null
                
                val start = parseVttTime(times[0])
                val end = parseVttTime(times[1])
                val text = groupLines.drop(textStart).joinToString("\n").replace(Regex("<[^>]*>"), "")
                
                SubtitleCue(start, end, text)
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.startTimeMs }
    }

    private fun parseSrtTime(time: String): Long {
        // 00:00:20,000
        val parts = time.replace(",", ".").split(":")
        val h = parts[0].toLong()
        val m = parts[1].toLong()
        val sAndMs = parts[2].split(".")
        val s = sAndMs[0].toLong()
        val ms = sAndMs[1].toLong()
        return (h * 3600 + m * 60 + s) * 1000 + ms
    }

    private fun parseVttTime(time: String): Long {
        // 00:00:20.000 or 00:20.000
        val parts = time.split(":")
        var h = 0L
        var m = 0L
        var sAndMs = ""
        
        if (parts.size == 3) {
            h = parts[0].toLong()
            m = parts[1].toLong()
            sAndMs = parts[2]
        } else {
            m = parts[0].toLong()
            sAndMs = parts[1]
        }
        
        val sAndMsParts = sAndMs.split(".")
        val s = sAndMsParts[0].toLong()
        val ms = sAndMsParts[1].toLong()
        return (h * 3600 + m * 60 + s) * 1000 + ms
    }
}
