package com.digest.app.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.digest.app.R
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.model.TipoSintoma
import com.digest.app.notificaciones.Permisos
import com.digest.app.theme.Gravedad
import com.digest.app.ui.abrirAjustesAlarmasExactas
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMATO_FECHA: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))

/** Cabecera con la fecha y el nombre de la condición del paciente. */
@Composable
fun CabeceraPantalla(perfil: PerfilEntity?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text(
            text = LocalDate.now().format(FORMATO_FECHA)
                .replaceFirstChar { it.uppercase(Locale("es", "ES")) },
            style = MaterialTheme.typography.headlineSmall,
        )
        perfil?.let {
            Text(
                text = stringResource(it.condicion.displayName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Estado vacío reutilizable. */
@Composable
fun Vacio(
    texto: String,
    detalle: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(texto, style = MaterialTheme.typography.bodyLarge)
            detalle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Chip de síntoma.
 *
 * El color codifica la gravedad, que es la lectura rápida que busca el
 * paciente: un solo vistazo dice cómo está el día sin leer los rótulos.
 */
@Composable
fun ChipSintoma(
    tipo: TipoSintoma,
    gravedad: Int?,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = gravedad != null,
        onClick = alPulsar,
        label = { Text(stringResource(tipo.displayName)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = gravedad
                ?.let { Gravedad[(it - 1).coerceIn(0, 4)] }
                ?: MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = modifier,
    )
}

/**
 * Aviso de permisos de notificaciones.
 *
 * Solo aparece cuando faltan. Es un dato de salud: si el sistema los tiene
 * desactivados y la app calla, el paciente se queda sin avisos de
 * medicación sin saber por qué.
 */
@Composable
fun AvisoPermisos(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current

    // Los permisos son estado del sistema: se releen al volver a la pantalla.
    val avisos by produceState(
        initialValue = Permisos.avisosActivados(contexto),
        contexto,
    ) {
        value = Permisos.avisosActivados(contexto)
    }
    val alarmaExacta by produceState(
        initialValue = Permisos.tieneAlarmasExactas(contexto),
        contexto,
    ) {
        value = Permisos.tieneAlarmasExactas(contexto)
    }

    if (avisos && alarmaExacta) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.permisos_titulo),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            if (!avisos) {
                Text(
                    text = stringResource(R.string.permisos_sin_avisos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            if (!alarmaExacta) {
                Text(
                    text = stringResource(R.string.permisos_sin_alarma_exacta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                TextButton(onClick = { abrirAjustesAlarmasExactas(contexto) }) {
                    Text(stringResource(R.string.permisos_abrir_ajustes))
                }
            }
        }
    }
}