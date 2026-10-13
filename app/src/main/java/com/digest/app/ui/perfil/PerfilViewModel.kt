package com.digest.app.ui.perfil

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.digest.app.DigestApplication
import com.digest.app.data.DigestRepository
import com.digest.app.data.local.entity.CirugiaEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.model.TipoCirugia
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EstadoPerfil(
    val perfil: PerfilEntity? = null,
    val cirugias: List<CirugiaEntity> = emptyList(),
) {
    /** IMC calculado, o `null` si todavía no hay peso y altura. */
    val imc: Double? get() = perfil?.imc
}

class PerfilViewModel(private val repositorio: DigestRepository) : ViewModel() {

    val estado: StateFlow<EstadoPerfil> = combine(
        repositorio.observarPerfil(),
        repositorio.observarCirugias(),
    ) { perfil, cirugias -> EstadoPerfil(perfil, cirugias) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoPerfil())

    fun guardarMedidas(peso: Double?, altura: Double?) {
        viewModelScope.launch {
            val actual = repositorio.obtenerPerfil() ?: return@launch
            repositorio.guardarPerfil(
                actual.copy(
                    pesoKg = peso ?: actual.pesoKg,
                    alturaCm = altura ?: actual.alturaCm,
                    actualizadoEn = System.currentTimeMillis(),
                ),
            )
        }
    }

    /**
     * Añade una intervención al historial.
     *
     * Si altera el tránsito, se marca también en el perfil: es el dato que
     * hace que la app pregunte por la hidratación y por el volumen de
     * nutrición enteral.
     */
    fun anadirCirugia(tipo: TipoCirugia) {
        viewModelScope.launch {
            repositorio.guardarCirugia(CirugiaEntity(tipo = tipo))

            if (tipo.alteraTransito) {
                val perfil = repositorio.obtenerPerfil() ?: return@launch
                repositorio.guardarPerfil(
                    perfil.copy(
                        tieneIleostomia = perfil.tieneIleostomia || tipo == TipoCirugia.ILEOSTOMIA,
                        tieneColostomia = perfil.tieneColostomia || tipo == TipoCirugia.COLOSTOMIA,
                        tieneResecionIleocecal = perfil.tieneResecionIleocecal ||
                            tipo == TipoCirugia.RESECCION_ILEOCECAL,
                        actualizadoEn = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    companion object {
        val fabrica = viewModelFactory {
            initializer {
                val aplicacion = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as DigestApplication
                PerfilViewModel(aplicacion.repositorio)
            }
        }
    }
}