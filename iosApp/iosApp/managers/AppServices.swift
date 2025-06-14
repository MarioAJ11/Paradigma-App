import Foundation
import shared

/**
 * Gestor central para los servicios compartidos de la aplicación.
 * Proporciona una única instancia (Singleton) del SDK y de la configuración remota.
 */
@MainActor
class AppServices {

    // Instancia única y compartida para toda la app.
    static let shared = AppServices()

    // Propiedades para los servicios del módulo `shared` de Kotlin.
    private let configService: RemoteConfigService
    let sdk: ParadigmaSDK

    /**
     * El inicializador es privado para asegurar que solo exista una instancia.
     */
    private init() {
        let database = Database(databaseDriverFactory: DatabaseDriverFactory())
        self.configService = RemoteConfigService(database: database)

        let defaultConfig = self.configService.getConfig()
        let repository = ParadigmaRepository(database: database, baseUrl: defaultConfig.wordpressApiBaseUrl)
        self.sdk = ParadigmaSDK(repository: repository)
    }

    /**
     * Inicia los servicios de la aplicación.
     * Descarga la configuración remota en segundo plano y maneja posibles errores.
     */
    func initialize() {
        Task(priority: .background) {
            do {
                try await configService.fetchAndCacheConfig()
                print("Configuración remota actualizada.")
            } catch {
                print("Error al descargar la configuración remota: \(error)")
            }
        }
    }

    /**
     * Proporciona acceso seguro a la configuración actual.
     */
    func getConfig() -> AppConfig {
        return self.configService.getConfig()
    }
}
