package com.digest.app.model

import androidx.annotation.StringRes
import com.digest.app.R

/**
 * Carácter del dolor abdominal.
 *
 * Distinguir cólico de dolor fijo es clínicamente relevante en Crohn:
 * un dolor cólico que se calma con la defecación sugiere espasmo, mientras
 * que un dolor fijo y continuo puede indicar inflamación localizada,
 * una estenosis o un absceso.
 *
 * El `nombreLegible` es el texto plano equivalente a `displayName`. Lo usa
 * el informe, que se genera sin contexto de Android y no puede leer
 * recursos.
 */
enum class TipoDolor(
    @StringRes val displayName: Int,
    @StringRes val pista: Int,
    val nombreLegible: String,
) {
    COLICO(R.string.dolor_colico, R.string.dolor_colico_pista, "Cólico"),
    FIJO(R.string.dolor_fijo, R.string.dolor_fijo_pista, "Fijo"),
    PUNZANTE(R.string.dolor_punzante, R.string.dolor_punzante_pista, "Punzante"),
    PRESIVO(R.string.dolor_presivo, R.string.dolor_presivo_pista, "Presivo"),
}

/**
 * Zona del abdomen donde se localiza el dolor.
 * El cuadrante inferior derecho (fosa ilíaca derecha) es el típico del Crohn
 * por afectación del íleon terminal.
 */
enum class LocalizacionDolor(@StringRes val displayName: Int, val nombreLegible: String) {
    EPIGASTRIO(R.string.dolor_epigastrio, "Epigastrio"),
    HIPOCONDRIO_IZQUIERDO(R.string.dolor_hipocondrio_izq, "Hipocondrio izquierdo"),
    PERIUMBILICAL(R.string.dolor_periumbilical, "Periumbilical"),
    HIPOCONDRIO_DERECHO(R.string.dolor_hipocondrio_der, "Hipocondrio derecho"),
    FOSA_ILIACA_IZQUIERDA(R.string.dolor_fosa_iliaca_izq, "Fosa ilíaca izquierda"),
    FOSA_ILIACA_DERECHA(R.string.dolor_fosa_iliaca_der, "Fosa ilíaca derecha"),
    PELVICA(R.string.dolor_pelvica, "Pélvica"),
    LUMBAR(R.string.dolor_lumbar, "Lumbar"),
    DIFUSA(R.string.dolor_difusa, "Difusa"),
}