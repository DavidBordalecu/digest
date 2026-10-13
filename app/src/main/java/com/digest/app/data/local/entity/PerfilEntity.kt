package com.digest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.digest.app.model.Afectacion
import com.digest.app.model.Condicion
import com.digest.app.model.TipoCirugia
import com.digest.app.model.TipoDieta
import java.time.LocalDate

/**
 * Perfil del paciente. Una sola fila (id = 1).
 *
 * Guarda lo que condiciona el resto de la app: con la [Condicion] se decide
 * qué cuestionario de síntomas se muestra, y con [tieneEstomia] o
 * [tieneResecionIleocecal] se adaptan los avisos de hidratación y tránsito.
 */
@Entity(tableName = "perfil")
data class PerfilEntity(
    @PrimaryKey val id: Int = 1,

    val condicion: Condicion,

    /** Puede haber más de una zona afectada (p. ej. íleo + perianal). */
    val afectaciones: List<Afectacion>,

    val fechaDiagnostico: LocalDate? = null,

    /**
     * Última consulta registrada por el paciente.
     *
     * No la guarda el médico: la apunta el usuario al salir de la cita. Es
     * lo que permite sugerir una revisión cuando toca, sin que nadie tenga
     * que acordarse de la fecha.
     */
    val ultimaConsulta: LocalDate? = null,

    /** Cada cuántos meses suele revisarse. Alimenta el aviso de revisión. */
    val intervaloConsultaMeses: Int = 6,

    // --- Peso y altura ---
    val pesoKg: Double?,
    val alturaCm: Double?,

    // --- Historial quirúrgico: determinante en Crohn ---
    val tieneIleostomia: Boolean = false,
    val tieneColostomia: Boolean = false,
    val tieneResecionIleocecal: Boolean = false,

    val dieta: TipoDieta? = null,
    val notas: String? = null,

    val actualizadoEn: Long = System.currentTimeMillis(),
) {
    /** Índice de masa corporal, si hay peso y altura suficientes. */
    val imc: Double?
        get() {
            val peso = pesoKg ?: return null
            val altura = alturaCm ?: return null
            if (altura <= 0) return null
            val m = altura / 100.0
            return peso / (m * m)
        }

    val tieneEstomia: Boolean
        get() = tieneIleostomia || tieneColostomia

    /** Si alguna intervención altera el tránsito intestinal de forma permanente. */
    val alteraTransito: Boolean
        get() = tieneEstomia || tieneResecionIleocecal
}

/**
 * Historial quirúrgico: relevante sobre todo en Crohn.
 */
@Entity(tableName = "cirugias")
data class CirugiaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: TipoCirugia,
    val fecha: LocalDate? = null,
    val hospital: String? = null,
    val notas: String? = null,
)