package com.digest.app.domain

import com.digest.app.model.Condicion
import com.digest.app.model.SangreEnHeces
import com.digest.app.model.SensacionGeneral
import com.digest.app.model.TipoSintoma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del motor de señales de alarma.
 *
 * Es la parte de la app con consecuencias más serias: un fallo aquí puede
 * hacer que alguien no llame a su médico, o que llame sin motivo. Cada regla
 * se prueba por separado y se comprueba también el caso de que no salte.
 */
class AlertasClinicasTest {

    private fun contexto(
        condicion: Condicion = Condicion.COLITIS_ULCEROSA,
        diasDesdeUltimaConsulta: Int? = null,
        intervaloConsultaMeses: Int = 6,
        sintomasHoy: Map<TipoSintoma, Int> = emptyMap(),
        diasConSintoma: Map<TipoSintoma, Int> = emptyMap(),
        deposicionesHoy: List<DeposicionDia> = emptyList(),
        deposicionesUltimosSieteDias: Int = 0,
        perdidaPesoPorcentaje: Float? = null,
        sensacion: SensacionGeneral? = null,
        manifestacionPerianalReciente: Boolean = false,
    ) = ContextoClinico(
        condicion = condicion,
        diasDesdeUltimaConsulta = diasDesdeUltimaConsulta,
        intervaloConsultaMeses = intervaloConsultaMeses,
        sintomasHoy = sintomasHoy,
        diasConSintoma = diasConSintoma,
        deposicionesHoy = deposicionesHoy,
        deposicionesUltimosSieteDias = deposicionesUltimosSieteDias,
        perdidaPesoPorcentaje = perdidaPesoPorcentaje,
        sensacion = sensacion,
        manifestacionPerianalReciente = manifestacionPerianalReciente,
    )

    // --- Caso base: no debe saltar nada con datos normales ---

    @Test
    fun `un dia sin nada especial no genera senales`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(
                sintomasHoy = mapOf(
                    TipoSintoma.FATIGA_ASTENIA to 2,
                    TipoSintoma.DOLOR_ABDOMINAL to 1,
                ),
                diasConSintoma = mapOf(TipoSintoma.FATIGA_ASTENIA to 2),
                deposicionesHoy = listOf(DeposicionDia(4, SangreEnHeces.NINGUNA)),
            ),
        )

        assertTrue("No debería saltar ninguna señal: $resultado", resultado.isEmpty())
    }

    // --- Sangrado ---

    @Test
    fun `sangre abundante genera senal urgente`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(deposicionesHoy = listOf(DeposicionDia(6, SangreEnHeces.ABUNDANTE))),
        )

        assertEquals(1, resultado.size)
        assertEquals(NivelUrgencia.URGENTE, resultado.first().nivel)
    }

    @Test
    fun `sangre ligera no genera senal urgente`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(deposicionesHoy = listOf(DeposicionDia(4, SangreEnHeces.LIGERA))),
        )

        assertTrue("La sangre ligera no debe alarma", resultado.isEmpty())
    }

    // --- Fiebre ---

    @Test
    fun `fiebre intensa es urgente pero la moderada no`() {
        val intensa = AlertasClinicas.evaluar(
            contexto(sintomasHoy = mapOf(TipoSintoma.FIEBRE_FEBRICULA to 4)),
        )
        assertEquals(NivelUrgencia.URGENTE, intensa.first().nivel)

        val moderada = AlertasClinicas.evaluar(
            contexto(sintomasHoy = mapOf(TipoSintoma.FIEBRE_FEBRICULA to 3)),
        )
        assertTrue("La febrícula no debe generar señal", moderada.isEmpty())
    }

    // --- Dolor ---

    @Test
    fun `dolor intenso es urgente`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(sintomasHoy = mapOf(TipoSintoma.DOLOR_ABDOMINAL to 5)),
        )

        assertEquals(NivelUrgencia.URGENTE, resultado.first().nivel)
    }

    @Test
    fun `el dolor explica distinto segun sea Crohn`() {
        val crohn = AlertasClinicas.evaluar(
            contexto(
                condicion = Condicion.CROHN,
                sintomasHoy = mapOf(TipoSintoma.DOLOR_ABDOMINAL to 4),
            ),
        ).first()

        val colitis = AlertasClinicas.evaluar(
            contexto(
                condicion = Condicion.COLITIS_ULCEROSA,
                sintomasHoy = mapOf(TipoSintoma.DOLOR_ABDOMINAL to 4),
            ),
        ).first()

        assertTrue(crohn.explicacion.contains("Crohn"))
        assertTrue(colitis.explicacion.contains("complicación"))
    }

    // --- Perianales: solo en Crohn ---

    @Test
    fun `un absceso perianal grave alerta en Crohn`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(
                condicion = Condicion.CROHN,
                sintomasHoy = mapOf(TipoSintoma.ABSCESO_ANAL to 4),
                manifestacionPerianalReciente = true,
            ),
        )

        assertTrue(
            "Debe alertar en Crohn",
            resultado.any { it.nivel == NivelUrgencia.URGENTE },
        )
    }

    @Test
    fun `un absceso perianal no alerta en colitis ulcerosa`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(
                condicion = Condicion.COLITIS_ULCEROSA,
                sintomasHoy = mapOf(TipoSintoma.ABSCESO_ANAL to 4),
                manifestacionPerianalReciente = true,
            ),
        )

        assertTrue(
            "Las complicaciones perianales son propias de Crohn: $resultado",
            resultado.isEmpty(),
        )
    }

    @Test
    fun `una manifestacion perianal antigua ya no alerta`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(
                condicion = Condicion.CROHN,
                sintomasHoy = mapOf(TipoSintoma.ABSCESO_ANAL to 4),
                manifestacionPerianalReciente = false,
            ),
        )

        assertTrue("Lo antiguo ya está en el historial", resultado.isEmpty())
    }

    // --- Deposiciones ---

    @Test
    fun `muchas deposiciones diarreicas es urgente`() {
        val deposiciones = List(9) { DeposicionDia(7, SangreEnHeces.NINGUNA) }
        val resultado = AlertasClinicas.evaluar(contexto(deposicionesHoy = deposiciones))

        assertEquals(NivelUrgencia.URGENTE, resultado.first().nivel)
    }

    @Test
    fun `seis deposiciones diarreicas no bastan`() {
        val deposiciones = List(6) { DeposicionDia(7, SangreEnHeces.NINGUNA) }
        assertTrue(AlertasClinicas.evaluar(contexto(deposicionesHoy = deposiciones)).isEmpty())
    }

    // --- Persistencia ---

    @Test
    fun `un sintoma de quince dias pide revision`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(
                sintomasHoy = mapOf(TipoSintoma.DIARREA to 2),
                diasConSintoma = mapOf(TipoSintoma.DIARREA to 15),
            ),
        )

        assertEquals(NivelUrgencia.PRONTO, resultado.first().nivel)
    }

    @Test
    fun `un sintoma de diez dias todavía no pide revision`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(
                sintomasHoy = mapOf(TipoSintoma.DIARREA to 2),
                diasConSintoma = mapOf(TipoSintoma.DIARREA to 10),
            ),
        )

        assertTrue(resultado.isEmpty())
    }

    // --- Peso ---

    @Test
    fun `perder un seis por ciento del peso pide revision`() {
        val resultado = AlertasClinicas.evaluar(contexto(perdidaPesoPorcentaje = 6f))
        assertEquals(NivelUrgencia.PRONTO, resultado.first().nivel)
    }

    @Test
    fun `perder un dos por ciento no pide revision`() {
        assertTrue(AlertasClinicas.evaluar(contexto(perdidaPesoPorcentaje = 2f)).isEmpty())
    }

    // --- Varios síntomas a la vez ---

    @Test
    fun `tres sintomas con mal estado general piden revision`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(
                sintomasHoy = mapOf(
                    TipoSintoma.DIARREA to 2,
                    TipoSintoma.FATIGA_ASTENIA to 3,
                    TipoSintoma.PERDIDA_APETITO to 2,
                ),
                sensacion = SensacionGeneral.MAL,
            ),
        )

        assertTrue(resultado.any { it.nivel == NivelUrgencia.PRONTO })
    }

    @Test
    fun `tres sintomas peroapore bien no piden revision`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(
                sintomasHoy = mapOf(
                    TipoSintoma.DIARREA to 1,
                    TipoSintoma.FATIGA_ASTENIA to 1,
                    TipoSintoma.PERDIDA_APETITO to 1,
                ),
                sensacion = SensacionGeneral.BIEN,
            ),
        )

        assertTrue(resultado.isEmpty())
    }

    // --- Revisión programada ---

    @Test
    fun `pasado el intervalo habitual se recuerda la revision`() {
        // Intervalo de 6 meses: el límite salta a los 135 días.
        val resultado = AlertasClinicas.evaluar(contexto(diasDesdeUltimaConsulta = 140))

        assertEquals(NivelUrgencia.PRONTO, resultado.first().nivel)
    }

    @Test
    fun `antes del intervalo habitual no se molesta`() {
        val resultado = AlertasClinicas.evaluar(contexto(diasDesdeUltimaConsulta = 100))
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `sin consulta registrada no se dice nada`() {
        val resultado = AlertasClinicas.evaluar(contexto(diasDesdeUltimaConsulta = null))
        assertTrue(resultado.isEmpty())
    }

    // --- Orden y combinaciones ---

    @Test
    fun `las senales urgentes van primero`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(
                sintomasHoy = mapOf(TipoSintoma.DIARREA to 2),
                diasConSintoma = mapOf(TipoSintoma.DIARREA to 20),
                deposicionesHoy = listOf(DeposicionDia(7, SangreEnHeces.ABUNDANTE)),
            ),
        )

        assertTrue("Debería haber dos señales", resultado.size >= 2)
        assertEquals(NivelUrgencia.URGENTE, resultado.first().nivel)
    }

    @Test
    fun `hayUrgente distingue el caso grave del normal`() {
        val grave = AlertasClinicas.hayUrgente(
            contexto(deposicionesHoy = listOf(DeposicionDia(7, SangreEnHeces.ABUNDANTE))),
        )
        val normal = AlertasClinicas.hayUrgente(contexto())

        assertTrue(grave)
        assertTrue(!normal)
    }

    @Test
    fun `toda senal explica por que ha saltado`() {
        val resultado = AlertasClinicas.evaluar(
            contexto(
                sintomasHoy = mapOf(TipoSintoma.DOLOR_ABDOMINAL to 4),
                diasDesdeUltimaConsulta = 200,
            ),
        )

        assertTrue("Ninguna puede ir sin explicación", resultado.all { it.porque.isNotBlank() })
    }
}