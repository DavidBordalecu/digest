package com.digest.app.ui.medicamentos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.digest.app.R
import com.digest.app.data.local.entity.HorarioEntity
import com.digest.app.data.local.entity.MedicamentoEntity
import com.digest.app.model.TipoPresentacion
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Alta de medicación.
 *
 * Se pide lo mínimo imprescindible (nombre, dosis y hora) porque un paciente
 * con fatiga no rellena formularios largos, y el alta de un fármaco es algo
 * que hace a menudo. Los horarios adicionales se añaden luego desde la ficha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorMedicamento(
    inicial: MedicamentoEntity,
    alGuardar: (MedicamentoEntity, LocalTime?, TipoPresentacion, Int) -> Unit,
    alCerrar: () -> Unit,
) {
    var nombre by remember { mutableStateOf(inicial.nombre) }
    var dosis by remember { mutableStateOf(inicial.dosis.orEmpty()) }
    var instrucciones by remember { mutableStateOf(inicial.instrucciones.orEmpty()) }
    var presentacion by remember { mutableStateOf(inicial.presentacion) }
    var dias by remember { mutableStateOf(HorarioEntity.TODOS_LOS_DIAS) }
    var mostrarReloj by remember { mutableStateOf(false) }

    val reloj = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.editor_titulo)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.editor_nombre)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dosis,
                    onValueChange = { dosis = it },
                    label = { Text(stringResource(R.string.editor_dosis)) },
                    placeholder = { Text(stringResource(R.string.editor_dosis_ejemplo)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = instrucciones,
                    onValueChange = { instrucciones = it },
                    label = { Text(stringResource(R.string.editor_instrucciones)) },
                    placeholder = { Text(stringResource(R.string.editor_instrucciones_ejemplo)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoPresentacion.entries.forEach { tipo ->
                        FilterChip(
                            selected = presentacion == tipo,
                            onClick = { presentacion = tipo },
                            label = { Text(stringResource(tipo.displayName)) },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.editor_hora),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        onClick = { mostrarReloj = !mostrarReloj },
                        label = {
                            Text(
                                if (mostrarReloj) {
                                    "%02d:%02d".format(reloj.hour, reloj.minute)
                                } else {
                                    stringResource(R.string.editor_elegir_hora)
                                },
                            )
                        },
                    )
                }

                if (mostrarReloj) {
                    TimePicker(state = reloj)
                }

                Text(
                    text = stringResource(R.string.editor_dias),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.editor_todos_los_dias),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DiasSemana(diasSeleccionados = dias, alCambiar = { dias = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    alGuardar(
                        inicial.copy(
                            nombre = nombre.trim(),
                            dosis = dosis.ifBlank { null },
                            instrucciones = instrucciones.ifBlank { null },
                        ),
                        LocalTime.of(reloj.hour, reloj.minute),
                        presentacion,
                        dias,
                    )
                },
                enabled = nombre.isNotBlank(),
            ) { Text(stringResource(R.string.editor_guardar)) }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) { Text(stringResource(R.string.editor_cancelar)) }
        },
    )
}

/** Abreviaturas españolas: lunes → L, martes → M, miércoles → X, jueves → J. */
private val ABREVIATURA_DIA = mapOf(
    DayOfWeek.MONDAY to "L",
    DayOfWeek.TUESDAY to "M",
    DayOfWeek.WEDNESDAY to "X",
    DayOfWeek.THURSDAY to "J",
    DayOfWeek.FRIDAY to "V",
    DayOfWeek.SATURDAY to "S",
    DayOfWeek.SUNDAY to "D",
)

/**
 * Selector de días de la semana.
 *
 * Por defecto salen los siete marcados, que es el caso mayoritario: un
 * fármaco diario. Desmarcar uno lo convierte en un horario concreto.
 */
@Composable
private fun DiasSemana(diasSeleccionados: Int, alCambiar: (Int) -> Unit) {
    val todos = HorarioEntity.TODOS_LOS_DIAS

    fun alternar(dia: DayOfWeek) {
        if (diasSeleccionados == todos) {
            // Partiendo de "todos", quitar uno deja los otros seis marcados.
            alCambiar(HorarioEntity.mascaraDe(DayOfWeek.entries.toSet() - dia))
        } else {
            val otros = DayOfWeek.entries.filter { it != dia }
            val nueva = otros.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
            alCambiar(
                if (nueva == HorarioEntity.mascaraDe(DayOfWeek.entries.toSet())) todos else nueva,
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.forEach { dia ->
            val activo = diasSeleccionados == todos ||
                (diasSeleccionados and (1 shl (dia.value - 1))) != 0
            FilterChip(
                selected = activo,
                onClick = { alternar(dia) },
                label = { Text(ABREVIATURA_DIA[dia].orEmpty()) },
            )
        }
    }
}