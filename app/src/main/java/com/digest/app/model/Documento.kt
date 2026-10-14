package com.digest.app.model

import androidx.annotation.StringRes
import com.digest.app.R

/**
 * Tipo de informe médico que se puede adjuntar.
 *
 * No es solo una etiqueta para ordenar: es lo que permite responder a la
 * pregunta que más le importa al gastroenterólogo, que es cuándo fue la
 * última colonoscopia y qué salió. Clasificar por tipo es lo que hace que
 * esa pregunta se pueda contestar de un vistazo en lugar de abriendo veinte
 * archivos.
 *
 * `OTRO` existe porque las pruebas no se dividen solo en estas: hay
 * estudios de motilidad, ecografías, RM… Prefieren filtrar mal a dejar
 * fuera un informe que importa.
 */
enum class TipoDocumento(@StringRes val displayName: Int) {
    ANALITICA(R.string.doc_analitica),
    COLONOSCOPIA(R.string.doc_colonoscopia),
    ENDOSCOPIA(R.string.doc_endoscopia),
    BIOPSIA(R.string.doc_biopisia),
    IMAGEN(R.string.doc_imagen),
    CALIBRE_ILEAL(R.string.doc_calibre_ileal),
    OTRO(R.string.doc_otro),
    ;

    /**
     * Texto plano equivalente a `displayName`, para el informe.
     *
     * El informe se genera sin contexto de Android y no puede resolver un
     * recurso.
     */
    val nombreLegible: String
        get() = when (this) {
            ANALITICA -> "Analítica"
            COLONOSCOPIA -> "Colonoscopia"
            ENDOSCOPIA -> "Endoscopia"
            BIOPSIA -> "Biopsia"
            IMAGEN -> "Prueba de imagen"
            CALIBRE_ILEAL -> "Calibre del íleon"
            OTRO -> "Otro documento"
        }
}