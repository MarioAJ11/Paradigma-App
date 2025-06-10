import SwiftUI

/**
 * Un componente de vista reutilizable para mostrar un ítem en la pantalla de ayuda/ajustes.
 * Presenta un icono, un título y una descripción.
 */
struct HelpItemView: View {
    let icon: String
    let title: String
    let description: String

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(.amarilloPrincipal)
                .frame(width: 24) // Ancho fijo para alinear los iconos

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.titleMedium)
                    .fontWeight(.semibold)

                Text(description)
                    .font(.bodyMedium)
                    .foregroundColor(.onSurfaceVariant)
            }
        }
    }
}