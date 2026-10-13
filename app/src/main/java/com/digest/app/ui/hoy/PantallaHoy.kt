package com.digest.app.ui.hoy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digest.app.R
import com.digest.app.data.local.TomaConDetalle
import com.digest.app.model.Condicion
import com.digest.app.model.EstadoToma
import com.digest.app.model.TipoPresentacion
import com.digest.app.model.TipoSintoma
import com.digest.app.theme.TomaHecha
import com.digest.app.theme.TomaOmitida
import com.digest.app.theme.TomaPendiente
import com.digest.app.ui.componentes.AvisoPermisos
import com.digest.app.ui.componentes.CabeceraPantalla
import com.digest.app.ui.componentes.ChipSintoma
import com.digest.app.ui.componentes.Vacio

/**
 * Pantalla principal: lo que el paciente necesita ver de un vistazo.
 *
 * El orden es deliberado: primero lo que tiene que hacer ahora (la próxima
 * toma), después lo que ya hizo, y al final el registro del día. Un paciente
 * con síntomas pasa por aquí varias veces al día, así que lo accionable va
 * arriba.
 */
@Composable
fun PantallaHoy(modifier: Modifier = Modifier) {
    val viewModel: HoyViewModel = viewModel(factory = HoyViewModel.fabrica)
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Al entrar en la pestaña se comprueban las tomas del día: así un
    // fármaco añadido hace un momento aparece con su aviso sin tener que
    // reiniciar la app.
    LaunchedEffect(Unit) { viewModel.prepararHoy() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { CabeceraPantalla(estado.perfil) }

        item { AvisoPermisos() }

        // --- Próxima toma ---
        estado.proxima?.let { proxima ->
            item {
                TarjetaProximaToma(
                    toma = proxima,
                    vencida = proxima.horaProgramada < java.time.LocalTime.now(),
                    alTomar = { viewModel.marcarTomada(proxima) },
                )
            }
        }

        // --- Tomas del día ---
        item {
            Seccion(
                titulo = stringResource(R.string.hoy_tomas),
                extra = if (estado.tomasVencidas > 0) {
                    stringResource(R.string.hoy_tomas_vencidas, estado.tomasVencidas)
                } else {
                    null
                },
            )
        }

        if (estado.tomas.isEmpty()) {
            item {
                Vacio(
                    texto = stringResource(R.string.hoy_sin_tomas),
                    detalle = stringResource(R.string.hoy_sin_tomas_detalle),
                )
            }
        }

        items(estado.tomas, key = { it.id }) { toma ->
            FilaToma(
                toma = toma,
                alTomar = { viewModel.marcarTomada(toma) },
                alOmitir = { viewModel.marcarOmitida(toma) },
                alReponer = { viewModel.volverAPendiente(toma) },
            )
        }

        // --- Registro del día ---
        item {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Seccion(stringResource(R.string.hoy_comidas))
        }

        if (estado.comidas.isEmpty()) {
            item { Vacio(texto = stringResource(R.string.hoy_sin_comidas)) }
        } else {
            items(estado.comidas, key = { it.id }) { comida ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(comida.tipo.displayName),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = comida.hora.toString().take(5),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                comida.alimentos?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Seccion(stringResource(R.string.hoy_sintomas))
        }

        item {
            SintomasDeHoy(
                condicion = estado.perfil?.condicion,
                marcados = estado.sintomasMarcados,
                alAlternar = viewModel::alternarSintoma,
            )
        }

        item { Spacer(Modifier.padding(8.dp)) }
    }
}

@Composable
private fun TarjetaProximaToma(
    toma: TomaConDetalle,
    vencida: Boolean,
    alTomar: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (vencida) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = if (vencida) {
                    stringResource(R.string.hoy_te_esta_pasando)
                } else {
                    stringResource(R.string.hoy_te_toca)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = toma.medicamentoNombre,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            toma.dosis?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = toma.horaProgramada.toString().take(5),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            FilledTonalButton(
                onClick = alTomar,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.hoy_marcar_tomada))
            }
        }
    }
}

@Composable
private fun FilaToma(
    toma: TomaConDetalle,
    alTomar: () -> Unit,
    alOmitir: () -> Unit,
    alReponer: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(
                        if (toma.presentacion == TipoPresentacion.SUPLEMENTO) {
                            R.drawable.ic_sintoma
                        } else {
                            R.drawable.ic_pastilla
                        },
                    ),
                    contentDescription = null,
                    tint = colorDeEstado(toma.estado),
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        text = toma.medicamentoNombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (toma.estado == EstadoToma.OMITIDA) {
                            FontWeight.Normal
                        } else {
                            FontWeight.Medium
                        },
                        color = if (toma.estado == EstadoToma.PENDIENTE) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = buildString {
                            append(toma.horaProgramada.toString().take(5))
                            toma.dosis?.let { append(" · ").append(it) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (toma.estado) {
                EstadoToma.PENDIENTE -> Row {
                    TextButton(onClick = alOmitir) {
                        Text(stringResource(R.string.accion_omitir))
                    }
                    FilledTonalButton(onClick = alTomar) {
                        Text(stringResource(R.string.hoy_tomada))
                    }
                }

                else -> TextButton(onClick = alReponer) {
                    Text(stringResource(R.string.hoy_reponer))
                }
            }
        }
    }
}

@Composable
private fun SintomasDeHoy(
    condicion: Condicion?,
    marcados: Map<TipoSintoma, Int>,
    alAlternar: (TipoSintoma) -> Unit,
) {
    val lista = remember(condicion) { condicion?.let { TipoSintoma.para(it) } ?: emptyList() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lista.groupBy { it.categoria }.forEach { (categoria, tipos) ->
            Text(
                text = stringResource(categoria.displayName),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Se agrupan en filas de dos para no depender de FlowRow, que no
            // viene en el artefacto estándar de Compose.
            Column {
                tipos.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fila.forEach { tipo ->
                            ChipSintoma(
                                tipo = tipo,
                                gravedad = marcados[tipo],
                                alPulsar = { alAlternar(tipo) },
                            )
                        }
                        if (fila.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, extra: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        extra?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun colorDeEstado(estado: EstadoToma) = when (estado) {
    EstadoToma.PENDIENTE -> MaterialTheme.colorScheme.onSurfaceVariant
    EstadoToma.TOMADA -> TomaHecha
    EstadoToma.OMITIDA -> TomaOmitida
    EstadoToma.RETRASADA -> TomaPendiente
}