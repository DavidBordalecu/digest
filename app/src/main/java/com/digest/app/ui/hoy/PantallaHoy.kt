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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
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
import com.digest.app.ui.componentes.TarjetaSenales
import com.digest.app.ui.directorio.PantallaDirectorio
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
    var directorioAbierto by remember { mutableStateOf(false) }

    // Al entrar en la pestaña se comprueban las tomas del día y se reevalúan
    // las señales: así un síntoma añadido hace un momento aparece con su
    // aviso sin tener que reiniciar la app.
    LaunchedEffect(Unit) {
        viewModel.prepararHoy()
        viewModel.recalcularSenales()
    }

    // Confirmación de cada registro. Marcar un síntoma sin ver nada pasar
    // deja la duda de si se ha guardado, y el paciente acaba marcándolo
    // otra vez por si acaso.
    //
    // `collectLatest` y no `collect`: si llega un registro nuevo mientras el
    // aviso anterior sigue en pantalla, el anterior se retira y se muestra
    // el nuevo. Con `collect` el paciente leería «ningún aviso» después de
    // haber marcado algo que sí lo ha disparado, porque el texto llega
    // tarde, describiendo lo que ya no es.
    val avisos = remember { SnackbarHostState() }
    val contexto = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.resultado.collectLatest { resultado ->
            avisos.showSnackbar(mensajeDe(contexto, resultado))
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(avisos) },
    ) { relleno ->
        ContenidoHoy(
            relleno = relleno,
            estado = estado,
            alAbrirDirectorio = { directorioAbierto = true },
            alMarcarTomada = viewModel::marcarTomada,
            alMarcarOmitida = viewModel::marcarOmitida,
            alReponer = viewModel::volverAPendiente,
            alAlternarSintoma = viewModel::alternarSintoma,
        )
    }

    if (directorioAbierto) {
        PantallaDirectorio(
            condicion = estado.perfil?.condicion,
            alCerrar = { directorioAbierto = false },
        )
    }
}

/**
 * Traduce el resultado de un registro al texto que se confirma.
 *
 * No es una función `@Composable` a propósito: se invoca desde dentro de
 * una corrutina, fuera del ciclo de recomposición, y ahí no se puede leer
 * un recurso con `stringResource`.
 */
private fun mensajeDe(contexto: Context, resultado: ResultadoRegistro): String =
    when (resultado) {
        is ResultadoRegistro.SinSenales ->
            contexto.getString(R.string.registro_sin_senales)

        is ResultadoRegistro.ConSenales -> if (resultado.urgentes > 0) {
            contexto.getString(R.string.registro_urgente, resultado.titulo)
        } else {
            // `resources.getQuantityString` y no `getString`: en español
            // "1 aviso" y "3 avisos" se escriben distinto, y la app solo
            // está en español, así que no hace falta un separador.
            contexto.resources.getQuantityString(
                R.plurals.registro_senales,
                resultado.total,
                resultado.total,
                resultado.titulo,
            )
        }
    }

@Composable
private fun ContenidoHoy(
    relleno: PaddingValues,
    estado: EstadoHoy,
    alAbrirDirectorio: () -> Unit,
    alMarcarTomada: (TomaConDetalle) -> Unit,
    alMarcarOmitida: (TomaConDetalle) -> Unit,
    alReponer: (TomaConDetalle) -> Unit,
    alAlternarSintoma: (TipoSintoma) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = relleno.calculateTopPadding() + 16.dp,
            bottom = relleno.calculateBottomPadding() + 16.dp,
        ),
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
                    alTomar = { alMarcarTomada(proxima) },
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
                alTomar = { alMarcarTomada(toma) },
                alOmitir = { alMarcarOmitida(toma) },
                alReponer = { alReponer(toma) },
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
                alAlternar = alAlternarSintoma,
            )
            Text(
                text = stringResource(R.string.hoy_gravedad_ayuda),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Las señales van al final, debajo de los síntomas.
        //
        // Estaban arriba, debajo de la cabecera, y ese es el peor sitio
        // posible: la tarjeta es larga (qué ha pasado, por qué salta, y
        // el descargo de que la app no diagnostica) y empujaba hacia abajo
        // lo único que no se puede dejar para más tarde, que es la toma
        // de medicación. Arriba obligaba a apartar la vista de la
        // medicación para leer un aviso.
        item {
            TarjetaSenales(
                senales = estado.senales,
                diasDesdeUltimaConsulta = estado.diasDesdeUltimaConsulta,
                alVerDirectorio = alAbrirDirectorio,
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