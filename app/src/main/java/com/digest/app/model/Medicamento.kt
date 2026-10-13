package com.digest.app.model

import androidx.annotation.StringRes
import com.digest.app.R

/**
 * Naturaleza del producto que se toma.
 *
 * Separar suplementos de medicamentos importa: un paciente con Crohn que se
 * salta el hierro o la vitamina B12 tiene una consecuencias distintas a quien
 * se salta el biológico, y el informe de adherencia debe separarlos.
 */
enum class TipoPresentacion(@StringRes val displayName: Int) {
    MEDICAMENTO(R.string.presentacion_medicamento),
    SUPLEMENTO(R.string.presentacion_suplemento),
}

/**
 * Cómo se administra el producto. Afecta a si tiene sentido un refuerzo
 * ("con comida", "en ayunas") y al tono del recordatorio.
 */
enum class ViaAdministracion(@StringRes val displayName: Int) {
    ORAL(R.string.via_oral),
    RECTAL(R.string.via_rectal),
    SUBCUTANEA(R.string.via_subcutanea),
    INTRAVENOSA(R.string.via_intravenosa),
}

/**
 * Estado de una toma programada.
 */
enum class EstadoToma(@StringRes val displayName: Int) {
    PENDIENTE(R.string.estado_pendiente),
    TOMADA(R.string.estado_tomada),
    OMITIDA(R.string.estado_omitida),
    RETRASADA(R.string.estado_retrasada),
}

/**
 * Grupo terapéutico, para el catálogo de fármacos y los iconos.
 */
enum class GrupoFarmaco(@StringRes val displayName: Int) {
    AMINOSALICILATO(R.string.grupo_aminosalicilato),
    CORTICOIDE(R.string.grupo_corticoide),
    INMUNOSUPRESOR(R.string.grupo_inmunosupresor),
    BIOLOGICO(R.string.grupo_biologico),
    PEPTIDO_ANALOGO(R.string.grupo_peptido_analogo),
    PEQUEÑA_MOLECULA(R.string.grupo_pequena_molecula),
    ANTIDIARREICO(R.string.grupo_antidiarreico),
    ANALGESICO(R.string.grupo_analgesico),
    SUPLEMENTO(R.string.grupo_suplemento),
    OTRO(R.string.grupo_otro),
}