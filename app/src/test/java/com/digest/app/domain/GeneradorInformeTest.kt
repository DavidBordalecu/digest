package com.digest.app.domain

import com.digest.app.data.local.entity.DeposicionEntity
import com.digest.app.data.local.entity.DiarioEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.data.local.entity.PesajeEntity
import com.digest.app.model.Condicion
import com.digest.app.model.EstadoBrote
import com.digest.app.model.EstadoToma
import com.digest.app.model.NivelMejora
import com.digest.app.model.SangreEnHeces
import com.digest.app.model.SensacionGeneral
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pruebas del informe que ve el médico.
 *
 * Aquí no hay nada.UI: solo cifras. Un error de cálculo aquí acaba
 * présentesentando unos datos equivocar al profesional que lleva el
 * tratamiento, así que se prueban los totales, los porcentajes y el
 * formato.
 */
class GeneradorInformeTest {

    private val hoy = LocalDate.of(2026, 10, 13)
    private val inicio = hoy.minusDays(6)

    private fun informe(
        perfil: PerfilEntity? = PerfilEntity(
            afectaciones = emptyList(),
            pesoKg = null,
            alturaCm = null,
            condicion = Condicion.CROHN,
            tieneIleostomia = true,
            ultimaConsulta = hoy.minusDays(200),
        ),
        deposiciones: List<DeposicionEntity> = emptyList(),
        sintomas: Map<LocalDate, List<SintomaDia>> = emptyMap(),
        diario: Map<LocalDate, DiarioEntity> = emptyMap(),
        pesajes: List<PesajeEntity> = emptyList(),
        senales: List<SenalClinica> = emptyList(),
    ) = InformePeriodo(
        desde = inicio,
        hasta = hoy,
        perfil = perfil,
        tomas = emptyList(),
        deposiciones = deposiciones,
        sintomas = sintomas,
        comidas = emptyMap(),
        diario = diario,
        pesajes = pesajes,
        senales = senales,
    )

    private fun deposicion(
        fecha: LocalDate,
        bristol: Int,
        sangre: SangreEnHeces = SangreEnHeces.NINGUNA,
    ) = com.digest.app.data.local.entity.DeposicionEntity(
        fecha = fecha,
        hora = LocalTime.of(9, 0),
        bristol = bristol,
        sangre = sangre,
    )

    // --- Estructura ---

    @Test
    fun `el periodo cuenta bien los días`() {
        assertEquals(7, informe().dias)
    }

    @Test
    fun `un periodo de un solo día no da cero`() {
        val unDia = InformePeriodo(
            desde = hoy,
            hasta = hoy,
            perfil = null,
            tomas = emptyList(),
            deposiciones = emptyList(),
            sintomas = emptyMap(),
            comidas = emptyMap(),
            diario = emptyMap(),
            pesajes = emptyList(),
            senales = emptyList(),
        )

        assertEquals(1, unDia.dias)
    }

    @Test
    fun `el informe incluye el periodo y las secciones`() {
        val texto = GeneradorInforme.texto(informe())

        assertTrue(texto.contains("13/10/2026"))
        assertTrue(texto.contains("07/10/2026"))
        assertTrue(texto.contains("7 días"))
        assertTrue(texto.contains("MEDICACIÓN"))
        assertTrue(texto.contains("SÍNTOMAS"))
        assertTrue(texto.contains("DEPOSICIONES"))
    }

    @Test
    fun `el informe aclara que no es una valoracion medica`() {
        val texto = GeneradorInforme.texto(informe())
        assertTrue(
            "Debe dejar claro que no es una valoración médica",
            texto.contains("No es una valoración médica"),
        )
    }

    // --- Paciente ---

    @Test
    fun `el informe recoge el diagnostico y los antecedentes`() {
        val texto = GeneradorInforme.texto(informe())

        assertTrue(texto.contains("Enfermedad de Crohn"))
        assertTrue(texto.contains("Ostomía: sí"))
    }

    @Test
    fun `si no hay perfil no inventa uno`() {
        val texto = GeneradorInforme.texto(informe(perfil = null))
        assertTrue(!texto.contains("PACIENTE"))
    }

    // --- Síntomas ---

    @Test
    fun `resume los sintomas con frecuencia e intensidad media`() {
        val texto = GeneradorInforme.texto(
            informe(
                sintomas = mapOf(
                    hoy to listOf(
                        SintomaDia("Diarrea", 4),
                        SintomaDia("Fatiga", 2),
                    ),
                    hoy.minusDays(1) to listOf(SintomaDia("Diarrea", 2)),
                ),
            ),
        )

        assertTrue(texto.contains("Diarrea: 2 de 7 días"))
        // Media de (4 + 2) / 2 = 3.0
        assertTrue(texto.contains("media 3,0"))
    }

    @Test
    fun `incluye el detalle dia a dia`() {
        val texto = GeneradorInforme.texto(
            informe(sintomas = mapOf(hoy to listOf(SintomaDia("Diarrea", 3)))),
        )

        assertTrue(texto.contains("Diarrea (3/5)"))
    }

    @Test
    fun `los dias sin sintomas no aparecen en el detalle`() {
        val texto = GeneradorInforme.texto(
            informe(sintomas = mapOf(hoy to listOf(SintomaDia("Diarrea", 3)))),
        )

        val detalle = texto.substringAfter("Detalle día a día:")
        assertEquals(1, detalle.lines().count { it.trim().startsWith("lun") || it.contains("(") })
    }

    // --- Deposiciones ---

    @Test
    fun `cuenta deposiciones y calcula la media diaria`() {
        val texto = GeneradorInforme.texto(
            informe(
                deposiciones = listOf(
                    deposicion(hoy, 4),
                    deposicion(hoy, 6),
                    deposicion(hoy.minusDays(1), 4),
                ),
            ),
        )

        assertTrue(texto.contains("Total: 3"))
        // 3 deposiciones en 7 días
        assertTrue(texto.contains("media 0,4 al día"))
    }

    @Test
    fun `cuenta las deposiciones con sangre`() {
        val texto = GeneradorInforme.texto(
            informe(
                deposiciones = listOf(
                    deposicion(hoy, 6, SangreEnHeces.ABUNDANTE),
                    deposicion(hoy.minusDays(1), 4),
                ),
            ),
        )

        assertTrue(texto.contains("Con sangre: 1"))
    }

    @Test
    fun `cuenta las deposiciones diarreicas por bristol`() {
        val texto = GeneradorInforme.texto(
            informe(
                deposiciones = List(3) { deposicion(hoy, 7) } +
                    listOf(deposicion(hoy.minusDays(1), 4)),
            ),
        )

        assertTrue(texto.contains("Diarreicas (Bristol 5-7): 3"))
    }

    // --- Peso ---

    @Test
    fun `indica la variacion de peso`() {
        val texto = GeneradorInforme.texto(
            informe(
                pesajes = listOf(
                    PesajeEntity(fecha = inicio, pesoKg = 70.0),
                    PesajeEntity(fecha = hoy, pesoKg = 66.5),
                ),
            ),
        )

        assertTrue(texto.contains("Variación: -3,5 kg"))
    }

    @Test
    fun `no inventa variacion cuando el peso apenas cambia`() {
        val texto = GeneradorInforme.texto(
            informe(
                pesajes = listOf(
                    PesajeEntity(fecha = inicio, pesoKg = 70.0),
                    PesajeEntity(fecha = hoy, pesoKg = 70.02),
                ),
            ),
        )

        assertTrue(!texto.contains("Variación"))
    }

    // --- Diario ---

    @Test
    fun `traslada las anotaciones del diario`() {
        val texto = GeneradorInforme.texto(
            informe(
                diario = mapOf(
                    hoy to DiarioEntity(
                        fecha = hoy,
                        mejora = NivelMejora.MEJORA_CLARA,
                        sensacion = SensacionGeneral.REGULAR,
                        estadoBrote = EstadoBrote.LEVE,
                    ),
                ),
            ),
        )

        assertTrue(texto.contains("mejora: mejora clara"))
        assertTrue(texto.contains("sensación: regular"))
        assertTrue(texto.contains("brote: brote leve"))
    }

    // --- Señales ---

    @Test
    fun `incluye las señales con su nivel`() {
        val texto = GeneradorInforme.texto(
            informe(
                senales = listOf(
                    SenalClinica("Fiebre alta", "explicación", "marada intensa", NivelUrgencia.URGENTE),
                    SenalClinica("Toca revisión", "explicación", "200 días", NivelUrgencia.PRONTO),
                ),
            ),
        )

        assertTrue(texto.contains("[URGENTE] Fiebre alta"))
        assertTrue(texto.contains("[REVISAR] Toca revisión"))
    }

    @Test
    fun `dice cuando no hay senales`() {
        val texto = GeneradorInforme.texto(informe())
        assertTrue(texto.contains("SEÑALES DE ALERTA REGISTRADAS"))
        assertTrue(texto.contains("Ninguna"))
    }

    // --- CSV ---

    @Test
    fun `el csv lleva cabecera y una fila por sintoma`() {
        val csv = GeneradorInforme.csv(
            informe(
                deposiciones = listOf(deposicion(hoy, 6, SangreEnHeces.LIGERA)),
                sintomas = mapOf(
                    hoy to listOf(SintomaDia("Diarrea", 4), SintomaDia("Fatiga", 2)),
                ),
            ),
        )

        val lineas = csv.lines()
        assertEquals("fecha;sintoma;intensidad;deposiciones;sangre;bristol", lineas.first())
        assertEquals(3, lineas.size)
        assertTrue(csv.contains("Diarrea;4;LIGERA;6"))
    }

    @Test
    fun `un dia sin sintomas pero con deposiciones tambien sale`() {
        val csv = GeneradorInforme.csv(
            informe(deposiciones = listOf(deposicion(hoy, 4))),
        )

        assertEquals(2, csv.lines().size)
        assertTrue(csv.contains(";;"))
    }

    @Test
    fun `un dia sin nada no genera fila`() {
        val csv = GeneradorInforme.csv(informe())
        assertEquals(1, csv.lines().size)
    }
}