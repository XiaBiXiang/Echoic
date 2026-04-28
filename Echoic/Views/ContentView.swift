import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = TTSViewModel()
    @Binding var showSettings: Bool

    var body: some View {
        ZStack {
            NavigationSplitView {
                SidebarView(viewModel: viewModel)
                    .navigationTitle("Echoic")
            } detail: {
                TTSPlayerView(viewModel: viewModel)
            }
            .background(.ultraThinMaterial)

            // Settings overlay panel
            if showSettings {
                SettingsOverlay(showSettings: $showSettings)
                    .transition(.move(edge: .trailing).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: showSettings)
    }
}

// MARK: - Settings Overlay

private struct SettingsOverlay: View {
    @Binding var showSettings: Bool
    @AppStorage("app_appearance") private var appearance: String = "system"

    var body: some View {
        HStack(spacing: 0) {
            // Dimmed backdrop — click to dismiss
            Color.black.opacity(0.2)
                .onTapGesture {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                        showSettings = false
                    }
                }

            // Settings panel
            SettingsView(showCloseButton: true, onClose: {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                    showSettings = false
                }
            })
            .frame(width: 400)
            .background(.ultraThinMaterial)
            .clipShape(UnevenRoundedRectangle(topLeadingRadius: 0, bottomLeadingRadius: 0, bottomTrailingRadius: 12, topTrailingRadius: 0))
            .shadow(color: .black.opacity(0.15), radius: 16, x: -4, y: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Sidebar

struct SidebarView: View {
    @ObservedObject var viewModel: TTSViewModel

    var body: some View {
        List {
            Section("Voice Settings") {
                Picker("Model", selection: $viewModel.selectedModel) {
                    ForEach(viewModel.selectedProvider.availableModels) { model in
                        Text(model.displayName).tag(model)
                    }
                }

                Picker("Voice", selection: $viewModel.selectedVoice) {
                    ForEach(viewModel.selectedProvider.availableVoices) { voice in
                        Text(voice.displayName).tag(voice)
                    }
                }

                Picker("Format", selection: $viewModel.selectedFormat) {
                    ForEach(AudioFormat.allCases) { format in
                        Text(format.rawValue.uppercased()).tag(format)
                    }
                }
            }

            Section("History") {
                Label("Coming soon", systemImage: "clock.arrow.circlepath")
                    .foregroundStyle(.secondary)
            }
        }
        .listStyle(.sidebar)
    }
}

// MARK: - TTS Player (Detail)

struct TTSPlayerView: View {
    @ObservedObject var viewModel: TTSViewModel
    @State private var filename = ""

    var body: some View {
        VStack(spacing: 0) {
            textInputSection
            Divider()
            actionBar
            Divider()
            playbackSection
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(nsColor: .controlBackgroundColor))
    }

    // MARK: - Text Input

    private var textInputSection: some View {
        VStack(alignment: .leading) {
            Label("Text to Speech", systemImage: "text.bubble")
                .font(.headline)

            TextEditor(text: $viewModel.inputText)
                .font(.body)
                .padding(4)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color(nsColor: .textBackgroundColor))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color(nsColor: .separatorColor), lineWidth: 1)
                )
                .frame(minHeight: 120, maxHeight: .infinity)
                .overlay(alignment: .topLeading) {
                    if viewModel.inputText.isEmpty {
                        Text("Enter or paste your English text here...")
                            .foregroundStyle(.tertiary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 12)
                            .allowsHitTesting(false)
                    }
                }
        }
    }

    // MARK: - Action Bar

    private var actionBar: some View {
        HStack {
            if viewModel.isSynthesizing {
                ProgressView()
                    .controlSize(.small)
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
                            ProgressView(
                                value: viewModel.playbackService.currentTime,
                                total: viewModel.playbackService.duration > 0 ? viewModel.playbackService.duration : 1
                            )
                            .tint(.accentColor)

                            HStack {
                                Text(formatTime(viewModel.playbackService.currentTime))
                                Spacer()
                                Text(formatTime(viewModel.playbackService.duration))
                            }
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        }

                        Spacer()

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
            if viewModel.playbackService.isPlaying {
                viewModel.playbackService.pause()
            } else if viewModel.audioData != nil {
                if viewModel.playbackService.currentTime > 0 {
                    viewModel.playbackService.resume()
                } else {
                    viewModel.playAudio()
                }
            }
        } label: {
            Image(systemName: viewModel.playbackService.isPlaying ? "pause.fill" : "play.fill")
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
}
