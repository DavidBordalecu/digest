package com.digest.app.ui.perfil

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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digest.app.R
import com.digest.app.model.TipoCirugia
import com.digest.app.ui.componentes.CabeceraPantalla
import com.digest.app.ui.directorio.PantallaDirectorio
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Ficha del paciente.
 *
 * Concentra el diagnóstico y, en el caso de Crohn, los antecedentes
 * quirúrgicos, que condicionan el tránsito intestinal y por tanto cómo se
 * interpretan los síntomas y la hidratación.
 */
@Composable
fun PantallaPerfil(modifier: Modifier = Modifier) {
    val viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.fabrica)
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    var peso by remember(estado.perfil) {
        mutableStateOf(estado.perfil?.pesoKg?.toString().orEmpty())
    }
    var altura by remember(estado.perfil) {
        mutableStateOf(estado.perfil?.alturaCm?.toString().orEmpty())
    }
    var intervalo by remember(estado.perfil) {
        mutableStateOf((estado.perfil?.intervaloConsultaMeses ?: 6).toString())
    }
    var directorioAbierto by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CabeceraPantalla(estado.perfil)

        estado.perfil?.let { perfil ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.perfil_condicion),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(perfil.condicion.displayName),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (perfil.afectaciones.isNotEmpty()) {
                        Text(
                            text = perfil.afectaciones
                                .map { stringResource(it.displayName) }
                                .joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (perfil.tieneEstomia) {
                        Text(
                            text = stringResource(R.string.perfil_tiene_estomia),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.perfil_medidas),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = peso,
                        onValueChange = { peso = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.bienvenida_peso)) },
                        suffix = { Text("kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = altura,
                        onValueChange = { altura = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.bienvenida_altura)) },
                        suffix = { Text("cm") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                val imc = estado.imc
                if (imc != null) {
                    Text(
                        text = stringResource(R.string.perfil_imc, imc),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = { viewModel.guardarMedidas(peso.toDoubleOrNull(), altura.toDoubleOrNull()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.perfil_guardar)) }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.perfil_ultima_consulta),
                    style = MaterialTheme.typography.titleMedium,
                )

                val perfilActual = estado.perfil
                val ultimaConsulta = perfilActual?.ultimaConsulta
                if (ultimaConsulta == null) {
                    Text(
                        text = stringResource(R.string.perfil_consulta_nunca),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val dias = ChronoUnit.DAYS.between(ultimaConsulta, LocalDate.now())
                    Text(
                        text = stringResource(R.string.senales_ultima_consulta, dias),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = ultimaConsulta.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                FilledTonalButton(
                    onClick = viewModel::registrarConsultaHoy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.perfil_registrar_consulta)) }

                OutlinedTextField(
                    value = intervalo,
                    onValueChange = { intervalo = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.perfil_intervalo_consulta)) },
                    suffix = { Text("meses") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedButton(
                    onClick = { intervalo.toIntOrNull()?.let(viewModel::guardarIntervalo) },
                    enabled = intervalo.toIntOrNull() in 1..36,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.perfil_guardar_intervalo)) }

                OutlinedButton(
                    onClick = { directorioAbierto = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.directorio_titulo)) }
            }
        }

        if (directorioAbierto) {
            PantallaDirectorio(
                condicion = estado.perfil?.condicion,
                alCerrar = { directorioAbierto = false },
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.perfil_cirugias),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.perfil_cirugias_ayuda),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (estado.cirugias.isEmpty()) {
                    Text(
                        text = stringResource(R.string.perfil_cirugias_vacio),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                estado.cirugias.forEach { cirugia ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(cirugia.tipo.displayName))
                        cirugia.fecha?.let {
                            Text(
                                text = it.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.perfil_agregar_cirugia),
                    style = MaterialTheme.typography.titleMedium,
                )
                TipoCirugia.entries.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fila.forEach { tipo ->
                            OutlinedButton(onClick = { viewModel.anadirCirugia(tipo) }) {
                                Text(
                                    text = stringResource(tipo.displayName),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}