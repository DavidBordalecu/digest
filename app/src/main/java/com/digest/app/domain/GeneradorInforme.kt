package com.digest.app.domain

import com.digest.app.data.local.TomaConDetalle
import com.digest.app.data.local.entity.ComidaEntity
import com.digest.app.data.local.entity.DeposicionEntity
import com.digest.app.data.local.entity.DiarioEntity
import com.digest.app.data.local.entity.DolorEntity
import com.digest.app.data.local.entity.NutricionEnteralEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.data.local.entity.PesajeEntity
import com.digest.app.model.Afectacion
import com.digest.app.model.Condicion
import com.digest.app.model.EstadoBrote
import com.digest.app.model.EstadoToma
import com.digest.app.model.NivelHidratacion
import com.digest.app.model.NivelMejora
import com.digest.app.model.SensacionGeneral
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/** Los datos de un periodo, ya filtrados y listos para presentar. */
data class InformePeriodo(
    val desde: LocalDate,
    val hasta: LocalDate,
    val perfil: PerfilEntity?,
    val tomas: List<TomaConDetalle>,
    val deposiciones: List<DeposicionEntity>,
    val dolores: List<DolorEntity>,
    val nutricion: List<NutricionEnteralEntity>,
    val sintomas: Map<LocalDate, List<SintomaDia>>,
    val comidas: Map<LocalDate, List<ComidaEntity>>,
    /** Comidas del periodo con su control posterior, para ver la reacción. */
    val comidasControladas: List<ComidaControlada> = emptyList(),
    val diario: Map<LocalDate, DiarioEntity>,
    val pesajes: List<PesajeEntity>,
    val senales: List<SenalClinica>,
) {
    val dias: Int
        get() = (ChronoUnit.DAYS.between(desde, hasta) + 1).toInt().coerceAtLeast(1)

    /** Fechas del periodo, en orden. Se usa para recorrer día a día. */
    val fechas: List<LocalDate>
        get() = (0 until dias).map { desde.plusDays(it.toLong()) }
}

/** Un síntoma con su intensidad, tal como se registró un día. */

/** Una comida con el resultado de su control posterior. */
data class ComidaControlada(
    val fecha: LocalDate,
    val hora: LocalTime,
    val tipo: String,
    val alimentos: String?,
    /** true = notó cambios, false = nada, null = todavía sin responder. */
    val huboCambios: Boolean?,
)

/** Lo que se pinta en la pantalla de hoy. */
data class SintomaDia(val nombre: String, val gravedad: Int)

/**
 * Genera el informe que el paciente lleva a su médico.
 *
 * Se genera en texto plano a propósito: se pega en un correo, se manda por
 * mensajería o se lleva impreso, y no necesita que el médico tenga nada
 * instalado. Para el análisis fino se exporta también un CSV de síntomas.
 */
/**
 * Añade una sección al informe solo si tiene algo que decir.
 *
 * Las secciones opcionales —peso, nutrición enteral— se saltan cuando no
 * hay datos, pero la línea en blanco que las separaba se quedaba igualmente
 * y el informe salía con huecos. Esto es un documento que va a leer un
 * médico: los huecos se notan.
 */
private fun seccion(s: StringBuilder, bloque: (StringBuilder) -> Unit) {
    val propio = StringBuilder()
    bloque(propio)

    val contenido = propio.toString().trimEnd()
    if (contenido.isEmpty()) return

    s.appendLine()
    s.appendLine(contenido)
}

object GeneradorInforme {

    /** Español de España. `Locale(...)` está obsoleto desde Java 19. */
    private val ES: Locale = Locale.Builder()
        .setLanguage("es")
        .setRegion("ES")
        .build()

    private val FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", ES)
    private val DIAS: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", ES)

    /**
     * Los números se formatean siempre en español.
     *
     * Sin indicar el locale, `String.format` usa el del sistema: un móvil en
     * inglés escribiría "media 3.0" y uno en español "media 3,0". En un
     * informe médico esa inconsistencia se nota, así que se fija aquí.
     */

    /**
 * Añade una sección al informe solo si tiene algo que decir.
 *
 * Las secciones opcionales —peso, nutrición enteral— se saltan cuando no
 * hay datos, pero la línea en blanco que las separaba se quedaba igualmente
 * y el informe salía con huecos. Esto es un documento que va a leer un
 * médico: los huecos se notan.
 */
private fun seccion(s: StringBuilder, bloque: (StringBuilder) -> Unit) {
    val propio = StringBuilder()
    bloque(propio)

    val contenido = propio.toString().trimEnd()
    if (contenido.isEmpty()) return

    s.appendLine()
    s.appendLine(contenido)
}

private fun decimal(valor: Double): String = String.format(ES, "%.1f", valor)

    private fun decimalConSigno(valor: Double): String = String.format(ES, "%+.1f", valor)

    fun texto(i: InformePeriodo): String {
        val s = StringBuilder()

        s.appendLine("INFORME DE SEGUIMIENTO — Digest")
        s.appendLine(
            "Periodo: ${i.desde.format(FECHA)} – ${i.hasta.format(FECHA)} " +
                "(${i.dias} días)",
        )
        s.appendLine()

        encabezado(s, i)
        seccion(s) { adherencia(it, i) }
        seccion(s) { resumenSintomas(it, i) }
        seccion(s) { deposiciones(it, i) }
        seccion(s) { comidas(it, i) }
        seccion(s) { dolor(it, i) }
        seccion(s) { nutricion(it, i) }
        seccion(s) { peso(it, i) }
        seccion(s) { diario(it, i) }
        seccion(s) { senales(it, i) }

        s.appendLine()
        s.appendLine(
            "Este informe lo genera el paciente a partir de lo que ha anotado " +
                "en la aplicación. No es una valoración médica.",
        )

        return s.toString().trimEnd()
    }

    /**
     * CSV de síntomas, una fila por día y síntoma.
     *
     * Es lo que le sirve al médico si quiere mirar tendencias fuera de la
     * consulta: se abre en cualquier hoja de cálculo.
     */
    fun csv(i: InformePeriodo): String {
        val s = StringBuilder()
        s.appendLine("fecha;sintoma;intensidad;deposiciones;sangre;bristol")

        for (fecha in i.fechas) {
            val deposicionesDelDia = i.deposiciones.filter { it.fecha == fecha }
            val sintomasDelDia = i.sintomas[fecha].orEmpty()

            if (sintomasDelDia.isEmpty()) {
                // Un día sin síntomas pero con deposiciones también es dato.
                for (d in deposicionesDelDia) {
                    s.appendLine(
                        "${d.fecha.format(FECHA)};;;" +
                            "${d.sangre.name};${d.bristol}",
                    )
                }
                continue
            }

            for (sintoma in sintomasDelDia) {
                if (deposicionesDelDia.isEmpty()) {
                    s.appendLine("${fecha.format(FECHA)};${sintoma.nombre};${sintoma.gravedad};;")
                } else {
                    for (d in deposicionesDelDia) {
                        s.appendLine(
                            "${d.fecha.format(FECHA)};${sintoma.nombre};${sintoma.gravedad};" +
                                "${d.sangre.name};${d.bristol}",
                        )
                    }
                }
            }
        }

        return s.toString().trimEnd()
    }

    // --- Secciones ---

    private fun encabezado(s: StringBuilder, i: InformePeriodo) {
        val perfil = i.perfil ?: return
        s.appendLine("PACIENTE")
        s.appendLine("  Diagnóstico: ${nombreCondicion(perfil.condicion)}")
        if (perfil.afectaciones.isNotEmpty()) {
            s.appendLine(
                "  Zona afectada: " +
                    perfil.afectaciones.joinToString(", ") { nombreAfectacion(it) },
            )
        }
        if (perfil.tieneEstomia) s.appendLine("  Ostomía: sí")
        if (perfil.tieneResecionIleocecal) s.appendLine("  Resección ileocecal: sí")
        perfil.ultimaConsulta?.let { s.appendLine("  Última consulta: ${it.format(FECHA)}") }
    }

    private fun adherencia(s: StringBuilder, i: InformePeriodo) {
        s.appendLine("MEDICACIÓN")
        val total = i.tomas.size
        if (total == 0) {
            s.appendLine("  Sin tomas registradas en el periodo")
            return
        }

        val tomadas = i.tomas.count { it.estado == EstadoToma.TOMADA }
        val omitidas = i.tomas.count { it.estado == EstadoToma.OMITIDA }
        val porcentaje = tomadas * 100 / total

        s.appendLine("  Tomas programadas: $total")
        s.appendLine("  Tomadas: $tomadas ($porcentaje %)")
        s.appendLine("  Omitidas: $omitidas")
        s.appendLine("  Pendientes: ${total - tomadas - omitidas}")
        s.appendLine("  Desglose por fármaco:")
        i.tomas.groupBy { it.medicamentoNombre }
            .toSortedMap()
            .forEach { (nombre, tomas) ->
                val hechas = tomas.count { it.estado == EstadoToma.TOMADA }
                s.appendLine("    · $nombre: $hechas de ${tomas.size}")
            }
    }

    private fun resumenSintomas(s: StringBuilder, i: InformePeriodo) {
        s.appendLine("SÍNTOMAS")
        val todos = i.sintomas.values.flatten()
        if (todos.isEmpty()) {
            s.appendLine("  Ninguno registrado")
            return
        }

        s.appendLine("  Frecuencia (intensidad 1-5):")
        todos.groupBy { it.nombre }
            .mapValues { (_, lista) -> lista.size to lista.map { it.gravedad }.average() }
            .toList()
            .sortedByDescending { it.second.first }
            .forEach { (nombre, datos) ->
                val (dias, media) = datos
                s.appendLine("    · $nombre: $dias de ${i.dias} días, media ${decimal(media)}")
            }

        s.appendLine()
        s.appendLine("  Detalle día a día:")
        var alguno = false
        for (fecha in i.fechas) {
            val delDia = i.sintomas[fecha].orEmpty()
            if (delDia.isEmpty()) continue
            alguno = true
            s.appendLine(
                "    ${fecha.format(DIAS)}: " +
                    delDia.joinToString(", ") { "${it.nombre} (${it.gravedad}/5)" },
            )
        }
        if (!alguno) s.appendLine("    Sin detalle")
    }

    private fun deposiciones(s: StringBuilder, i: InformePeriodo) {
        s.appendLine("DEPOSICIONES")
        if (i.deposiciones.isEmpty()) {
            s.appendLine("  Ninguna registrada")
            return
        }

        val conSangre = i.deposiciones.count { it.sangre.ordinal >= 1 }
        val diarreicas = i.deposiciones.count { it.bristol >= 5 }
        val media = i.deposiciones.map { it.bristol }.average()
        val porDia = i.deposiciones.size.toDouble() / i.dias

        s.appendLine("  Total: ${i.deposiciones.size} (media ${decimal(porDia)} al día)")
        s.appendLine("  Consistencia media (Bristol 1-7): ${decimal(media)}")
        s.appendLine("  Diarreicas (Bristol 5-7): $diarreicas")
        s.appendLine("  Con sangre: $conSangre")
    }

    /**
     * Dolor abdominal: intensidad, carácter y localización.
     *
     * El desglose por carácter y por zona es lo que distingue un espasmo de
     * un dolor inflamatorio fijo, así que se listan los episodios
     * fuertes en lugar de quedarse solo con la media.
     */
    private fun dolor(s: StringBuilder, i: InformePeriodo) {
        s.appendLine("DOLOR ABDOMINAL")
        if (i.dolores.isEmpty()) {
            s.appendLine("  Ninguno registrado")
            return
        }

        val media = i.dolores.map { it.intensidad }.average()
        val maximo = i.dolores.maxOf { it.intensidad }
        s.appendLine(
            "  Episodios: ${i.dolores.size} " +
                "(intensidad media ${decimal(media)}, máxima $maximo de 5)",
        )

        val porTipo = i.dolores.groupingBy { it.tipo }.eachCount()
            .toList().sortedByDescending { it.second }
        s.appendLine(
            "  Por carácter: " + porTipo.joinToString(", ") {
                "${it.first.nombreLegible} (${it.second})"
            },
        )

        val zonas = i.dolores.mapNotNull { it.localizacion }.distinct()
        if (zonas.isNotEmpty()) {
            s.appendLine("  Zonas: " + zonas.joinToString(", ") { it.nombreLegible })
        }
    }

    /**
     * Nutrición enteral: el volumen es lo que se contabiliza en consulta,
     * así que es el total diario el dato que encabeza la sección.
     */
    private fun nutricion(s: StringBuilder, i: InformePeriodo) {
        if (i.nutricion.isEmpty()) return
        s.appendLine("NUTRICIÓN ENTERAL")

        val total = i.nutricion.sumOf { it.volumenMl }
        val mediaDiaria = total.toDouble() / i.dias
        s.appendLine(
            "  Total: $total ml (media ${decimal(mediaDiaria)} ml al día, " +
                "meta habitual 1.500-2.000 ml)",
        )
        s.appendLine(
            "  Días con nutrición enteral: ${i.nutricion.map { it.fecha }.distinct().size}",
        )

        val productos = i.nutricion.mapNotNull { it.producto }.filter { it.isNotBlank() }
        if (productos.isNotEmpty()) {
            s.appendLine("  Producto: " + productos.distinct().joinToString(", "))
        }
    }

    /**
 * Comidas y reacción posterior.
 *
 * Es la correlación que el paciente no consigue apuntar por su cuenta:
 * nadie apunta que dos horas después de un plato были cólicos, porque a
 * esas horas ya está pensando en otra cosa. Aquí queda escrito, y de un
 * vistazo se ve qué se repite cuando la reacción fue mala.
 *
 * Se listan solo las que provokearon cambios, y al final las que se
 * toleraron bien: las dos listas soninformativas, pero al médico le
 * interesa sobre todo la primera.
 */
private fun comidas(s: StringBuilder, i: InformePeriodo) {
    if (i.comidasControladas.isEmpty()) return

    s.appendLine("COMIDAS Y REACCIÓN")

    val conCambios = i.comidasControladas.filter { it.huboCambios == true }
    val sinCambios = i.comidasControladas.filter { it.huboCambios == false }
    val sinResponder = i.comidasControladas.count { it.huboCambios == null }

    if (conCambios.isEmpty()) {
        s.appendLine("  Ninguna comidaCollectorshooting síntomas")
    } else {
        s.appendLine("  Con cambios después:")
        conCambios.forEach { comida ->
            s.appendLine("    · ${comida.hora.toString().take(5)} ${comida.tipo}")
            comida.alimentos?.let { s.appendLine("      $it") }
        }
    }

    if (sinCambios.isNotEmpty()) {
        s.appendLine("  Se toleraron bien: ${sinCambios.size}")
        sinCambios
            .filter { !it.alimentos.isNullOrBlank() }
            .mapNotNull { it.alimentos }
            .distinct()
            .take(8)
            .forEach { s.appendLine("    · $it") }
    }

    if (sinResponder > 0) {
        s.appendLine("  Sin control pendiente: $sinResponder")
    }
}

private fun peso(s: StringBuilder, i: InformePeriodo) {
        if (i.pesajes.isEmpty()) return
        s.appendLine("PESO")
        val primero = i.pesajes.first()
        val ultimo = i.pesajes.last()
        s.appendLine("  Inicial: ${decimal(primero.pesoKg)} kg (${primero.fecha.format(FECHA)})")
        s.appendLine("  Final: ${decimal(ultimo.pesoKg)} kg (${ultimo.fecha.format(FECHA)})")
        val diferencia = ultimo.pesoKg - primero.pesoKg
        if (abs(diferencia) >= 0.1) {
            s.appendLine("  Variación: ${decimalConSigno(diferencia)} kg")
        }
    }

    private fun diario(s: StringBuilder, i: InformePeriodo) {
        val conContenido = i.diario.filterValues { it.mejora != null || it.sensacion != null }
        if (conContenido.isEmpty()) {
            s.appendLine("DIARIO")
            s.appendLine("  Sin anotaciones")
            return
        }

        s.appendLine("DIARIO")
        for (fecha in i.fechas) {
            val d = i.diario[fecha] ?: continue
            val partes = buildList {
                d.mejora?.let { add("mejora: ${nombreMejora(it)}") }
                d.sensacion?.let { add("sensación: ${nombreSensacion(it)}") }
                d.estadoBrote?.let { add("brote: ${nombreBrote(it)}") }
                d.hidratacion?.let { add("hidratación: ${nombreHidratacion(it)}") }
            }
            if (partes.isNotEmpty()) {
                s.appendLine("  ${fecha.format(DIAS)}: ${partes.joinToString(", ")}")
            }
        }
    }

    private fun senales(s: StringBuilder, i: InformePeriodo) {
        s.appendLine("SEÑALES DE ALERTA REGISTRADAS")
        if (i.senales.isEmpty()) {
            s.appendLine("  Ninguna")
            return
        }
        // El nivel se nombra con las tres palabras y no con dos etiquetas
        // genéricas: el médico tiene que distinguir de un vistazo lo que
        // amerita contacto inmediato de lo que solo conviene revisar.
        for (senal in i.senales) {
            val nivel = when (senal.nivel) {
                NivelUrgencia.URGENTE -> "URGENTE"
                NivelUrgencia.PRONTO -> "PRONTO"
                NivelUrgencia.NORMAL -> "REVISAR"
            }
            s.appendLine("  [$nivel] ${senal.titulo} — ${senal.porque}")
        }
    }

    // --- Nombres legibles ---
    //
    // Se resuelven aquí y no con `R.string` porque el informe se genera
    // también desde las pruebas unitarias, donde no hay contexto de Android
    // para resolver un recurso.

    internal fun nombreCondicion(c: Condicion) = when (c) {
        Condicion.COLITIS_ULCEROSA -> "Colitis ulcerosa"
        Condicion.CROHN -> "Enfermedad de Crohn"
        Condicion.INDETERMINADA -> "Colitis indeterminada"
    }

    internal fun nombreAfectacion(a: Afectacion) = when (a) {
        Afectacion.PROCTITIS -> "Proctitis"
        Afectacion.COLITIS_IZQUIERDA -> "Colitis izquierda"
        Afectacion.COLITIS_EXTENSA -> "Colitis extensa"
        Afectacion.PANCOLITIS -> "Pancolitis"
        Afectacion.ILEITIS -> "Íleitis"
        Afectacion.ILEOCOLITIS -> "Íleo-colítica"
        Afectacion.COLITIS_CROHN -> "Colitis de Crohn"
        Afectacion.GASTRODUODENAL -> "Gastroduodenal"
        Afectacion.PERIANAL -> "Perianal"
    }

    internal fun nombreMejora(m: NivelMejora) = when (m) {
        NivelMejora.SIN_MEJORAS -> "sin mejoras"
        NivelMejora.POCA_MEJORA -> "poca mejora"
        NivelMejora.MEJORA_CLARA -> "mejora clara"
        NivelMejora.GRAN_MEJORA -> "gran mejora"
        NivelMejora.EMPEORA -> "empeora"
        NivelMejora.NO_APLICA -> "no aplica"
    }

    internal fun nombreSensacion(s: SensacionGeneral) = when (s) {
        SensacionGeneral.MUY_BIEN -> "muy bien"
        SensacionGeneral.BIEN -> "bien"
        SensacionGeneral.REGULAR -> "regular"
        SensacionGeneral.MAL -> "mal"
        SensacionGeneral.MUY_MAL -> "muy mal"
    }

    internal fun nombreBrote(b: EstadoBrote) = when (b) {
        EstadoBrote.EN_REMISION -> "en remisión"
        EstadoBrote.LEVE -> "brote leve"
        EstadoBrote.MODERADO -> "brote moderado"
        EstadoBrote.SEVERO -> "brote severo"
        EstadoBrote.NO_APLICA -> "no aplica"
    }

    internal fun nombreHidratacion(h: NivelHidratacion) = when (h) {
        NivelHidratacion.NORMAL -> "normal"
        NivelHidratacion.INSUFICIENTE -> "insuficiente"
        NivelHidratacion.EXCESIVA -> "excesiva"
        NivelHidratacion.NO_APLICA -> "no aplica"
    }
}