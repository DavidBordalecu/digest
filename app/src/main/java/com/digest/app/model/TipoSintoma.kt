package com.digest.app.model

import androidx.annotation.StringRes
import com.digest.app.R

/**
 * Conjuntos de condiciones usados para filtrar los cuestionarios.
 *
 * Viven a nivel de archivo (y no en el `companion`) porque las entradas de un
 * enum se inicializan antes que su companion: referenciarlo desde aquí
 * rompería la inicialización de la clase.
 */
private val ambas: Set<Condicion> = Condicion.entries.toSet()
private val conIleon: Set<Condicion> = setOf(Condicion.CROHN, Condicion.INDETERMINADA)

/**
 * Categorías de síntomas. Determina cómo se agrupan y pintan en el registro.
 */
enum class CategoriaSintoma(@StringRes val displayName: Int) {
    INTESTINAL(R.string.categoria_intestinal),
    PERIANAL(R.string.categoria_perianal),
    GENERAL(R.string.categoria_general),
    EXTRAINTESTINAL(R.string.categoria_extraintestinal),
}

/**
 * Síntomas que puede registrar el paciente.
 *
 * Cada uno declara para qué enfermedades tiene sentido: en Crohn pesan más el
 * dolor en cuadrante inferior derecho, la fiebre y las manifestaciones
 * perianales; en colitis ulcerosa, el sangrado, el tenesmo y la mucosidad.
 * El paciente puede además añadir síntomas propios desde Ajustes.
 */
enum class TipoSintoma(
    @StringRes val displayName: Int,
    val categoria: CategoriaSintoma,
    val condiciones: Set<Condicion>,
) {
    // --- Intestinales, comunes a ambas ---
    DIARREA(R.string.sintoma_diarrea, CategoriaSintoma.INTESTINAL, ambas),
    DOLOR_ABDOMINAL(R.string.sintoma_dolor_abdominal, CategoriaSintoma.INTESTINAL, ambas),
    TENESMO(R.string.sintoma_tenesmo, CategoriaSintoma.INTESTINAL, ambas),
    SANGRE_EN_HECES(R.string.sintoma_sangre, CategoriaSintoma.INTESTINAL, ambas),
    MUCOSIDAD(R.string.sintoma_mucosidad, CategoriaSintoma.INTESTINAL, ambas),
    DISTENSION_ABDOMINAL(R.string.sintoma_distension, CategoriaSintoma.INTESTINAL, ambas),

    // --- Perianales (frecuentes en Crohn) ---
    FISURA_ANAL(R.string.sintoma_fisura, CategoriaSintoma.PERIANAL, conIleon),
    FISTULA_ANAL(R.string.sintoma_fistula, CategoriaSintoma.PERIANAL, conIleon),
    ABSCESO_ANAL(R.string.sintoma_absceso, CategoriaSintoma.PERIANAL, conIleon),
    DOLOR_O_SUPURACION_PERIANAL(R.string.sintoma_dolor_supuracion, CategoriaSintoma.PERIANAL, conIleon),
    INFLAMACION_EXTERNA_PERIANAL(R.string.sintoma_inflamacion_externa, CategoriaSintoma.PERIANAL, conIleon),

    // --- Generales ---
    FATIGA_ASTENIA(R.string.sintoma_fatiga, CategoriaSintoma.GENERAL, ambas),
    FIEBRE_FEBRICULA(R.string.sintoma_fiebre, CategoriaSintoma.GENERAL, ambas),
    NAUSEAS(R.string.sintoma_nauseas, CategoriaSintoma.GENERAL, ambas),
    PERDIDA_APETITO(R.string.sintoma_perdida_apetito, CategoriaSintoma.GENERAL, ambas),
    PERDIDA_PESO(R.string.sintoma_perdida_peso, CategoriaSintoma.GENERAL, ambas),
    DESNUTRICION(R.string.sintoma_desnutricion, CategoriaSintoma.GENERAL, ambas),

    // --- Extraintestinales ---
    AFTA_BUCAL(R.string.sintoma_afta_bucal, CategoriaSintoma.EXTRAINTESTINAL, ambas),
    DOLOR_ARTICULAR(R.string.sintoma_dolor_articular, CategoriaSintoma.EXTRAINTESTINAL, ambas),
    MANCHAS_EN_PIEL(R.string.sintoma_manchas_piel, CategoriaSintoma.EXTRAINTESTINAL, ambas),
    UVEITIS_OJO_ROJO(R.string.sintoma_uveitis, CategoriaSintoma.EXTRAINTESTINAL, ambas);

    companion object {
        /**
         * Síntomas sugeridos para una condición, ordenados por categoría.
         * La indeterminada recibe la lista de Crohn por ser la más amplia.
         */
        fun para(condicion: Condicion): List<TipoSintoma> =
            entries
                .filter { condicion in it.condiciones }
                .sortedBy { it.categoria.ordinal }
    }
}