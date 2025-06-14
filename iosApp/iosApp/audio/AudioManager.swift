import Foundation
import AVFoundation
import shared
import Combine

/**
 * Define el tipo de contenido que el reproductor puede manejar.
 */
enum PlayerContent: Equatable {
    case episodio(Episodio)
    case liveStream
}

/**
 * Gestiona de forma centralizada toda la lógica de reproducción de audio.
 * Se asegura de que todas las actualizaciones de la UI ocurran en el hilo principal.
 */
@MainActor
class AudioManager: NSObject, ObservableObject {

    static let shared = AudioManager()
    private var player: AVPlayer?

    @Published var content: PlayerContent? = nil
    var currentEpisode: Episodio? {
        if case .episodio(let episodio) = content { return episodio }
        return nil
    }

    @Published var isPlaying: Bool = false
    @Published var progress: Double = 0.0
    @Published var duration: Double = 0.0
    @Published var isFullScreenPlayerVisible: Bool = false

    private var timeObserverToken: Any?
    private var statusObserver: NSKeyValueObservation?
    private var cancellables = Set<AnyCancellable>()
    private var liveStreamURL: URL? {
        URL(string: AppServices.shared.getConfig().liveStreamUrl)
    }

    private override init() {
        super.init()
        setupAudioSession()
    }

    deinit {
        removeObservers()
    }

    func toggleFullScreenPlayer() {
        isFullScreenPlayerVisible.toggle()
    }

    func selectEpisode(_ episodio: Episodio) {
        // Compara los IDs de los episodios de forma segura convirtiéndolos a Int32.
        if let current = currentEpisode, current.id.int32Value == episodio.id.int32Value {
            togglePlayPause()
            return
        }

        var urlToPlay: URL?
        if let localURL = localFilePath(for: episodio), FileManager.default.fileExists(atPath: localURL.path) {
            urlToPlay = localURL
        } else if let remoteURLString = episodio.archiveUrl {
            urlToPlay = URL(string: remoteURLString)
        }

        guard let finalURL = urlToPlay else { return }

        self.content = .episodio(episodio)
        preparePlayer(with: finalURL)
        play()
    }

    func toggleLiveStream() {
        if case .liveStream = content, isPlaying {
            pause()
            content = nil
            return
        }
        guard let url = liveStreamURL else { return }
        self.content = .liveStream
        preparePlayer(with: url)
        play()
    }

    func play() { player?.play(); isPlaying = true }
    func pause() { player?.pause(); isPlaying = false }
    func togglePlayPause() { isPlaying ? pause() : play() }

    func seek(to progress: Double) {
        guard case .episodio = content, let player = player else { return }
        let targetTime = CMTime(seconds: duration * progress, preferredTimescale: 600)
        player.seek(to: targetTime)
    }

    func isBuffering(episodeId: KotlinInt) -> Bool {
        guard let currentId = self.currentEpisode?.id else { return false }
        // Compara los IDs usando su valor numérico para evitar errores de tipo.
        return currentId.int32Value == episodeId.int32Value && !self.isPlaying && self.progress == 0.0
    }
    
    private func setupAudioSession() {
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)
    }
    
    private func preparePlayer(with url: URL) {
        removeObservers()
        progress = 0.0
        duration = 0.0
        let playerItem = AVPlayerItem(url: url)
        player = AVPlayer(playerItem: playerItem)
        addObservers(for: playerItem)
    }

    private func addObservers(for playerItem: AVPlayerItem) {
        statusObserver = playerItem.observe(\.status, options: [.new, .initial]) { [weak self] item, _ in
            guard let self = self, item.status == .readyToPlay else { return }
            self.duration = item.duration.seconds
        }

        let interval = CMTime(seconds: 0.5, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        timeObserverToken = player?.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            guard let self = self, self.duration > 0 else { return }
            self.progress = time.seconds / self.duration
        }
    }

    private func removeObservers() {
        if let token = timeObserverToken {
            player?.removeTimeObserver(token)
            timeObserverToken = nil
        }
        statusObserver?.invalidate()
        statusObserver = nil
    }
}
