package com.digest.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.digest.app.model.EstadoBrote
import com.digest.app.model.NivelHidratacion
import com.digest.app.model.NivelMejora
import com.digest.app.model.SensacionGeneral
import com.digest.app.model.TipoNutricionEnteral
import com.digest.app.model.TipoSintoma
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * El resumen diario: una fila por fecha.
 *
 * Reúne las dos preguntas que el paciente quiere responder sobre su
 * tratamiento — si la medicación le mejora o no, y cómo se siente en
 * general — para poder contrastarlas después con los síntomas registrados.
 */
@Entity(tableName = "diario")
data class DiarioEntity(
    @PrimaryKey val fecha: LocalDate,
    val mejora: NivelMejora? = null,
    val sensacion: SensacionGeneral? = null,
    val estadoBrote: EstadoBrote? = null,
    val hidratacion: NivelHidratacion? = null,
    val notas: String? = null,
    val actualizadoEn: Instant = Instant.now(),
)

/**
 * Pesaje. Se guarda de forma aislada para poder dibujar la curva y detectar
 * caídas bruscas, que en Crohn suelen indicar malabsorción o brote activo.
 */
@Entity(tableName = "pesajes", indices = [Index("fecha")])
data class PesajeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val pesoKg: Double,
    val notas: String? = null,
    val registradoEn: Instant = Instant.now(),
)

/**
 * Nutrición enteral: volumen de fórmula tomado.
 *
 * Se registra por horas del día y se totaliza al día, que es como lo
 * contabilizan los equipos de nutrición.
 */
@Entity(tableName = "nutricion_enteral", indices = [Index("fecha")])
data class NutricionEnteralEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val hora: LocalTime,
    val producto: String?,
    val volumenMl: Int,
    val tipo: TipoNutricionEnteral,
    val notas: String? = null,
)

/**
 * Manifestación perianal registrada.
 *
 * Se separa del resto de síntomas porque en Crohn requiere seguimiento
 * propio: la aparición de una fístula o un absceso cambia el plan
 * terapéutico, y el paciente necesita poder identificar cuándo y dónde
 * aparecieron para contárselo a su médico.
 */
@Entity(tableName = "manifestaciones_perianales", indices = [Index("fecha")])
data class ManifestacionPerianalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val tipo: TipoSintoma,
    val gravedad: Int,
    val haySupuracion: Boolean = false,
    val notas: String? = null,
    val registradoEn: Instant = Instant.now(),
)