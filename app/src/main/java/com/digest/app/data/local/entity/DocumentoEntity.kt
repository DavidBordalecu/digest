package com.digest.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.digest.app.model.TipoDocumento
import java.time.Instant
import java.time.LocalDate

/**
 * Informe médico que el paciente adjunta a su historia.
 *
 * Lo que la app guarda de verdad es el **contenido**, copiado dentro de su
 * carpeta privada, y no una referencia al archivo que tenía en el móvil.
 *
 * La razón es que una referencia no sobrevive: el permiso de lectura que
 * Android concede al elegir un archivo caduca, el paciente puede borrar el
 * PDF de la carpeta de Descargas sin darse cuenta, y mover el teléfono deja
 * la app con veinte informes que ya no puede abrir ni compartir. Copiando,
 * el informe está ahí dentro de dos años, que es cuando hace falta.
 *
 * Los datos de salud no salen del dispositivo: los archivos se quedan en la
 * carpeta privada de la app y solo se comparten cuando el paciente lo pide.
 */
@Entity(
    tableName = "documentos",
    indices = [Index("fechaDocumento")],
)
data class DocumentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Nombre que se ve en la lista. El paciente lo escribe o se sugiere. */
    val nombre: String,

    val tipo: TipoDocumento,

    /**
     * Fecha a la que se hizo la prueba.
     *
     * Va separada de `creadoEn` a propósito: se sube hoy un análisis de
     * hace dos años, y al médico le importa la fecha de la prueba, no la
     * del día que se fotografió. Es nullable porque muchas veces el
     * paciente no la sabe.
     */
    val fechaDocumento: LocalDate? = null,

    val notas: String? = null,

    /** Nombre real del archivo dentro de la carpeta de la app. */
    val nombreArchivo: String,

    val mime: String? = null,

    val tamanoBytes: Long = 0,

    val creadoEn: Instant = Instant.now(),
)