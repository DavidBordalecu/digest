package com.digest.app.ui.medicamentos

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.digest.app.DigestApplication
import com.digest.app.data.DigestRepository
import com.digest.app.data.local.entity.HorarioEntity
import com.digest.app.data.local.entity.MedicamentoEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.model.TipoPresentacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class EstadoMedicamentos(
    val perfil: PerfilEntity? = null,
    val medicamentos: List<MedicamentoEntity> = emptyList(),
    /** Horarios agrupados por medicamento, para pintar la lista de un vistazo. */
    val horariosPorMedicamento: Map<Long, List<HorarioEntity>> = emptyMap(),
)

class MedicamentosViewModel(private val repositorio: DigestRepository) : ViewModel() {

    private val _editorAbierto = MutableStateFlow<MedicamentoEntity?>(null)

    /** Fármaco en edición, o `null` si el editor está cerrado. */
    val enEdicion: StateFlow<MedicamentoEntity?> = _editorAbierto.asStateFlow()

    val estado: StateFlow<EstadoMedicamentos> = combine(
        repositorio.observarPerfil(),
        repositorio.observarMedicamentos(),
        repositorio.observarTodosLosHorarios(),
    ) { perfil, medicamentos, horarios ->
        EstadoMedicamentos(
            perfil = perfil,
            medicamentos = medicamentos,
            horariosPorMedicamento = horarios.groupBy { it.medicamentoId },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoMedicamentos())

    fun nuevoMedicamento() {
        _editorAbierto.value = MedicamentoEntity(id = 0, nombre = "")
    }

    fun abrirEditor(medicamento: MedicamentoEntity) {
        _editorAbierto.value = medicamento
    }

    fun cerrarEditor() {
        _editorAbierto.value = null
    }

    /**
     * Guarda el fármaco con un único horario a las [hora] indicada.
     *
     * El alta se hace en un solo paso: nombre, dosis y hora. Un paciente con
     * fatiga no rellena un formulario largo, y los horarios múltiples se
     * añaden después desde la ficha del fármaco.
     */
    fun guardar(
        medicamento: MedicamentoEntity,
        hora: LocalTime?,
        presentacion: TipoPresentacion,
        diasSemana: Int,
    ) {
        viewModelScope.launch {
            val id = if (medicamento.id == 0L) {
                repositorio.guardarMedicamento(
                    medicamento.copy(presentacion = presentacion),
                )
            } else {
                repositorio.actualizarMedicamento(
                    medicamento.copy(presentacion = presentacion),
                )
                medicamento.id
            }

            if (hora != null) {
                repositorio.guardarHorario(
                    HorarioEntity(
                        medicamentoId = id,
                        hora = hora,
                        diasSemana = diasSemana,
                        dosis = medicamento.dosis,
                    ),
                )
            }
            cerrarEditor()
        }
    }

    companion object {
        val fabrica = viewModelFactory {
            initializer {
                val aplicacion = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as DigestApplication
                MedicamentosViewModel(aplicacion.repositorio)
            }
        }
    }
}