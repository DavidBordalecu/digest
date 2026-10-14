package com.digest.app.ui.hoy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digest.app.R

/**
 * Pregunta de control posterior a la comida, dentro de la pantalla de hoy.
 *
 * Es el mismo control que salta en la notificación, duplicado. La
 * notificación puede no llegar —sin permiso, con el móvil en silencio, o
 * porque el paciente abrió la app antes de tiempo— y la pregunta no puede
 * quedarse sin hacer solo por eso.
 *
 * Al contestar «sí» la tarjeta desaparece, pero los síntomas que marque el
 * paciente un poco más abajo son los que quedan en el diario: no se intenta
 * adivinar qué ha notado.
 */
@Composable
fun TarjetaControlComida(
    controles: List<ControlPendiente>,
    alResponder: (ControlPendiente, huboCambios: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (controles.isEmpty()) return

    val control = controles.first()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.control_tarjeta_titulo),
                style = MaterialTheme.typography.labelLarge,
            )

            Text(
                text = stringResource(R.string.control_tarjeta_pregunta),
                style = MaterialTheme.typography.titleMedium,
            )

            // Se dice qué se comió: «¿has notado algo desde el pollo con
            // arroz?» es mucho más fácil de responder que una pregunta
            // genérica, y además es lo que le da sentido al control.
            val referencia = control.alimentos?.takeIf { it.isNotBlank() }
            if (referencia != null) {
                Text(
                    text = referencia,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Text(
                text = stringResource(
                    R.string.control_hora_comida,
                    control.horaComida.toString().take(5),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (controles.size > 1) {
                Text(
                    text = stringResource(R.string.control_pendientes, controles.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                OutlinedButton(
                    onClick = { alResponder(control, false) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.control_no)) }

                // El «sí» deja paso a marcar los síntomas de abajo, que es de
                // donde se guardan.
                OutlinedButton(
                    onClick = { alResponder(control, true) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.control_si)) }
            }
        }
    }
}