package com.digest.app.ui.secciones

import android.app.Application
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class EstadoComidas(
    val perfil: PerfilEntity? = null,
    val comidas: List<ComidaEntity> = emptyList(),
)

class ComidasViewModel(private val repositorio: DigestRepository) : ViewModel() {

    private val fecha: LocalDate = LocalDate.now()

    val estado: StateFlow<EstadoComidas> = combine(
        repositorio.observarPerfil(),
        repositorio.observarComidas(fecha),
    ) { perfil, comidas -> EstadoComidas(perfil, comidas) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoComidas())

    /** Prepara el tipo de comida siguiente según la hora actual. */
    fun nuevaComida() {
        val hora = LocalTime.now()
        val tipo = TipoComida.entries
            .minByOrNull { kotlin.math.abs(it.horaSugerida - hora.hour) }
            ?: TipoComida.ALMUERZO

        viewModelScope.launch {
            repositorio.guardarComida(
                ComidaEntity(fecha = fecha, hora = hora, tipo = tipo),
            )
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
                ComidasViewModel(aplicacion.repositorio)
            }
        }
    }
}