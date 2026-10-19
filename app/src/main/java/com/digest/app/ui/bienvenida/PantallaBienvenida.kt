package com.digest.app.ui.bienvenida

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.digest.app.R
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.model.Afectacion
import com.digest.app.model.Condicion
import com.digest.app.model.TipoDieta

/**
 * Alta inicial.
 *
 * Lo único imprescindible es elegir la condición: de ella depende qué
 * cuestionario de síntomas se muestra después. Todo lo demás es opcional y se
 * puede completar luego, porque pedir demasiado en el primer arranque hace
 * que la gente abandone.
 */
@Composable
fun PantallaBienvenida(
    alGuardar: (PerfilEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var condicion by remember { mutableStateOf<Condicion?>(null) }
    var diagnostico by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var dieta by remember { mutableStateOf<TipoDieta?>(null) }
    var afectaIntestinoDelgado by remember { mutableStateOf(false) }
    val afectaciones = remember { mutableStateListOf<Afectacion>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.bienvenida_titulo),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.bienvenida_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.bienvenida_condicion),
            style = MaterialTheme.typography.titleMedium,
        )

        Condicion.entries.forEach { opcion ->
            TarjetaCondicion(
                condicion = opcion,
                seleccionada = condicion == opcion,
                alSeleccionar = {
                    condicion = opcion
                    // La localización y la cirugía dependen de la condición:
                    // se reinician para no arrastrar datos de otra enfermedad.
                    afectaciones.clear()
                    afectaIntestinoDelgado = opcion.afectaIntestinoDelgado
                },
            )
        }

        // --- Datos opcionales ---
        OutlinedTextField(
            value = diagnostico,
            onValueChange = { diagnostico = it },
            label = { Text(stringResource(R.string.bienvenida_fecha_diagnostico)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = peso,
                onValueChange = { peso = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(R.string.bienvenida_peso)) },
                suffix = { Text("kg") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = altura,
                onValueChange = { altura = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(R.string.bienvenida_altura)) },
                suffix = { Text("cm") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }

        val condicionActual = condicion
        // El colon irritable no tiene zonas de afectación ni antecedentes
        // quirúrgicos que registrar: preguntarlos solo confunde.
        if (condicionActual != null && condicionActual.esInflamatoria) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.bienvenida_afectacion),
                style = MaterialTheme.typography.titleMedium,
            )
            Afectacion.para(condicionActual).forEach { zona ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = zona in afectaciones,
                        onCheckedChange = { marcado ->
                            if (marcado) {
                                if (zona !in afectaciones) afectaciones += zona
                                if (zona == Afectacion.ILEITIS || zona == Afectacion.ILEOCOLITIS) {
                                    afectaIntestinoDelgado = true
                                }
                            } else {
                                afectaciones -= zona
                            }
                        },
                    )
                    Text(stringResource(zona.displayName))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.bienvenida_diagnostico_quirurgico),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.bienvenida_diagnostico_quirurgico_ayuda),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = afectaIntestinoDelgado,
                    onCheckedChange = { afectaIntestinoDelgado = it },
                )
                Text(stringResource(R.string.bienvenida_ileostomia_colostomia))
            }
        }

        Button(
            onClick = {
                alGuardar(
                    PerfilEntity(
                        condicion = condicionActual ?: Condicion.INDETERMINADA,
                        afectaciones = afectaciones.toList(),
                        pesoKg = peso.toDoubleOrNull(),
                        alturaCm = altura.toDoubleOrNull(),
                        tieneIleostomia = afectaIntestinoDelgado,
                        dieta = dieta,
                    ),
                )
            },
            enabled = condicion != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.bienvenida_empezar))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TarjetaCondicion(
    condicion: Condicion,
    seleccionada: Boolean,
    alSeleccionar: () -> Unit,
) {
    Card(
        onClick = alSeleccionar,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(condicion.displayName),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    when (condicion) {
                        Condicion.COLITIS_ULCEROSA -> R.string.bienvenida_cu_desc
                        Condicion.CROHN -> R.string.bienvenida_crohn_desc
                        Condicion.INDETERMINADA -> R.string.bienvenida_indeterminada_desc
                        Condicion.COLON_IRRITABLE -> R.string.bienvenida_colon_irritable_desc
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}