# Pocket Nexus - Streaming App

> **LEGAL DISCLAIMER - Educational Purpose Only**
> 
> This application is developed for educational and personal use only. The developer does not own, host, or distribute any content accessible through this application. All media content is streamed from third-party sources over which the developer has no control.
> 
> Users are solely responsible for ensuring their usage complies with local laws and regulations, respecting copyright and intellectual property rights, and adhering to the terms of service of content providers.
> 
> The developer does NOT endorse piracy or copyright infringement, provides this software "as is" without warranties, and is NOT liable for any misuse of this application.

---

## About

Pocket Nexus is a modern Android streaming application built with Jetpack Compose and Material 3. It features a sleek black and purple theme, advanced player controls, and multi-language subtitle support.

## Features

### Modern UI/UX
- Rich black and purple theme with premium design
- Material 3 components following latest Android guidelines
- Smooth animations for polished user experience
- Dark mode optimized for comfortable viewing

### Advanced Player
- Custom subtitle engine with native SRT/VTT support and automatic discovery
- Gesture controls: double-tap to seek, long-press for 2x speed
- Rotation lock with manual orientation control (Auto/Portrait/Landscape)
- Quality selection to choose your preferred video quality
- Immersive mode with full-screen playback and hidden system bars

### Smart Features
- Config-based subtitle discovery that automatically finds available subtitles
- Multi-language support ready for international content
- Lifecycle management that pauses playback when app is minimized
- Error handling with robust retry mechanism and clear error messages

## Installation

### Download APK
1. Go to the Releases page
2. Download the latest APK file
3. Enable "Install from Unknown Sources" in your Android settings
4. Install the APK on your device

### Build from Source

Prerequisites:
- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 34

Steps:
```bash
git clone https://github.com/Puneeth-R-140/pocket-nexus-streaming.git
cd pocket-nexus-streaming
./gradlew assembleDebug
```

The APK will be located at: `app/build/outputs/apk/debug/app-debug.apk`

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVVM with Repository pattern
- **Dependency Injection:** Hilt
- **Networking:** Retrofit and OkHttp
- **Database:** Room
- **Video Player:** ExoPlayer
- **Image Loading:** Coil
- **Navigation:** Jetpack Navigation Compose

## Project Structure

```
app/
├── src/main/
│   ├── java/com/vidora/app/
│   │   ├── data/          # Data layer (repositories, models)
│   │   ├── ui/            # UI layer (screens, components, viewmodels)
│   │   ├── player/        # Custom player implementation
│   │   └── di/            # Dependency injection modules
│   └── res/               # Resources (layouts, drawables, etc.)
```

## Contributing

Contributions are welcome. Please read CONTRIBUTING.md for guidelines on how to contribute to this project.

## License

This project is licensed under the MIT License. See the LICENSE file for details.

## Acknowledgments

- Built with Jetpack Compose
- Video playback powered by ExoPlayer
- Icons from Material Icons

## Disclaimer

This is an educational project. The developer is not responsible for any content accessed through this application or any misuse of the software. Users must comply with all applicable laws and respect intellectual property rights.

---

Made for learning Android development
