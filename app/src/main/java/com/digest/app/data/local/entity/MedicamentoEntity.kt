package com.digest.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.digest.app.model.GrupoFarmaco
import com.digest.app.model.TipoPresentacion
import com.digest.app.model.ViaAdministracion
import java.time.LocalTime

/**
 * Un fármaco o suplemento que el paciente tiene asignado.
 *
 * El nombre es texto libre porque el catálogo oficial no cubre todas las
 * marcas ni los fármacos en ensayo, pero se autocompleta con [nombreNormalizado]
 * para poder buscar sin recordar la grafía exacta.
 */
@Entity(tableName = "medicamentos", indices = [Index("nombreNormalizado")])
data class MedicamentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,

    /**
     * Copia normalizada del nombre para buscar sin depender de cómo lo escribió
     * el paciente. Se guarda en la tabla (y no se calcula al vuelo) para poder
     * indexarla: la búsqueda es la vía principal para añadir medicación.
     */
    val nombreNormalizado: String = normalizar(nombre),

    val dosis: String? = null,
    val presentacion: TipoPresentacion = TipoPresentacion.MEDICAMENTO,
    val via: ViaAdministracion = ViaAdministracion.ORAL,
    val grupo: GrupoFarmaco? = null,
    /** Indicaciones tipo "con comida" o "en ayunas". */
    val instrucciones: String? = null,
    val color: Int = 0,
    val activo: Boolean = true,
    val creadoEn: Long = System.currentTimeMillis(),
) {
    companion object {
        /** Minúsculas, sin acentos y con espacios simples. */
        fun normalizar(texto: String): String =
            java.text.Normalizer.normalize(texto.lowercase(), java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .replace(Regex("\\s+"), " ")
                .trim()
    }
}

/**
 * Horario de toma: un medicamento a una hora concreta en ciertos días.
 *
 * Un mismo medicamento puede tener varios horarios (p. ej. corticoide:
 * desayuno y cena), de ahí que sea una entidad aparte y no una columna.
 */
@Entity(
    tableName = "horarios",
    foreignKeys = [
        ForeignKey(
            entity = MedicamentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicamentoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medicamentoId"), Index("hora")],
)
data class HorarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicamentoId: Long,
    val hora: LocalTime,

    /**
     * Días de la semana en que se toma, como máscara de bits.
     * Lunes = bit 0 … domingo = bit 6. Una máscara vacía significa "todos
     * los días", que es el caso más común y el que no debe obligar al
     * paciente a marcar los siete.
     */
    val diasSemana: Int = TODOS_LOS_DIAS,

    val dosis: String? = null,
    val nota: String? = null,
) {
    fun aplicaEn(dia: java.time.DayOfWeek): Boolean =
        diasSemana == TODOS_LOS_DIAS || (diasSemana and (1 shl dia.value - 1)) != 0

    companion object {
        const val TODOS_LOS_DIAS = 0
        val LUNES_A_VIERNES = 0b0011111
        val FIN_DE_SEMANA = 0b1100000

        fun mascaraDe(dias: Set<java.time.DayOfWeek>): Int =
            if (dias.isEmpty() || dias.size == 7) {
                TODOS_LOS_DIAS
            } else {
                dias.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
            }
    }
}