package com.digest.app.model

import androidx.annotation.StringRes
import com.digest.app.R

/**
 * Cómo nota el paciente que le está funcionando la medicación.
 *
 * Es una de las dos preguntas centrales que pediste: el paciente indica si
 * la medicación le mejora o no, y se cruza con los síntomas del mismo día
 * para ver si el mapa de efectos tiene sentido.
 */
enum class NivelMejora(@StringRes val displayName: Int) {
    SIN_MEJORAS(R.string.mejora_sin),
    POCA_MEJORA(R.string.mejora_poca),
    MEJORA_CLARA(R.string.mejora_clara),
    GRAN_MEJORA(R.string.mejora_gran),
    EMPEORA(R.string.mejora_empeora),
    NO_APLICA(R.string.mejora_no_aplica),
}

/**
 * Cómo se siente en general ese día (bienestar global).
 */
enum class SensacionGeneral(@StringRes val displayName: Int) {
    MUY_BIEN(R.string.sensacion_muy_bien),
    BIEN(R.string.sensacion_bien),
    REGULAR(R.string.sensacion_regular),
    MAL(R.string.sensacion_mal),
    MUY_MAL(R.string.sensacion_muy_mal),
}

/**
 * Régimen alimenticio que sigue el paciente. Influye en cómo se registran
 * las comidas y en los avisos de hidratación.
 */
enum class TipoDieta(@StringRes val displayName: Int) {
    LIBRE(R.string.dieta_libre),
    SIN_GLUTEN(R.string.dieta_sin_gluten),
    BAJA_EN_FODMAP(R.string.dieta_baja_fodmap),
    VEGETARIANA_O_VEGANA(R.string.dieta_vegetariana),
    SIN_LACTOSA(R.string.dieta_sin_lactosa),
    NUTRICION_ENTERAL(R.string.dieta_enteral),
    OTRA(R.string.dieta_otra),
}

/**
 * Estado general de la enfermedad ese día. Ayuda a detectar fases de brote
 * en el histórico junto con los síntomas.
 */
enum class EstadoBrote(@StringRes val displayName: Int) {
    EN_REMISION(R.string.brote_remision),
    LEVE(R.string.brote_leve),
    MODERADO(R.string.brote_moderado),
    SEVERO(R.string.brote_severo),
    NO_APLICA(R.string.brote_no_aplica),
}