package com.digest.app.ui.secciones

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digest.app.R
import com.digest.app.data.local.entity.ComidaEntity
import com.digest.app.ui.componentes.CabeceraPantalla
import com.digest.app.ui.componentes.Vacio

/**
 * Registro de comidas.
 *
 * Guarda qué y a qué hora, que es lo que permite después cruzar la comida
 * con los síntomas que aparecen horas más tarde.
 */
@Composable
fun PantallaComidas(modifier: Modifier = Modifier) {
    val viewModel: ComidasViewModel = viewModel(factory = ComidasViewModel.fabrica)
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var dialogoAbierto by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogoAbierto = true }) {
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

            if (estado.comidas.isEmpty()) {
                item {
                    Vacio(
                        texto = stringResource(R.string.comidas_vacio),
                        detalle = stringResource(R.string.comidas_vacio_detalle),
                    )
                }
            }

            items(estado.comidas, key = { it.id }) { comida ->
                FilaComida(comida, alBorrar = { viewModel.borrar(comida.id) })
            }
        }
    }

    if (dialogoAbierto) {
        DialogoComida(
            tipoSugerido = estado.tipoSugerido,
            alimentosRecientes = estado.alimentosRecientes,
            alGuardar = { tipo, alimentos, hora ->
                viewModel.guardarComida(tipo, alimentos, hora)
                dialogoAbierto = false
            },
            alCerrar = { dialogoAbierto = false },
        )
    }
}

@Composable
private fun FilaComida(comida: ComidaEntity, alBorrar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(comida.tipo.displayName),
                    style = MaterialTheme.typography.titleMedium,
                )
                comida.alimentos?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = comida.hora.toString().take(5),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}