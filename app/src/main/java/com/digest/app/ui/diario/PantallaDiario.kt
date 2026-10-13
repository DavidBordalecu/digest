package com.digest.app.ui.diario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.digest.app.ui.componentes.CabeceraPantalla

/**
 * Diario del día.
 *
 * Responde a las dos preguntas centrales del paciente: si la medicación le
 * está mejorando o no, y cómo se siente. Se rellena en un minuto porque el
 * objetivo es que se use a diario, no que quede perfecto.
 */
@Composable
fun PantallaDiario(modifier: Modifier = Modifier) {
    val viewModel: DiarioViewModel = viewModel(factory = DiarioViewModel.fabrica)
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CabeceraPantalla(estado.perfil)

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
    }
}

/**
 * Fila de opciones excluyentes.
 *
 * Se recorre en dos filas porque las listas largas (por ejemplo, el estado
 * del brote) no caben en una sola sin comprimir los textos.
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