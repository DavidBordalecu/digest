package com.digest.app.model

import androidx.annotation.StringRes
import com.digest.app.R

/**
 * Diagnóstico digestivo que lleva el paciente.
 *
 * La app cubre las tres enfermedades inflamatorias intestinales y el colon
 * irritable porque comparten el 90% del modelo de datos; lo que las diferencia
 * son los cuestionarios de síntomas y los datos de diagnóstico, que se derivan
 * de aquí.
 */
enum class Condicion(@StringRes val displayName: Int) {
    COLITIS_ULCEROSA(R.string.condicion_colitis_ulcerosa),
    CROHN(R.string.condicion_crohn),
    INDETERMINADA(R.string.condicion_indeterminada),
    COLON_IRRITABLE(R.string.condicion_colon_irritable);

    /**
     * El colon irritable no es una enfermedad inflamatoria: no tiene zonas
     * de afectación ni antecedentes quirúrgicos propios, y su cuestionario
     * excluye los signos que obligan a descartar otra cosa.
     */
    val esInflamatoria: Boolean
        get() = this != COLON_IRRITABLE

    /** El íleon solo puede estar afectado en Crohn o en una indeterminada. */
    val afectaIntestinoDelgado: Boolean
        get() = this == CROHN || this == INDETERMINADA
}

/**
 * Zona del tubo digestivo afectada, según el diagnóstico del paciente.
 *
 * Cada afecion declara a qué condiciones se aplica, para que el perfil solo
 * ofrezca lo que le corresponde: la ileítis no existe en una colitis
 * ulcerosa, y la pancolitis no existe en un Crohn.
 */
enum class Afectacion(
    @StringRes val displayName: Int,
    val condiciones: Set<Condicion>,
) {
    // --- Colitis ulcerosa ---
    PROCTITIS(
        R.string.afectacion_proctitis,
        setOf(Condicion.COLITIS_ULCEROSA, Condicion.INDETERMINADA),
    ),
    COLITIS_IZQUIERDA(
        R.string.afectacion_colitis_izquierda,
        setOf(Condicion.COLITIS_ULCEROSA, Condicion.INDETERMINADA),
    ),
    COLITIS_EXTENSA(
        R.string.afectacion_colitis_extensa,
        setOf(Condicion.COLITIS_ULCEROSA, Condicion.INDETERMINADA),
    ),
    PANCOLITIS(
        R.string.afectacion_pancolitis,
        setOf(Condicion.COLITIS_ULCEROSA, Condicion.INDETERMINADA),
    ),

    // --- Crohn ---
    ILEITIS(
        R.string.afectacion_ileitis,
        setOf(Condicion.CROHN, Condicion.INDETERMINADA),
    ),
    ILEOCOLITIS(
        R.string.afectacion_ileocolitis,
        setOf(Condicion.CROHN, Condicion.INDETERMINADA),
    ),
    COLITIS_CROHN(
        R.string.afectacion_colitis_crohn,
        setOf(Condicion.CROHN, Condicion.INDETERMINADA),
    ),
    GASTRODUODENAL(
        R.string.afectacion_gastroduodenal,
        setOf(Condicion.CROHN, Condicion.INDETERMINADA),
    ),
    PERIANAL(
        R.string.afectacion_perianal,
        setOf(Condicion.CROHN, Condicion.INDETERMINADA),
    );

    companion object {
        /** Afectaciones que tienen sentido para una condición concreta. */
        fun para(condicion: Condicion): List<Afectacion> =
            entries.filter { condicion in it.condiciones }
    }
}