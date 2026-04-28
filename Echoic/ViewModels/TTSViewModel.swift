import Foundation
import SwiftUI
import Combine

@MainActor
class TTSViewModel: ObservableObject {
    // MARK: - Text Input
    @Published var inputText = ""

    // MARK: - Configuration
    @Published var selectedProvider: TTSProvider = .openai
    @Published var selectedModel: TTSModel = .openai_tts1
    @Published var selectedVoice: Voice = Voice(id: "alloy", displayName: "Alloy", provider: .openai)
    @Published var selectedFormat: AudioFormat = .mp3

    // MARK: - Synthesis State
    @Published var isSynthesizing = false
    @Published var synthesisProgress: Double = 0
    @Published var errorMessage: String?

    // MARK: - Audio Output
    @Published var audioData: Data?
    @Published var savedFileURL: URL?

    // MARK: - Playback State (forwarded from service)
    @Published var isPlaying = false
    @Published var currentTime: TimeInterval = 0
    @Published var duration: TimeInterval = 0
    @Published var playbackRate: Float = 1.0

    // MARK: - Dependencies
    let playbackService = AudioPlaybackService()
    let ttsService: TTSService
    private var synthesisTask: Task<Void, Never>?
    private var cancellables = Set<AnyCancellable>()

    init(ttsService: TTSService = TTSService()) {
        self.ttsService = ttsService

        // Forward playback service state to ViewModel's @Published properties
        playbackService.$isPlaying
            .receive(on: DispatchQueue.main)
            .assign(to: &$isPlaying)

        playbackService.$currentTime
            .receive(on: DispatchQueue.main)
            .assign(to: &$currentTime)

        playbackService.$duration
            .receive(on: DispatchQueue.main)
            .assign(to: &$duration)

        playbackService.$playbackRate
            .receive(on: DispatchQueue.main)
            .assign(to: &$playbackRate)
    }

    // MARK: - Actions

    func synthesize() {
        synthesisTask?.cancel()
        synthesisTask = Task {
            await performSynthesis()
        }
    }

    func cancelSynthesis() {
        synthesisTask?.cancel()
        isSynthesizing = false
        synthesisProgress = 0
    }

    func testConnection(for provider: TTSProvider) async throws {
        try await ttsService.testConnection(for: provider)
    }

    func saveAudio(filename: String? = nil) {
        guard let data = audioData else { return }
        do {
            let url = try playbackService.saveToDisk(data: data, format: selectedFormat, filename: filename)
            savedFileURL = url
        } catch {
            errorMessage = "Failed to save file: \(error.localizedDescription)"
        }
    }

    func playAudio() {
        guard let data = audioData else { return }
        do {
            try playbackService.play(data: data, format: selectedFormat)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func stopAudio() {
        playbackService.stop()
    }

    // MARK: - Provider Change Handling

    func updateModelAndVoiceForProvider() {
        let models = selectedProvider.availableModels
        if !models.contains(where: { $0 == selectedModel }) {
            selectedModel = models.first!
        }

        let voices = selectedProvider.availableVoices
        if !voices.contains(where: { $0.id == selectedVoice.id }) {
            selectedVoice = voices.first!
        }
    }

    // MARK: - Private

    private func performSynthesis() async {
        isSynthesizing = true
        synthesisProgress = 0
        errorMessage = nil
        audioData = nil
        savedFileURL = nil

        defer {
            isSynthesizing = false
            synthesisProgress = 1.0
        }

        do {
            let data = try await ttsService.synthesize(
                text: inputText,
                model: selectedModel,
                voice: selectedVoice,
                format: selectedFormat
            )

            guard !Task.isCancelled else { return }

            audioData = data
            synthesisProgress = 1.0
        } catch is CancellationError {
            // User cancelled, silent
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
