package com.digest.app.ui.diario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digest.app.R
import com.digest.app.domain.DeposicionDia
import com.digest.app.model.ConsistenciaBristol
import com.digest.app.model.LocalizacionDolor
import com.digest.app.model.SangreEnHeces
import com.digest.app.model.TipoDolor
import com.digest.app.model.TipoNutricionEnteral
import com.digest.app.theme.bristolColor

/**
 * Registro de una deposición con la escala de Bristol.
 *
 * Se registra en un diálogo y no en una pantalla completa porque se hace
 * varias veces al día y tiene que ser un gesto de pocos segundos.
 */
@Composable
fun DialogoDeposicion(
    alGuardar: (bristol: Int, sangre: SangreEnHeces) -> Unit,
    alCerrar: () -> Unit,
) {
    var bristol by remember { mutableStateOf(4) }
    var sangre by remember { mutableStateOf(SangreEnHeces.NINGUNA) }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.deposicion_titulo)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.deposicion_consistencia),
                    style = MaterialTheme.typography.titleMedium,
                )
                EscalaBristol(bristol) { bristol = it }

                Text(
                    text = stringResource(R.string.deposicion_sangre),
                    style = MaterialTheme.typography.titleMedium,
                )
                SangreEnHeces.entries.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fila.forEach { opcion ->
                            FilterChip(
                                selected = sangre == opcion,
                                onClick = { sangre = opcion },
                                label = { Text(stringResource(opcion.displayName)) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { alGuardar(bristol, sangre) }) {
                Text(stringResource(R.string.deposicion_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) { Text(stringResource(R.string.cerrar)) }
        },
    )
}

/**
 * Escala de Bristol como lista vertical.
 *
 * En dos columnas las etiquetas no caben y el chip se estrecha hasta
 * partir el texto en una letra por línea. Vertical cada opción tiene su
 * ancho completo, y el punto de color hace que se comparen de un vistazo.
 */
@Composable
private fun EscalaBristol(actual: Int, alSeleccionar: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ConsistenciaBristol.entries.forEach { nivel ->
            val seleccionado = actual == nivel.nivel
            Surface(
                onClick = { alSeleccionar(nivel.nivel) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = if (seleccionado) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(bristolColor(nivel.nivel)),
                    )
                    Text(
                        text = stringResource(nivel.displayName),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

/**
 * Registro de un episodio de dolor abdominal.
 *
 * Además de la intensidad guarda el carácter y la localización porque en
 * Crohn la diferencia entre un dolor cólico y uno fijo es lo que separa un
 * espasmo de una posible estenosis.
 */
@Composable
fun DialogoDolor(
    alGuardar: (intensidad: Int, tipo: TipoDolor, localizacion: LocalizacionDolor?) -> Unit,
    alCerrar: () -> Unit,
) {
    var intensidad by remember { mutableStateOf(3) }
    var tipo by remember { mutableStateOf(TipoDolor.COLICO) }
    var localizacion by remember { mutableStateOf<LocalizacionDolor?>(null) }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.dolor_titulo)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.dolor_intensidad, intensidad),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { nivel ->
                        FilterChip(
                            selected = intensidad == nivel,
                            onClick = { intensidad = nivel },
                            label = { Text("$nivel") },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.dolor_caracter),
                    style = MaterialTheme.typography.titleMedium,
                )
                TipoDolor.entries.forEach { opcion ->
                    Column {
                        FilterChip(
                            selected = tipo == opcion,
                            onClick = { tipo = opcion },
                            label = { Text(stringResource(opcion.displayName)) },
                        )
                        // La pista explica la diferencia clínica: es la
                        // parte que hace útil este dato para el médico.
                        Text(
                            text = stringResource(opcion.pista),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.dolor_localizacion),
                    style = MaterialTheme.typography.titleMedium,
                )
                // Vertical porque los rótulos son largos («Fosa ilíaca
                // derecha, típica de Crohn»); en dos columnas no caben.
                LocalizacionDolor.entries.forEach { opcion ->
                    Surface(
                        onClick = {
                            localizacion = if (localizacion == opcion) null else opcion
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = if (localizacion == opcion) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ) {
                        Text(
                            text = stringResource(opcion.displayName),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 10.dp,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { alGuardar(intensidad, tipo, localizacion) }) {
                Text(stringResource(R.string.dolor_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) { Text(stringResource(R.string.cerrar)) }
        },
    )
}

/**
 * Registro de nutrición enteral.
 *
 * El volumen es lo que se contabiliza al día en la consulta, así que es el
 * dato que se pide siempre; el producto es opcional porque cada paciente
 * toma una fórmula distinta.
 */
@Composable
fun DialogoNutricionEnteral(
    alGuardar: (producto: String?, volumenMl: Int, tipo: TipoNutricionEnteral) -> Unit,
    alCerrar: () -> Unit,
) {
    var producto by remember { mutableStateOf("") }
    var volumen by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoNutricionEnteral.PARCIAL) }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.enteral_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = producto,
                    onValueChange = { producto = it },
                    label = { Text(stringResource(R.string.enteral_producto)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = volumen,
                    onValueChange = { volumen = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.enteral_volumen)) },
                    suffix = { Text("ml") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                TipoNutricionEnteral.entries.forEach { opcion ->
                    FilterChip(
                        selected = tipo == opcion,
                        onClick = { tipo = opcion },
                        label = { Text(stringResource(opcion.displayName)) },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    alGuardar(producto.ifBlank { null }, volumen.toIntOrNull() ?: 0, tipo)
                },
                enabled = (volumen.toIntOrNull() ?: 0) > 0,
            ) { Text(stringResource(R.string.enteral_guardar)) }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) { Text(stringResource(R.string.cerrar)) }
        },
    )
}

/** Lista de los registros de hoy, agrupados por tipo. */
@Composable
fun ResumenDeposiciones(deposiciones: List<DeposicionDia>, modifier: Modifier = Modifier) {
    if (deposiciones.isEmpty()) return
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.deposiciones_de_hoy, deposiciones.size),
                style = MaterialTheme.typography.titleMedium,
            )
            deposiciones.forEach { deposicion ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(bristolColor(deposicion.nivelBristol)),
                        )
                        Text(
                            text = "  ${ConsistenciaBristol.porNivel(deposicion.nivelBristol).let { stringResource(it.displayName) }}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (deposicion.sangre != SangreEnHeces.NINGUNA) {
                        Text(
                            text = stringResource(deposicion.sangre.displayName),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}