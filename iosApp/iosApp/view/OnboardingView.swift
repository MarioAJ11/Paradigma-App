import SwiftUI

/**
 * Vista de bienvenida (Onboarding) que se muestra al usuario la primera vez que abre la app.
 * Presenta información sobre el proyecto y un botón para continuar.
 */
struct OnboardingView: View {
    // El ViewModel se recibe del entorno para poder cambiar el estado.
    @EnvironmentObject var settingsViewModel: SettingsViewModel

    // Textos de la pantalla, idénticos a los de Android.
    private let textoTitulo = "Bienvenid@ a Paradigma Media"
    private let textoLargoPrincipal = "Paradigma Media Andalucía (en adelante Paradigma) es una iniciativa ciudadana que surge de la necesidad de cubrir las carencias incuestionables que tiene la sociedad en general, y la cordobesa en particular, sobre la información que le afecta de primera mano.\n\nParadigma tiene entidad sin ánimo de lucro. Todo lo recaudado por la Asociación será dirigido a conseguir los medios técnicos y humanos necesarios para mantener la mínima calidad exigible a un medio de comunicación en manos de la ciudadanía.\n\nTodas nuestras producciones se harán y distribuirán bajo licencia de Creative Commons."

    var body: some View {
        // Un ZStack para poner el color de fondo.
        ZStack {
            Color.amarilloPrincipal.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    Spacer(minLength: 50)

                    // Título principal
                    Text(textoTitulo)
                        .font(.displaySmall)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)

                    // Texto descriptivo
                    Text(textoLargoPrincipal)
                        .font(.bodyLarge)
                        .multilineTextAlignment(.leading)

                    // Fila de información adicional
                    InfoRow()

                    Spacer()

                    // Botón para continuar
                    Button(action: {
                        // Al pulsar, se llama a la función del ViewModel para marcar el onboarding como completado.
                        settingsViewModel.completeOnboarding()
                    }) {
                        Text("ACEPTAR")
                            .fontWeight(.bold)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.onPrimary) // Color de fondo del botón
                            .foregroundColor(.primary)   // Color del texto del botón
                            .cornerRadius(12)
                    }
                }
                .padding()
            }
        }
        .foregroundColor(.onPrimary) // Color de texto por defecto para toda la vista
    }
}

/**
 * Subvista para la fila de información, similar a la de Android.
 */
private struct InfoRow: View {
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "info.circle.fill")
            Text("En la sección de 'Ajustes' encontrarás una guía con todas las funcionalidades de la aplicación.")
                .font(.bodyMedium)
        }
        .padding(.vertical)
    }
}