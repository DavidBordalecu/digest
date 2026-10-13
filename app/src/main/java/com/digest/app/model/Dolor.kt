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
 */
enum class TipoDolor(@StringRes val displayName: Int, @StringRes val pista: Int) {
    COLICO(R.string.dolor_colico, R.string.dolor_colico_pista),
    FIJO(R.string.dolor_fijo, R.string.dolor_fijo_pista),
    PUNZANTE(R.string.dolor_punzante, R.string.dolor_punzante_pista),
    PRESIVO(R.string.dolor_presivo, R.string.dolor_presivo_pista),
}

/**
 * Zona del abdomen donde se localiza el dolor.
 * El cuadrante inferior derecho (fosa ilíaca derecha) es el típico del Crohn
 * por afectación del íleon terminal.
 */
enum class LocalizacionDolor(@StringRes val displayName: Int) {
    EPIGASTRIO(R.string.dolor_epigastrio),
    HIPOCONDRIO_IZQUIERDO(R.string.dolor_hipocondrio_izq),
    PERIUMBILICAL(R.string.dolor_periumbilical),
    HIPOCONDRIO_DERECHO(R.string.dolor_hipocondrio_der),
    FOSA_ILIACA_IZQUIERDA(R.string.dolor_fosa_iliaca_izq),
    FOSA_ILIACA_DERECHA(R.string.dolor_fosa_iliaca_der),
    PELVICA(R.string.dolor_pelvica),
    LUMBAR(R.string.dolor_lumbar),
    DIFUSA(R.string.dolor_difusa),
}