package com.digest.app.ui.medicamentos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digest.app.R
import com.digest.app.data.local.entity.MedicamentoEntity
import com.digest.app.ui.componentes.CabeceraPantalla
import com.digest.app.ui.componentes.Vacio

/**
 * Listado de medicación y suplementos.
 *
 * Todos comparten agenda porque el paciente no distingue a la hora de
 * tomarlos: lo que le importa es el horario, no si es unscyank o el hierro.
 */
@Composable
fun PantallaMedicamentos(modifier: Modifier = Modifier) {
    val viewModel: MedicamentosViewModel = viewModel(factory = MedicamentosViewModel.fabrica)
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::nuevoMedicamento) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { relleno ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = relleno.calculateTopPadding() + 16.dp,
                bottom = relleno.calculateBottomPadding() + 80.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { CabeceraPantalla(estado.perfil) }

            if (estado.medicamentos.isEmpty()) {
                item {
                    Vacio(
                        texto = stringResource(R.string.medicamentos_vacio),
                        detalle = stringResource(R.string.medicamentos_vacio_detalle),
                    )
                }
            }

            items(estado.medicamentos, key = { it.id }) { medicamento ->
                FilaMedicamento(
                    medicamento = medicamento,
                    horarios = estado.horariosPorMedicamento[medicamento.id].orEmpty(),
                    alPulsar = { viewModel.abrirEditor(medicamento) },
                )
            }
        }
    }

    // El editor se monta encima: la lista queda intacta mientras se edita.
    val enEdicion by viewModel.enEdicion.collectAsStateWithLifecycle()
    enEdicion?.let { medicamento ->
        EditorMedicamento(
            inicial = medicamento,
            alGuardar = viewModel::guardar,
            alCerrar = viewModel::cerrarEditor,
        )
    }
}

@Composable
private fun FilaMedicamento(
    medicamento: MedicamentoEntity,
    horarios: List<com.digest.app.data.local.entity.HorarioEntity>,
    alPulsar: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = alPulsar),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = medicamento.nombre,
                    style = MaterialTheme.typography.titleMedium,
                )
                medicamento.dosis?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (medicamento.instrucciones != null) {
                    Text(
                        text = medicamento.instrucciones,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(medicamento.presentacion.displayName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = horarios
                        .joinToString(" · ") { it.hora.toString().take(5) }
                        .ifEmpty { stringResource(R.string.medicamentos_sin_horario) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}