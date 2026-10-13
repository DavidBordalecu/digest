package com.digest.app.ui.diario

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.digest.app.DigestApplication
import com.digest.app.R
import com.digest.app.data.DigestRepository
import com.digest.app.data.local.entity.DiarioEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.data.local.entity.SintomaEntity
import com.digest.app.domain.GeneradorInforme
import com.digest.app.model.EstadoBrote
import com.digest.app.model.NivelHidratacion
import com.digest.app.model.NivelMejora
import com.digest.app.model.SensacionGeneral
import com.digest.app.model.TipoSintoma
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

/** Periodos que se pueden exportar. */
enum class PeriodoInforme(val dias: Int, @StringRes val etiqueta: Int) {
    SEMANA(7, R.string.periodo_semana),
    QUINCENA(15, R.string.periodo_quincena),
    MES(30, R.string.periodo_mes),
}

/** Formato del archivo que se genera. */
enum class FormatoInforme(val extension: String, val tipoMime: String) {
    TEXTO("txt", "text/plain"),
    CSV("csv", "text/csv"),
}

/** Un informe ya generado, listo para enviar. */
data class InformeParaCompartir(
    val contenido: String,
    val nombreArchivo: String,
    val tipoMime: String,
)

data class EstadoDiario(
    val perfil: PerfilEntity? = null,
    val mejora: NivelMejora? = null,
    val sensacion: SensacionGeneral? = null,
    val estadoBrote: EstadoBrote? = null,
    val hidratacion: NivelHidratacion? = null,
    val notas: String = "",
    /** Síntomas marcados hoy, con su intensidad. */
    val sintomasHoy: Map<TipoSintoma, Int> = emptyMap(),
    val periodo: PeriodoInforme = PeriodoInforme.SEMANA,
    val generando: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DiarioViewModel(private val repositorio: DigestRepository) : ViewModel() {

    private val fecha: LocalDate = LocalDate.now()

    /** Las notas se editan en local y se guardan al salir del campo. */
    private val notasLocales = MutableStateFlow<String?>(null)
    private val _periodo = MutableStateFlow(PeriodoInforme.SEMANA)
    private val _generando = MutableStateFlow(false)
    private val _informe = MutableStateFlow<InformeParaCompartir?>(null)

    /** Informe ya generado: la interfaz lo envía y lo descarta. */
    val informeParaCompartir: StateFlow<InformeParaCompartir?> = _informe.asStateFlow()

    val estado: StateFlow<EstadoDiario> = repositorio.observarPerfil()
        .flatMapLatest { perfil ->
            kotlinx.coroutines.flow.combine(
                repositorio.observarDiario(fecha),
                repositorio.observarSintomas(fecha),
                _periodo,
                _generando,
            ) { diario, sintomas, periodo, generando ->
                EstadoDiario(
                    perfil = perfil,
                    mejora = diario?.mejora,
                    sensacion = diario?.sensacion,
                    estadoBrote = diario?.estadoBrote,
                    hidratacion = diario?.hidratacion,
                    notas = notasLocales.value ?: diario?.notas.orEmpty(),
                    sintomasHoy = sintomas.associate { it.tipo to it.gravedad },
                    periodo = periodo,
                    generando = generando,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoDiario())

    fun elegirPeriodo(periodo: PeriodoInforme) {
        _periodo.value = periodo
    }

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

    /**
     * Ajusta un síntoma desde el resumen del diario.
     *
     * Reutiliza el mismo ciclo de gravedad que la pantalla de hoy, para que
     * el paciente no aprenda dos manners distintos de marcar lo mismo.
     */
    fun alternarSintoma(tipo: TipoSintoma) {
        viewModelScope.launch {
            val actual = estado.value.sintomasHoy[tipo]
            when {
                actual == null -> repositorio.marcarSintoma(fecha, tipo, 1)
                actual >= 5 -> repositorio.quitarSintoma(fecha, tipo)
                else -> repositorio.marcarSintoma(fecha, tipo, actual + 1)
            }
        }
    }

    /** Genera el informe del periodo elegido y lo deja listo para enviar. */
    fun exportar(formato: FormatoInforme) {
        viewModelScope.launch {
            _generando.value = true
            try {
                val dias = estado.value.periodo.dias
                val hasta = LocalDate.now()
                val desde = hasta.minusDays((dias - 1).toLong())
                val informe = repositorio.prepararInforme(desde, hasta)

                val contenido = when (formato) {
                    FormatoInforme.TEXTO -> GeneradorInforme.texto(informe)
                    FormatoInforme.CSV -> GeneradorInforme.csv(informe)
                }

                _informe.value = InformeParaCompartir(
                    contenido = contenido,
                    nombreArchivo = "digest-${desde}_$hasta.${formato.extension}",
                    tipoMime = formato.tipoMime,
                )
            } finally {
                _generando.value = false
            }
        }
    }

    fun consumirInforme() {
        _informe.value = null
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