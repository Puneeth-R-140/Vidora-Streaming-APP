# Vidora Streaming APP

Look, we all know mobile browsers are a nightmare for streaming. That's why I built Vidora Streaming. It is a premium, native Android experience for the Vidora ecosystem. No fluff, no clunky tabs—just you and your content.

![Vidora Icon](app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

## What's the Deal?
This app isn't just a basic WebView wrapper. I've spent time fine-tuning the navigation logic to make sure the "Watch Now" button actually works. It deep-links directly into the Vidora player, bypassing all the landing page nonsense.

### The Anime Situation
Yes, there's an Anime section. Yes, the library is huge. But here is the reality: Anime servers are notoriously finicky. While most movies and mainstream TV shows stream like butter, some anime titles might struggle to find a stable provider. If a source doesn't load, it is usually a server-side issue, not the app. I am working on better provider fallback for the future.

### Potential Glitches and the Reality
No software is perfect, and if you are looking for a bug-free utopia, you are in the wrong place. Here is what you might encounter:
- **UI Flickering**: Occasionally, while entering or exiting the immersive full-screen mode, you might see a brief flicker of the system bars. This is a known behavior with how Android handles window insets and is harmless.
- **WebView Hangs**: If a provider is serving garbage code or heavy ads, the WebView might hang for a second. If it does, just back out and try a different source.
- **Navigation Loops**: While I have implemented a navigation lock, some aggressive redirects from third-party players might still try to hijack the view. Just use the physical back button; the app is programmed to prioritize your native experience.

### Regional Restrictions and The Law
- **Regional Restrictions**: If your ISP or country has a beef with Vidora's providers, you might see "Source Not Found" or infinite loading. Use a VPN. Don't complain to me if your government blocks the good stuff.
- **Provider Stability**: Sometimes providers go down. It happens. If it does, try again in an hour.
- **Device Compatibility**: This app is built for Android 7.0 (Nougat) and up (API 24+). If you are running a potato from 2015, don't expect 4K 120fps. It is optimized for modern mid-to-high-end devices.

## Why this app slaps
- **Material 3 Excellence**: Built with Jetpack Compose. It looks and feels like a professional subscription app, but it is free.
- **Smart Navigation**: TV shows automatically default to Season 1, Episode 1 if you are lazy.
- **Ad-Light Experience**: Integrated basic ad-blocking into the player view to keep the experience as clean as possible.
- **Immersive Playback**: Status and gesture bars are automatically hidden during video playback for a true full-screen experience.
- **PiP Mode**: Minimizable player so you can pretend to be productive while watching your shows.

## Hardware Support
- **Architecture**: Supports arm64-v8a, armeabi-v7a, x86, and x86_64.
- **Minimum OS**: Android 7.0 (API 24)
- **Target OS**: Android 14 (API 34)

## How to use it
1. Grab the latest APK from the Releases section.
2. Sideload it (enable "Install from Unknown Sources").
3. Search for a show. 
4. Hit "Watch Now".
5. Profit.

---
*Built by a human, for humans. Stay Chad.*
