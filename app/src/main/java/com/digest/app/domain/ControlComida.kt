package com.digest.app.domain

import java.time.Duration
import java.time.LocalTime

/**
 * Reglas del control posterior a la comida.
 *
 * La pregunta no sale a la hora fija del aviso: sale un rato después, porque
 * a las dos horas exactas casi nadie sabe todavía si ha notado algo y
 * contestaría «no» por inercia. Entre las dos horas y media y las tres es
 * cuando la respuesta ya es de verdad.
 */
object ControlComida {

    /** Distancia desde la comida hasta el momento de preguntar. */
    val DESFASE: Duration = Duration.ofHours(2)

    /**
     * Margen en el que la pregunta sigue siendo válida.
     *
     * Pasada la hora, la pregunta pierde el sentido: el paciente ya no
     * recuerda qué notó y contestaría por educación. A las tres horas y
     * media se da por perdida y no se insiste más.
     */
    val VENTANA: Duration = Duration.ofMinutes(90)

    /** Instante en el que hay que preguntar, a partir de la hora de comida. */
    fun momentoDe(horaComida: LocalTime): LocalTime = horaComida.plus(DESFASE)

    /** ¿Toca preguntar ya? Solo dentro de la ventana de utilidad. */
    fun tocaPreguntar(horaComida: LocalTime, ahora: LocalTime): Boolean {
        val pregunta = momentoDe(horaComida)
        return !ahora.isBefore(pregunta) && ahora.isBefore(pregunta.plus(VENTANA))
    }

    /**
     * ¿Se puede seguir insistiendo?
     *
     * Se responde «no» una sola vez y se deja de preguntar, como pidió el
     * paciente: preguntar cada media hora a alguien que ya dijo que no
     * convertiría la app en un martillo. La comida queda anotada como
     * tolerada y se sigue con lo demás.
     */
    fun seguirInsistiendo(respuestas: Int): Boolean = respuestas < 1
}