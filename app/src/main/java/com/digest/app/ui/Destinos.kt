package com.digest.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.digest.app.R
import kotlinx.serialization.Serializable

@Serializable data object Bienvenida : NavKey

@Serializable data object Hoy : NavKey

@Serializable data object Medicamentos : NavKey

@Serializable data object Comidas : NavKey

@Serializable data object Diario : NavKey

@Serializable data object Perfil : NavKey

/**
 * Destinos de la barra inferior.
 *
 * Las cinco pantallas comparten esqueleto, así que la barra inferior se
 * managesin usar el NavDisplay: no hay pila de navegación real que resolver y
 * se evita reconstruirla en cada cambio de pestaña.
 */
enum class Seccion(
    val icono: Int,
    @StringRes val etiqueta: Int,
) {
    HOY(R.drawable.ic_hoy, R.string.seccion_hoy),
    MEDICAMENTOS(R.drawable.ic_pastilla, R.string.seccion_medicamentos),
    COMIDAS(R.drawable.ic_comida, R.string.seccion_comidas),
    DIARIO(R.drawable.ic_diario, R.string.seccion_diario),
    PERFIL(R.drawable.ic_perfil, R.string.seccion_perfil),
}