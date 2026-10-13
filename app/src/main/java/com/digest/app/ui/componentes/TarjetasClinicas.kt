package com.digest.app.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digest.app.R
import com.digest.app.domain.NivelUrgencia
import com.digest.app.domain.SenalClinica

/**
 * Aviso de señal de alarma.
 *
 * El texto es deliberadamente prudente: la app **no diagnostica**, reconoce
 * patrones que obligan a consultar. Por eso siempre va acompañado del
 * descargo y del enlace al directorio, en lugar de una conclusión cerrada.
 *
 * Solo se muestra si hay alguna señal activa: un aviso permanente que no
 * dice nada acaba ignorándose, y entonces no sirve de nada.
 */
@Composable
fun TarjetaSenales(
    senales: List<SenalClinica>,
    diasDesdeUltimaConsulta: Int?,
    alVerDirectorio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (senales.isEmpty()) return

    val urgente = senales.any { it.nivel == NivelUrgencia.URGENTE }
    val principal = senales.first()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (urgente) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            },
        ),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_alerta),
                    contentDescription = null,
                    tint = if (urgente) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(
                        if (urgente) R.string.senales_urgente else R.string.senales_pronto,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (urgente) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    modifier = Modifier.padding(start = 10.dp),
                )
            }

            senales.forEach { senal ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = senal.titulo,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (urgente) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        },
                    )
                    Text(
                        text = senal.explicacion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (urgente) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        },
                    )
                    Text(
                        text = "Por qué: ${senal.porque}",
                        style = MaterialTheme.typography.labelSmall,
                        color = (if (urgente) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        }).copy(alpha = 0.75f),
                    )
                }
            }

            Text(
                text = stringResource(R.string.senales_descargo),
                style = MaterialTheme.typography.bodyMedium,
                color = (if (urgente) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }).copy(alpha = 0.85f),
            )

            FilledTonalButton(
                onClick = alVerDirectorio,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.senales_ver_directorio)) }

            diasDesdeUltimaConsulta?.let {
                Text(
                    text = stringResource(R.string.senales_ultima_consulta, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Ficha de un profesional del directorio.
 *
 * Muestra las iniciales en lugar de una foto porque los datos son ficticios:
 * una foto de stock de una persona concreta sería engañoso.
 */
@Composable
fun FichaProfesional(
    iniciales: String,
    nombre: String,
    centro: String,
    ciudad: String,
    especialidad: String,
    dias: String,
    horario: String,
    telefono: String,
    aceptaOnline: Boolean,
    especializado: Boolean,
    alLlamar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // El color por defecto del tema tira a rosado y choca con la
            // paleta verde azulado; se fuerza el fondo de superficie.
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = iniciales,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = especialidad,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (especializado) {
                Text(
                    text = stringResource(R.string.profesional_especializado),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Text(
                text = "$centro · $ciudad",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$dias, $horario",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (aceptaOnline) {
                Text(
                    text = stringResource(R.string.profesional_cita_online),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            TextButton(onClick = { alLlamar(telefono) }) {
                Text(stringResource(R.string.profesional_llamar, telefono))
            }
        }
    }
}