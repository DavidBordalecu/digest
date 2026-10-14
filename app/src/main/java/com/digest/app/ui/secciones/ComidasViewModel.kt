package com.digest.app.ui.secciones

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.digest.app.DigestApplication
import com.digest.app.data.DigestRepository
import com.digest.app.data.local.entity.ComidaEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.model.TipoComida
import com.digest.app.notificaciones.PlanificadorControlComida
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class EstadoComidas(
    val perfil: PerfilEntity? = null,
    val comidas: List<ComidaEntity> = emptyList(),
    /** Tipo deducido de la hora, para dejarlo ya puesto en el diálogo. */
    val tipoSugerido: TipoComida = TipoComida.ALMUERZO,
    /** Últimas comidas anotadas, para proponerlas con un toque. */
    val alimentosRecientes: List<String> = emptyList(),
)

class ComidasViewModel(
    private val repositorio: DigestRepository,
    private val contexto: Context,
) : ViewModel() {

    private val fecha: LocalDate = LocalDate.now()

    /**
     * El tipo se deduce de la hora para que el diálogo venga con algo
     * puesto. Es una sugerencia y el paciente la cambia con un toque, así
     * que no importa que a las nueve de la noche proponga merienda: está
     * más abajo en la lista y se elige la de al lado.
     */
    private val tipoSugerido: TipoComida = sugerirPorHora()

    private fun sugerirPorHora(): TipoComida {
        val hora = LocalTime.now().hour
        return TipoComida.entries.minByOrNull {
            kotlin.math.abs(it.horaSugerida - hora)
        } ?: TipoComida.ALMUERZO
    }

    /**
     * Las comidas de días anteriores se piden una vez y se guardan en
     * memoria: no cambian mientras la pantalla esté abierta y consultarlas
     * en cada recomposición sería absurdo.
     */
    private val recientes = mutableStateOf<List<String>>(emptyList())

    val estado: StateFlow<EstadoComidas> = combine(
        repositorio.observarPerfil(),
        repositorio.observarComidas(fecha),
    ) { perfil, comidas ->
        EstadoComidas(
            perfil = perfil,
            comidas = comidas,
            tipoSugerido = tipoSugerido,
            alimentosRecientes = recientes.value,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoComidas())

    init {
        viewModelScope.launch {
            recientes.value = repositorio.alimentosRecientes(fecha)
        }
    }

    fun guardarComida(tipo: TipoComida, alimentos: String?, hora: LocalTime) {
        viewModelScope.launch {
            val id = repositorio.guardarComida(
                ComidaEntity(
                    fecha = fecha,
                    hora = hora,
                    tipo = tipo,
                    alimentos = alimentos,
                ),
            )

            // Al guardar la comida se programa la pregunta de las dos horas.
            // Va fuera del diálogo a propósito: el paciente ya ha answered
            // lo que tenía que responder y la app puede estar en segundo
            // plano, así que no se depende de que siga abierta.
            PlanificadorControlComida.registrarYProgramar(
                context = contexto,
                comidaId = id,
                fecha = fecha,
                horaComida = hora,
            )

            // Lo recién anotado pasa a estar disponible como sugerencia,
            // por si a la hora siguiente vuelve a comerse lo mismo.
            if (alimentos != null) {
                recientes.value = (listOf(alimentos) + recientes.value)
                    .distinct()
                    .take(8)
            }
        }
    }

    fun borrar(id: Long) {
        viewModelScope.launch { repositorio.borrarComida(id) }
    }

    companion object {
        val fabrica = viewModelFactory {
            initializer {
                val aplicacion = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as DigestApplication
                ComidasViewModel(aplicacion.repositorio, aplicacion)
            }
        }
    }
}