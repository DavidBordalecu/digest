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
private val eii: Set<Condicion> = setOf(
    Condicion.COLITIS_ULCEROSA,
    Condicion.CROHN,
    Condicion.INDETERMINADA,
)
private val todas: Set<Condicion> = Condicion.entries.toSet()
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
 * En colon irritable se excluyen los signos de alarma (sangre, fiebre,
 * pérdida de peso), porque obligarían a descartar otra causa. El paciente
 * puede además añadir síntomas propios desde Ajustes.
 */
enum class TipoSintoma(
    @StringRes val displayName: Int,
    val categoria: CategoriaSintoma,
    val condiciones: Set<Condicion>,
) {
    // --- Intestinales, comunes a todos los diagnósticos ---
    DIARREA(R.string.sintoma_diarrea, CategoriaSintoma.INTESTINAL, todas),
    DOLOR_ABDOMINAL(R.string.sintoma_dolor_abdominal, CategoriaSintoma.INTESTINAL, todas),
    TENESMO(R.string.sintoma_tenesmo, CategoriaSintoma.INTESTINAL, eii),
    SANGRE_EN_HECES(R.string.sintoma_sangre, CategoriaSintoma.INTESTINAL, eii),
    MUCOSIDAD(R.string.sintoma_mucosidad, CategoriaSintoma.INTESTINAL, todas),
    DISTENSION_ABDOMINAL(R.string.sintoma_distension, CategoriaSintoma.INTESTINAL, todas),

    // --- Perianales (frecuentes en Crohn) ---
    FISURA_ANAL(R.string.sintoma_fisura, CategoriaSintoma.PERIANAL, conIleon),
    FISTULA_ANAL(R.string.sintoma_fistula, CategoriaSintoma.PERIANAL, conIleon),
    ABSCESO_ANAL(R.string.sintoma_absceso, CategoriaSintoma.PERIANAL, conIleon),
    DOLOR_O_SUPURACION_PERIANAL(R.string.sintoma_dolor_supuracion, CategoriaSintoma.PERIANAL, conIleon),
    INFLAMACION_EXTERNA_PERIANAL(R.string.sintoma_inflamacion_externa, CategoriaSintoma.PERIANAL, conIleon),

    // --- Generales ---
    FATIGA_ASTENIA(R.string.sintoma_fatiga, CategoriaSintoma.GENERAL, todas),
    FIEBRE_FEBRICULA(R.string.sintoma_fiebre, CategoriaSintoma.GENERAL, eii),
    NAUSEAS(R.string.sintoma_nauseas, CategoriaSintoma.GENERAL, todas),
    PERDIDA_APETITO(R.string.sintoma_perdida_apetito, CategoriaSintoma.GENERAL, eii),
    PERDIDA_PESO(R.string.sintoma_perdida_peso, CategoriaSintoma.GENERAL, eii),
    DESNUTRICION(R.string.sintoma_desnutricion, CategoriaSintoma.GENERAL, eii),

    // --- Extraintestinales ---
    AFTA_BUCAL(R.string.sintoma_afta_bucal, CategoriaSintoma.EXTRAINTESTINAL, eii),
    DOLOR_ARTICULAR(R.string.sintoma_dolor_articular, CategoriaSintoma.EXTRAINTESTINAL, eii),
    MANCHAS_EN_PIEL(R.string.sintoma_manchas_piel, CategoriaSintoma.EXTRAINTESTINAL, eii),
    UVEITIS_OJO_ROJO(R.string.sintoma_uveitis, CategoriaSintoma.EXTRAINTESTINAL, eii);

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