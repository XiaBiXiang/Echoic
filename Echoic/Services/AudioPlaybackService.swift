import Foundation
import AVFoundation

class AudioPlaybackService: NSObject, ObservableObject {
    @Published var isPlaying = false
    @Published var duration: TimeInterval = 0
    @Published var currentTime: TimeInterval = 0
    @Published var playbackRate: Float = 1.0

    private var player: AVAudioPlayer?
    private var updateTimer: Timer?

    static let availableRates: [Float] = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0]

    // MARK: - Playback Controls

    func play(data: Data, format: AudioFormat? = nil) throws {
        stop()

        // Use data-based init to avoid file extension mismatch issues
        // (e.g. MiMo returns WAV but user selected MP3 format)
        let player = try AVAudioPlayer(data: data)
        player.delegate = self
        player.enableRate = true
        player.rate = playbackRate
        player.prepareToPlay()

        self.duration = player.duration
        self.player = player

        guard player.play() else {
            throw TTSError.playbackFailed("Unable to start playback.")
        }

        isPlaying = true
        startProgressTimer()
    }

    func stop() {
        player?.stop()
        player = nil
        isPlaying = false
        currentTime = 0
        duration = 0
        invalidateTimer()
    }

    func pause() {
        player?.pause()
        isPlaying = false
        invalidateTimer()
    }

    func resume() {
        guard let player, !player.isPlaying else { return }
        player.play()
        isPlaying = true
        startProgressTimer()
    }

    // MARK: - Seek

    func seek(to time: TimeInterval) {
        player?.currentTime = time
        currentTime = time
    }

    // MARK: - Rate

    func setRate(_ rate: Float) {
        playbackRate = rate
        player?.rate = rate
    }

    // MARK: - Export

    func saveToDisk(data: Data, format: AudioFormat, filename: String?) throws -> URL {
        // Use custom save directory if set, otherwise default to Downloads
        let directory: URL
        if let customPath = UserDefaults.standard.string(forKey: "save_directory"),
           !customPath.isEmpty {
            directory = URL(fileURLWithPath: customPath)
        } else {
            directory = FileManager.default.urls(for: .downloadsDirectory, in: .userDomainMask).first!
        }

        let safeName = filename?.components(separatedBy: .init(charactersIn: "/\\:*?\"<>|")).joined()
            ?? "echoic_output"
        let destination = directory.appendingPathComponent("\(safeName).\(format.fileExtension)")

        // Avoid overwriting
        var finalURL = destination
        var counter = 1
        while FileManager.default.fileExists(atPath: finalURL.path) {
            finalURL = directory.appendingPathComponent("\(safeName) (\(counter)).\(format.fileExtension)")
            counter += 1
        }

        try data.write(to: finalURL, options: .atomic)
        return finalURL
    }

    // MARK: - Timer

    private func startProgressTimer() {
        invalidateTimer()
        updateTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self, let player = self.player else { return }
            self.currentTime = player.currentTime
        }
    }

    private func invalidateTimer() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
}

// MARK: - AVAudioPlayerDelegate

extension AudioPlaybackService: AVAudioPlayerDelegate {
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        isPlaying = false
        currentTime = 0
        invalidateTimer()
    }
}
