# Pocket Nexus

**LEGAL DISCLAIMER:** This is an educational project. I don't own or host any of the content accessible through this app. Everything is streamed from third-party sources that I have no control over. Use this responsibly and make sure you're following your local laws and respecting copyright. I'm not responsible for how you use this.

## What is this?

A streaming app I built while learning Android development. Started as a simple video player and ended up with a bunch of cool features like gesture controls, automatic subtitle discovery, and a custom player.

## Features

**Player Stuff:**
- Double-tap left/right sides to seek backward/forward 10 seconds
- Long-press anywhere to play at 2x speed
- Rotation lock button (Auto/Portrait/Landscape modes)
- Quality selection
- Full-screen immersive mode

**Subtitles:**
- Automatic subtitle discovery from player configs
- Supports SRT and VTT formats
- Multi-language support (currently works best with English)
- Custom subtitle parser and renderer

**UI:**
- Black and purple theme
- Material 3 design
- Smooth animations
- Dark mode optimized

**Other:**
- Gesture controls only on edges so player controls still work
- Pauses playback when you minimize the app
- Error handling with retry option

## Known Issues

- Subtitle sync can be off on some streams (working on manual offset controls)
- Subtitle discovery takes a few seconds sometimes
- Some older devices might struggle with 2x playback speed

## Installation

**Option 1: Download APK**
1. Go to Releases
2. Download the latest APK
3. Enable "Unknown Sources" in Android settings
4. Install it

**Option 2: Build from source**
```bash
git clone https://github.com/Puneeth-R-140/pocket-nexus-streaming.git
cd pocket-nexus-streaming
./gradlew assembleDebug
```

APK will be in `app/build/outputs/apk/debug/`

## Tech Stack

- Kotlin
- Jetpack Compose for UI
- ExoPlayer for video playback
- Hilt for dependency injection
- Retrofit + OkHttp for networking
- Room for local database
- Coil for image loading

## Project Structure

```
app/src/main/java/com/vidora/app/
├── data/       # repositories, API, database
├── ui/         # screens, components, viewmodels
├── player/     # custom player and subtitle engine
└── di/         # dependency injection
```

## Contributing

Found a bug or want to add something? Open an issue or PR. Just make sure to test your changes.

## License

MIT License - see LICENSE file for details

## Disclaimer

This is purely educational. Don't use it for anything illegal. Respect copyright laws and content creators.

---

Built while learning Android dev. Still improving it.
