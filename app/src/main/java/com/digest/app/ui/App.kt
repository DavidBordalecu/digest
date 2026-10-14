package com.digest.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digest.app.notificaciones.Permisos
import com.digest.app.ui.bienvenida.PantallaBienvenida
import com.digest.app.ui.diario.PantallaDiario
import com.digest.app.ui.hoy.PantallaHoy
import com.digest.app.ui.medicamentos.PantallaMedicamentos
import com.digest.app.ui.perfil.PantallaPerfil
import com.digest.app.ui.secciones.PantallaComidas

/**
 * Raíz de la interfaz.
 *
 * Decide entre el alta inicial y la app en sí según exista o no el perfil.
 */
@Composable
fun App(
    modifier: Modifier = Modifier,
    /**
     * Cuántas veces se ha abierto la app desde el aviso de un control de
     * comida. Al cambiar, se salta a la pestaña de hoy para que el paciente
     * pueda marcar lo que ha notado, que es para lo que se le ha preguntado.
     */
    controlAbierto: Int = 0,
) {
    val contexto = LocalContext.current
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.fabrica)
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Preparar el día: crear las tomas pendientes y programar sus avisos.
    LaunchedEffect(estado.perfilConfigurado) {
        if (estado.perfilConfigurado) viewModel.prepararHoy()
    }

    // Aviso de control de comida pulsado: a la pestaña de hoy, que es donde
    // se marcan los síntomas que ha notado.
    LaunchedEffect(controlAbierto) {
        if (controlAbierto > 0) viewModel.irA(Seccion.HOY)
    }

    when {
        estado.cargando -> Unit
        !estado.perfilConfigurado -> PantallaBienvenida(
            alGuardar = viewModel::crearPerfil,
            modifier = modifier,
        )

        else -> Principal(
            seccion = estado.seccion,
            alCambiarSeccion = viewModel::irA,
            modifier = modifier,
        )
    }
}

@Composable
private fun Principal(
    seccion: Seccion,
    alCambiarSeccion: (Seccion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                Seccion.entries.forEach { seccionActual ->
                    NavigationBarItem(
                        selected = seccionActual == seccion,
                        onClick = { alCambiarSeccion(seccionActual) },
                        icon = {
                            Icon(
                                painter = painterResource(seccionActual.icono),
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(seccionActual.etiqueta)) },
                    )
                }
            }
        },
    ) { relleno ->
        Box(Modifier.padding(relleno)) {
            when (seccion) {
                Seccion.HOY -> PantallaHoy()
                Seccion.MEDICAMENTOS -> PantallaMedicamentos()
                Seccion.COMIDAS -> PantallaComidas()
                Seccion.DIARIO -> PantallaDiario()
                Seccion.PERFIL -> PantallaPerfil()
            }
        }
    }
}

/** ¿Se pueden programar alarmas exactas en este dispositivo? */
fun tieneAlarmasExactas(contexto: Context): Boolean = Permisos.tieneAlarmasExactas(contexto)

/**
 * Lleva al usuario a los ajustes del sistema para conceder el permiso de
 * alarmas exactas. Devuelve `false` si la pantalla no existe en su versión.
 */
fun abrirAjustesAlarmasExactas(contexto: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    return runCatching {
        contexto.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${contexto.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        true
    }.getOrDefault(false)
}

/** ¿Están activadas las notificaciones del sistema para la app? */
fun avisosActivados(contexto: Context): Boolean = Permisos.avisosActivados(contexto)