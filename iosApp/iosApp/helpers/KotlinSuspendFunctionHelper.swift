import Foundation
import shared

/**
 * Una función genérica de ayuda que envuelve una `KotlinSuspendFunction` (la forma en que el framework
 * compartido expone las funciones `suspend` a Swift) y la convierte en una función `async`
 * que puede lanzar errores, integrándose perfectamente con el `async/await` de Swift.
 *
 * - Parameter function: La función `suspend` de Kotlin que se va a ejecutar.
 * - Returns: El resultado `T` de la función suspendida si tiene éxito.
 * - Throws: El `Error` que la función suspendida pueda lanzar.
 */
func asyncResult<T>(for function: KotlinSuspendFunction<T, Error>) async throws -> T {
    return try await withCheckedThrowingContinuation { continuation in
        function.subscribe(
            onSuccess: { data in
                continuation.resume(returning: data)
            },
            onThrow: { error in
                continuation.resume(throwing: error)
            }
        )
    }
}