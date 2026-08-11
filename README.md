# 🎵 Music Player for Android

**A lightweight, ad-free, open-source music player built specifically for Android.**

No ads. No tracking. No fluff. Just your music, organized by songs and folders, with all the playback controls you need while driving or using Bluetooth devices.

---

## ✨ Features

### 🎼 Library Management
- **All Songs**: Browse your entire music collection alphabetically
- **Folder View**: Organized by folder structure on your device
- **Search**: Fast in-app search across titles and artists
- **Album Art**: Displays embedded album art with a placeholder for tracks without it

### 🎧 Playback
- **Play/Pause**, **Next**, **Previous**
- **Seek** to any position in a track
- **Repeat Modes**: None / Repeat All / Repeat One
- **Shuffle**: Randomize your queue
- **Bluetooth**: Automatic pause when disconnected; supports Bluetooth media controls
- **Lock Screen Controls**: Full media controls via notification and Android MediaSession

### 🚗 Perfect for Driving
- Large, tap-friendly controls (especially in Now Playing screen)
- Bluetooth auto-pause when device disconnects
- Persistent foreground notification with transport controls
- Hardware media button support (headset/remote)

---

## 🏗️ Architecture

- **Kotlin** – 100% Kotlin codebase
- **MVVM** – `MusicViewModel` drives UI state with LiveData
- **Foreground Service** – [`MusicService`](app/src/main/java/com/musicplayer/app/service/MusicService.kt) manages MediaPlayer, audio focus, and notifications
- **MediaStore** – Scans device audio files via [`SongRepository`](app/src/main/java/com/musicplayer/app/repository/SongRepository.kt)
- **Material 3 UI** – Bottom navigation, RecyclerView, Toolbar, Palette-driven Now Playing colors

---

## 📦 Build & Install

### Prerequisites

1. **Android Studio** (latest stable: Electric Eel or newer)  
   [Download here](https://developer.android.com/studio)

2. **Java JDK 17+** (required for Gradle 8.4)

3. **Android SDK**:
   - API 34 (`compileSdk`)
   - API 21+ (`minSdk`) – Compatible with Android 5.0 Lollipop and above

### Steps

#### 1. Clone or Download

```bash
git clone https://github.com/yourname/music-player-android.git
cd music-player-android
```

*(If you downloaded as a ZIP, extract and open the folder.)*

#### 2. Open in Android Studio

- Launch Android Studio
- **File** → **Open** → Select this project folder
- Wait for Gradle sync to complete

#### 3. Build the APK

**Via Android Studio:**

1. **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Once done, click **locate** to find the APK:  
   `app/build/outputs/apk/debug/app-debug.apk`

**Via Command Line (gradlew):**

On **Windows** (PowerShell):
```powershell
.\gradlew assembleDebug
```

On **macOS/Linux**:
```bash
./gradlew assembleDebug
```

The APK will be at:  
`app/build/outputs/apk/debug/app-debug.apk`

#### 4. Install on Your Device

**Option A: USB Cable (Developer Mode)**

1. Enable **Developer Options** and **USB Debugging** on your phone:
   - Settings → About Phone → Tap "Build Number" 7 times
   - Developer Options → Enable USB Debugging
2. Connect phone via USB
3. In Android Studio: **Run** → **Run 'app'** (or press Shift+F10)

**Option B: Direct APK Transfer**

1. Copy `app-debug.apk` to your phone (USB, email, cloud storage, etc.)
2. On your phone: Open the APK file from Files app
3. Tap **Install** (you may need to allow "Install from Unknown Sources")

---

## 🔐 Permissions

This app requests the following permissions:

| Permission | Purpose |
|------------|---------|
| `READ_EXTERNAL_STORAGE` (Android ≤ 12) | Scan audio files |
| `READ_MEDIA_AUDIO` (Android 13+) | Granular audio-only access |
| `FOREGROUND_SERVICE` | Keep playback alive in background |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Foreground service type |
| `POST_NOTIFICATIONS` (Android 13+) | Display playback notification |
| `BLUETOOTH` / `BLUETOOTH_CONNECT` | Respond to Bluetooth events |
| `WAKE_LOCK` | Keep audio playing when screen is off |

All permissions are **requested at runtime** with user consent.

---

## 🗂️ Project Structure

```
app/src/main/
├── java/com/musicplayer/app/
│   ├── model/               # Data classes: Song, Folder, RepeatMode
│   ├── repository/          # SongRepository (MediaStore queries)
│   ├── service/             # MusicService, MediaButtonReceiver
│   ├── ui/
│   │   ├── main/            # MainActivity, bottom nav host
│   │   ├── songs/           # SongsFragment, SongAdapter
│   │   ├── folders/         # FoldersFragment, FolderAdapter, FolderDetailFragment
│   │   └── nowplaying/      # NowPlayingActivity
│   ├── viewmodel/           # MusicViewModel (MVVM state holder)
│   └── utils/               # ServiceUtils
├── res/
│   ├── layout/              # All XML layouts
│   ├── drawable/            # Vector icons (play, pause, shuffle, etc.)
│   ├── navigation/          # Nav graph (bottom nav)
│   ├── menu/                # Bottom navigation menu
│   └── values/              # strings, colors, themes
└── AndroidManifest.xml
```

---

## 🎨 UI Screenshots

*(Add screenshots here after building the app — screenshots of Songs list, Folders, Now Playing, Mini Player)*

---

## 🚀 Usage

1. **Grant Permissions**: On first launch, allow storage/audio access
2. **Browse**: Tap **All Songs** or **Folders** tabs at the bottom
3. **Play a Song**: Tap any track → music starts immediately
4. **Mini Player**: Appears at bottom of main screen → tap to open full Now Playing screen
5. **Controls**:
   - ⏯️ Play/Pause
   - ⏭️ Next / ⏮️ Previous
   - 🔀 Shuffle
   - 🔁 Repeat (cycles: None → All → One)
6. **Bluetooth**: Connect your Bluetooth device before or during playback — controls work seamlessly

---

## 🔧 Troubleshooting

### No songs appear
- Grant storage permission: Settings → Apps → Music Player → Permissions
- Ensure audio files are in device storage (internal/SD card)
- The app filters out tracks shorter than 10 seconds (to exclude notification sounds)

### Bluetooth controls don't work
- Ensure `BLUETOOTH_CONNECT` permission is granted (Android 12+)
- Some Bluetooth devices require pairing before media controls activate

### App crashes on startup
- Check that your device is **Android 5.0 (API 21) or higher**
- Clear app data: Settings → Apps → Music Player → Storage → Clear Data

### Build errors
- Sync project with Gradle: File → Sync Project with Gradle Files
- Ensure Android SDK 34 is installed: Tools → SDK Manager → SDK Platforms → Android 14 (API 34)

---

## 🛠️ Development

### Dependencies (from [`app/build.gradle`](app/build.gradle))

| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.appcompat` | 1.6.1 | AppCompat components |
| `material` | 1.11.0 | Material Design 3 UI |
| `lifecycle-viewmodel-ktx` | 2.7.0 | ViewModel + LiveData |
| `navigation-fragment-ktx` | 2.7.7 | Navigation component |
| `recyclerview` | 1.3.2 | List rendering |
| `glide` | 4.16.0 | Image loading (album art) |
| `palette-ktx` | 1.0.0 | Extract colors from images |

### Key Classes

- **[`MusicService.kt`](app/src/main/java/com/musicplayer/app/service/MusicService.kt)**: Core playback engine (MediaPlayer, AudioFocus, Notification)
- **[`MusicViewModel.kt`](app/src/main/java/com/musicplayer/app/viewmodel/MusicViewModel.kt)**: MVVM state management, service binding
- **[`SongRepository.kt`](app/src/main/java/com/musicplayer/app/repository/SongRepository.kt)**: MediaStore queries for scanning audio files
- **[`MainActivity.kt`](app/src/main/java/com/musicplayer/app/ui/main/MainActivity.kt)**: Main UI, bottom navigation, mini player
- **[`NowPlayingActivity.kt`](app/src/main/java/com/musicplayer/app/ui/nowplaying/NowPlayingActivity.kt)**: Full-screen playback controls with dynamic theming

---

## 📝 License

This project is **open source** under the MIT License.  
Feel free to use, modify, and distribute as needed.

```
MIT License

Copyright (c) 2026 [Your Name]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🤝 Contributing

Contributions are welcome! Please fork the repo and submit a pull request.

### Areas for improvement:
- [ ] Playlists support
- [ ] Equalizer integration
- [ ] Sleep timer
- [ ] Widgets (home screen)
- [ ] Dark/Light theme toggle
- [ ] Custom themes
- [ ] Last.fm scrobbling

---

## 📧 Contact

For questions, bug reports, or feature requests:  
[Open an issue](https://github.com/yourname/music-player-android/issues)

---

**Enjoy your ad-free music experience! 🎶**
