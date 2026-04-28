import Foundation

// MARK: - TTS Provider

enum TTSProvider: String, CaseIterable, Identifiable, Codable {
    case openai = "openai"
    case mimo = "mimo"
    case elevenlabs = "elevenlabs"
    case google = "google"
    case azure = "azure"
    case fishAudio = "fish_audio"
    case edgeTTS = "edge_tts"
    case glm = "glm"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .openai: "OpenAI"
        case .mimo: "MiMo"
        case .elevenlabs: "ElevenLabs"
        case .google: "Google Cloud"
        case .azure: "Azure TTS"
        case .fishAudio: "Fish Audio"
        case .edgeTTS: "Edge TTS"
        case .glm: "GLM (Zhipu AI)"
        }
    }

    var subtitle: String {
        switch self {
        case .openai: "TTS-1 / TTS-1 HD"
        case .mimo: "MiMo-V2-TTS · Free"
        case .elevenlabs: "Multilingual v2"
        case .google: "WaveNet / Neural2"
        case .azure: "Neural TTS"
        case .fishAudio: "OpenAI Compatible"
        case .edgeTTS: "Free · No Key"
        case .glm: "GLM-TTS"
        }
    }

    var systemImage: String {
        switch self {
        case .openai: "brain"
        case .mimo: "mic"
        case .elevenlabs: "waveform"
        case .google: "cloud"
        case .azure: "server.rack"
        case .fishAudio: "fish"
        case .edgeTTS: "globe"
        case .glm: "sparkles"
        }
    }

    // UserDefaults keys
    var apiKeyStorageKey: String {
        "\(rawValue)_api_key"
    }

    var baseURLStorageKey: String {
        "\(rawValue)_base_url"
    }

    var defaultBaseURL: String {
        switch self {
        case .openai: "https://api.openai.com"
        case .mimo: "https://api.xiaomimimo.com"
        case .elevenlabs: "https://api.elevenlabs.io"
        case .google: "https://texttospeech.googleapis.com"
        case .azure: "https://eastus.tts.speech.microsoft.com"
        case .fishAudio: "https://api.fish.audio"
        case .edgeTTS: ""
        case .glm: "https://open.bigmodel.cn"
        }
    }

    var apiKeyPlaceholder: String {
        switch self {
        case .openai: "sk-..."
        case .mimo: "mimo-..."
        case .elevenlabs: "xi-..."
        case .google: "AIza..."
        case .azure: "subscription-key"
        case .fishAudio: "fish-..."
        case .edgeTTS: ""
        case .glm: ""
        }
    }

    var apiKeyDescription: String {
        switch self {
        case .openai: "Used for TTS-1 / TTS-1 HD. Change Base URL if using a proxy."
        case .mimo: "Xiaomi MiMo TTS API key. Free for a limited time."
        case .elevenlabs: "Used for multilingual v2 / turbo v2.5 speech synthesis."
        case .google: "Google Cloud API Key for Text-to-Speech service."
        case .azure: "Azure Cognitive Services subscription key. Change Base URL to match your region."
        case .fishAudio: "Fish Audio API key for TTS. OpenAI-compatible endpoint."
        case .edgeTTS: "Free TTS service from Microsoft Edge. No API key needed."
        case .glm: "Zhipu AI API Key for GLM TTS. OpenAI-compatible endpoint."
        }
    }

    var helpURL: String? {
        switch self {
        case .openai: "https://platform.openai.com/api-keys"
        case .mimo: "https://platform.xiaomimimo.com"
        case .elevenlabs: "https://elevenlabs.io/app/settings/api-keys"
        case .google: "https://console.cloud.google.com/apis/credentials"
        case .azure: "https://portal.azure.com/#view/Microsoft_Azure_ProjectOxford/CognitiveServicesHub/~/SpeechServices"
        case .fishAudio: "https://fish.audio/settings/api"
        case .edgeTTS: nil
        case .glm: "https://open.bigmodel.cn/usercenter/apikeys"
        }
    }

    var requiresAPIKey: Bool {
        switch self {
        case .edgeTTS: false
        default: true
        }
    }

    var isConfigured: Bool {
        if !requiresAPIKey { return true }
        guard let key = UserDefaults.standard.string(forKey: apiKeyStorageKey) else { return false }
        return !key.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var availableModels: [TTSModel] {
        switch self {
        case .openai: [.openai_tts1, .openai_tts1hd]
        case .mimo: [.mimo_v2_tts]
        case .elevenlabs: [.elevenlabs_multilingual, .elevenlabs_turbo]
        case .google: [.google_standard, .google_wavenet, .google_neural2]
        case .azure: [.azure_tts]
        case .fishAudio: [.fish_tts1]
        case .edgeTTS: [.edge_tts]
        case .glm: [.glm_tts]
        }
    }

    var availableVoices: [Voice] {
        switch self {
        case .openai: Voice.openaiVoices
        case .mimo: Voice.mimoVoices
        case .elevenlabs: Voice.elevenlabsVoices
        case .google: Voice.googleVoices
        case .azure: Voice.azureVoices
        case .fishAudio: Voice.fishAudioVoices
        case .edgeTTS: Voice.edgeTTSVoices
        case .glm: Voice.glmVoices
        }
    }
}

// MARK: - TTS Model

enum TTSModel: String, CaseIterable, Identifiable, Codable {
    case openai_tts1 = "tts-1"
    case openai_tts1hd = "tts-1-hd"
    case mimo_v2_tts = "mimo-v2-tts"
    case elevenlabs_multilingual = "eleven_multilingual_v2"
    case elevenlabs_turbo = "eleven_turbo_v2_5"
    case google_standard = "standard"
    case google_wavenet = "wavenet"
    case google_neural2 = "neural2"
    case azure_tts = "azure_tts"
    case fish_tts1 = "fish_tts1"
    case edge_tts = "edge_tts"
    case glm_tts = "glm-tts"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .openai_tts1: "TTS-1 (Fast)"
        case .openai_tts1hd: "TTS-1 HD (Quality)"
        case .mimo_v2_tts: "MiMo V2 TTS"
        case .elevenlabs_multilingual: "Multilingual v2"
        case .elevenlabs_turbo: "Turbo v2.5 (Fast)"
        case .google_standard: "Standard"
        case .google_wavenet: "WaveNet"
        case .google_neural2: "Neural2 (Quality)"
        case .azure_tts: "Neural TTS"
        case .fish_tts1: "Fish TTS v1"
        case .edge_tts: "Edge TTS"
        case .glm_tts: "GLM-TTS"
        }
    }

    var provider: TTSProvider {
        switch self {
        case .openai_tts1, .openai_tts1hd: .openai
        case .mimo_v2_tts: .mimo
        case .elevenlabs_multilingual, .elevenlabs_turbo: .elevenlabs
        case .google_standard, .google_wavenet, .google_neural2: .google
        case .azure_tts: .azure
        case .fish_tts1: .fishAudio
        case .edge_tts: .edgeTTS
        case .glm_tts: .glm
        }
    }
}

// MARK: - Voice

struct Voice: Identifiable, Codable, Hashable {
    let id: String
    let displayName: String
    let provider: TTSProvider

    static let openaiVoices: [Voice] = [
        Voice(id: "alloy", displayName: "Alloy", provider: .openai),
        Voice(id: "echo", displayName: "Echo", provider: .openai),
        Voice(id: "fable", displayName: "Fable", provider: .openai),
        Voice(id: "onyx", displayName: "Onyx", provider: .openai),
        Voice(id: "nova", displayName: "Nova", provider: .openai),
        Voice(id: "shimmer", displayName: "Shimmer", provider: .openai),
    ]

    static let mimoVoices: [Voice] = [
        Voice(id: "mimo_default", displayName: "MiMo Default", provider: .mimo),
        Voice(id: "default_zh", displayName: "Chinese Female", provider: .mimo),
        Voice(id: "default_en", displayName: "English Female", provider: .mimo),
    ]

    static let elevenlabsVoices: [Voice] = [
        Voice(id: "21m00Tcm4TlvDq8ikWAM", displayName: "Rachel", provider: .elevenlabs),
        Voice(id: "AZnzlk1XvdvUeBnXmlld", displayName: "Drew", provider: .elevenlabs),
        Voice(id: "EXAVITQu4vr4xnSDxMaL", displayName: "Bella", provider: .elevenlabs),
        Voice(id: "ErXwobaYiN019PkySvjV", displayName: "Antoni", provider: .elevenlabs),
        Voice(id: "MF3mGyEYCl7XYWbV9V6O", displayName: "Elli", provider: .elevenlabs),
        Voice(id: "TxGEqnHWrfWFTfGW9XjX", displayName: "Josh", provider: .elevenlabs),
        Voice(id: "VR6AewLTigWG4xSOukaG", displayName: "Arnold", provider: .elevenlabs),
        Voice(id: "pNInz6obpgDQGcFmaJgB", displayName: "Adam", provider: .elevenlabs),
        Voice(id: "yoZ06aMxZJJ28mfd3POQ", displayName: "Sam", provider: .elevenlabs),
    ]

    static let googleVoices: [Voice] = [
        Voice(id: "en-US-Neural2-A", displayName: "Neural2 A (Female)", provider: .google),
        Voice(id: "en-US-Neural2-B", displayName: "Neural2 B (Male)", provider: .google),
        Voice(id: "en-US-Neural2-C", displayName: "Neural2 C (Female)", provider: .google),
        Voice(id: "en-US-Neural2-D", displayName: "Neural2 D (Male)", provider: .google),
        Voice(id: "en-US-Wavenet-A", displayName: "WaveNet A (Female)", provider: .google),
        Voice(id: "en-US-Wavenet-B", displayName: "WaveNet B (Male)", provider: .google),
        Voice(id: "en-US-Standard-A", displayName: "Standard A (Female)", provider: .google),
        Voice(id: "en-US-Standard-B", displayName: "Standard B (Male)", provider: .google),
    ]

    static let azureVoices: [Voice] = [
        Voice(id: "en-US-JennyNeural", displayName: "Jenny (Female)", provider: .azure),
        Voice(id: "en-US-GuyNeural", displayName: "Guy (Male)", provider: .azure),
        Voice(id: "en-US-AriaNeural", displayName: "Aria (Female)", provider: .azure),
        Voice(id: "en-US-DavisNeural", displayName: "Davis (Male)", provider: .azure),
        Voice(id: "en-US-AndrewNeural", displayName: "Andrew (Male)", provider: .azure),
        Voice(id: "en-US-EmmaNeural", displayName: "Emma (Female)", provider: .azure),
    ]

    static let fishAudioVoices: [Voice] = [
        Voice(id: "fish-audio-1", displayName: "Default", provider: .fishAudio),
    ]

    static let edgeTTSVoices: [Voice] = [
        Voice(id: "en-US-AriaNeural", displayName: "Aria (Female)", provider: .edgeTTS),
        Voice(id: "en-US-JennyNeural", displayName: "Jenny (Female)", provider: .edgeTTS),
        Voice(id: "en-US-GuyNeural", displayName: "Guy (Male)", provider: .edgeTTS),
        Voice(id: "en-US-DavisNeural", displayName: "Davis (Male)", provider: .edgeTTS),
        Voice(id: "en-US-EmmaNeural", displayName: "Emma (Female)", provider: .edgeTTS),
    ]

    static let glmVoices: [Voice] = [
        Voice(id: "male-01", displayName: "Male 01", provider: .glm),
        Voice(id: "female-01", displayName: "Female 01", provider: .glm),
        Voice(id: "male-02", displayName: "Male 02", provider: .glm),
        Voice(id: "female-02", displayName: "Female 02", provider: .glm),
    ]
}

// MARK: - Audio Output Format

enum AudioFormat: String, CaseIterable, Identifiable {
    case mp3
    case opus
    case aac
    case flac
    case wav

    var id: String { rawValue }

    var fileExtension: String {
        switch self {
        case .mp3: "mp3"
        case .opus: "opus"
        case .aac: "aac"
        case .flac: "flac"
        case .wav: "wav"
        }
    }

    var utType: String {
        switch self {
        case .mp3: "public.mp3"
        case .opus: "org.xiph.opus-audio"
        case .aac: "public.aac-audio"
        case .flac: "org.xiph.flac"
        case .wav: "com.microsoft.waveform-audio"
        }
    }
}
