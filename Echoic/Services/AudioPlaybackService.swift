import Foundation
import AVFoundation

class AudioPlaybackService: NSObject, ObservableObject {
    @Published var isPlaying = false
    @Published var duration: TimeInterval = 0
    @Published var currentTime: TimeInterval = 0

    private var player: AVAudioPlayer?
    private var updateTimer: Timer?

    // MARK: - Playback Controls

    func play(data: Data, format: AudioFormat) throws {
        stop()

        let tempURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("echoic_playback.\(format.fileExtension)")

        try data.write(to: tempURL, options: .atomic)

        let player = try AVAudioPlayer(contentsOf: tempURL)
        player.delegate = self
        player.prepareToPlay()

        self.duration = player.duration
        self.player = player

        guard player.play() else {
            throw TTSError.playbackFailed("Unable to start playback.")
        }

        isPlaying = true
        startProgressTimer()

        try? FileManager.default.removeItem(at: tempURL)
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

    // MARK: - Export

    func saveToDisk(data: Data, format: AudioFormat, filename: String?) throws -> URL {
        let directory = FileManager.default.urls(for: .desktopDirectory, in: .userDomainMask).first!
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
