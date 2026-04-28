import SwiftUI

struct SettingsView: View {
    var showCloseButton: Bool = false
    var onClose: (() -> Void)? = nil

    var body: some View {
        Form {
            // Cloud providers — most important, topmost
            ForEach(TTSProvider.allCases) { provider in
                if provider.requiresAPIKey {
                    ProviderConfigSection(provider: provider)
                }
            }

            // Free providers
            Section {
                ForEach(TTSProvider.allCases) { provider in
                    if !provider.requiresAPIKey {
                        HStack {
                            Label(provider.displayName, systemImage: provider.systemImage)
                            Spacer()
                            Text("Free — no key needed")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            } header: {
                Text("Free Services")
            }

            Section("Defaults") {
                Picker("Output Format", selection: $defaultFormat) {
                    ForEach(AudioFormat.allCases) { format in
                        Text(format.rawValue.uppercased()).tag(format)
                    }
                }

                Picker("Default Model", selection: $defaultModel) {
                    ForEach(TTSModel.allCases) { model in
                        Text(model.displayName).tag(model.rawValue)
                    }
                }

                Picker("Default Voice", selection: $defaultVoice) {
                    ForEach(Voice.openaiVoices) { voice in
                        Text(voice.displayName).tag(voice.id)
                    }
                }
            }

            Section("Storage") {
                LabeledContent("Audio Output") {
                    Text("Desktop")
                        .foregroundStyle(.secondary)
                }
            }

            Section("About") {
                LabeledContent("Version") {
                    Text("0.1.0")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .formStyle(.grouped)
        .overlay(alignment: .topTrailing) {
            if showCloseButton {
                Button {
                    onClose?()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
                .padding(.trailing, 12)
                .padding(.top, 8)
                .keyboardShortcut(.escape, modifiers: [])
            }
        }
    }

    @AppStorage("default_output_format") private var defaultFormat: AudioFormat = .mp3
    @AppStorage("default_model") private var defaultModel: String = TTSModel.openai_tts1.rawValue
    @AppStorage("default_voice") private var defaultVoice: String = "alloy"
}

// MARK: - Provider Config Section

struct ProviderConfigSection: View {
    let provider: TTSProvider
    @State private var apiKey: String = ""
    @State private var baseURL: String = ""
    @State private var showKey = false

    init(provider: TTSProvider) {
        self.provider = provider
    }

    private func load() {
        apiKey = UserDefaults.standard.string(forKey: provider.apiKeyStorageKey) ?? ""
        baseURL = UserDefaults.standard.string(forKey: provider.baseURLStorageKey) ?? provider.defaultBaseURL
    }

    private func save() {
        UserDefaults.standard.set(apiKey, forKey: provider.apiKeyStorageKey)
        UserDefaults.standard.set(baseURL, forKey: provider.baseURLStorageKey)
    }

    var body: some View {
        Section {
            HStack {
                if showKey {
                    TextField(provider.apiKeyPlaceholder, text: $apiKey)
                        .textFieldStyle(.roundedBorder)
                } else {
                    SecureField(provider.apiKeyPlaceholder, text: $apiKey)
                        .textFieldStyle(.roundedBorder)
                }

                Button { showKey.toggle() } label: {
                    Image(systemName: showKey ? "eye.slash" : "eye")
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.borderless)
            }

            HStack {
                TextField("Base URL", text: $baseURL)
                    .textFieldStyle(.roundedBorder)
                    .font(.system(.body, design: .monospaced))

                if baseURL != provider.defaultBaseURL {
                    Button { baseURL = provider.defaultBaseURL } label: {
                        Image(systemName: "arrow.counterclockwise")
                            .foregroundStyle(.secondary)
                    }
                    .buttonStyle(.borderless)
                    .help("Reset to default")
                }
            }

            HStack {
                if apiKey.isEmpty {
                    Label("Not configured", systemImage: "xmark.circle")
                        .foregroundStyle(.secondary)
                        .font(.caption)
                } else {
                    Label("Configured", systemImage: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                        .font(.caption)
                }

                if baseURL != provider.defaultBaseURL {
                    Label("Custom endpoint", systemImage: "link.circle")
                        .foregroundStyle(.orange)
                        .font(.caption)
                }

                Spacer()

                if let url = provider.helpURL, let linkURL = URL(string: url) {
                    Link(destination: linkURL) {
                        Label("Get API Key", systemImage: "arrow.up.right.square")
                            .font(.caption)
                    }
                    .buttonStyle(.borderless)
                }
            }
        } header: {
            Label(provider.displayName, systemImage: provider.systemImage)
        } footer: {
            Text(provider.apiKeyDescription)
                .font(.caption2)
        }
        .onAppear { load() }
        .onChange(of: apiKey) { _, _ in save() }
        .onChange(of: baseURL) { _, _ in save() }
    }
}
