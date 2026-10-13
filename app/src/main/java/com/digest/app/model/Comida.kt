package com.digest.app.model

import androidx.annotation.StringRes
import com.digest.app.R

/**
 * Momento del día de una comida.
 *
 * El horario importa tanto como el contenido: permite correlacionar lo que se
 * ingiere con la aparición de síntomas horas después.
 */
enum class TipoComida(@StringRes val displayName: Int, val horaSugerida: Int) {
    DESAYUNO(R.string.comida_desayuno, 8),
    MEDIA_MANANA(R.string.comida_media_manana, 11),
    ALMUERZO(R.string.comida_almuerzo, 14),
    MERIENDA(R.string.comida_merienda, 17),
    CENA(R.string.comida_cena, 21),
    NOCTURNO(R.string.comida_nocturno, 2),
}

/**
 * Tipo de nutrición enteral.
 */
enum class TipoNutricionEnteral(@StringRes val displayName: Int) {
    EXCLUSIVA(R.string.enteral_exclusiva),
    PARCIAL(R.string.enteral_parcial),
    SUPLEMENTO_COMIDA(R.string.enteral_suplemento),
}

/**
 * Nivel de hidratación declarado por el paciente.
 *
 * En pacientes con estomía o resección la pérdida de líquidos por el
 * dispositivo es el riesgo principal, así que se registra a diario.
 */
enum class NivelHidratacion(@StringRes val displayName: Int) {
    NORMAL(R.string.hidratacion_normal),
    INSUFICIENTE(R.string.hidratacion_insuficiente),
    EXCESIVA(R.string.hidratacion_excesiva),
    NO_APLICA(R.string.hidratacion_no_aplica),
}