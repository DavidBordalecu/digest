package com.digest.app.ui.hoy

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
import com.digest.app.data.local.entity.DeposicionEntity
import com.digest.app.data.local.entity.DiarioEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.data.local.entity.SintomaEntity
import com.digest.app.domain.AlertasClinicas
import com.digest.app.domain.ContextoClinico
import com.digest.app.domain.DeposicionDia
import com.digest.app.domain.NivelUrgencia
import com.digest.app.domain.SenalClinica
import com.digest.app.model.EstadoToma
import com.digest.app.model.TipoSintoma
import com.digest.app.notificaciones.PlanificadorAlarmas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Lo que se pinta en la pantalla de hoy. */
data class EstadoHoy(
    val perfil: PerfilEntity? = null,
    val tomas: List<TomaConDetalle> = emptyList(),
    val comidas: List<ComidaEntity> = emptyList(),
    val sintomas: List<SintomaEntity> = emptyList(),
    val diario: DiarioEntity? = null,
    val deposiciones: List<DeposicionEntity> = emptyList(),
    /** Días consecutivos con cada síntoma activo. */
    val rachaSintomas: Map<TipoSintoma, Int> = emptyMap(),
    /** Señales de alarma activas, ordenadas de más a menos urgente. */
    val senales: List<SenalClinica> = emptyList(),
) {
    val pendientes: List<TomaConDetalle>
        get() = tomas.filter { it.estado == EstadoToma.PENDIENTE }

    val completadas: List<TomaConDetalle>
        get() = tomas.filter { it.estado != EstadoToma.PENDIENTE }

    /** La siguiente toma que aún hay que hacer. */
    val proxima: TomaConDetalle?
        get() = pendientes.minByOrNull { it.horaProgramada }

    val tomasVencidas: Int
        get() = pendientes.count { it.horaProgramada < LocalTime.now() }

    /** Síntomas de hoy marcados, para pintar los chips. */
    val sintomasMarcados: Map<TipoSintoma, Int>
        get() = sintomas.associate { it.tipo to it.gravedad }

    /** La señal más urgente, que encabeza el aviso. */
    val senalPrincipal: SenalClinica? get() = senales.firstOrNull()

    /** ¿Alguna señal pide contactar hoy? */
    val haySenalUrgente: Boolean
        get() = senales.any { it.nivel == NivelUrgencia.URGENTE }

    val diasDesdeUltimaConsulta: Int?
        get() = perfil?.ultimaConsulta?.let {
            ChronoUnit.DAYS.between(it, LocalDate.now()).toInt()
        }
}

/**
 * Confirmación de un registro, para mostrarla justo después de marcar.
 *
 * Es un tipo y no un texto para que la traducción se quede en la capa de
 * interfaz: la lógica no debe decidir cómo se escribe una frase en español.
 */
sealed interface ResultadoRegistro {
    /** Se guardó y no hay ninguna señal activa. */
    data object SinSenales : ResultadoRegistro

    /** Se guardó y hay al menos una señal, con su título. */
    data class ConSenales(val titulo: String, val total: Int, val urgentes: Int) :
        ResultadoRegistro
}

/** Datos reactivos del día, antes de mezclarlos con las señales. */
private data class DatosDelDia(
    val perfil: PerfilEntity?,
    val tomas: List<TomaConDetalle>,
    val comidas: List<ComidaEntity>,
    val sintomas: List<SintomaEntity>,
    val diario: DiarioEntity?,
    val deposiciones: List<DeposicionEntity>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HoyViewModel(
    private val repositorio: DigestRepository,
    private val contexto: Context,
) : ViewModel() {

    private val fecha: LocalDate = LocalDate.now()

    /**
     * Se usan seis flujos, y `combine` solo tiene sobrecargas hasta cinco:
     * a partir de ahí recibe un vararg y devuelve `Array<Any?>`, de ahí las
     * conversiones.
     */
    private val datos = combine(
        repositorio.observarPerfil(),
        repositorio.observarTomas(fecha),
        repositorio.observarComidas(fecha),
        repositorio.observarSintomas(fecha),
        repositorio.observarDiario(fecha),
        repositorio.observarDeposiciones(fecha),
    ) { valores ->
        @Suppress("UNCHECKED_CAST")
        DatosDelDia(
            perfil = valores[0] as PerfilEntity?,
            tomas = valores[1] as List<TomaConDetalle>,
            comidas = valores[2] as List<ComidaEntity>,
            sintomas = valores[3] as List<SintomaEntity>,
            diario = valores[4] as DiarioEntity?,
            deposiciones = valores[5] as List<DeposicionEntity>,
        )
    }

    /** Las señales se recalculan aparte porque exigen consultar el histórico. */
    private val senales = MutableStateFlow<List<SenalClinica>>(emptyList())

    /**
     * Confirmación del último registro.
     *
     * Existe porque marcar un síntoma sin que pase nada visible es
     * desconcertante: el paciente no sabe si se ha guardado, y acaba
     * marcándolo otra vez por si acaso. Con esto sabe al instante si ha
     * quedado registrado y si haSaltado algún aviso.
     */
    private val _resultado = MutableSharedFlow<ResultadoRegistro>(extraBufferCapacity = 1)
    val resultado: SharedFlow<ResultadoRegistro> = _resultado.asSharedFlow()

    val estado: StateFlow<EstadoHoy> = combine(datos, senales) { base, avisos ->
        EstadoHoy(
            perfil = base.perfil,
            tomas = base.tomas,
            comidas = base.comidas,
            sintomas = base.sintomas,
            diario = base.diario,
            deposiciones = base.deposiciones,
            senales = avisos,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoHoy(),
    )

    init {
        prepararHoy()
        recalcularSenales()
    }

    /** Crea las tomas que faltan hoy y programa sus avisos. */
    fun prepararHoy() {
        viewModelScope.launch {
            val horarios = repositorio.horariosDeHoy(fecha)
            repositorio.materializarTomasDelDia(fecha, horarios)
            PlanificadorAlarmas.programar(contexto, repositorio)
        }
    }

    /**
     * Vuelve a evaluar las señales de alarma.
     *
     * Se llama al entrar en la pantalla y después de registrar cualquier
     * síntoma o deposición: las reglas dependen del día completo, así que
     * un cambio a media mañana tiene que reflejarse sin reiniciar la app.
     */
    fun recalcularSenales() {
        viewModelScope.launch { recalcularYNotificar() }
    }

    /**
     * Recalcula las señales y avisa de qué ha quedado.
     *
     * Sin este aviso, marcar un síntoma no produce ninguna señal visible y
     * el paciente no sabe si se ha guardado. Devuelve las señales para que
     * quien la llama pueda informar sin esperar al siguiente redibujado.
     */
    private suspend fun recalcularYNotificar() {
        val evaluadas = construirContexto()?.let { AlertasClinicas.evaluar(it) }.orEmpty()
        senales.value = evaluadas

        _resultado.tryEmit(
            if (evaluadas.isEmpty()) {
                ResultadoRegistro.SinSenales
            } else {
                ResultadoRegistro.ConSenales(
                    titulo = evaluadas.first().titulo,
                    total = evaluadas.size,
                    urgentes = evaluadas.count { it.nivel == NivelUrgencia.URGENTE },
                )
            },
        )
    }

    /**
     * Reúne los datos que necesitan las reglas.
     *
     * Lee de la base en vez de usar `estado`: las reglas se recalculan justo
     * después de escribir un síntoma, y el flujo reactivo todavía no ha
     * emitido cuando eso ocurre, así que desde el estado se leería siempre
     * el valor anterior y las señales irían un paso por detrás.
     *
     * Devuelve `null` si aún no hay perfil: sin diagnóstico no se puede
     * evaluar nada, y mostrar señales sin contexto sería peor que no
     * mostrar ninguna.
     */
    private suspend fun construirContexto(): ContextoClinico? {
        val perfil = repositorio.obtenerPerfil() ?: return null
        val sintomasHoy = repositorio.sintomasDe(fecha)
        val tiposHoy = sintomasHoy.map { it.tipo }.toSet()

        return ContextoClinico(
            condicion = perfil.condicion,
            diasDesdeUltimaConsulta = perfil.ultimaConsulta?.let {
                ChronoUnit.DAYS.between(it, fecha).toInt()
            },
            intervaloConsultaMeses = perfil.intervaloConsultaMeses,
            sintomasHoy = sintomasHoy.associate { it.tipo to it.gravedad },
            diasConSintoma = repositorio.rachasDeSintomas(tiposHoy, fecha),
            deposicionesHoy = repositorio.deposicionesDe(fecha)
                .map { DeposicionDia(it.bristol, it.sangre) },
            deposicionesUltimosSieteDias = repositorio.deposicionesEntre(
                fecha.minusDays(6),
                fecha,
            ),
            perdidaPesoPorcentaje = repositorio.perdidaDePesoPorcentaje(),
            sensacion = repositorio.diarioDe(fecha)?.sensacion,
            manifestacionPerianalReciente = repositorio.manifestacionesPerianalesEntre(
                fecha.minusDays(10),
                fecha,
            ) > 0,
        )
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

    /**
     * Cambia la gravedad de un síntoma al tocar su chip.
     *
     * La gravedad se recorre con toques sucesivos: nada → 1 → 2 → 3 → 4 → 5 →
     * nada. Un interruptor simple no serviría, porque la diferencia entre una
     * febrícula y una fiebre de 39 grados es justo lo que dispara el aviso, y
     * sin ella las señales más graves no saltarían nunca.
     */
    fun alternarSintoma(tipo: TipoSintoma) {
        viewModelScope.launch {
            val actual = estado.value.sintomasMarcados[tipo]
            if (actual == null) {
                repositorio.marcarSintoma(fecha, tipo, GRAVEDAD_INICIAL)
            } else if (actual >= GRAVEDAD_MAXIMA) {
                // En el máximo, el siguiente toque lo desactiva.
                repositorio.quitarSintoma(fecha, tipo)
            } else {
                repositorio.marcarSintoma(fecha, tipo, actual + 1)
            }
            recalcularYNotificar()
        }
    }

    companion object {
        /** Al pulsar un síntoma por primera vez se marca como leve. */
        const val GRAVEDAD_INICIAL = 1

        /** Escala de gravedad del 1 al 5. */
        const val GRAVEDAD_MAXIMA = 5

        val fabrica = viewModelFactory {
            initializer {
                val aplicacion = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as DigestApplication
                HoyViewModel(aplicacion.repositorio, aplicacion)
            }
        }
    }
}