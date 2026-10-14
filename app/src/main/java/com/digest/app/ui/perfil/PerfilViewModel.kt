package com.digest.app.ui.perfil

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.digest.app.DigestApplication
import com.digest.app.data.AlmacenDocumentos
import com.digest.app.data.DigestRepository
import com.digest.app.data.local.entity.CirugiaEntity
import com.digest.app.data.local.entity.DocumentoEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.model.TipoCirugia
import com.digest.app.model.TipoDocumento
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EstadoPerfil(
    val perfil: PerfilEntity? = null,
    val cirugias: List<CirugiaEntity> = emptyList(),
    val documentos: List<DocumentoEntity> = emptyList(),
) {
    /** IMC calculado, o `null` si todavía no hay peso y altura. */
    val imc: Double? get() = perfil?.imc
}

class PerfilViewModel(
    private val repositorio: DigestRepository,
    private val almacen: AlmacenDocumentos,
) : ViewModel() {

    val estado: StateFlow<EstadoPerfil> = combine(
        repositorio.observarPerfil(),
        repositorio.observarCirugias(),
        repositorio.observarDocumentos(),
    ) { perfil, cirugias, documentos ->
        EstadoPerfil(perfil, cirugias, documentos)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoPerfil())

    init {
        // Los archivos huérfanos no se limpian solos: si la app muere entre
        // copiar el archivo y guardar la fila, el PDF se queda ahí sin que
        // nada lo muestre ni lo borre, ocupando sitio para siempre.
        viewModelScope.launch {
            val nombres = repositorio.documentos().map { it.nombreArchivo }.toSet()
            almacen.limpiarHuerfanos(nombres)
        }
    }


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

    /** El paciente anota hoy que ha ido a la consulta. */
    fun registrarConsultaHoy() {
        viewModelScope.launch {
            repositorio.guardarConsulta(java.time.LocalDate.now())
        }
    }

    fun guardarIntervalo(meses: Int) {
        viewModelScope.launch {
            repositorio.guardarIntervaloConsulta(meses)
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

    /**
     * Adjunta un informe.
     *
     * El archivo se copia antes de tocar la base: si la copia falla no queda
     * una fila apuntando a un archivo que no está.
     */
    fun adjuntar(
        origen: Uri,
        tipo: TipoDocumento,
        fechaDocumento: LocalDate?,
        notas: String?,
        nombre: String?,
        alTerminar: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            almacen.adjuntar(origen, tipo, fechaDocumento, notas, nombre)
                .onSuccess { documento ->
                    repositorio.guardarDocumento(documento)
                    alTerminar(true)
                }
                .onFailure {
                    Log.e(TAG, "No se pudo adjuntar el informe", it)
                    alTerminar(false)
                }
        }
    }

    fun borrar(documento: DocumentoEntity) {
        viewModelScope.launch {
            almacen.borrar(documento) { repositorio.borrarDocumento(it) }
        }
    }

    /** Ruta en disco de un informe, para abrirlo o compartirlo. */
    fun archivoDe(documento: DocumentoEntity): File = almacen.archivoDe(documento)

    companion object {
        private const val TAG = "PerfilViewModel"

        val fabrica = viewModelFactory {
            initializer {
                val aplicacion = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as DigestApplication
                PerfilViewModel(
                    repositorio = aplicacion.repositorio,
                    almacen = aplicacion.almacenDocumentos,
                )
            }
        }
    }
}