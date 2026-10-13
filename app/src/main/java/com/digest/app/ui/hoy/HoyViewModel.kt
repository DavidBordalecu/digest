package com.digest.app.ui.hoy

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.digest.app.DigestApplication
import com.digest.app.data.DigestRepository
import com.digest.app.data.local.TomaConDetalle
import com.digest.app.data.local.entity.ComidaEntity
import com.digest.app.data.local.entity.DiarioEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.data.local.entity.SintomaEntity
import com.digest.app.model.EstadoToma
import com.digest.app.model.TipoSintoma
import com.digest.app.notificaciones.PlanificadorAlarmas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Todo lo que se pinta en la pantalla de hoy. */
data class EstadoHoy(
    val perfil: PerfilEntity? = null,
    val tomas: List<TomaConDetalle> = emptyList(),
    val comidas: List<ComidaEntity> = emptyList(),
    val sintomas: List<SintomaEntity> = emptyList(),
    val diario: DiarioEntity? = null,
) {
    val pendientes: List<TomaConDetalle>
        get() = tomas.filter { it.estado == EstadoToma.PENDIENTE }

    val completadas: List<TomaConDetalle>
        get() = tomas.filter { it.estado != EstadoToma.PENDIENTE }

    /** La siguiente toma que aún hay que hacer. */
    val proxima: TomaConDetalle?
        get() = pendientes.minByOrNull { it.horaProgramada }

    val tomasVencidas: Int
        get() = pendientes.count { it.horaProgramada < java.time.LocalTime.now() }

    /** Síntomas de hoy marcados, para pintar los chips. */
    val sintomasMarcados: Map<TipoSintoma, Int>
        get() = sintomas.associate { it.tipo to it.gravedad }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HoyViewModel(
    private val repositorio: DigestRepository,
    private val contexto: Context,
) : ViewModel() {

    private val fecha: LocalDate = LocalDate.now()

    val estado: StateFlow<EstadoHoy> = combine(
        repositorio.observarPerfil(),
        repositorio.observarTomas(fecha),
        repositorio.observarComidas(fecha),
        repositorio.observarSintomas(fecha),
        repositorio.observarDiario(fecha),
    ) { perfil, tomas, comidas, sintomas, diario ->
        EstadoHoy(
            perfil = perfil,
            tomas = tomas,
            comidas = comidas,
            sintomas = sintomas,
            diario = diario,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoHoy(),
    )

    init {
        prepararHoy()
    }

    /**
     * Crea las tomas que faltan hoy y programa sus avisos.
     *
     * Se llama al construir el ViewModel y cada vez que se entra en la
     * pantalla, porque entre medias el paciente puede añadir un fármaco: sin
     * repetirlo, el horario nuevo no generaría su toma ni su aviso.
     */
    fun prepararHoy() {
        viewModelScope.launch {
            val horarios = repositorio.horariosDeHoy(fecha)
            repositorio.materializarTomasDelDia(fecha, horarios)
            PlanificadorAlarmas.programar(contexto, repositorio)
        }
    }

    fun marcarTomada(toma: TomaConDetalle) {
        viewModelScope.launch {
            repositorio.marcarTomada(toma.id)
            // Ya avisó: se retira la alarma y se limpia el aviso en pantalla.
            PlanificadorAlarmas.cancelar(contexto, toma.id)
        }
    }

    fun marcarOmitida(toma: TomaConDetalle) {
        viewModelScope.launch {
            repositorio.marcarOmitida(toma.id)
            PlanificadorAlarmas.cancelar(contexto, toma.id)
        }
    }

    fun volverAPendiente(toma: TomaConDetalle) {
        viewModelScope.launch {
            repositorio.volverAPendiente(toma.id)
            // Al reponerla hay que volver a programar el aviso.
            PlanificadorAlarmas.programar(contexto, repositorio)
        }
    }

    fun alternarSintoma(tipo: TipoSintoma) {
        viewModelScope.launch {
            val gravedadActual = estado.value.sintomasMarcados[tipo] ?: gravedadPorDefecto
            if (tipo in estado.value.sintomasMarcados) {
                repositorio.quitarSintoma(fecha, tipo)
            } else {
                repositorio.marcarSintoma(fecha, tipo, gravedadActual)
            }
        }
    }

    /** Gravedad con la que se marca un síntoma nuevo. */
    private val gravedadPorDefecto = 2

    companion object {
        val fabrica = viewModelFactory {
            initializer {
                val aplicacion = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as DigestApplication
                HoyViewModel(aplicacion.repositorio, aplicacion)
            }
        }
    }
}