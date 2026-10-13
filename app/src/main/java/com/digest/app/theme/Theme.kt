package com.digest.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val EsquemaOscuro = darkColorScheme(
    primary = VerdePrimarioOscuro,
    onPrimary = Color(0xFF00363A),
    primaryContainer = VerdePrimarioContainerOscuro,
    onPrimaryContainer = VerdePrimarioContainer,
    secondary = VerdeSecundarioOscuro,
    onSecondary = Color(0xFF1C3537),
    secondaryContainer = VerdeSecundarioContainerOscuro,
    onSecondaryContainer = VerdeSecundarioContainer,
    tertiary = VerdeTerceroOscuro,
    onTertiary = Color(0xFF232F4C),
    tertiaryContainer = VerdeTerceroContainerOscuro,
    onTertiaryContainer = VerdeTerceroContainer,
)

private val EsquemaClaro = lightColorScheme(
    primary = VerdePrimario,
    onPrimary = Color.White,
    primaryContainer = VerdePrimarioContainer,
    onPrimaryContainer = Color(0xFF002022),
    secondary = VerdeSecundario,
    onSecondary = Color.White,
    secondaryContainer = VerdeSecundarioContainer,
    onSecondaryContainer = Color(0xFF051F21),
    tertiary = VerdeTercero,
    onTertiary = Color.White,
    tertiaryContainer = VerdeTerceroContainer,
    onTertiaryContainer = Color(0xFF0B1B3A),
    background = Color(0xFFFAFDFC),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFC),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE4E5),
    onSurfaceVariant = Color(0xFF3F4949),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

/**
 * Tema de Digest.
 *
 * El color dinámico está desactivado a propósito: los colores de estado
 * (toma pendiente, hecha u omitida) son los que dan significado a la app, y
 * si el móvil impone su propia paleta dejarían de ser reconocibles.
 */
@Composable
fun DigestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val esquema = if (darkTheme) EsquemaOscuro else EsquemaClaro

    MaterialTheme(
        colorScheme = esquema,
        typography = Tipografia,
        content = content,
    )
}

/** ¿Soporta el sistema el color dinámico? (Android 12+) */
val soportaColorDinamico: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S