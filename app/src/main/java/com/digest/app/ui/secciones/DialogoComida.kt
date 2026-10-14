package com.digest.app.ui.secciones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.digest.app.R
import com.digest.app.model.TipoComida
import java.time.LocalTime

/**
 * Registro de una comida.
 *
 * Antes el botón `+` guardaba directamente un tipo deducido de la hora y
 * nada más, así que el registro decía «Almuerzo» y no qué se había comido,
 * que es justo el dato que después se cruza con los síntomas de las horas
 * siguientes.
 *
 * El campo de texto es el núcleo, no un extra: una comida sin contenudo no
 * sirve para nada. Se rellena en pocos segundos porque el paciente repite
 * comidas con mucha frecuencia: se ofrecen las últimas que anotó y basta
 * con tocar una.
 */
@Composable
fun DialogoComida(
    tipoSugerido: TipoComida,
    alimentosRecientes: List<String>,
    alGuardar: (tipo: TipoComida, alimentos: String?, hora: LocalTime) -> Unit,
    alCerrar: () -> Unit,
) {
    var tipo by remember { mutableStateOf(tipoSugerido) }
    var alimentos by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf(LocalTime.now()) }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.comida_registrar)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = alimentos,
                    onValueChange = { alimentos = it },
                    label = { Text(stringResource(R.string.comida_alimentos)) },
                    placeholder = { Text(stringResource(R.string.comida_alimentos_pista)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Lo último que comió: un toque y listo. Solo se ofrecen
                // comidas de días anteriores, no las de hoy, para que la
                // lista no se llene de lo que ya está apuntado.
                if (alimentosRecientes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.comida_recientes),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    alimentosRecientes.chunked(2).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fila.forEach { alimento ->
                                AssistChip(
                                    onClick = {
                                        alimentos =
                                            if (alimentos.isBlank()) alimento else "$alimentos, $alimento"
                                    },
                                    label = { Text(alimento) },
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.comida_tipo),
                    style = MaterialTheme.typography.titleSmall,
                )
                TipoComida.entries.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fila.forEach { opcion ->
                            FilterChip(
                                selected = tipo == opcion,
                                onClick = { tipo = opcion },
                                label = { Text(stringResource(opcion.displayName)) },
                            )
                        }
                    }
                }

                SelectorHora(hora = hora, alCambiar = { hora = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    alGuardar(tipo, alimentos.trim().ifBlank { null }, hora)
                },
                // Guardar una comida sin decir qué se ha comido repetiría
                // justo el problema que este diálogo viene a resolver.
                enabled = alimentos.isNotBlank(),
            ) { Text(stringResource(R.string.comida_guardar)) }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) { Text(stringResource(R.string.cerrar)) }
        },
    )
}

/**
 * Hora de la comida, con dos botones en vez de un campo de texto.
 *
 * Apuntar la comida cuando te acuerdas, que es lo habitual, y no a la hora
 * exacta: subirla un poco es normal y tener que escribir «21:47» es lo que
 * hace que la app se quede sin usar.
 */
@Composable
private fun SelectorHora(hora: LocalTime, alCambiar: (LocalTime) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.comida_hora, hora.toString().take(5)),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            TextButton(onClick = { alCambiar(hora.minusMinutes(15)) }) { Text("−15") }
            TextButton(onClick = { alCambiar(hora.plusMinutes(15)) }) { Text("+15") }
            TextButton(onClick = { alCambiar(LocalTime.now()) }) {
                Text(stringResource(R.string.comida_hora_ahora))
            }
        }
    }
}