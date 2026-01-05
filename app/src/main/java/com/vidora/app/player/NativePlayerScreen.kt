package com.vidora.app.player

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem as ExoMediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.vidora.app.data.remote.MediaItem as VidoraMediaItem
import com.vidora.app.ui.components.ErrorStateView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "NativePlayer"

data class VideoQuality(
    val height: Int,
    val label: String,
    val index: Int
)

@Composable
fun NativePlayerScreen(
    media: VidoraMediaItem,
    playerUrl: String,
    onBack: () -> Unit
) {
    var state by remember { mutableStateOf<PlayerState>(PlayerState.Loading("Initializing player...")) }
    var extractedUrl by remember { mutableStateOf<String?>(null) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var availableQualities by remember { mutableStateOf<List<VideoQuality>>(emptyList()) }
    var selectedQuality by remember { mutableStateOf<VideoQuality?>(null) }
    var exoPlayerInstance by remember { mutableStateOf<ExoPlayer?>(null) }
    val detectedSubtitles = remember { mutableStateMapOf<String, SubtitleTrack>() }
    var currentSubtitleCues by remember { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var activeSubtitleTrack by remember { mutableStateOf<SubtitleTrack?>(null) }
    var webViewKey by remember { mutableStateOf(0) } // For forcing WebView recreation on retry
    
    val scope = rememberCoroutineScope()
    val okHttpClient = remember { OkHttpClient() }
    val subtitleParser = remember { SubtitleParser() }

    // Fetch subtitles when track changes
    LaunchedEffect(activeSubtitleTrack) {
        val track = activeSubtitleTrack
        if (track != null) {
            scope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        try {
                            val request = Request.Builder().url(track.url).build()
                            okHttpClient.newCall(request).execute().use { response ->
                                response.body?.string() ?: ""
                            }
                        } catch (e: Exception) { 
                            Log.e(TAG, "Failed to fetch subtitle: ${track.url}", e)
                            ""
                        }
                    }
                    if (content.isNotEmpty()) {
                        currentSubtitleCues = subtitleParser.parse(content, track.url.contains(".vtt") || content.startsWith("WEBVTT"))
                        Log.d(TAG, "Loaded ${currentSubtitleCues.size} subtitle cues for ${track.label}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing subtitles", e)
                }
            }
        } else {
            currentSubtitleCues = emptyList()
        }
    }
    
    // Timeout tracking with extended duration
    LaunchedEffect(webViewKey) {
        delay(30000) // 30 seconds
        if (state is PlayerState.Loading) {
            state = PlayerState.Error("Stream extraction timed out. The website may be blocking automated access.")
        }
    }

    // Retry function
    val retry = {
        Log.d(TAG, "Retrying stream extraction...")
        extractedUrl = null
        detectedSubtitles.clear()
        currentSubtitleCues = emptyList()
        activeSubtitleTrack = null
        webViewKey++ // Force WebView recreation
        state = PlayerState.Loading("Retrying stream extraction...")
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val currentState = state) {
            is PlayerState.Loading -> {
                LoadingState(media, currentState.message, onBack)
                
                // Invisible WebView for extraction - recreated when webViewKey changes
                LaunchedEffect(webViewKey) {
                    // This effect will restart when webViewKey changes, forcing AndroidView recreation
                }
                
                AndroidView(
                    factory = { context ->
                        android.webkit.WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            alpha = 0f
                            
                            Log.d(TAG, "WebView created, loading: $playerUrl")

                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d(TAG, "Page finished loading: ${url}")
                                    
                                    // CONFIG-BASED subtitle discovery - extracts from player config immediately
                                    val script = """
                                        (function() {
                                            console.log('[PN] Starting config-based subtitle discovery...');
                                            let foundTracks = new Set();
                                            
                                            function reportTrack(lang, label, url) {
                                                const key = lang + '|' + url;
                                                if (!foundTracks.has(key) && url) {
                                                    foundTracks.add(key);
                                                    console.log('[PN] Found subtitle:', lang, label, url);
                                                    try {
                                                        AndroidSubtitles.onSubtitleFound(lang, label, url);
                                                    } catch(e) {
                                                        console.error('[PN] Error reporting subtitle:', e);
                                                    }
                                                }
                                            }
                                            
                                            function deepSearchConfig(obj, depth = 0, maxDepth = 5) {
                                                if (depth > maxDepth || !obj || typeof obj !== 'object') return;
                                                
                                                // Look for tracks/subtitles/captions arrays
                                                const trackKeys = ['tracks', 'subtitles', 'captions', 'textTracks', 'subs'];
                                                for (const key of trackKeys) {
                                                    if (obj[key] && Array.isArray(obj[key])) {
                                                        console.log('[PN] Found tracks array at:', key);
                                                        obj[key].forEach(track => {
                                                            const url = track.file || track.src || track.url;
                                                            const lang = track.language || track.lang || track.srclang || 'unknown';
                                                            const label = track.label || track.name || lang.toUpperCase();
                                                            if (url) {
                                                                reportTrack(lang, label, url);
                                                            }
                                                        });
                                                    }
                                                }
                                                
                                                // Recursively search nested objects
                                                for (const key in obj) {
                                                    if (obj.hasOwnProperty(key) && typeof obj[key] === 'object') {
                                                        deepSearchConfig(obj[key], depth + 1, maxDepth);
                                                    }
                                                }
                                            }
                                            
                                            function extractSubtitles() {
                                                console.log('[PN] Scanning for player configurations...');
                                                
                                                // Method 1: Check window.player and variants
                                                const playerVars = ['player', 'videoPlayer', 'jwplayer', 'videojs', 'plyr', 'Player'];
                                                for (const varName of playerVars) {
                                                    if (window[varName]) {
                                                        console.log('[PN] Found window.' + varName);
                                                        
                                                        // Check if it's a function (like jwplayer())
                                                        if (typeof window[varName] === 'function') {
                                                            try {
                                                                const playerInstance = window[varName]();
                                                                if (playerInstance) {
                                                                    deepSearchConfig(playerInstance);
                                                                    
                                                                    // JWPlayer specific
                                                                    if (playerInstance.getConfig) {
                                                                        deepSearchConfig(playerInstance.getConfig());
                                                                    }
                                                                    if (playerInstance.getCaptionsList) {
                                                                        const caps = playerInstance.getCaptionsList();
                                                                        console.log('[PN] JWPlayer captions:', caps);
                                                                    }
                                                                }
                                                            } catch(e) {
                                                                console.log('[PN] Error calling ' + varName + '():', e);
                                                            }
                                                        } else {
                                                            // It's an object, search it
                                                            deepSearchConfig(window[varName]);
                                                        }
                                                    }
                                                }
                                                
                                                // Method 2: Check common config object names
                                                const configVars = ['playerConfig', 'config', 'videoConfig', 'setup', 'options', 'settings'];
                                                for (const varName of configVars) {
                                                    if (window[varName]) {
                                                        console.log('[PN] Found window.' + varName);
                                                        deepSearchConfig(window[varName]);
                                                    }
                                                }
                                                
                                                // Method 3: Parse script tags for embedded config
                                                document.querySelectorAll('script').forEach(script => {
                                                    const content = script.textContent || script.innerHTML;
                                                    
                                                    // Look for common patterns
                                                    const patterns = [
                                                        /tracks\s*:\s*\[(.*?)\]/gs,
                                                        /subtitles\s*:\s*\[(.*?)\]/gs,
                                                        /captions\s*:\s*\[(.*?)\]/gs
                                                    ];
                                                    
                                                    for (const pattern of patterns) {
                                                        const matches = content.match(pattern);
                                                        if (matches) {
                                                            console.log('[PN] Found tracks in script tag');
                                                            // Try to parse as JSON
                                                            try {
                                                                const jsonStr = '{' + matches[0] + '}';
                                                                const parsed = eval('(' + jsonStr + ')');
                                                                deepSearchConfig(parsed);
                                                            } catch(e) {
                                                                console.log('[PN] Could not parse script config:', e);
                                                            }
                                                        }
                                                    }
                                                });
                                                
                                                // Method 4: Check HTML5 video elements (immediate)
                                                document.querySelectorAll('video').forEach(video => {
                                                    video.querySelectorAll('track').forEach(track => {
                                                        if (track.src) {
                                                            reportTrack(
                                                                track.srclang || 'unknown',
                                                                track.label || track.srclang || 'Unknown',
                                                                track.src
                                                            );
                                                        }
                                                    });
                                                });
                                                
                                                // Method 5: Check data attributes
                                                document.querySelectorAll('[data-setup], [data-config], [data-player]').forEach(el => {
                                                    const dataAttrs = ['data-setup', 'data-config', 'data-player'];
                                                    for (const attr of dataAttrs) {
                                                        const data = el.getAttribute(attr);
                                                        if (data) {
                                                            try {
                                                                const config = JSON.parse(data);
                                                                deepSearchConfig(config);
                                                            } catch(e) {}
                                                        }
                                                    }
                                                });
                                                
                                                console.log('[PN] Config scan complete. Found:', foundTracks.size, 'tracks');
                                            }
                                            
                                            // Run immediately
                                            extractSubtitles();
                                            
                                            // Also run after a short delay in case config loads dynamically
                                            setTimeout(extractSubtitles, 1000);
                                            setTimeout(extractSubtitles, 2000);
                                            setTimeout(extractSubtitles, 3000);
                                        })();
                                    """.trimIndent()
                                    
                                    view?.evaluateJavascript(script, null)
                                }

                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): android.webkit.WebResourceResponse? {
                                    val url = request?.url?.toString() ?: ""
                                    
                                    // Intercept HLS manifest
                                    if (url.contains(".m3u8") && extractedUrl == null) {
                                        Log.d(TAG, "Intercepted M3U8: $url")
                                        extractedUrl = url
                                        scope.launch {
                                            state = PlayerState.Loading("Parsing stream manifest...")
                                            try {
                                                val parser = HlsManifestParser()
                                                val streamInfo = parser.parseManifest(url)
                                                Log.d(TAG, "Parsed manifest, found ${streamInfo.subtitles.size} subtitle tracks")
                                                
                                                // Merge with detected subtitles
                                                val mergedSubs = (streamInfo.subtitles + detectedSubtitles.values).distinctBy { it.url }
                                                Log.d(TAG, "Total subtitles after merge: ${mergedSubs.size}")
                                                
                                                state = PlayerState.Playing(streamInfo.copy(subtitles = mergedSubs))
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Failed to parse manifest, using raw URL", e)
                                                state = PlayerState.Playing(StreamInfo(url, detectedSubtitles.values.toList()))
                                            }
                                        }
                                    }

                                    // AGGRESSIVE SUBTITLE INTERCEPTION
                                    
                                    // 1. Intercept sub.wyzie.ru URLs
                                    if (url.contains("sub.wyzie.ru")) {
                                        Log.d(TAG, "Intercepted sub.wyzie.ru: $url")
                                        
                                        // Extract language from URL or use ID
                                        val lang = when {
                                            url.contains("lang=") -> url.substringAfter("lang=").substringBefore("&")
                                            url.contains("/id/") -> {
                                                // Use the ID as a unique identifier
                                                val id = url.substringAfter("/id/").substringBefore("?")
                                                "track_$id"
                                            }
                                            else -> "unknown_${detectedSubtitles.size}"
                                        }
                                        
                                        // Try to determine format
                                        val format = when {
                                            url.contains("format=srt") -> "SRT"
                                            url.contains("format=vtt") -> "VTT"
                                            url.contains(".srt") -> "SRT"
                                            url.contains(".vtt") -> "VTT"
                                            else -> "SUB"
                                        }
                                        
                                        val label = "$format - ${lang.uppercase()}"
                                        detectedSubtitles[lang] = SubtitleTrack(lang, label, url)
                                        Log.d(TAG, "Added subtitle track: $label")
                                    }
                                    
                                    // 2. Intercept .vtt files
                                    if (url.endsWith(".vtt") || url.contains(".vtt?")) {
                                        Log.d(TAG, "Intercepted VTT: $url")
                                        val lang = extractLanguageFromUrl(url)
                                        detectedSubtitles[lang] = SubtitleTrack(lang, "VTT - ${lang.uppercase()}", url)
                                    }
                                    
                                    // 3. Intercept .srt files
                                    if (url.endsWith(".srt") || url.contains(".srt?")) {
                                        Log.d(TAG, "Intercepted SRT: $url")
                                        val lang = extractLanguageFromUrl(url)
                                        detectedSubtitles[lang] = SubtitleTrack(lang, "SRT - ${lang.uppercase()}", url)
                                    }
                                    
                                    // 4. Intercept blob URLs (these are tricky, just log them)
                                    if (url.startsWith("blob:")) {
                                        Log.d(TAG, "Detected blob URL (cannot intercept): $url")
                                    }
                                    
                                    return super.shouldInterceptRequest(view, request)
                                }
                            }
                            
                            // Add JavaScript interface for subtitle reporting
                            addJavascriptInterface(object {
                                @android.webkit.JavascriptInterface
                                fun onSubtitleFound(language: String, label: String, url: String) {
                                    scope.launch {
                                        if (url.isNotEmpty()) {
                                            Log.d(TAG, "JS reported subtitle: $language - $label - $url")
                                            detectedSubtitles[language] = SubtitleTrack(language, label, url)
                                        }
                                    }
                                }
                            }, "AndroidSubtitles")
                            
                            loadUrl(playerUrl)
                        }
                    },
                    modifier = Modifier.size(0.dp)
                )
            }
            
            is PlayerState.Playing -> {
                ExoPlayerView(
                    streamInfo = currentState.streamInfo,
                    onBack = onBack,
                    onQualitiesDetected = { qualities, player ->
                        availableQualities = qualities
                        exoPlayerInstance = player
                        if (selectedQuality == null && qualities.isNotEmpty()) {
                            selectedQuality = qualities.first()
                        }
                    },
                    onShowQualityDialog = { showQualityDialog = true },
                    onShowSubtitleDialog = { showSubtitleDialog = true },
                    currentSubtitleCues = currentSubtitleCues,
                    detectedSubtitles = detectedSubtitles.values.toList()
                )
            }
            
            is PlayerState.Error -> {
                PlayerErrorState(currentState.message, retry)
            }
        }
        
        if (showQualityDialog && availableQualities.isNotEmpty()) {
            QualitySelectionDialog(
                qualities = availableQualities,
                selectedQuality = selectedQuality,
                onQualitySelected = { quality ->
                    selectedQuality = quality
                    exoPlayerInstance?.let { setVideoQuality(it, quality) }
                    showQualityDialog = false
                },
                onDismiss = { showQualityDialog = false }
            )
        }
        
        if (showSubtitleDialog) {
            SubtitleSelectionDialog(
                tracks = detectedSubtitles.values.toList(),
                activeTrack = activeSubtitleTrack,
                onSubtitleSelected = { track ->
                    activeSubtitleTrack = track
                    showSubtitleDialog = false
                },
                onDismiss = { showSubtitleDialog = false }
            )
        }
    }
}

// Helper function to extract language from URL
private fun extractLanguageFromUrl(url: String): String {
    // Try common patterns
    val patterns = listOf(
        Regex("[/_-]([a-z]{2})[._-]"),  // /en. or _en_ or -en-
        Regex("lang[=:]([a-z]{2})"),     // lang=en or lang:en
        Regex("/([a-z]{2})/"),           // /en/
        Regex("\\.([a-z]{2})\\.")          // .en.
    )
    
    for (pattern in patterns) {
        val match = pattern.find(url.lowercase())
        if (match != null) {
            return match.groupValues[1]
        }
    }
    
    // If no pattern matches, generate a unique ID
    return "track_${url.hashCode().toString(16)}"
}

enum class RotationMode {
    AUTO, PORTRAIT, LANDSCAPE;
    
    fun next(): RotationMode = when(this) {
        AUTO -> PORTRAIT
        PORTRAIT -> LANDSCAPE
        LANDSCAPE -> AUTO
    }
    
    fun toOrientation(): Int = when(this) {
        AUTO -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        PORTRAIT -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        LANDSCAPE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}

@Composable
private fun ExoPlayerView(
    streamInfo: StreamInfo,
    onBack: () -> Unit,
    onQualitiesDetected: (List<VideoQuality>, ExoPlayer) -> Unit,
    onShowQualityDialog: () -> Unit,
    onShowSubtitleDialog: () -> Unit,
    currentSubtitleCues: List<SubtitleCue>,
    detectedSubtitles: List<SubtitleTrack>
) {
    val context = LocalContext.current
    var currentPos by remember { mutableStateOf(0L) }
    var rotationMode by remember { mutableStateOf(RotationMode.AUTO) }
    val activity = context.findActivity()
    val window = activity?.window
    
    // Apply rotation mode whenever it changes
    LaunchedEffect(rotationMode) {
        activity?.requestedOrientation = rotationMode.toOrientation()
    }
    
    DisposableEffect(Unit) {
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window?.let { w ->
            val controller = WindowCompat.getInsetsController(w, w.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.let { w ->
                val controller = WindowCompat.getInsetsController(w, w.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            // Restore orientation
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    val exoPlayer = remember {
        val trackSelector = DefaultTrackSelector(context)
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                setMediaItem(ExoMediaItem.fromUri(streamInfo.streamUrl))
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            onQualitiesDetected(getAvailableQualities(this@apply), this@apply)
                            Log.d(TAG, "Player ready, ${detectedSubtitles.size} subtitles available")
                        }
                        if (playbackState == Player.STATE_ENDED) onBack()
                    }
                    
                    override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                        Log.e(TAG, "Playback error: ${error.message}", error)
                    }
                })
            }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPos = exoPlayer.currentPosition
            delay(100)
        }
    }
    
    // Pause playback when app is minimized or closed
    DisposableEffect(exoPlayer) {
        val lifecycleOwner = context as? androidx.lifecycle.LifecycleOwner
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                }
                else -> {}
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { 
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            exoPlayer.release()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                StyledPlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    useController = true
                    controllerShowTimeoutMs = 5000
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    post { showController() }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        
        SubtitleOverlay(cues = currentSubtitleCues, currentPositionMs = currentPos)
        
        // Gesture Controls Overlay
        GestureControlsOverlay(
            exoPlayer = exoPlayer,
            modifier = Modifier.fillMaxSize()
        )
        
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(
                onClick = onShowQualityDialog,
                containerColor = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp).size(48.dp)
            ) { Icon(Icons.Default.Settings, "Quality", tint = Color.White) }
            
            FloatingActionButton(
                onClick = onShowSubtitleDialog,
                containerColor = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp).size(48.dp)
            ) { 
                Icon(Icons.Default.Subtitles, "Subtitles", tint = Color.White)
            }
            
            // Rotation lock button
            FloatingActionButton(
                onClick = { rotationMode = rotationMode.next() },
                containerColor = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            ) { 
                Text(
                    text = when(rotationMode) {
                        RotationMode.AUTO -> "AUTO"
                        RotationMode.PORTRAIT -> "⬆"
                        RotationMode.LANDSCAPE -> "⬅"
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun GestureControlsOverlay(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier
) {
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }
    var speedFeedback by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Box(modifier = modifier) {
        // LEFT edge gesture area (30% width, top 85% height - excludes bottom controls)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight(0.85f)
                .align(Alignment.TopStart)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            val seekAmount = -10000L
                            val newPosition = (exoPlayer.currentPosition + seekAmount).coerceAtLeast(0)
                            exoPlayer.seekTo(newPosition)
                            
                            seekFeedback = SeekFeedback(true, "-10s")
                            scope.launch {
                                delay(800)
                                seekFeedback = null
                            }
                        },
                        onLongPress = {
                            exoPlayer.setPlaybackSpeed(2f)
                            speedFeedback = true
                        },
                        onPress = {
                            tryAwaitRelease()
                            if (speedFeedback) {
                                exoPlayer.setPlaybackSpeed(1f)
                                speedFeedback = false
                            }
                        }
                    )
                }
        )
        
        // RIGHT edge gesture area (30% width, top 85% height - excludes bottom controls)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight(0.85f)
                .align(Alignment.TopEnd)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            val seekAmount = 10000L
                            val newPosition = (exoPlayer.currentPosition + seekAmount).coerceAtLeast(0)
                            exoPlayer.seekTo(newPosition)
                            
                            seekFeedback = SeekFeedback(false, "+10s")
                            scope.launch {
                                delay(800)
                                seekFeedback = null
                            }
                        },
                        onLongPress = {
                            exoPlayer.setPlaybackSpeed(2f)
                            speedFeedback = true
                        },
                        onPress = {
                            tryAwaitRelease()
                            if (speedFeedback) {
                                exoPlayer.setPlaybackSpeed(1f)
                                speedFeedback = false
                            }
                        }
                    )
                }
        )
        
        // Seek feedback
        seekFeedback?.let { feedback ->
            Box(
                modifier = Modifier
                    .align(if (feedback.isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 48.dp)
            ) {
                Text(
                    text = feedback.text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                )
            }
        }
        
        // Speed feedback - smaller and more transparent
        AnimatedVisibility(
            visible = speedFeedback,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Text(
                text = "2x",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

private data class SeekFeedback(
    val isLeft: Boolean,
    val text: String
)

@Composable
private fun SubtitleOverlay(cues: List<SubtitleCue>, currentPositionMs: Long) {
    val activeCue = remember(cues, currentPositionMs) {
        cues.find { currentPositionMs in it.startTimeMs..it.endTimeMs }
    }
    activeCue?.let { cue ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp, start = 32.dp, end = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = cue.text,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SubtitleSelectionDialog(
    tracks: List<SubtitleTrack>,
    activeTrack: SubtitleTrack?,
    onSubtitleSelected: (SubtitleTrack?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).widthIn(max = 300.dp)) {
                Text("Subtitles (${tracks.size} available)", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                SubtitleOption("Off", activeTrack == null) { onSubtitleSelected(null) }
                
                if (tracks.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.3f))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(tracks) { track ->
                            SubtitleOption(track.label, activeTrack == track) {
                                onSubtitleSelected(track)
                            }
                        }
                    }
                } else {
                    Text(
                        "No subtitles detected yet. They may appear as the video loads.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitleOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 16.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
    }
}

@Composable
private fun QualitySelectionDialog(
    qualities: List<VideoQuality>,
    selectedQuality: VideoQuality?,
    onQualitySelected: (VideoQuality?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).widthIn(max = 300.dp)) {
                Text("Video Quality", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                QualityOption("Auto (Recommended)", selectedQuality == null) { onQualitySelected(null) }
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.3f))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(qualities) { quality ->
                        QualityOption(quality.label, selectedQuality == quality) {
                            onQualitySelected(quality)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 16.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
    }
}

fun setVideoQuality(player: ExoPlayer, quality: VideoQuality?) {
    val trackSelector = player.trackSelector as? DefaultTrackSelector ?: return
    if (quality == null) {
        trackSelector.parameters = trackSelector.buildUponParameters()
            .clearVideoSizeConstraints()
            .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            .build()
    } else {
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setMaxVideoSize(Int.MAX_VALUE, quality.height)
            .setMinVideoSize(quality.height, quality.height)
            .build()
    }
}

fun getAvailableQualities(player: ExoPlayer): List<VideoQuality> {
    val mappedTrackInfo = (player.trackSelector as? DefaultTrackSelector)?.currentMappedTrackInfo ?: return emptyList()
    val qualities = mutableListOf<VideoQuality>()
    for (i in 0 until mappedTrackInfo.rendererCount) {
        if (mappedTrackInfo.getRendererType(i) == com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO) {
            val groups = mappedTrackInfo.getTrackGroups(i)
            for (j in 0 until groups.length) {
                val group = groups[j]
                for (k in 0 until group.length) {
                    val h = group.getFormat(k).height
                    if (h > 0 && qualities.none { it.height == h }) {
                        val label = when {
                            h >= 2160 -> "4K"
                            h >= 1080 -> "1080p"
                            h >= 720 -> "720p"
                            h >= 480 -> "480p"
                            else -> "${h}p"
                        }
                        qualities.add(VideoQuality(h, label, k))
                    }
                }
            }
        }
    }
    return qualities.sortedByDescending { it.height }
}

@Composable
private fun LoadingState(media: VidoraMediaItem, message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        media.posterPath?.let {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w500$it",
                contentDescription = null,
                modifier = Modifier.width(200.dp).height(300.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, fontSize = 16.sp, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(media.displayTitle, fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancel", color = Color.White)
        }
    }
}

@Composable
private fun PlayerErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Playback Error",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Retry Stream Extraction")
            }
        }
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

private sealed class PlayerState {
    data class Loading(val message: String) : PlayerState()
    data class Playing(val streamInfo: StreamInfo) : PlayerState()
    data class Error(val message: String) : PlayerState()
}
