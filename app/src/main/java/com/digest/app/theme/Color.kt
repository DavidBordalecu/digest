package com.digest.app.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de Digest.
 *
 * Verde azulado como color base: transmite calma y evita el rojo de los
 * avisos de error, que aquí se reserva para síntomas y para las tomas
 * perdidas. Los tonos cálidos se usan solo en el diario, para separar
 * visualmente "cómo estoy" de "qué me tomo".
 */

// --- Claro ---
val VerdePrimario = Color(0xFF00696E)
val VerdePrimarioContainer = Color(0xFF9CF1F6)
val VerdeSecundario = Color(0xFF4A6365)
val VerdeSecundarioContainer = Color(0xFFCCE8EA)
val VerdeTercero = Color(0xFF525E7D)
val VerdeTerceroContainer = Color(0xFFDAE2FF)

// --- Oscuro ---
val VerdePrimarioOscuro = Color(0xFF80D5DA)
val VerdePrimarioContainerOscuro = Color(0xFF004F53)
val VerdeSecundarioOscuro = Color(0xFFB0CCCE)
val VerdeSecundarioContainerOscuro = Color(0xFF324B4D)
val VerdeTerceroOscuro = Color(0xFFBAC6EA)
val VerdeTerceroContainerOscuro = Color(0xFF3A4665)

// --- Semánticos: estado de las tomas ---
val TomaPendiente = Color(0xFFB58900)
val TomaPendienteContainer = Color(0xFFFFE08A)
val TomaPendienteContainerOscuro = Color(0xFF5C4300)

val TomaHecha = Color(0xFF2E7D32)
val TomaHechaContainer = Color(0xFFB7EFB9)

val TomaOmitida = Color(0xFF8E4B4B)
val TomaOmitidaContainer = Color(0xFFFFDAD6)

/** Gravedad de un síntoma, de leve (1) a intensa (5). */
val Gravedad = listOf(
    Color(0xFF4CAF50), // 1 leve
    Color(0xFF9CCC65), // 2
    Color(0xFFFFB74D), // 3 moderada
    Color(0xFFE57373), // 4
    Color(0xFFC62828), // 5 intensa
)

/** Consistencia según la escala de Bristol. */
val Bristol = listOf(
    Color(0xFF8D6E63), // 1-2 estreñimiento
    Color(0xFFA1887F), // 3
    Color(0xFF66BB6A), // 4 ideal
    Color(0xFFFFB74D), // 5
    Color(0xFFE57373), // 6
    Color(0xFFC62828), // 7
)