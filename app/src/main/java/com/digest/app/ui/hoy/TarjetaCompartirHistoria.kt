package com.digest.app.ui.hoy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.digest.app.DigestApplication
import com.digest.app.R
import com.digest.app.ui.diario.PeriodoInforme
import com.digest.app.ui.compartir.CompartirHistoria
import kotlinx.coroutines.launch

/**
 * Botón de compartir la historia entera.
 *
 * Va en la pantalla de Hoy y no escondida en el Diario porque es la acción
 * que el paciente busca justo cuando viene de la consulta, y si hay que
 * rebuscarlo entre cuestionarios no lo encuentra. Está al final del todo,
 * después de registrar el día: primero lo que importa del día, después lo
 * que es para el médico.
 */
@Composable
fun TarjetaCompartirHistoria(
    hayDocumentos: Boolean,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val aplicacion = contexto.applicationContext as DigestApplication
    val alcance = rememberCoroutineScope()

    var periodo by remember { mutableStateOf(PeriodoInforme.MES) }
    var generando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.historia_titulo),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.historia_explicacion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodoInforme.entries.forEach { opcion ->
                    FilterChip(
                        selected = periodo == opcion,
                        onClick = { periodo = opcion },
                        label = { Text(stringResource(opcion.etiqueta)) },
                    )
                }
            }

            if (generando) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = stringResource(R.string.historia_generando),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Button(
                    onClick = {
                        generando = true
                        error = false
                        alcance.launch {
                            val ok = runCatching {
                                CompartirHistoria.compartir(
                                    contexto = contexto,
                                    repositorio = aplicacion.repositorio,
                                    periodo = periodo,
                                )
                            }.isSuccess

                            generando = false
                            // Si falla al preparar, se avisa. Si lo que
                            // falla es el menú de compartir —que no depende
                            // de la app— no se dice nada: el paciente ya
                            // ve en pantalla qué ha pasado.
                            error = !ok
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.historia_compartir)) }

                if (!hayDocumentos) {
                    Text(
                        text = stringResource(R.string.historia_solo_texto),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (error) {
                    Text(
                        text = stringResource(R.string.historia_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}