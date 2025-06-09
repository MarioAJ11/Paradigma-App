import Foundation
import AVFoundation
import shared
import Combine

/**
 * El tipo de contenido que el reproductor puede manejar.
 */
enum PlayerContent: Equatable {
    case episodio(Episodio)
    case liveStream
}

/**
 * Gestiona la reproducción de audio de toda la aplicación.
 * Es capaz de reproducir archivos locales si están descargados, o de hacer streaming.
 */
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

    // MARK: - Propiedades Privadas

    private var timeObserverToken: Any?
    private var statusObserver: NSKeyValueObservation?
    private var cancellables = Set<AnyCancellable>()
    private let liveStreamURL = URL(string: "https://radio.andaina.net/8042/stream")

    private override init() {
        super.init()
        setupAudioSession()
    }

    // MARK: - Controles Públicos

    func selectEpisode(_ episodio: Episodio) {
        if let current = currentEpisode, current.id == episodio.id {
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

    func play() {
        player?.play()
        isPlaying = true
    }

    func pause() {
        player?.pause()
        isPlaying = false
    }

    func togglePlayPause() {
        isPlaying ? pause() : play()
    }

    func seek(to progress: Double) {
        guard case .episodio = content else { return }
        let targetTime = CMTime(seconds: duration * progress, preferredTimescale: 600)
        player?.seek(to: targetTime)
    }

    func isBuffering(episodeId: KotlinInt) -> Bool {
        return self.currentEpisode?.id == episodeId && !self.isPlaying && self.progress == 0.0
    }

    // MARK: - Lógica Interna

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

    /// Añade los observadores necesarios a un AVPlayerItem para actualizar la UI.
    private func addObservers(for playerItem: AVPlayerItem) {
        // Observador de estado
        statusObserver = playerItem.observe(\.status, options: .initial) { [weak self] item, _ in
            if item.status == .readyToPlay {
                self?.duration = item.duration.seconds
            }
        }

        // Observador de tiempo
        removePeriodicTimeObserver()
        let interval = CMTime(seconds: 0.5, preferredTimescale: 1_000_000_000)
        let mainQueue = DispatchQueue.main

        // Definimos el bloque de código por separado.
        let observerBlock = { [weak self] (time: CMTime) -> Void in
            guard let self = self else { return }

            let hasValidDuration = self.duration.isFinite && self.duration > 0
            if hasValidDuration {
                self.progress = time.seconds / self.duration
            }
        }

        // Añadimos el observador usando todos los parámetros explícitamente.
        self.timeObserverToken = self.player?.addPeriodicTimeObserver(forInterval: interval, queue: mainQueue, using: observerBlock)

        // Observador de fin de reproducción
        NotificationCenter.default.publisher(for: .AVPlayerItemDidPlayToEndTime, object: playerItem)
            .sink { [weak self] _ in
                self?.isPlaying = false
                self?.progress = 1.0
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