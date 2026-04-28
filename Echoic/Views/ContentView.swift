import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = TTSViewModel()
    @Binding var showSettings: Bool
    @AppStorage("provider_config_timestamp") private var configTimestamp: Double = 0

    var body: some View {
        ZStack(alignment: .trailing) {
            VStack(spacing: 0) {
                // Toolbar: provider + model + voice + format
                toolbarSection

                Divider()

                // Main content
                TTSPlayerView(viewModel: viewModel)
            }
            .background(.ultraThinMaterial)

            // Settings overlay — always present, slides in/out
            SettingsOverlay(showSettings: $showSettings)
                .offset(x: showSettings ? 0 : 380 + 20)
                .opacity(showSettings ? 1 : 0)
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: showSettings)
        .onChange(of: configTimestamp) { _, _ in
            // Re-select provider if current one became unavailable
            if !viewModel.selectedProvider.isConfigured {
                if let first = TTSProvider.allCases.first(where: { $0.isConfigured }) {
                    viewModel.selectedProvider = first
                }
            }
        }
    }

    // MARK: - Toolbar

    private var toolbarSection: some View {
        let _ = configTimestamp // Force SwiftUI dependency tracking
        return HStack(spacing: 16) {
            // Provider picker
            Picker("", selection: $viewModel.selectedProvider) {
                ForEach(TTSProvider.allCases.filter { $0.isConfigured }) { p in
                    Text(p.displayName).tag(p)
                }
            }
            .labelsHidden()

            // Format picker
            Picker("Format", selection: $viewModel.selectedFormat) {
                ForEach(AudioFormat.allCases) { f in
                    Text(f.rawValue.uppercased()).tag(f)
                }
            }

            Spacer()

            // Settings gear button
            Button {
                showSettings.toggle()
            } label: {
                Image(systemName: "gearshape")
                    .foregroundStyle(.secondary)
            }
            .buttonStyle(.borderless)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
        .onChange(of: viewModel.selectedProvider) { _, _ in
            viewModel.updateModelAndVoiceForProvider()
        }
    }
}

// MARK: - Settings Overlay

private struct SettingsOverlay: View {
    @Binding var showSettings: Bool

    var body: some View {
        HStack(spacing: 0) {
            // Dimmed backdrop
            Color.black.opacity(showSettings ? 0.2 : 0)
                .onTapGesture {
                    showSettings = false
                }
                .allowsHitTesting(showSettings)

            // Settings panel
            SettingsView(showCloseButton: true, onClose: { showSettings = false })
                .frame(width: 380)
                .background(.ultraThinMaterial)
                .clipShape(UnevenRoundedRectangle(topLeadingRadius: 0, bottomLeadingRadius: 0, bottomTrailingRadius: 12, topTrailingRadius: 0))
                .shadow(color: .black.opacity(0.15), radius: 16, x: -4, y: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - TTS Player

struct TTSPlayerView: View {
    @ObservedObject var viewModel: TTSViewModel
    @State private var filename = ""
    @State private var showRateMenu = false
    @State private var isSeeking = false

    var body: some View {
        VStack(spacing: 0) {
            textInputSection
            Divider()
            actionBar
            Divider()
            playbackSection
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Text Input

    private var textInputSection: some View {
        VStack(alignment: .leading) {
            TextEditor(text: $viewModel.inputText)
                .font(.body)
                .padding(6)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color(nsColor: .textBackgroundColor))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color(nsColor: .separatorColor), lineWidth: 1)
                )
                .frame(minHeight: 160, maxHeight: .infinity)
                .overlay(alignment: .topLeading) {
                    if viewModel.inputText.isEmpty {
                        Text("Enter or paste text here...")
                            .foregroundStyle(.tertiary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 9)
                            .allowsHitTesting(false)
                    }
                }
        }
    }

    // MARK: - Action Bar

    private var actionBar: some View {
        HStack {
            if viewModel.isSynthesizing {
                ProgressView().controlSize(.small)
                Text("Synthesizing...")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Spacer()

                Button("Cancel") {
                    viewModel.cancelSynthesis()
                }
                .buttonStyle(.bordered)
            } else {
                Button {
                    viewModel.synthesize()
                } label: {
                    Label("Synthesize", systemImage: "waveform")
                }
                .buttonStyle(.borderedProminent)
                .disabled(viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Spacer()

                Text("\(viewModel.inputText.count) characters")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 8)
    }

    // MARK: - Playback

    private var playbackSection: some View {
        Group {
            if let audioData = viewModel.audioData {
                VStack(spacing: 12) {
                    HStack(spacing: 16) {
                        playPauseButton

                        VStack(alignment: .leading, spacing: 4) {
                            Slider(
                                value: $viewModel.currentTime,
                                in: 0...max(viewModel.duration, 0.01),
                                onEditingChanged: { editing in
                                    isSeeking = editing
                                    if !editing {
                                        viewModel.playbackService.seek(to: viewModel.currentTime)
                                    }
                                }
                            )
                            .tint(.accentColor)

                            HStack {
                                Text(formatTime(isSeeking ? viewModel.currentTime : viewModel.currentTime))
                                Spacer()
                                Text(formatTime(viewModel.duration))
                            }
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        }

                        Spacer()

                        // Playback rate
                        Menu {
                            ForEach(AudioPlaybackService.availableRates, id: \.self) { rate in
                                Button {
                                    viewModel.playbackService.setRate(rate)
                                } label: {
                                    if rate == viewModel.playbackRate {
                                        Label("\(formatRate(rate))x", systemImage: "checkmark")
                                    } else {
                                        Text("\(formatRate(rate))x")
                                    }
                                }
                            }
                        } label: {
                            Text("\(formatRate(viewModel.playbackRate))x")
                                .font(.caption)
                                .monospacedDigit()
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Capsule().fill(Color.accentColor.opacity(0.15)))
                                .foregroundStyle(.secondary)
                        }
                        .menuStyle(.borderlessButton)
                        .fixedSize()

                        Button {
                            viewModel.saveAudio(filename: filename.isEmpty ? nil : filename)
                        } label: {
                            Label("Save", systemImage: "square.and.arrow.down")
                        }
                        .buttonStyle(.bordered)
                    }

                    if let url = viewModel.savedFileURL {
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(.green)
                            Text("Saved to \(url.lastPathComponent)")
                                .font(.caption)
                        }
                    }

                    Text("\(formatFileSize(audioData.count))")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
                .padding(12)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(.ultraThinMaterial)
                )
            } else if viewModel.errorMessage != nil {
                errorBanner
            } else {
                ContentUnavailableView(
                    "No Audio Yet",
                    systemImage: "waveform",
                    description: Text("Enter text and click Synthesize to generate speech")
                )
            }
        }
    }

    // MARK: - Play/Pause Button

    private var playPauseButton: some View {
        Button {
            if viewModel.isPlaying {
                viewModel.playbackService.pause()
            } else if viewModel.audioData != nil {
                if viewModel.currentTime > 0 {
                    viewModel.playbackService.resume()
                } else {
                    viewModel.playAudio()
                }
            }
        } label: {
            Image(systemName: viewModel.isPlaying ? "pause.fill" : "play.fill")
                .font(.title2)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.borderless)
    }

    // MARK: - Error Banner

    private var errorBanner: some View {
        HStack {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(.orange)
            if let message = viewModel.errorMessage {
                Text(message)
                    .font(.callout)
            }
            Spacer()
            Button("Dismiss") {
                viewModel.errorMessage = nil
            }
            .buttonStyle(.bordered)
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(nsColor: .controlBackgroundColor))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.orange.opacity(0.3), lineWidth: 1)
        )
    }

    // MARK: - Helpers

    private func formatTime(_ time: TimeInterval) -> String {
        let minutes = Int(time) / 60
        let seconds = Int(time) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    private func formatFileSize(_ bytes: Int) -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .file
        return formatter.string(fromByteCount: Int64(bytes))
    }

    private func formatRate(_ rate: Float) -> String {
        if rate == floor(rate) {
            return String(format: "%.0f", rate)
        }
        return String(format: "%.2g", rate)
    }
}
