package com.digest.app.model

import androidx.annotation.StringRes
import com.digest.app.R

/**
 * Escala de Bristol: consistencia de las heces.
 *
 * En Crohn es especialmente útil para detectar cambios de tránsito (una
 * transición de 4 a 6-7 suele preceder a un brote) y para valorar el efecto
 * de los cambios de dieta o de la medicación.
 */
enum class ConsistenciaBristol(
    val nivel: Int,
    @StringRes val displayName: Int,
    @StringRes val descripcion: Int,
) {
    HECES_DURAS_SEPARADAS(1, R.string.bristol_1, R.string.bristol_1_desc),
    HECES_DURAS_EN_SALCHICHA(2, R.string.bristol_2, R.string.bristol_2_desc),
    HECES_NORMALES_TEXTURIZADAS(3, R.string.bristol_3, R.string.bristol_3_desc),
    HECES_NORMALES_LISAS(4, R.string.bristol_4, R.string.bristol_4_desc),
    HECES_BLANDAS_CON_BORDES(5, R.string.bristol_5, R.string.bristol_5_desc),
    HECES_BLANDAS_DESHILACHADAS(6, R.string.bristol_6, R.string.bristol_6_desc),
    HECES_LIQUIDAS(7, R.string.bristol_7, R.string.bristol_7_desc);

    /** 1-4 tránsito normal, 5-7 diarrea. */
    val esDiarrea: Boolean get() = nivel >= 5

    companion object {
        fun porNivel(nivel: Int): ConsistenciaBristol =
            entries.firstOrNull { it.nivel == nivel } ?: HECES_NORMALES_LISAS
    }
}

/**
 * Cantidad de sangre visible en las heces.
 * Clave en colitis ulcerosa, donde es el marcador de actividad de la enfermedad.
 */
enum class SangreEnHeces(@StringRes val displayName: Int) {
    NINGUNA(R.string.sangre_ninguna),
    LIGERA(R.string.sangre_ligera),
    MODERADA(R.string.sangre_moderada),
    ABUNDANTE(R.string.sangre_abundante),
}