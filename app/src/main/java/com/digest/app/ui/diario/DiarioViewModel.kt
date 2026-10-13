package com.digest.app.ui.diario

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.digest.app.DigestApplication
import com.digest.app.data.DigestRepository
import com.digest.app.data.local.entity.DiarioEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.model.EstadoBrote
import com.digest.app.model.NivelHidratacion
import com.digest.app.model.NivelMejora
import com.digest.app.model.SensacionGeneral
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EstadoDiario(
    val perfil: PerfilEntity? = null,
    val mejora: NivelMejora? = null,
    val sensacion: SensacionGeneral? = null,
    val estadoBrote: EstadoBrote? = null,
    val hidratacion: NivelHidratacion? = null,
    val notas: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
class DiarioViewModel(private val repositorio: DigestRepository) : ViewModel() {

    private val fecha: LocalDate = LocalDate.now()

    /** Las notas se editan en local y se guardan al salir del campo. */
    private val notasLocales = MutableStateFlow<String?>(null)

    val estado: StateFlow<EstadoDiario> = repositorio.observarPerfil()
        .flatMapLatest { perfil ->
            repositorio.observarDiario(fecha).map { diario ->
                EstadoDiario(
                    perfil = perfil,
                    mejora = diario?.mejora,
                    sensacion = diario?.sensacion,
                    estadoBrote = diario?.estadoBrote,
                    hidratacion = diario?.hidratacion,
                    notas = notasLocales.value ?: diario?.notas.orEmpty(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoDiario())

    fun elegirMejora(valor: NivelMejora) = guardar { it.copy(mejora = valor) }

    fun elegirSensacion(valor: SensacionGeneral) = guardar { it.copy(sensacion = valor) }

    fun elegirBrote(valor: EstadoBrote) = guardar { it.copy(estadoBrote = valor) }

    fun elegirHidratacion(valor: NivelHidratacion) = guardar { it.copy(hidratacion = valor) }

    fun cambiarNotas(texto: String) {
        notasLocales.value = texto
    }

    fun guardarNotas() {
        val texto = notasLocales.value ?: return
        guardar { it.copy(notas = texto.ifBlank { null }) }
    }

    private fun guardar(transform: (DiarioEntity) -> DiarioEntity) {
        viewModelScope.launch {
            repositorio.actualizarDiario(fecha, transform)
            // Las notas ya están en la base: se sueltan las locales.
            notasLocales.value = null
        }
    }

    companion object {
        val fabrica = viewModelFactory {
            initializer {
                val aplicacion = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as DigestApplication
                DiarioViewModel(aplicacion.repositorio)
            }
        }
    }
}