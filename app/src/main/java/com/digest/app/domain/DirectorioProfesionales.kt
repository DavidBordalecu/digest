package com.digest.app.domain

import com.digest.app.model.Condicion

/** Especialidad del profesional. */
enum class Especialidad {
    GASTROENTEROLOGIA,
    NUTRICION,
}

/**
 * Un profesional del directorio.
 *
 * El directorio va incluido en la app y **es ficticio**: los nombres, el
 * teléfono y la dirección no corresponden a nadie real. Está así porque
 * Listar profesionales de verdad exige un acuerdo con cada centro y
 * comprobar que siguen aceptando pacientes. Mientras tanto es preferible
 * una lista claramente ficticia que una de datos caducados.
 */
data class Profesional(
    val nombre: String,
    val especialidad: Especialidad,
    val centro: String,
    val ciudad: String,
    val telefono: String,
    /** Si atiende específicamente enfermedad inflamatoria intestinal. */
    val especializadoEnEii: Boolean,
    /** Días de consulta, en texto corto para la ficha. */
    val dias: String,
    /** Horario de consulta. */
    val horario: String,
    /** Si concertar cita online. */
    val admiteCitaOnline: Boolean,
) {
    val iniciales: String
        get() = nombre
            .split(' ')
            .filter { it.isNotBlank() }
            .let { partes ->
                val primero = partes.firstOrNull()?.firstOrNull()?.toString().orEmpty()
                val segundo = partes.getOrNull(1)?.firstOrNull()?.toString().orEmpty()
                (primero + segundo).uppercase()
            }
}

/**
 * Directorio de profesionales incluido en la app.
 *
 * Ficticio a propósito. Cuando se sustituya por datos reales, este objeto
 * pasa a ser la única fuente: el resto de la app ya consulta a través de
 * [porEspecialidad] y no necesita cambios.
 */
object DirectorioProfesionales {

    val gastroenterologos: List<Profesional> = listOf(
        Profesional(
            nombre = "Dra. Elena Márquez Santamaría",
            especialidad = Especialidad.GASTROENTEROLOGIA,
            centro = "Hospital Universitario de Digestive",
            ciudad = "Madrid",
            telefono = "+34 910 000 001",
            especializadoEnEii = true,
            dias = "Lunes, miércoles y viernes",
            horario = "09:00 – 14:00",
            admiteCitaOnline = true,
        ),
        Profesional(
            nombre = "Dr. Tomás Iglesias Bermúdez",
            especialidad = Especialidad.GASTROENTEROLOGIA,
            centro = "Instituto de Digestive Norte",
            ciudad = "Barcelona",
            telefono = "+34 930 000 002",
            especializadoEnEii = true,
            dias = "Martes y jueves",
            horario = "16:00 – 20:00",
            admiteCitaOnline = false,
        ),
        Profesional(
            nombre = "Dra. Nuria Bassols Prieto",
            especialidad = Especialidad.GASTROENTEROLOGIA,
            centro = "Centro Médico Ebro",
            ciudad = "Zaragoza",
            telefono = "+34 976 000 003",
            especializadoEnEii = false,
            dias = "Miércoles",
            horario = "10:00 – 13:00",
            admiteCitaOnline = true,
        ),
        Profesional(
            nombre = "Dr. Ignacio Roa Valdivieso",
            especialidad = Especialidad.GASTROENTEROLOGIA,
            centro = "Hospital de Día Carles Ferrer",
            ciudad = "Valencia",
            telefono = "+34 960 000 004",
            especializadoEnEii = true,
            dias = "Lunes a viernes",
            horario = "08:00 – 15:00",
            admiteCitaOnline = false,
        ),
    )

    val nutricionistas: List<Profesional> = listOf(
        Profesional(
            nombre = "Lda. Marta Serrano Ruiz",
            especialidad = Especialidad.NUTRICION,
            centro = "Unidad de Nutrición Clínica",
            ciudad = "Madrid",
            telefono = "+34 910 000 011",
            especializadoEnEii = true,
            dias = "Lunes, miércoles y viernes",
            horario = "09:00 – 15:00",
            admiteCitaOnline = true,
        ),
        Profesional(
            nombre = "Lda. Álvaro Peña Castaño",
            especialidad = Especialidad.NUTRICION,
            centro = "Consultorio Digest Nutrition",
            ciudad = "Barcelona",
            telefono = "+34 930 000 012",
            especializadoEnEii = true,
            dias = "Martes y jueves",
            horario = "17:00 – 21:00",
            admiteCitaOnline = true,
        ),
        Profesional(
            nombre = "Lda. Carmen Vidal Ortiz",
            especialidad = Especialidad.NUTRICION,
            centro = "Centro de Nutrición Delta",
            ciudad = "Sevilla",
            telefono = "+34 954 000 013",
            especializadoEnEii = false,
            dias = "Miércoles y viernes",
            horario = "10:00 – 14:00",
            admiteCitaOnline = false,
        ),
        Profesional(
            nombre = "Lda. Rubén García Nieto",
            especialidad = Especialidad.NUTRICION,
            centro = "ClínicaNutrición Sur",
            ciudad = "Valencia",
            telefono = "+34 960 000 014",
            especializadoEnEii = true,
            dias = "Lunes a viernes",
            horario = "08:30 – 13:30",
            admiteCitaOnline = true,
        ),
    )

    val todos: List<Profesional> = gastroenterologos + nutricionistas

    fun porEspecialidad(especialidad: Especialidad): List<Profesional> =
        todos.filter { it.especialidad == especialidad }

    /**
     * Los profesionales que mejor encajan para el caso del paciente.
     *
     * Se prioriza a quien se especializa en enfermedad inflamatoria intestinal,
     * que es lo que marca la diferencia entre una consulta útil y una en la
     * que hay que explicar el diagnóstico desde cero.
     */
    fun recomendados(condicion: Condicion, especialidad: Especialidad): List<Profesional> =
        porEspecialidad(especialidad).sortedByDescending { it.especializadoEnEii }

    fun buscar(texto: String): List<Profesional> {
        val limpio = texto.trim().lowercase()
        if (limpio.isEmpty()) return todos
        return todos.filter {
            it.nombre.lowercase().contains(limpio) ||
                it.centro.lowercase().contains(limpio) ||
                it.ciudad.lowercase().contains(limpio)
        }
    }
}