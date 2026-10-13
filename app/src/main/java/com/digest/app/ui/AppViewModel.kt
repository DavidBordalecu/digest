package com.digest.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.digest.app.DigestApplication
import com.digest.app.data.DigestRepository
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.notificaciones.PlanificadorAlarmas
import com.digest.app.notificaciones.Permisos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Estado global de la navegación y del arranque. */
data class EstadoApp(
    val cargando: Boolean = true,
    val perfilConfigurado: Boolean = false,
    val seccion: Seccion = Seccion.HOY,
    val avisosActivados: Boolean = true,
    val alarmasExactas: Boolean = true,
)

/**
 * Estado de la aplicación.
 *
 * Al arrancar comprueba si ya hay perfil y, si lo hay, prepara el día: crea
 * las tomas que tocan y programa sus avisos. Así las notificaciones están
 * puestas aunque el paciente no abra la app a diario.
 */
class AppViewModel(
    private val repositorio: DigestRepository,
    private val contexto: Context,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoApp())
    val estado: StateFlow<EstadoApp> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val configurado = repositorio.perfilConfigurado()
            _estado.update {
                it.copy(
                    cargando = false,
                    perfilConfigurado = configurado,
                    avisosActivados = Permisos.avisosActivados(contexto),
                    alarmasExactas = Permisos.tieneAlarmasExactas(contexto),
                )
            }
            if (configurado) prepararHoy()
        }
    }

    /** Crea las tomas del día y programa los avisos correspondientes. */
    fun prepararHoy() {
        viewModelScope.launch {
            val hoy = LocalDate.now()
            val horarios = repositorio.horariosDeHoy(hoy)
            repositorio.materializarTomasDelDia(hoy, horarios)
            PlanificadorAlarmas.programar(contexto, repositorio)
        }
    }

    fun crearPerfil(perfil: PerfilEntity) {
        viewModelScope.launch {
            repositorio.guardarPerfil(perfil)
            _estado.update { it.copy(perfilConfigurado = true, cargando = false) }
            prepararHoy()
        }
    }

    fun irA(seccion: Seccion) {
        _estado.update { it.copy(seccion = seccion) }
    }

    /** Refresca el estado de los permisos tras volver de los ajustes. */
    fun refrescarPermisos() {
        _estado.update {
            it.copy(
                avisosActivados = Permisos.avisosActivados(contexto),
                alarmasExactas = Permisos.tieneAlarmasExactas(contexto),
            )
        }
    }

    companion object {
        val fabrica = viewModelFactory {
            initializer {
                val aplicacion = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as DigestApplication
                AppViewModel(aplicacion.repositorio, aplicacion)
            }
        }

        fun paraPruebas(repositorio: DigestRepository, contexto: Context) =
            viewModelFactory {
                initializer { AppViewModel(repositorio, contexto) }
            }
    }
}