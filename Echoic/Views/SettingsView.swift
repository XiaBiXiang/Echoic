import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    var showCloseButton: Bool = false
    var onClose: (() -> Void)? = nil

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Header
                HStack {
                    Text("Settings")
                        .font(.title2.bold())
                    Spacer()
                    if showCloseButton {
                        Button {
                            onClose?()
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .font(.title2)
                                .foregroundStyle(.secondary)
                        }
                        .buttonStyle(.plain)
                        .keyboardShortcut(.escape, modifiers: [])
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)

                // Providers
                SettingsProviderList()

                Divider().padding(.horizontal, 20)

                // Defaults
                SettingsDefaultsSection(
                    defaultFormat: $defaultFormat,
                    defaultModel: $defaultModel,
                    defaultVoice: $defaultVoice
                )

                Divider().padding(.horizontal, 20)

                // Save Directory
                SettingsSaveDirectorySection()

                Divider().padding(.horizontal, 20)

                // About
                HStack {
                    Text("Echoic v0.1.0")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                    Spacer()
                    Text("Audio saved to \(saveDirectoryName)")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 16)
            }
        }
        .frame(minWidth: 360, idealWidth: 380, maxWidth: 420)
    }

    @AppStorage("default_output_format") private var defaultFormat: AudioFormat = .mp3
    @AppStorage("default_model") private var defaultModel: String = TTSModel.openai_tts1.rawValue
    @AppStorage("default_voice") private var defaultVoice: String = "alloy"
    @AppStorage("save_directory") private var saveDirectory: String = ""

    private var saveDirectoryName: String {
        if saveDirectory.isEmpty {
            return "Downloads"
        } else {
            return URL(fileURLWithPath: saveDirectory).lastPathComponent
        }
    }
}

// MARK: - Provider List

private struct SettingsProviderList: View {
    @State private var expanded: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("PROVIDERS")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 20)
                .padding(.bottom, 6)

            ForEach(TTSProvider.allCases) { provider in
                if provider.requiresAPIKey {
                    SettingsProviderRow(
                        provider: provider,
                        isExpanded: expanded == provider.id
                    ) {
                        withAnimation(.spring(response: 0.25)) {
                            expanded = expanded == provider.id ? nil : provider.id
                        }
                    }
                } else {
                    SettingsFreeProviderRow(provider: provider)
                }
            }
        }
    }
}

// MARK: - Provider Row (Collapsible)

private struct SettingsProviderRow: View {
    let provider: TTSProvider
    let isExpanded: Bool
    let toggle: () -> Void
    @State private var apiKey: String = ""
    @State private var baseURL: String = ""
    @State private var showKey = false

    var body: some View {
        VStack(spacing: 0) {
            Button(action: toggle) {
                HStack(spacing: 10) {
                    Image(systemName: provider.systemImage)
                        .font(.system(size: 14))
                        .foregroundStyle(apiKey.isEmpty ? .secondary : Color.accentColor)
                        .frame(width: 20)

                    Text(provider.displayName)
                        .font(.subheadline)
                        .fontWeight(.medium)

                    Spacer()

                    if apiKey.isEmpty {
                        Image(systemName: "circle")
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                    } else {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.caption)
                            .foregroundStyle(.green)
                    }

                    Image(systemName: "chevron.right")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(.tertiary)
                        .rotationEffect(.degrees(isExpanded ? 90 : 0))
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if isExpanded {
                VStack(spacing: 8) {
                    HStack(spacing: 6) {
                        Text("Key")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .frame(width: 40, alignment: .trailing)
                        Group {
                            if showKey {
                                TextField(provider.apiKeyPlaceholder, text: $apiKey)
                            } else {
                                SecureField(provider.apiKeyPlaceholder, text: $apiKey)
                            }
                        }
                        .textFieldStyle(.roundedBorder)
                        Button { showKey.toggle() } label: {
                            Image(systemName: showKey ? "eye.slash" : "eye")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .buttonStyle(.borderless)
                    }

                    HStack(spacing: 6) {
                        Text("URL")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .frame(width: 40, alignment: .trailing)
                        TextField("Base URL", text: $baseURL)
                            .textFieldStyle(.roundedBorder)
                            .font(.system(.caption, design: .monospaced))
                        if baseURL != provider.defaultBaseURL {
                            Button { baseURL = provider.defaultBaseURL } label: {
                                Image(systemName: "arrow.counterclockwise")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            .buttonStyle(.borderless)
                        }
                    }

                    HStack {
                        if let url = provider.helpURL, let link = URL(string: url) {
                            Link(destination: link) {
                                Label("Get Key", systemImage: "arrow.up.right.square")
                                    .font(.caption2)
                            }
                            .buttonStyle(.borderless)
                        }
                        Spacer()
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 10)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .onAppear {
            apiKey = UserDefaults.standard.string(forKey: provider.apiKeyStorageKey) ?? ""
            baseURL = UserDefaults.standard.string(forKey: provider.baseURLStorageKey) ?? provider.defaultBaseURL
        }
        .onChange(of: apiKey) { _, _ in
            UserDefaults.standard.set(apiKey, forKey: provider.apiKeyStorageKey)
            UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: "provider_config_timestamp")
        }
        .onChange(of: baseURL) { _, _ in
            UserDefaults.standard.set(baseURL, forKey: provider.baseURLStorageKey)
        }
    }
}

// MARK: - Free Provider Row

private struct SettingsFreeProviderRow: View {
    let provider: TTSProvider

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: provider.systemImage)
                .font(.system(size: 14))
                .foregroundStyle(Color.accentColor)
                .frame(width: 20)
            Text(provider.displayName)
                .font(.subheadline)
            Spacer()
            Text("Free")
                .font(.caption2)
                .foregroundStyle(.green)
            Image(systemName: "checkmark.circle.fill")
                .font(.caption)
                .foregroundStyle(.green)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }
}

// MARK: - Save Directory Section

private struct SettingsSaveDirectorySection: View {
    @AppStorage("save_directory") private var saveDirectory: String = ""
    @State private var showFolderPicker = false

    private var displayPath: String {
        if saveDirectory.isEmpty {
            let downloads = FileManager.default.urls(for: .downloadsDirectory, in: .userDomainMask).first!
            return downloads.path
        }
        return saveDirectory
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("SAVE LOCATION")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 20)

            HStack(spacing: 8) {
                Text(displayPath)
                    .font(.system(.caption, design: .monospaced))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .truncationMode(.middle)
                Spacer()
                Button {
                    NSWorkspace.shared.open(URL(fileURLWithPath: displayPath))
                } label: {
                    Image(systemName: "folder")
                        .font(.caption)
                }
                .buttonStyle(.borderless)
                .help("Open in Finder")

                Button {
                    showFolderPicker = true
                } label: {
                    Image(systemName: "folder.badge.gearshape")
                        .font(.caption)
                }
                .buttonStyle(.borderless)
                .help("Change save location")

                if !saveDirectory.isEmpty {
                    Button {
                        saveDirectory = ""
                    } label: {
                        Image(systemName: "arrow.counterclockwise")
                            .font(.caption)
                    }
                    .buttonStyle(.borderless)
                    .help("Reset to Downloads")
                }
            }
            .padding(.horizontal, 20)
        }
        .fileImporter(
            isPresented: $showFolderPicker,
            allowedContentTypes: [.folder],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case .success(let urls):
                if let url = urls.first {
                    saveDirectory = url.path
                }
            case .failure:
                break
            }
        }
    }
}

// MARK: - Defaults Section

private struct SettingsDefaultsSection: View {
    @Binding var defaultFormat: AudioFormat
    @Binding var defaultModel: String
    @Binding var defaultVoice: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("DEFAULTS")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 20)

            HStack(spacing: 6) {
                Text("Format")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(width: 52, alignment: .trailing)
                Picker("", selection: $defaultFormat) {
                    ForEach(AudioFormat.allCases) { f in
                        Text(f.rawValue.uppercased()).tag(f)
                    }
                }
                .labelsHidden()
                .frame(maxWidth: .infinity)
            }
            .padding(.horizontal, 20)
        }
    }
}
