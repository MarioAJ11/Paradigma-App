import Foundation
import AVFoundation
import shared
import Combine

// (El enum PlayerContent se mantiene igual)
enum PlayerContent: Equatable {
    case episodio(Episodio)
    case liveStream
}


@MainActor
class AudioManager: NSObject, ObservableObject {

    static let shared = AudioManager()
    private var player: AVPlayer?

    // MARK: - Propiedades Publicadas

    @Published var content: PlayerContent? = nil
    var currentEpisode: Episodio? {
        if case .episodio(let episodio) = content { return episodio }
        return nil
    }

    @Published var isPlaying: Bool = false
    @Published var progress: Double = 0.0
    @Published var duration: Double = 0.0

    /// NUEVO: Controla la visibilidad del reproductor a pantalla completa.
    @Published var isFullScreenPlayerVisible: Bool = false

    // MARK: - Propiedades Privadas

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

    // MARK: - Controles Públicos

    /// Muestra u oculta el reproductor a pantalla completa.
    func toggleFullScreenPlayer() {
        isFullScreenPlayerVisible.toggle()
    }

    func selectEpisode(_ episodio: Episodio) {
        if let current = currentEpisode, current.id == episodio.id {
            togglePlayPause()
            return
        }

        var urlToPlay: URL?
        if let localURL = localFilePath(for: episodio), FileManager.default.fileExists(atPath: localURL.path) {
            urlToPlay = localURL
            print("Reproduciendo desde archivo local: \(localURL.path)")
        } else if let remoteURLString = episodio.archiveUrl {
            urlToPlay = URL(string: remoteURLString)
            print("Reproduciendo desde URL remota: \(remoteURLString)")
        }

        guard let finalURL = urlToPlay else {
            print("Error: No se encontró URL válida para el episodio \(episodio.id)")
            return
        }

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
        guard case .episodio = content else { return }
        let targetTime = CMTime(seconds: duration * progress, preferredTimescale: 600)
        player?.seek(to: targetTime)
    }

    /// Avanza la reproducción un número determinado de segundos.
    func skipForward(_ seconds: Double = 30.0) {
        guard case .episodio = content, let currentTime = player?.currentTime() else { return }
        let newTime = CMTimeAdd(currentTime, CMTime(seconds: seconds, preferredTimescale: 600))
        player?.seek(to: newTime)
    }

    /// Retrocede la reproducción un número determinado de segundos.
    func rewind(_ seconds: Double = 10.0) {
        guard case .episodio = content, let currentTime = player?.currentTime() else { return }
        let newTime = CMTimeSubtract(currentTime, CMTime(seconds: seconds, preferredTimescale: 600))
        player?.seek(to: newTime)
    }

    func isBuffering(episodeId: KotlinInt) -> Bool {
        return self.currentEpisode?.id == episodeId && !self.isPlaying && self.progress == 0.0
    }

    // El resto de la clase (Lógica Interna, observers, etc.) se mantiene sin cambios...
    private func setupAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Error al configurar la sesión de audio: \(error.localizedDescription)")
        }
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
        statusObserver = playerItem.observe(\.status, options: .initial) { [weak self] item, _ in
            if item.status == .readyToPlay {
                self?.duration = item.duration.seconds
            }
        }

        removePeriodicTimeObserver()
        let interval = CMTime(seconds: 0.5, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        timeObserverToken = player?.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            guard let self = self, let player = self.player, player.currentItem?.status == .readyToPlay else { return }
            let duration = self.duration
            if duration.isFinite && duration > 0 {
                self.progress = time.seconds / duration
            }
        }

        NotificationCenter.default.publisher(for: .AVPlayerItemDidPlayToEndTime, object: playerItem)
            .sink { [weak self] _ in
                self?.isPlaying = false
                self?.progress = 1.0
                // Aquí iría la lógica para pasar al siguiente episodio de la cola.
            }
            .store(in: &cancellables)
    }

    private func removeObservers() {
        removePeriodicTimeObserver()
        statusObserver?.invalidate()
        cancellables.forEach { $0.cancel() }
        cancellables.removeAll()
    }

    private func removePeriodicTimeObserver() {
        if let token = timeObserverToken {
            player?.removeTimeObserver(token)
            timeObserverToken = nil
        }
    }

    deinit {
        removeObservers()
    }
}