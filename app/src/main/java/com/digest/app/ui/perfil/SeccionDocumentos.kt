package com.digest.app.ui.perfil

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.digest.app.R
import com.digest.app.data.local.entity.DocumentoEntity
import com.digest.app.model.TipoDocumento
import com.digest.app.ui.componentes.Vacio
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Carpeta de informes médicos del paciente.
 *
 * Va en Perfil y no en el Diario porque no es un registro del día: es
 * documentación que dura años y que se consulta en la consulta, igual que
 * el resto de la historia.
 */
@Composable
fun SeccionDocumentos(
    documentos: List<DocumentoEntity>,
    alAdjuntar: (
        origen: android.net.Uri,
        tipo: TipoDocumento,
        fecha: LocalDate?,
        notas: String?,
        nombre: String?,
        alTerminar: (Boolean) -> Unit,
    ) -> Unit,
    archivoDe: (DocumentoEntity) -> java.io.File,
    alBorrar: (DocumentoEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    var pendienteDeMetadatos by remember { mutableStateOf<android.net.Uri?>(null) }

    val elegirArchivo = rememberLauncherForActivityResult(
        // OpenDocument en vez de GetContent: el que segundo da acceso
        // únicamente a ese archivo, y además permite pedir el permiso
        // persistente. GetContent no lo concede y el archivo dejaría de
        // poder leerse al reiniciar el dispositivo.
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendienteDeMetadatos = uri }

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.documentos_titulo),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.documentos_explicacion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (documentos.isEmpty()) {
                Vacio(
                    texto = stringResource(R.string.documentos_vacio),
                    detalle = stringResource(R.string.documentos_vacio_detalle),
                )
            } else {
                documentos.forEach { documento ->
                    FilaDocumento(
                        documento = documento,
                        alAbrir = { abrirDocumento(contexto, archivoDe(documento), documento.mime) },
                        alCompartir = { compartirUno(contexto, archivoDe(documento), documento.mime, documento.nombre) },
                        alBorrar = { alBorrar(documento) },
                    )
                }

                // Aquí solo se mandan los archivos. El resumen escrito se
                // envía desde «Compartir mi historia con el médico», en la
                // pestaña Hoy: mandar por separado el texto y los PDF es
                // justo lo que hace que al final se envíe solo una cosa.
                OutlinedButton(
                    onClick = { compartirVarios(contexto, documentos, archivoDe) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.documento_compartir_solo_este)) }
            }

            FilledTonalButton(
                onClick = { elegirArchivo.launch(TIPOS_ACEPTADOS) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.documento_anadir)) }
        }
    }

    pendienteDeMetadatos?.let { uri ->
        DialogoMetadatos(
            alGuardar = { tipo, fecha, notas, nombre ->
                alAdjuntar(uri, tipo, fecha, notas, nombre) { ok ->
                    if (ok) pendienteDeMetadatos = null
                }
            },
            alCerrar = { pendienteDeMetadatos = null },
        )
    }
}

@Composable
private fun FilaDocumento(
    documento: DocumentoEntity,
    alAbrir: () -> Unit,
    alCompartir: () -> Unit,
    alBorrar: () -> Unit,
) {
    var confirmarBorrado by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alAbrir)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = documento.nombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = buildString {
                    append(stringResource(documento.tipo.displayName))
                    append(" · ")
                    append(
                        documento.fechaDocumento?.format(FECHA)
                            ?: stringResource(R.string.documento_sin_fecha),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            documento.notas?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        MenuInforme(
            alCompartir = alCompartir,
            alBorrar = { confirmarBorrado = true },
        )
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text(stringResource(R.string.documento_borrar)) },
            text = {
                Text(stringResource(R.string.documento_borrar_confirmacion, documento.nombre))
            },
            confirmButton = {
                Button(onClick = { confirmarBorrado = false; alBorrar() }) {
                    Text(stringResource(R.string.documento_borrar))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = false }) {
                    Text(stringResource(R.string.cerrar))
                }
            },
        )
    }
}

/** Menú de tres puntos con las acciones de un informe. */
@Composable
private fun MenuInforme(
    alCompartir: () -> Unit,
    alBorrar: () -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    Column {
        IconButton(onClick = { abierto = true }) {
            Text("⋮", style = MaterialTheme.typography.titleLarge)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.documento_compartir)) },
                onClick = { abierto = false; alCompartir() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.documento_borrar)) },
                onClick = { abierto = false; alBorrar() },
            )
        }
    }
}

/**
 * Metadatos del informe.
 *
 * El tipo y la fecha son lo que convierte un PDF suelto en algo utilizable:
 * son los dos datos que permiten responder «¿cuándo fue tu última
 * colonoscopia?» sin abrir nada.
 */
@Composable
private fun DialogoMetadatos(
    alGuardar: (TipoDocumento, LocalDate?, String?, String?) -> Unit,
    alCerrar: () -> Unit,
) {
    var tipo by remember { mutableStateOf(TipoDocumento.ANALITICA) }
    var nombre by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    val fechaValida = remember(fecha) {
        runCatching { LocalDate.parse(fecha, FECHAS_ACEPTADAS) }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.documento_anadir)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.documento_nombre)) },
                    placeholder = { Text(stringResource(R.string.documento_nombre_pista)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = fecha,
                    onValueChange = { entrada ->
                        fecha = entrada.filter { it.isDigit() || it == '/' }.take(10)
                    },
                    label = { Text(stringResource(R.string.documento_fecha)) },
                    placeholder = { Text("dd/mm/aaaa") },
                    supportingText = {
                        // Se avisa en vez de impedir: una fecha mal escrita
                        // no debe dejar al paciente sin poder adjuntar el
                        // informe. Si no la sabe, la deja vacía.
                        if (fecha.isNotBlank() && fechaValida == null) {
                            Text(stringResource(R.string.documento_fecha_desconocida))
                        }
                    },
                    isError = fecha.isNotBlank() && fechaValida == null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Text(
                    text = stringResource(R.string.documento_tipo),
                    style = MaterialTheme.typography.titleSmall,
                )
                TipoDocumento.entries.chunked(2).forEach { fila ->
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

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text(stringResource(R.string.documento_notas)) },
                    placeholder = { Text(stringResource(R.string.documento_notas_pista)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    alGuardar(
                        tipo,
                        fechaValida,
                        notas.trim().ifBlank { null },
                        nombre.trim().ifBlank { null },
                    )
                },
            ) { Text(stringResource(R.string.documento_guardar)) }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) { Text(stringResource(R.string.cerrar)) }
        },
    )
}

/** Abre el informe con la app que corresponda al tipo de archivo. */
private fun abrirDocumento(contexto: Context, archivo: java.io.File, mime: String?) {
    if (!archivo.exists()) return

    val uri = FileProvider.getUriForFile(
        contexto,
        "${contexto.packageName}.documentos",
        archivo,
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime ?: "application/octet-stream")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching { contexto.startActivity(intent) }.onFailure {
        contexto.startActivity(
            Intent.createChooser(intent, contexto.getString(R.string.documento_abrir))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun compartirUno(
    contexto: Context,
    archivo: java.io.File,
    mime: String?,
    nombre: String,
) {
    if (!archivo.exists()) return

    val uri = FileProvider.getUriForFile(
        contexto,
        "${contexto.packageName}.documentos",
        archivo,
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime ?: "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, nombre)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newRawUri(nombre, uri)
    }

    contexto.startActivity(
        Intent.createChooser(intent, contexto.getString(R.string.documento_compartir))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/**
 * Comparte todos los informes de golpe.
 *
 * Va en un solo `ACTION_SEND_MULTIPLE` a propósito: mandar veinte correos
 * separados es peor que no mandar nada. El paciente elige el destino una vez
 * y se lleva la historia entera.
 */
private fun compartirVarios(
    contexto: Context,
    documentos: List<DocumentoEntity>,
    archivoDe: (DocumentoEntity) -> java.io.File,
) {
    val uris = documentos.mapNotNull { documento ->
        val archivo = archivoDe(documento)
        if (!archivo.exists()) return@mapNotNull null
        FileProvider.getUriForFile(contexto, "${contexto.packageName}.documentos", archivo)
    }

    if (uris.isEmpty()) return

    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "application/octet-stream"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        putExtra(
            Intent.EXTRA_SUBJECT,
            contexto.getString(R.string.documento_compartir_todos_titulo),
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Ver `CompartirHistoria`: el permiso llega por el ClipData.
        clipData = ClipData.newRawUri(
            contexto.getString(R.string.documento_compartir_todos_titulo),
            uris.first(),
        ).also { datos -> uris.drop(1).forEach { datos.addItem(ClipData.Item(it)) } }
    }

    contexto.startActivity(
        Intent.createChooser(intent, contexto.getString(R.string.documento_compartir_solo_este))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private val FECHA: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.Builder().setLanguage("es").build())

private val FECHAS_ACEPTADAS: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.Builder().setLanguage("es").build())

/** Se aceptan imágenes porque muchos informes llegan fotografiados. */
private val TIPOS_ACEPTADOS = arrayOf(
    "application/pdf",
    "image/*",
    "text/plain",
)