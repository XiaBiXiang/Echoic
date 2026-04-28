import SwiftUI

// MARK: - Language Manager

class LanguageManager: ObservableObject {
    static let shared = LanguageManager()

    @Published var language: String {
        didSet {
            UserDefaults.standard.set(language, forKey: "app_language")
        }
    }

    init() {
        self.language = UserDefaults.standard.string(forKey: "app_language") ?? "en"
    }

    var isChinese: Bool { language == "zh" }

    // MARK: - All UI Strings

    // Onboarding
    var personalize: String { isChinese ? "个性化设置" : "Personalize" }
    var personalizeDesc: String { isChinese ? "让 Echoic 更适合你" : "Make Echoic feel like yours" }
    var appearance: String { isChinese ? "外观" : "Appearance" }
    var languageLabel: String { isChinese ? "语言" : "Language" }
    var light: String { isChinese ? "浅色" : "Light" }
    var dark: String { isChinese ? "深色" : "Dark" }
    var system: String { isChinese ? "跟随系统" : "System" }

    var chooseMode: String { isChinese ? "选择使用方式" : "Choose Your Mode" }
    var chooseModeDesc: String { isChinese ? "随时可以在设置中更改" : "You can change this anytime in Settings" }
    var cloudAPI: String { isChinese ? "云端 API" : "Cloud API" }
    var cloudAPIDesc: String { isChinese ? "在线语音合成服务\n无需下载" : "Online TTS services\nNo download needed" }
    var localModel: String { isChinese ? "本地模型" : "Local Model" }
    var localModelDesc: String { isChinese ? "下载模型离线运行\n隐私优先" : "Download & run offline\nPrivacy first" }
    var bothMode: String { isChinese ? "两者都要" : "Both" }
    var bothModeDesc: String { isChinese ? "云端 + 本地\n最大灵活性" : "Cloud + Local\nMaximum flexibility" }

    var configureServices: String { isChinese ? "配置服务" : "Configure Services" }
    var cloudServices: String { isChinese ? "云端服务" : "Cloud Services" }
    var localModels: String { isChinese ? "本地模型" : "Local Models" }
    var testConnection: String { isChinese ? "测试连接" : "Test Connection" }
    var testing: String { isChinese ? "测试中..." : "Testing..." }
    var testAgain: String { isChinese ? "再次测试" : "Test Again" }
    var retry: String { isChinese ? "重试" : "Retry" }
    var connectionSuccess: String { isChinese ? "连接成功！一切就绪。" : "Connection successful! You're all set." }
    var noProviderConfigured: String { isChinese ? "请至少配置一个服务的 API 密钥" : "Please configure at least one provider's API key" }
    var freeNoKey: String { isChinese ? "免费服务，无需 API 密钥" : "Free service, no API key required" }
    var ready: String { isChinese ? "就绪" : "Ready" }

    var apiKey: String { isChinese ? "API 密钥" : "API Key" }
    var endpoint: String { isChinese ? "接口地址" : "Endpoint" }
    var required: String { isChinese ? "必填" : "Required" }
    var entered: String { isChinese ? "已填写" : "Entered" }
    var getKey: String { isChinese ? "获取密钥" : "Get Key" }
    var localComingSoon: String { isChinese ? "本地模型支持即将推出" : "Local model support coming soon" }
    var localComingSoonDesc: String { isChinese ? "Fish Audio、Coqui TTS 等" : "Fish Audio, Coqui TTS, and more" }
    var changeAnytime: String { isChinese ? "随时在设置中更改 (Cmd+,)" : "Change anytime in Settings (Cmd+,)" }

    var back: String { isChinese ? "返回" : "Back" }
    var next: String { isChinese ? "下一步" : "Next" }
    var getStarted: String { isChinese ? "开始使用" : "Get Started" }

    // Main UI
    var voiceSettings: String { isChinese ? "语音设置" : "Voice Settings" }
    var model: String { isChinese ? "模型" : "Model" }
    var voice: String { isChinese ? "声音" : "Voice" }
    var format: String { isChinese ? "格式" : "Format" }
    var history: String { isChinese ? "历史记录" : "History" }
    var comingSoon: String { isChinese ? "即将推出" : "Coming soon" }

    var textToSpeech: String { isChinese ? "文本转语音" : "Text to Speech" }
    var textPlaceholder: String { isChinese ? "在这里输入或粘贴英文文本..." : "Enter or paste your English text here..." }
    var synthesize: String { isChinese ? "合成语音" : "Synthesize" }
    var synthesizing: String { isChinese ? "合成中..." : "Synthesizing..." }
    var characters: String { isChinese ? "个字符" : "characters" }
    var save: String { isChinese ? "保存" : "Save" }
    var cancel: String { isChinese ? "取消" : "Cancel" }
    var savedTo: String { isChinese ? "已保存到" : "Saved to" }
    var noAudioYet: String { isChinese ? "还没有音频" : "No Audio Yet" }
    var noAudioDesc: String { isChinese ? "输入文本并点击合成来生成语音" : "Enter text and click Synthesize to generate speech" }
    var dismiss: String { isChinese ? "关闭" : "Dismiss" }
}
