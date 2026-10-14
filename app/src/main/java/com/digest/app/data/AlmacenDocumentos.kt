package com.digest.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.digest.app.data.local.entity.DocumentoEntity
import com.digest.app.model.TipoDocumento
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate

/**
 * Guarda y saca los informes que el paciente adjunta.
 *
 * El archivo se **copia** dentro de la carpeta privada de la app en vez de
 * guardarse una referencia al original. Ver [DocumentoEntity]: una
 * referencia caduca con el permiso de Android y desaparece en cuanto el
 * paciente borra el PDF de Descargas, y entonces el informe que el médico
 * pidió no está.
 */
class AlmacenDocumentos(private val context: Context) {

    private val carpeta: File
        get() = File(context.filesDir, CARPETA).apply { mkdirs() }

    /** Cuántos bytes ocupa todo lo guardado, para avisar si se dispara. */
    fun espacioUsado(): Long = carpeta.listFiles()?.sumOf { it.length() } ?: 0L

    /**
     * Copia el archivo elegido y devuelve la entidad, ya guardada.
     *
     * @param nombreVisible lo que se verá en la lista; si viene vacío se usa
     *   el nombre del propio archivo.
     */
    fun adjuntar(
        origen: Uri,
        tipo: TipoDocumento,
        fechaDocumento: LocalDate?,
        notas: String?,
        nombreVisible: String?,
    ): Result<DocumentoEntity> = runCatching {
        val destino = File(carpeta, nombreUnico(origen))
        copiar(origen, destino)

        val entidad = DocumentoEntity(
            nombre = nombreVisible?.takeIf { it.isNotBlank() } ?: nombreDe(origen),
            tipo = tipo,
            fechaDocumento = fechaDocumento,
            notas = notas?.takeIf { it.isNotBlank() },
            nombreArchivo = destino.name,
            mime = context.contentResolver.getType(origen),
            tamanoBytes = destino.length(),
        )
        entidad
    }.onFailure {
        Log.e(TAG, "No se pudo copiar el archivo $origen", it)
    }

    /** Ruta absoluta del archivo guardado. */
    fun archivoDe(documento: DocumentoEntity): File = File(carpeta, documento.nombreArchivo)

    /** Borra el archivo del disco y el registro. El archivo va primero. */
    /**
     * Borra el archivo del disco y avisa para que se borre la fila.
     *
     * El archivo va primero: si se borrara la fila y luego fallara el
     * archivo, quedaría un PDF ocupando sitio que ya nadie puede abrir ni
     * borrar desde la app.
     */
    fun borrar(documento: DocumentoEntity, alBorrarFila: suspend (DocumentoEntity) -> Unit) {
        val borrado = runCatching { archivoDe(documento).delete() }
            .onFailure { Log.w(TAG, "No se pudo borrar el archivo", it) }
            .isSuccess

        if (!borrado) {
            Log.w(TAG, "El archivo ${documento.nombreArchivo} no se pudo borrar")
        }
    }

    /**
     * Limpia archivos huérfanos.
     *
     * Puede pasar que quede un archivo sin su fila: si la app muere entre
     * copiar el archivo e insertar la fila. Esos archivos no los va a
     * mostrar nadie y no se van a borrar solos.
     */
    fun limpiarHuerfanos(nombresEnBase: Set<String>): Int {
        val archivos = carpeta.listFiles() ?: return 0
        val huerfanos = archivos.filter { it.name !in nombresEnBase }
        huerfanos.forEach { runCatching { it.delete() } }
        if (huerfanos.isNotEmpty()) {
            Log.i(TAG, "Borrados ${huerfanos.size} archivos sin registro")
        }
        return huerfanos.size
    }

    private fun copiar(origen: Uri, destino: File) {
        val entrada = context.contentResolver.openInputStream(origen)
            ?: error("No se pudo abrir el archivo seleccionado")

        entrada.use { flujo ->
            FileOutputStream(destino).use { salida -> flujo.copyTo(salida) }
        }
    }

    /**
     * Nombre único en disco.
     *
     * Lleva marca de tiempo y un número, porque dos colonoscopias pueden
     * llamarse igual y sobrescribir una a otra sería perder un informe
     * médico sin avisar.
     */
    private fun nombreUnico(origen: Uri): String {
        val marca = Instant.now().toEpochMilli()
        val extension = nombreDe(origen).substringAfterLast('.', "").take(8)
        return if (extension.isEmpty()) "doc_$marca" else "doc_$marca.$extension"
    }

    /** Nombre real del archivo, tal como lo tiene el móvil. */
    private fun nombreDe(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val columna = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columna >= 0 && it.moveToFirst()) {
                return it.getString(columna) ?: FALLBACK
            }
        }
        // Algunos proveedores no dan nombre; se usa el último trozo de la ruta.
        return uri.lastPathSegment?.substringAfterLast('/') ?: FALLBACK
    }

    private companion object {
        const val TAG = "AlmacenDocumentos"
        const val CARPETA = "documentos"
        const val FALLBACK = "informe"
    }
}