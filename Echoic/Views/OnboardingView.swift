import SwiftUI

private var L: LanguageManager { LanguageManager.shared }

struct OnboardingView: View {
    @State private var step = 0
    @State private var ttsMode: String = ""
    @State private var goingForward = true
    @ObservedObject private var lm = LanguageManager.shared

    /// At least one provider is ready (free = always ready, paid = key entered)
    private var hasReadyProvider: Bool {
        TTSProvider.allCases.contains { $0.isConfigured }
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                ForEach(0..<3, id: \.self) { i in
                    Capsule()
                        .fill(i == step ? Color.accentColor : Color(nsColor: .separatorColor))
                        .frame(height: 3)
                        .animation(.easeInOut(duration: 0.3), value: step)
                }
            }
            .padding(.horizontal, 32)
            .padding(.top, 20)

            ZStack {
                if step == 0 {
                    AppearanceStep().transition(slideTransition)
                } else if step == 1 {
                    ModeSelectStep(ttsMode: $ttsMode).transition(slideTransition)
                } else if step == 2 {
                    ConfigStep(ttsMode: ttsMode).transition(slideTransition)
                }
            }
            .animation(.easeInOut(duration: 0.35), value: step)
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            Divider()

            HStack {
                if step > 0 {
                    Button {
                        goingForward = false
                        withAnimation(.easeInOut(duration: 0.35)) { step -= 1 }
                    } label: {
                        Label(L.back, systemImage: "chevron.left")
                    }
                    .buttonStyle(.bordered)
                }

                Spacer()

                Button {
                    goingForward = true
                    withAnimation(.easeInOut(duration: 0.35)) {
                        switch step {
                        case 0: step = 1
                        case 1: step = 2
                        case 2: UserDefaults.standard.set(true, forKey: "has_completed_onboarding")
                        default: break
                        }
                    }
                } label: {
                    Text(stepLabel)
                }
                .buttonStyle(.borderedProminent)
                .disabled(step == 1 && ttsMode.isEmpty)
                .disabled(step == 2 && (ttsMode == "cloud" || ttsMode == "both") && !hasReadyProvider)
            }
            .padding(20)
        }
        .background(.ultraThinMaterial)
    }

    private var slideTransition: AnyTransition {
        goingForward
            ? .asymmetric(insertion: .move(edge: .trailing).combined(with: .opacity),
                          removal: .move(edge: .leading).combined(with: .opacity))
            : .asymmetric(insertion: .move(edge: .leading).combined(with: .opacity),
                          removal: .move(edge: .trailing).combined(with: .opacity))
    }

    private var stepLabel: String {
        switch step {
        case 2: L.getStarted
        default: L.next
        }
    }
}

// MARK: - Step 0

private struct AppearanceStep: View {
    @AppStorage("app_appearance") private var appearance: String = "system"
    @ObservedObject private var lm = LanguageManager.shared

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            Image(systemName: "paintbrush.pointed")
                .font(.system(size: 48, weight: .light))
                .foregroundStyle(Color.accentColor)
                .padding(.bottom, 16)

            Text(L.personalize)
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .contentTransition(.numericText())

            Text(L.personalizeDesc)
                .font(.body)
                .foregroundStyle(.secondary)
                .padding(.top, 4)
                .contentTransition(.numericText())

            Spacer().frame(height: 40)

            VStack(alignment: .leading, spacing: 10) {
                Text(L.appearance)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.leading, 4)

                HStack(spacing: 12) {
                    ThemeButton(icon: "sun.max", label: L.light, selected: appearance == "light") {
                        withAnimation(.spring(response: 0.3)) { appearance = "light" }
                    }
                    ThemeButton(icon: "moon", label: L.dark, selected: appearance == "dark") {
                        withAnimation(.spring(response: 0.3)) { appearance = "dark" }
                    }
                    ThemeButton(icon: "circle.lefthalf.filled", label: L.system, selected: appearance == "system") {
                        withAnimation(.spring(response: 0.3)) { appearance = "system" }
                    }
                }
            }

            Spacer().frame(height: 28)

            VStack(alignment: .leading, spacing: 10) {
                Text(L.languageLabel)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.leading, 4)

                HStack(spacing: 12) {
                    LangButton(label: "English", subtitle: "EN", selected: lm.language == "en") {
                        withAnimation(.spring(response: 0.3)) { lm.language = "en" }
                    }
                    .frame(minWidth: 160)
                    LangButton(label: "中文", subtitle: "ZH", selected: lm.language == "zh") {
                        withAnimation(.spring(response: 0.3)) { lm.language = "zh" }
                    }
                    .frame(minWidth: 160)
                }
            }

            Spacer()
        }
        .padding(.horizontal, 80)
        .frame(maxWidth: 440)
        .animation(.easeInOut(duration: 0.25), value: lm.language)
    }
}

private struct ThemeButton: View {
    let icon: String
    let label: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(selected ? Color.accentColor.opacity(0.15) : Color(nsColor: .controlBackgroundColor))
                        .frame(width: 56, height: 56)
                    Image(systemName: icon)
                        .font(.system(size: 22))
                        .foregroundStyle(selected ? Color.accentColor : .secondary)
                }
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(selected ? Color.accentColor : Color.clear, lineWidth: 2))
                .scaleEffect(selected ? 1.05 : 1.0)
                Text(label)
                    .font(.caption)
                    .foregroundStyle(selected ? .primary : .secondary)
                    .fontWeight(selected ? .semibold : .regular)
            }
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: selected)
    }
}

private struct LangButton: View {
    let label: String
    let subtitle: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Text(subtitle)
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundStyle(selected ? .white : .secondary)
                    .frame(width: 30, height: 24)
                    .background(RoundedRectangle(cornerRadius: 6).fill(selected ? Color.accentColor : Color(nsColor: .controlBackgroundColor)))

                Text(label)
                    .font(.subheadline)
                    .lineLimit(1)
                    .fixedSize()
                    .foregroundStyle(selected ? .primary : .secondary)
                    .fontWeight(selected ? .semibold : .regular)

                Spacer(minLength: 4)

                if selected {
                    Image(systemName: "checkmark")
                        .font(.caption)
                        .foregroundStyle(Color.accentColor)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(RoundedRectangle(cornerRadius: 10).fill(selected ? Color.accentColor.opacity(0.08) : Color.clear))
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(selected ? Color.accentColor.opacity(0.3) : Color(nsColor: .separatorColor), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .animation(.easeOut(duration: 0.2), value: selected)
    }
}

// MARK: - Step 1

private struct ModeSelectStep: View {
    @Binding var ttsMode: String
    @ObservedObject private var lm = LanguageManager.shared

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            Image(systemName: "arrow.triangle.2.circlepath")
                .font(.system(size: 48, weight: .light))
                .foregroundStyle(Color.accentColor)
                .padding(.bottom, 16)

            Text(L.chooseMode)
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .contentTransition(.numericText())

            Text(L.chooseModeDesc)
                .font(.body)
                .foregroundStyle(.secondary)
                .padding(.top, 4)
                .contentTransition(.numericText())

            Spacer().frame(height: 40)

            HStack(spacing: 20) {
                ModeCard(icon: "cloud", title: L.cloudAPI, desc: L.cloudAPIDesc, selected: ttsMode == "cloud") {
                    withAnimation(.spring(response: 0.3)) { ttsMode = "cloud" }
                }
                ModeCard(icon: "internaldrive", title: L.localModel, desc: L.localModelDesc, selected: ttsMode == "local") {
                    withAnimation(.spring(response: 0.3)) { ttsMode = "local" }
                }
                ModeCard(icon: "arrow.triangle.2.circlepath", title: L.bothMode, desc: L.bothModeDesc, selected: ttsMode == "both") {
                    withAnimation(.spring(response: 0.3)) { ttsMode = "both" }
                }
            }

            Spacer()
        }
        .padding(.horizontal, 60)
        .animation(.easeInOut(duration: 0.25), value: lm.language)
    }
}

private struct ModeCard: View {
    let icon: String
    let title: String
    let desc: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 14) {
                ZStack {
                    Circle().fill(selected ? Color.accentColor : Color(nsColor: .controlBackgroundColor)).frame(width: 60, height: 60)
                    Image(systemName: icon).font(.system(size: 24)).foregroundStyle(selected ? .white : Color.accentColor)
                }
                Text(title).font(.headline).foregroundStyle(.primary)
                Text(desc).font(.caption).foregroundStyle(.secondary).multilineTextAlignment(.center).lineSpacing(2)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 24)
            .background(RoundedRectangle(cornerRadius: 16).fill(selected ? Color.accentColor.opacity(0.06) : Color.clear))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(selected ? Color.accentColor : Color(nsColor: .separatorColor), lineWidth: selected ? 2 : 1))
            .scaleEffect(selected ? 1.03 : 1.0)
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: selected)
    }
}

// MARK: - Step 2

private struct ConfigStep: View {
    let ttsMode: String
    @ObservedObject private var lm = LanguageManager.shared

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text(L.configureServices)
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .padding(.top, 20)

                if ttsMode == "cloud" || ttsMode == "both" {
                    CloudConfigSection()
                }

                if ttsMode == "local" || ttsMode == "both" {
                    LocalModelSection()
                }

                HStack {
                    Spacer()
                    Label(L.changeAnytime, systemImage: "info.circle").font(.caption2).foregroundStyle(.tertiary)
                    Spacer()
                }
                .padding(.bottom, 16)
            }
            .padding(.horizontal, 60)
        }
        .animation(.easeInOut(duration: 0.25), value: lm.language)
    }
}

private enum TestState: Equatable { case idle, testing, success, failure(String) }

// MARK: - Cloud Config Section

private struct CloudConfigSection: View {
    @State private var testState: TestState = .idle
    @State private var expandedProviders: Set<String> = []
    @ObservedObject private var lm = LanguageManager.shared

    private var paidProviders: [TTSProvider] {
        TTSProvider.allCases.filter { $0.requiresAPIKey }
    }

    private var freeProviders: [TTSProvider] {
        TTSProvider.allCases.filter { !$0.requiresAPIKey }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Label(L.cloudServices, systemImage: "cloud").font(.headline)

            // Paid providers — collapsible cards
            VStack(spacing: 8) {
                ForEach(paidProviders) { provider in
                    ProviderAccordion(
                        provider: provider,
                        isExpanded: expandedProviders.contains(provider.id)
                    ) {
                        withAnimation(.spring(response: 0.3)) {
                            if expandedProviders.contains(provider.id) {
                                expandedProviders.remove(provider.id)
                            } else {
                                expandedProviders.insert(provider.id)
                            }
                        }
                    }
                }
            }

            // Free providers — compact badges
            if !freeProviders.isEmpty {
                VStack(spacing: 6) {
                    ForEach(freeProviders) { provider in
                        FreeProviderRow(provider: provider)
                    }
                }
            }

            // Test connection (optional)
            TestConnectionButton(testState: $testState)
        }
        .onAppear {
            // Auto-expand first unconfigured provider
            if let first = paidProviders.first(where: { !$0.isConfigured }) {
                expandedProviders.insert(first.id)
            } else if let first = paidProviders.first {
                expandedProviders.insert(first.id)
            }
        }
    }
}

// MARK: - Provider Accordion

private struct ProviderAccordion: View {
    let provider: TTSProvider
    let isExpanded: Bool
    let toggle: () -> Void
    @State private var apiKey: String = ""
    @State private var baseURL: String = ""
    @State private var showKey = false
    @ObservedObject private var lm = LanguageManager.shared

    var body: some View {
        VStack(spacing: 0) {
            // Header row — always visible
            Button(action: toggle) {
                HStack(spacing: 12) {
                    Image(systemName: provider.systemImage)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(apiKey.isEmpty ? .secondary : Color.accentColor)
                        .frame(width: 24)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(provider.displayName)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundStyle(.primary)
                        Text(provider.subtitle)
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                    }

                    Spacer()

                    if apiKey.isEmpty {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.caption)
                            .foregroundStyle(.orange)
                    } else {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.caption)
                            .foregroundStyle(.green)
                    }

                    Image(systemName: "chevron.right")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                        .rotationEffect(.degrees(isExpanded ? 90 : 0))
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(RoundedRectangle(cornerRadius: 10).fill(Color(nsColor: .controlBackgroundColor)))
            }
            .buttonStyle(.plain)

            // Expandable config area
            if isExpanded {
                VStack(spacing: 10) {
                    // API Key row
                    HStack(spacing: 8) {
                        Text(L.apiKey)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .frame(width: 60, alignment: .trailing)
                        if showKey {
                            TextField(provider.apiKeyPlaceholder, text: $apiKey)
                                .textFieldStyle(.roundedBorder)
                        } else {
                            SecureField(provider.apiKeyPlaceholder, text: $apiKey)
                                .textFieldStyle(.roundedBorder)
                        }
                        Button { showKey.toggle() } label: {
                            Image(systemName: showKey ? "eye.slash" : "eye")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .buttonStyle(.borderless)
                    }

                    // Endpoint row
                    HStack(spacing: 8) {
                        Text(L.endpoint)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .frame(width: 60, alignment: .trailing)
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

                    // Status + help link
                    HStack {
                        if let url = provider.helpURL, let link = URL(string: url) {
                            Link(destination: link) {
                                Label(L.getKey, systemImage: "arrow.up.right.square")
                            }
                            .font(.caption2)
                            .buttonStyle(.borderless)
                        }
                        Spacer()
                        Text(provider.apiKeyDescription)
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                            .lineLimit(2)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.top, 8)
                .padding(.bottom, 12)
                .background(RoundedRectangle(cornerRadius: 10).fill(Color(nsColor: .controlBackgroundColor).opacity(0.5)))
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(isExpanded ? Color.accentColor.opacity(0.3) : Color(nsColor: .separatorColor), lineWidth: 1)
        )
        .onAppear {
            apiKey = UserDefaults.standard.string(forKey: provider.apiKeyStorageKey) ?? ""
            baseURL = UserDefaults.standard.string(forKey: provider.baseURLStorageKey) ?? provider.defaultBaseURL
        }
        .onChange(of: apiKey) { _, _ in UserDefaults.standard.set(apiKey, forKey: provider.apiKeyStorageKey) }
        .onChange(of: baseURL) { _, _ in UserDefaults.standard.set(baseURL, forKey: provider.baseURLStorageKey) }
    }
}

// MARK: - Free Provider Row

private struct FreeProviderRow: View {
    let provider: TTSProvider
    @ObservedObject private var lm = LanguageManager.shared

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: provider.systemImage)
                .font(.caption)
                .foregroundStyle(Color.accentColor)
                .frame(width: 24)
            Text(provider.displayName)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text("·")
                .font(.caption2)
                .foregroundStyle(.tertiary)
            Text(L.freeNoKey)
                .font(.caption2)
                .foregroundStyle(.tertiary)
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .font(.caption2)
                .foregroundStyle(.green)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 7)
        .background(RoundedRectangle(cornerRadius: 8).fill(Color(nsColor: .controlBackgroundColor).opacity(0.4)))
    }
}

// MARK: - Test Connection Button

private struct TestConnectionButton: View {
    @Binding var testState: TestState
    @ObservedObject private var lm = LanguageManager.shared

    var body: some View {
        VStack(spacing: 10) {
            HStack {
                Spacer()
                Button { runTest() } label: {
                    HStack(spacing: 6) {
                        switch testState {
                        case .idle: Image(systemName: "bolt.horizontal")
                        case .testing: ProgressView().controlSize(.small)
                        case .success: Image(systemName: "checkmark.circle.fill")
                        case .failure: Image(systemName: "xmark.circle.fill")
                        }
                        Text(buttonLabel)
                    }
                    .animation(nil, value: testState)
                }
                .buttonStyle(.bordered)
                .disabled(testState == .testing)
                .tint(testState == .success ? .green : nil)
            }

            Group {
                switch testState {
                case .failure(let msg):
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(.red)
                        Text(msg).font(.callout).foregroundStyle(.red).lineLimit(3)
                        Spacer()
                    }
                    .padding(10)
                    .background(RoundedRectangle(cornerRadius: 8).fill(Color.red.opacity(0.06)))
                case .success:
                    HStack {
                        Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                        Text(L.connectionSuccess).font(.callout).foregroundStyle(.green)
                        Spacer()
                    }
                    .padding(10)
                    .background(RoundedRectangle(cornerRadius: 8).fill(Color.green.opacity(0.06)))
                default: EmptyView()
                }
            }
            .animation(.easeOut(duration: 0.2), value: testState)
        }
    }

    private var buttonLabel: String {
        switch testState {
        case .idle: L.testConnection
        case .testing: L.testing
        case .success: L.testAgain
        case .failure: L.retry
        }
    }

    private func runTest() {
        testState = .testing
        let service = TTSService()
        let configured = TTSProvider.allCases.filter { $0.requiresAPIKey && $0.isConfigured }
        guard !configured.isEmpty else {
            testState = .failure(L.noProviderConfigured)
            return
        }
        Task {
            do {
                for provider in configured {
                    try await service.testConnection(for: provider)
                }
                withAnimation { testState = .success }
            } catch {
                withAnimation { testState = .failure(error.localizedDescription) }
            }
        }
    }
}

// MARK: - Local Model Placeholder

private struct LocalModelSection: View {
    @ObservedObject private var lm = LanguageManager.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(L.localModels, systemImage: "internaldrive").font(.headline)
            GroupBox {
                VStack(spacing: 12) {
                    Image(systemName: "arrow.down.circle").font(.system(size: 28)).foregroundStyle(.tertiary)
                    Text(L.localComingSoon).font(.callout).foregroundStyle(.secondary)
                    Text(L.localComingSoonDesc).font(.caption).foregroundStyle(.tertiary)
                }
                .frame(maxWidth: .infinity).padding(.vertical, 16)
            }
        }
    }
}
