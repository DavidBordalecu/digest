package com.digest.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Control de cómo se ha tolerado una comida.
 *
 * Pasa un par de horas desde que se come y es cuando suelen aparecer las
 * molestias, así que a esa distancia la app pregunta si ha notado algo.
 * Es la forma de construir la relación entre lo que se ha comido y lo que
 * ha pasado después, que es justo lo que el paciente no consigue apuntar
 * solo: nadie apunta que dos horas después del postre le empezó el dolor.
 *
 * Un control por comida. `huboCambios` a null significa que todavía no se
 * ha respondido: hay un aviso en pantalla o una tarjeta dentro de la app
 * pidiendo respuesta.
 */
@Entity(
    tableName = "control_comidas",
    indices = [Index("comidaId", unique = true)],
)
data class ControlComidaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Comida a la que corresponde este control. */
    val comidaId: Long,

    val fecha: LocalDate,

    /** Hora a la que se comió. Se copia para no depender de la comida. */
    val horaComida: LocalTime,

    /** Hora a la que debe saltar la pregunta: dos horas después. */
    val programadoPara: LocalTime,

    /** Cuándo se respondió, o null si sigue pendiente. */
    val respondidoEn: LocalTime? = null,

    /**
     * Qué contestó el paciente: true si notó cambios, false si no.
     *
     * Se guardan también los negativos a propósito. Un «no» vale tanto como
     * un «sí» para el médico: sin ellos no se puede construir la lista de
     * alimentos que no dan problemas, y es tan útil como la de los que sí
     * los provocan.
     */
    val huboCambios: Boolean? = null,

    val registradoEn: Instant = Instant.now(),
)