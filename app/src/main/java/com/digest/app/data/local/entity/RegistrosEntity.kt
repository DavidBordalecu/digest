package com.digest.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.digest.app.model.EstadoToma
import com.digest.app.model.LocalizacionDolor
import com.digest.app.model.SangreEnHeces
import com.digest.app.model.TipoComida
import com.digest.app.model.TipoDolor
import com.digest.app.model.TipoSintoma
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Registro de una toma concreta.
 *
 * Se materializa en el momento en que se programa el horario, para poder
 * distinguir "todavía no era la hora" de "ya pasó la hora y no la tomaste":
 * esa diferencia es la que permite medir la adherencia real.
 */
@Entity(
    tableName = "tomas",
    foreignKeys = [
        ForeignKey(
            entity = HorarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["horarioId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["horarioId", "fecha"]), Index("fecha")],
)
data class TomaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val horarioId: Long,
    val fecha: LocalDate,
    val horaProgramada: LocalTime,
    val estado: EstadoToma = EstadoToma.PENDIENTE,
    /** Momento real en que se tomó, si se tomó. */
    val horaReal: LocalTime? = null,
    val notas: String? = null,
    val registradoEn: Instant = Instant.now(),
)

/**
 * Una comida registrada.
 *
 * El texto de [alimentos] es libre a propósito: el objetivo es que apuntar
 * lo que se ha comido sea rápido. Lo que sí se estructura es el momento y el
 * tipo de comida, que es lo que permite correlacionar con los síntomas.
 */
@Entity(tableName = "comidas", indices = [Index("fecha"), Index(value = ["fecha", "hora"])])
data class ComidaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val hora: LocalTime,
    val tipo: TipoComida,
    val alimentos: String? = null,
    val notas: String? = null,
    val registradoEn: Instant = Instant.now(),
)

/**
 * Registro de deposición con la escala de Bristol.
 *
 * En Crohn es el indicador más sensible de cambio de tránsito, así que se
 * guarda de forma independiente y no como un síntoma más.
 */
@Entity(tableName = "deposiciones", indices = [Index("fecha")])
data class DeposicionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val hora: LocalTime,
    /** Nivel 1-7 de la escala de Bristol. */
    val bristol: Int,
    val sangre: SangreEnHeces = SangreEnHeces.NINGUNA,
    val notas: String? = null,
)

/**
 * Episodio de dolor abdominal con su localización y carácter.
 */
@Entity(tableName = "dolores", indices = [Index("fecha")])
data class DolorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val hora: LocalTime,
    val intensidad: Int,
    val tipo: TipoDolor,
    val localizacion: LocalizacionDolor?,
    val notas: String? = null,
)

/**
 * Un síntoma marcado por el paciente en una fecha concreta.
 *
 * Un registro por (fecha, tipo): marcar dos veces el mismo síntoma el mismo
 * día solo actualiza la gravedad, que es lo que espera el paciente.
 */
@Entity(
    tableName = "sintomas",
    indices = [Index(value = ["fecha", "tipo"], unique = true), Index("fecha")],
)
data class SintomaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val tipo: TipoSintoma,
    val gravedad: Int,
    val notas: String? = null,
    val registradoEn: Instant = Instant.now(),
)