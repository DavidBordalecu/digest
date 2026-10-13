package com.digest.app.ui.directorio

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.digest.app.R
import com.digest.app.domain.DirectorioProfesionales
import com.digest.app.domain.Especialidad
import com.digest.app.domain.Profesional
import com.digest.app.ui.componentes.FichaProfesional

/**
 * Directorio de profesionales.
 *
 * Se abre desde el aviso de señales de alarma, y también se puede llegar
 * desde Perfil. El orden pone primero a quien se especializa en enfermedad
 * inflamatoria intestinal: es la diferencia entre una consulta donde hay que
 * explicar el diagnóstico desde cero y una donde no.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDirectorio(
    condicion: com.digest.app.model.Condicion?,
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var especialidad by remember { mutableStateOf<Especialidad?>(null) }
    var busqueda by remember { mutableStateOf("") }
    val contexto = LocalContext.current

    val lista = remember(especialidad, busqueda, condicion) {
        val base = when {
            busqueda.isNotBlank() -> DirectorioProfesionales.buscar(busqueda)
            especialidad != null -> DirectorioProfesionales.recomendados(
                condicion ?: com.digest.app.model.Condicion.COLITIS_ULCEROSA,
                especialidad!!,
            )

            else -> DirectorioProfesionales.todos
        }
        base.sortedWith(
            compareByDescending<Profesional> { it.especializadoEnEii }
                .thenBy { it.ciudad },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.directorio_titulo)) },
                navigationIcon = {
                    IconButton(onClick = alCerrar) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cerrar),
                            contentDescription = stringResource(R.string.cerrar),
                        )
                    }
                },
            )
        },
    ) { relleno ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = relleno.calculateTopPadding() + 8.dp,
                bottom = relleno.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.directorio_aviso_ficticio),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    label = { Text(stringResource(R.string.directorio_buscar)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = especialidad == null && busqueda.isBlank(),
                        onClick = {
                            especialidad = null
                            busqueda = ""
                        },
                        label = { Text(stringResource(R.string.directorio_todos)) },
                    )
                    FilterChip(
                        selected = especialidad == Especialidad.GASTROENTEROLOGIA,
                        onClick = { especialidad = Especialidad.GASTROENTEROLOGIA },
                        label = { Text(stringResource(R.string.directorio_gastro)) },
                    )
                    FilterChip(
                        selected = especialidad == Especialidad.NUTRICION,
                        onClick = { especialidad = Especialidad.NUTRICION },
                        label = { Text(stringResource(R.string.directorio_nutricion)) },
                    )
                }
            }

            items(lista, key = { it.nombre }) { profesional ->
                FichaProfesional(
                    iniciales = profesional.iniciales,
                    nombre = profesional.nombre,
                    centro = profesional.centro,
                    ciudad = profesional.ciudad,
                    especialidad = stringResource(
                        if (profesional.especialidad == Especialidad.GASTROENTEROLOGIA) {
                            R.string.directorio_gastro
                        } else {
                            R.string.directorio_nutricion
                        },
                    ),
                    dias = profesional.dias,
                    horario = profesional.horario,
                    telefono = profesional.telefono,
                    aceptaOnline = profesional.admiteCitaOnline,
                    especializado = profesional.especializadoEnEii,
                    alLlamar = { telefono ->
                        runCatching {
                            contexto.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefono")),
                            )
                        }
                    },
                )
            }

            if (lista.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.directorio_sin_resultados),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
        }
    }
}