package com.digest.app.ui.diario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digest.app.R
import com.digest.app.model.EstadoBrote
import com.digest.app.model.NivelHidratacion
import com.digest.app.model.NivelMejora
import com.digest.app.model.SensacionGeneral
import com.digest.app.model.TipoSintoma
import com.digest.app.ui.componentes.CabeceraPantalla
import com.digest.app.ui.componentes.ChipSintoma

/**
 * Diario del día.
 *
 * Responde a las dos preguntas centrales del paciente — si la medicación le
 * mejora o no, y cómo se siente — y además deja a la vista **qué síntomas
 * ha marcado hoy**, para que el diario sea un resumen del día y no solo unas
 * casillas sueltas.
 */
@Composable
fun PantallaDiario(modifier: Modifier = Modifier) {
    val viewModel: DiarioViewModel = viewModel(factory = DiarioViewModel.fabrica)
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var exporting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CabeceraPantalla(estado.perfil)

        // --- Resumen de lo registrado hoy ---
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.diario_resumen_hoy),
                    style = MaterialTheme.typography.titleMedium,
                )

                if (estado.sintomasHoy.isEmpty()) {
                    Text(
                        text = stringResource(R.string.diario_sin_sintomas),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.diario_sintomas_marcados,
                            estado.sintomasHoy.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        estado.sintomasHoy.entries.chunked(2).forEach { fila ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                fila.forEach { (tipo, gravedad) ->
                                    ChipSintoma(
                                        tipo = tipo,
                                        gravedad = gravedad,
                                        alPulsar = { viewModel.alternarSintoma(tipo) },
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.diario_pulsa_ajustar),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Grupo(
            titulo = stringResource(R.string.diario_mejora),
            opciones = NivelMejora.entries,
            seleccionado = estado.mejora,
            etiqueta = { it.displayName },
            alSeleccionar = viewModel::elegirMejora,
        )

        Grupo(
            titulo = stringResource(R.string.diario_sensacion),
            opciones = SensacionGeneral.entries,
            seleccionado = estado.sensacion,
            etiqueta = { it.displayName },
            alSeleccionar = viewModel::elegirSensacion,
        )

        Grupo(
            titulo = stringResource(R.string.diario_brote),
            opciones = EstadoBrote.entries,
            seleccionado = estado.estadoBrote,
            etiqueta = { it.displayName },
            alSeleccionar = viewModel::elegirBrote,
        )

        Grupo(
            titulo = stringResource(R.string.diario_hidratacion),
            opciones = NivelHidratacion.entries,
            seleccionado = estado.hidratacion,
            etiqueta = { it.displayName },
            alSeleccionar = viewModel::elegirHidratacion,
        )

        OutlinedTextField(
            value = estado.notas,
            onValueChange = viewModel::cambiarNotas,
            label = { Text(stringResource(R.string.diario_notas)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        // --- Informe para el médico ---
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.informe_titulo),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.informe_explicacion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                FilaPeriodos(
                    seleccion = estado.periodo,
                    alSeleccionar = viewModel::elegirPeriodo,
                )

                if (exporting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(R.string.informe_generando),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.exportar(FormatoInforme.TEXTO) },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.informe_texto)) }

                        OutlinedButton(
                            onClick = { viewModel.exportar(FormatoInforme.CSV) },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.informe_csv)) }
                    }
                }
            }
        }
    }

    // Compartir el informe ya generado, si lo hay. El `LaunchedEffect`
    // dentro de CompartirInforme evita volver a abrir el menú en cada
    // recomposición.
    val informe = viewModel.informeParaCompartir.collectAsStateWithLifecycle().value
    informe?.let {
        CompartirInforme(it)
        viewModel.consumirInforme()
    }
}

/**
 * Fila de filtros reusable.
 *
 * Se recorre en dos filas porque las listas largas no caben en una sola sin
 * comprimir los textos hasta hacerlos ilegibles.
 */
@Composable
private fun <T> Grupo(
    titulo: String,
    opciones: List<T>,
    seleccionado: T?,
    etiqueta: (T) -> Int,
    alSeleccionar: (T) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            opciones.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fila.forEach { opcion ->
                        FilterChip(
                            selected = opcion == seleccionado,
                            onClick = { alSeleccionar(opcion) },
                            label = { Text(stringResource(etiqueta(opcion))) },
                        )
                    }
                }
            }
        }
    }
}

/** Selector del periodo que se quiere exportar. */
@Composable
private fun FilaPeriodos(seleccion: PeriodoInforme, alSeleccionar: (PeriodoInforme) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PeriodoInforme.entries.forEach { periodo ->
            FilterChip(
                selected = periodo == seleccion,
                onClick = { alSeleccionar(periodo) },
                label = { Text(stringResource(periodo.etiqueta)) },
            )
        }
    }
}