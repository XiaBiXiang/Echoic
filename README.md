# Echoic

A native macOS text-to-speech app for English learners. Write diary entries in Chinese, translate them into natural English, and generate high-quality speech audio for pronunciation practice.

Built with **SwiftUI** for macOS 14 (Sonoma)+, following Apple Human Interface Guidelines.

## Features

- **Multi-provider TTS** — OpenAI, MiMo (Xiaomi), ElevenLabs, Google Cloud, Azure, Fish Audio, and Edge TTS (free)
- **Onboarding wizard** — First-launch setup for appearance, language (EN/ZH), and API configuration
- **Real-time appearance switching** — Light, Dark, and System themes
- **Bilingual UI** — Full English and Chinese support with smooth transitions
- **Connection testing** — Verify API keys before you start
- **Audio playback** — Built-in player with progress bar and save-to-disk
- **Settings panel** — Slide-in panel with Cmd+, (follows app theme)

## Supported Providers

| Provider | Auth | Notes |
|----------|------|-------|
| OpenAI | API Key | TTS-1 / TTS-1 HD |
| MiMo (Xiaomi) | API Key | MiMo-V2-TTS, free for a limited time |
| ElevenLabs | API Key | Multilingual v2 / Turbo v2.5 |
| Google Cloud | API Key | WaveNet / Neural2 voices |
| Azure TTS | Subscription Key | Neural voices, region-aware |
| Fish Audio | API Key | OpenAI-compatible endpoint |
| Edge TTS | None | Free, no API key needed |

## Requirements

- macOS 14.0 (Sonoma) or later
- Xcode 16.0+
- [xcodegen](https://github.com/yonaskolb/XcodeGen) (for project generation)

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/XiaBiXiang/Echoic.git
   cd Echoic
   ```

2. **Generate the Xcode project**
   ```bash
   xcodegen generate
   ```

3. **Open and run**
   ```bash
   open Echoic.xcodeproj
   ```
   Press `Cmd+R` in Xcode to build and run.

## Project Structure

```
Echoic/
├── EchoicApp.swift              # @main entry point
├── Models/
│   └── TTSProvider.swift         # Providers, models, voices, formats
├── ViewModels/
│   └── TTSViewModel.swift        # Synthesis & playback state
├── Views/
│   ├── OnboardingView.swift      # 3-step onboarding wizard
│   ├── ContentView.swift         # Main UI + settings overlay
│   └── SettingsView.swift        # Provider & defaults config
├── Services/
│   ├── TTSService.swift          # Multi-provider TTS client
│   └── AudioPlaybackService.swift # AVAudioPlayer wrapper
├── Utilities/
│   └── Localization.swift        # EN/ZH string manager
├── Echoic.entitlements
└── Info
```

## Architecture

MVVM with strict separation:

- **Views** — Pure SwiftUI layout, zero business logic
- **ViewModels** — `@MainActor` `ObservableObject`, holds state and calls services
- **Services** — Network (URLSession + async/await), file I/O, audio playback
- **Models** — Data structures (enums, structs)

## License

MIT
