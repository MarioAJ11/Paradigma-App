import Foundation
import shared

/**
 * Gestor central para los servicios compartidos de la aplicación.
 * Proporciona una única instancia (Singleton) del SDK y del servicio de configuración remota,
 * asegurando que toda la app utilice la misma configuración y conexiones.
 */
@MainActor
class AppServices {

    // Instancia única y compartida para toda la app.
    static let shared = AppServices()

    // Propiedades para los servicios del módulo `shared` de Kotlin.
    private let configService: RemoteConfigService
    let sdk: ParadigmaSDK

    /**
     * El inicializador es privado para asegurar que solo exista una instancia (Singleton).
     * Crea los servicios base y la configuración por defecto.
     */
    private init() {
        let database = Database(databaseDriverFactory: DatabaseDriverFactory())
        self.configService = RemoteConfigService(database: database)

        // Inicializa el SDK con la configuración por defecto.
        // Se re-inicializará con la configuración remota después de descargarla.
        let defaultConfig = self.configService.getConfig()
        let repository = ParadigmaRepository(database: database, baseUrl: defaultConfig.wordpressApiBaseUrl)
        self.sdk = ParadigmaSDK(repository: repository)
    }

    /**
     * Inicia los servicios de la aplicación. Debe llamarse al arrancar la app.
     * Descarga la configuración remota en segundo plano.
     */
    func initialize() {
        Task(priority: .background) {
            await configService.fetchAndCacheConfig()
            print("Configuración remota actualizada.")
            // Opcional: Podrías re-instanciar el SDK aquí si las URLs cambian drásticamente
            // durante la sesión, pero generalmente no es necesario.
        }
    }

    /**
     * Proporciona acceso seguro a la configuración actual.
     * @return El objeto `AppConfig` con las URLs y valores a usar.
     */
    func getConfig() -> AppConfig {
        return self.configService.getConfig()
    }
}