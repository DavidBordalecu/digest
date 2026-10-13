package com.digest.app.domain

import com.digest.app.model.Condicion
import com.digest.app.model.SangreEnHeces
import com.digest.app.model.SensacionGeneral
import com.digest.app.model.TipoSintoma

/**
 * Cuánta urgencia hay en consultar con la médico.
 *
 * Ninguno de estos niveles es un diagnóstico: la app no sabe qué le pasa al
 * paciente, solo reconoce patrones que en la práctica obligan a no esperar.
 */
enum class NivelUrgencia {
    /** Consultation programada, no hace falta adelantarla. */
    NORMAL,

    /** Conviene pedir cita en los próximos días. */
    PRONTO,

    /** Hay que contactar hoy con el centro de salud. */
    URGENTE,
}

/**
 * Una señal que ha saltado, con el motivo por el que lo ha hecho.
 *
 * [porque] no es un adorno: una alerta sin explicar el motivo hace
 * que el paciente no entienda por qué la app le está preocupando, y acaba
 * ignorar todas las alertas.
 */
data class SenalClinica(
    val titulo: String,
    val explicacion: String,
    val porque: String,
    val nivel: NivelUrgencia,
)

/**
 * Foto del paciente en un momento concreto, en los términos que necesita el
 * evaluador.
 *
 * Se construye aparte de las entidades de la base de datos para que las
 * reglas se puedan probar sin Room ni Android.
 */
data class ContextoClinico(
    val condicion: Condicion,
    /** Días desde la última consulta, o `null` si nunca se ha registrado. */
    val diasDesdeUltimaConsulta: Int?,
    /** Cada cuántos meses suele revisarse. */
    val intervaloConsultaMeses: Int,
    /** Síntomas marcados hoy, con su gravedad del 1 al 5. */
    val sintomasHoy: Map<TipoSintoma, Int>,
    /** Días consecutivos que lleva presente cada síntoma. */
    val diasConSintoma: Map<TipoSintoma, Int>,
    /** Deposiciones de hoy, con su sangre y consistencia. */
    val deposicionesHoy: List<DeposicionDia>,
    /** Número de deposiciones en los últimos siete días. */
    val deposicionesUltimosSieteDias: Int,
    /** Pérdida de peso acumulada en tres meses, en porcentaje. */
    val perdidaPesoPorcentaje: Float?,
    /** Cómo se siente en general. */
    val sensacion: SensacionGeneral?,
    /** Manifestación perianal registrada en los últimos diez días. */
    val manifestacionPerianalReciente: Boolean,
) {
    companion object {
        val VACIO = ContextoClinico(
            condicion = Condicion.COLITIS_ULCEROSA,
            diasDesdeUltimaConsulta = null,
            intervaloConsultaMeses = 6,
            sintomasHoy = emptyMap(),
            diasConSintoma = emptyMap(),
            deposicionesHoy = emptyList(),
            deposicionesUltimosSieteDias = 0,
            perdidaPesoPorcentaje = null,
            sensacion = null,
            manifestacionPerianalReciente = false,
        )
    }
}

/** Datos de una deposición que el evaluador necesita. */
data class DeposicionDia(
    val nivelBristol: Int,
    val sangre: SangreEnHeces,
)

/**
 * Señales de alarma.
 *
 * Los umbrales salen de lo que se considera habitual en el seguimiento
 * de la enfermedad inflamatoria intestinal. No sustituyen al criterio de la
 * unidad de Digestivo: sirven para que nadie se quede sin avisar por no
 * acordarse, que es el problema real que resuelven estas apps.
 */
object AlertasClinicas {

    /** Gravedad 4 o más: el paciente la describe como intensa. */
    private const val GRAVEDAD_INTENSA = 4

    /** Gravedad 3 o más: moderada o intensa. */
    private const val GRAVEDAD_MODERADA = 3

    /** Días de síntomas seguidos antes de pedir revisión. */
    private const val DIAS_SINTOMA_PERSISTENTE = 14

    /** Porcentaje de pérdida de peso que obliga a revisar. */
    private const val PESO_QUE_PREOCUPA = 5f

    /** Más de esto en un día apunta a descompensación. */
    private const val DEPOSICIONES_ALERTAS = 8

    /** Margen sobre el intervalo habitual de revisión. */
    private const val MARGEN_REVISION = 0.75

    /**
     * Evalúa el contexto y devuelve las señales activas, de más a menos
     * urgente. Si no hay ninguna, la lista va vacía.
     */
    fun evaluar(contexto: ContextoClinico): List<SenalClinica> {
        val senales = mutableListOf<SenalClinica>()

        sangradoAbundante(contexto)?.let(senales::add)
        fiebreAlta(contexto)?.let(senales::add)
        dolorIntenso(contexto)?.let(senales::add)
        manifestacionPerianalGrave(contexto)?.let(senales::add)
        deposicionesDescompensacion(contexto)?.let(senales::add)
        sintomaPersistente(contexto)?.let(senales::add)
        perdidaDePeso(contexto)?.let(senales::add)
        sintomaNuevoProlongado(contexto)?.let(senales::add)
        revisionPendiente(contexto)?.let(senales::add)

        return senales.sortedByDescending { it.nivel.ordinal }
    }

    /** ¿Hay alguna señal que pida contactar hoy? */
    fun hayUrgente(contexto: ContextoClinico): Boolean =
        evaluar(contexto).any { it.nivel == NivelUrgencia.URGENTE }

    // --- ReglasIndividuales ---
    // Cada una devuelve null si no se cumple, para que se puedan probar por
    // separado sin montar un contexto entero.

    private fun sangradoAbundante(c: ContextoClinico): SenalClinica? {
        val hayAbundante = c.deposicionesHoy.any { it.sangre == SangreEnHeces.ABUNDANTE }
        if (!hayAbundante) return null
        return SenalClinica(
            titulo = "Sangrado abundante",
            explicacion = "Has registrado sangre abundante en las heces. " +
                "En una enfermedad inflamatoria puede indicar que la mucosa está muy dañada.",
            porque = "Sangre abundante registrada hoy",
            nivel = NivelUrgencia.URGENTE,
        )
    }

    private fun fiebreAlta(c: ContextoClinico): SenalClinica? {
        val gravedad = c.sintomasHoy[TipoSintoma.FIEBRE_FEBRICULA] ?: return null
        if (gravedad < GRAVEDAD_INTENSA) return null
        return SenalClinica(
            titulo = "Fiebre alta",
            explicacion = "La fiebre en una enfermedad inflamatoria puede indicar una infección " +
                "sobreañadida o un absceso, y no solo un brote.",
            porque = "Has marcado la fiebre como intensa",
            nivel = NivelUrgencia.URGENTE,
        )
    }

    private fun dolorIntenso(c: ContextoClinico): SenalClinica? {
        val gravedad = c.sintomasHoy[TipoSintoma.DOLOR_ABDOMINAL] ?: return null
        if (gravedad < GRAVEDAD_INTENSA) return null
        val esCrohn = c.condicion == Condicion.CROHN
        return SenalClinica(
            titulo = "Dolor abdominal intenso",
            explicacion = if (esCrohn) {
                "En Crohn un dolor intenso y continuo puede indicar inflamación localizada, " +
                    "una estenosis o un absceso. No debería calmarse con la analgesia habitual."
            } else {
                "Un dolor intenso que no cede puede indicar una complicación. " +
                    "Conviene valorarlo hoy."
            },
            porque = "Has marcado el dolor como intenso",
            nivel = NivelUrgencia.URGENTE,
        )
    }

    private fun manifestacionPerianalGrave(c: ContextoClinico): SenalClinica? {
        // Solo tiene sentido en Crohn: las complicaciones perianales son su
        // característica propia y pueden requerir cirugía.
        if (!c.condicion.afectaIntestinoDelgado) return null

        val gravedad = c.sintomasHoy[TipoSintoma.ABSCESO_ANAL]
        val grave = gravedad != null && gravedad >= GRAVEDAD_MODERADA
        val nueva = c.manifestacionPerianalReciente && (grave || c.sintomasHoy[TipoSintoma.FISTULA_ANAL] != null)
        if (!nueva) return null

        return SenalClinica(
            titulo = "Posible absceso o fístula perianal",
            explicacion = "Una afección perianal nueva en Crohn puede necesitar drenaje " +
                "quirúrgico, y esperar aumenta las probabilidades de intervención posterior.",
            porque = "Has registrado una manifestación perianal en los últimos días",
            nivel = NivelUrgencia.URGENTE,
        )
    }

    private fun deposicionesDescompensacion(c: ContextoClinico): SenalClinica? {
        val diarreicas = c.deposicionesHoy.count { it.nivelBristol >= 5 }
        if (diarreicas < DEPOSICIONES_ALERTAS) return null
        return SenalClinica(
            titulo = "Many deposiciones diarreicas",
            explicacion = "Un volumen alto de deposiciones diarreicas puede deshidratarte, " +
                "sobre todo si hay fiebre o estomía.",
            porque = "$diarreicas deposiciones diarreicas registradas hoy",
            nivel = NivelUrgencia.URGENTE,
        )
    }

    private fun sintomaPersistente(c: ContextoClinico): SenalClinica? {
        val persistente = c.sintomasHoy
            .filter { (c.diasConSintoma[it.key] ?: 0) >= DIAS_SINTOMA_PERSISTENTE }
            .keys
            .firstOrNull()
            ?: return null

        val dias = c.diasConSintoma[persistente] ?: 0
        return SenalClinica(
            titulo = "Síntoma que se mantiene",
            explicacion = "«${persistente.nombreLegible}» lleva $dias días seguidos. " +
                "Un síntoma que no se calma necesita revisión, aunque sea leve.",
            porque = "$dias días con el síntoma activo",
            nivel = NivelUrgencia.PRONTO,
        )
    }

    private fun perdidaDePeso(c: ContextoClinico): SenalClinica? {
        val perdida = c.perdidaPesoPorcentaje ?: return null
        if (perdida < PESO_QUE_PREOCUPA) return null
        return SenalClinica(
            titulo = "Pérdida de peso relevante",
            explicacion = "Has perdido un ${"%.1f".format(perdida)} % de tu peso en tres meses. " +
                "En Crohn puede indicar malabsorción o un brote activo.",
            porque = "Descenso de peso superior al 5 % en tres meses",
            nivel = NivelUrgencia.PRONTO,
        )
    }

    private fun sintomaNuevoProlongado(c: ContextoClinico): SenalClinica? {
        // Sensación general mala con varios síntomas a la vez: suele
        // acompañar a un brote que no se está tratando.
        val cuantos = c.sintomasHoy.size
        val mal = c.sensacion == SensacionGeneral.MAL || c.sensacion == SensacionGeneral.MUY_MAL
        if (cuantos < 3 || !mal) return null

        return SenalClinica(
            titulo = "Varios síntomas a la vez",
            explicacion = "Tienes $cuantos síntomas activos y te sientes mal en general. " +
                "Esa combinación suele indicar que la enfermedad no está bien controlada.",
            porque = "$cuantos síntomas activos con sensación general mala",
            nivel = NivelUrgencia.PRONTO,
        )
    }

    private fun revisionPendiente(c: ContextoClinico): SenalClinica? {
        val dias = c.diasDesdeUltimaConsulta ?: return null
        val limite = (c.intervaloConsultaMeses * 30 * MARGEN_REVISION).toInt()
        if (dias < limite) return null

        return SenalClinica(
            titulo = "Toca revisión",
            explicacion = "Tu última consulta fue hace $dias días y sueles revisar cada " +
                "${c.intervaloConsultaMeses} meses. Pedir cita ahora evita esperar semanas.",
            porque = "Han pasado $dias días desde la última consulta",
            nivel = NivelUrgencia.PRONTO,
        )
    }
}

/** Nombre legible del síntoma, sin depender de los recursos de Android. */
val TipoSintoma.nombreLegible: String
    get() = when (this) {
        TipoSintoma.DIARREA -> "Diarrea"
        TipoSintoma.DOLOR_ABDOMINAL -> "Dolor abdominal"
        TipoSintoma.TENESMO -> "Tenesmo"
        TipoSintoma.SANGRE_EN_HECES -> "Sangre en heces"
        TipoSintoma.MUCOSIDAD -> "Mucosidad"
        TipoSintoma.DISTENSION_ABDOMINAL -> "Distensión"
        TipoSintoma.FISURA_ANAL -> "Fisura anal"
        TipoSintoma.FISTULA_ANAL -> "Fístula anal"
        TipoSintoma.ABSCESO_ANAL -> "Absceso perianal"
        TipoSintoma.DOLOR_O_SUPURACION_PERIANAL -> "Dolor o supuración perianal"
        TipoSintoma.INFLAMACION_EXTERNA_PERIANAL -> "Inflamación externa"
        TipoSintoma.FATIGA_ASTENIA -> "Fatiga"
        TipoSintoma.FIEBRE_FEBRICULA -> "Fiebre"
        TipoSintoma.NAUSEAS -> "Náuseas"
        TipoSintoma.PERDIDA_APETITO -> "Pérdida de apetito"
        TipoSintoma.PERDIDA_PESO -> "Pérdida de peso"
        TipoSintoma.DESNUTRICION -> "Desnutrición"
        TipoSintoma.AFTA_BUCAL -> "Aftas en la boca"
        TipoSintoma.DOLOR_ARTICULAR -> "Dolor articular"
        TipoSintoma.MANCHAS_EN_PIEL -> "Manchas en la piel"
        TipoSintoma.UVEITIS_OJO_ROJO -> "Ojo rojo"
    }