package com.digest.app.domain

import com.digest.app.data.local.entity.DeposicionEntity
import com.digest.app.data.local.entity.DiarioEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.data.local.entity.PesajeEntity
import com.digest.app.model.Condicion
import com.digest.app.model.LocalizacionDolor
import com.digest.app.model.EstadoBrote
import com.digest.app.model.EstadoToma
import com.digest.app.model.NivelMejora
import com.digest.app.model.SangreEnHeces
import com.digest.app.model.SensacionGeneral
import com.digest.app.model.TipoDolor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        dolores: List<com.digest.app.data.local.entity.DolorEntity> = emptyList(),
        nutricion: List<com.digest.app.data.local.entity.NutricionEnteralEntity> = emptyList(),
        comidasControladas: List<ComidaControlada> = emptyList(),
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
        dolores = dolores,
        nutricion = nutricion,
        comidasControladas = comidasControladas,
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
            dolores = emptyList(),
            nutricion = emptyList(),
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
        assertTrue(texto.contains("[PRONTO] Toca revisión"))
        // Los tres niveles se nombran: el médico distingue de un vistazo lo
        // urgente de lo que solo conviene revisar.
        assertTrue(
            GeneradorInforme.texto(
                informe(
                    senales = listOf(
                        SenalClinica(
                            "Rutina",
                            "explicación",
                            "motivo",
                            NivelUrgencia.NORMAL,
                        ),
                    ),
                ),
            ).contains("[REVISAR] Rutina"),
        )
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

    @Test
    fun `el informe resume el dolor con su caracter y su zona`() {
        val texto = GeneradorInforme.texto(
            informe(
                dolores = listOf(
                    dolor(hoy, 4, TipoDolor.FIJO, LocalizacionDolor.FOSA_ILIACA_DERECHA),
                    dolor(hoy, 2, TipoDolor.COLICO, LocalizacionDolor.PERIUMBILICAL),
                    dolor(hoy.minusDays(1), 3, TipoDolor.FIJO, LocalizacionDolor.PERIUMBILICAL),
                ),
            ),
        )

        assertTrue(texto.contains("DOLOR ABDOMINAL"))
        assertTrue(texto.contains("Episodios: 3"))
        // Media de 4, 2 y 3 = 3,0 con formato español.
        assertTrue(texto.contains("intensidad media 3,0"))
        assertTrue(texto.contains("máxima 4 de 5"))
        assertTrue(texto.contains("Fijo (2)"))
        assertTrue(texto.contains("Cólico (1)"))
        assertTrue(texto.contains("Fosa ilíaca derecha"))
    }

    @Test
    fun `sin dolor registrado el informe lo dice`() {
        val texto = GeneradorInforme.texto(informe())

        assertTrue(texto.contains("DOLOR ABDOMINAL"))
        assertTrue(texto.contains("Ninguno registrado"))
    }

    @Test
    fun `la nutricion enteral se contabiliza por volumen diario`() {
        val texto = GeneradorInforme.texto(
            informe(
                nutricion = listOf(
                    enteral(hoy, 1000, "Nutribiomil"),
                    enteral(hoy, 500, "Nutribiomil"),
                    enteral(hoy.minusDays(1), 1500, "Nutribiomil"),
                ),
            ),
        )

        assertTrue(texto.contains("NUTRICIÓN ENTERAL"))
        assertTrue(texto.contains("Total: 3000 ml"))
        // 3000 ml repartidos en 7 días.
        assertTrue(texto.contains("428,6 ml al día"))
        assertTrue(texto.contains("Días con nutrición enteral: 2"))
        assertTrue(texto.contains("Producto: Nutribiomil"))
    }

    @Test
    fun `sin nutricion enteral no aparece la seccion`() {
        val texto = GeneradorInforme.texto(informe())

        assertFalse(texto.contains("NUTRICIÓN ENTERAL"))
    }

    private fun dolor(
        fecha: LocalDate,
        intensidad: Int,
        tipo: TipoDolor,
        localizacion: LocalizacionDolor? = null,
    ) = com.digest.app.data.local.entity.DolorEntity(
        fecha = fecha,
        hora = LocalTime.of(9, 0),
        intensidad = intensidad,
        tipo = tipo,
        localizacion = localizacion,
    )

    private fun enteral(
        fecha: LocalDate,
        volumenMl: Int,
        producto: String?,
    ) = com.digest.app.data.local.entity.NutricionEnteralEntity(
        fecha = fecha,
        hora = LocalTime.of(9, 0),
        producto = producto,
        volumenMl = volumenMl,
        tipo = com.digest.app.model.TipoNutricionEnteral.PARCIAL,
    )

    @Test
    fun `el informe separa las comidas que provocaron cambios de las que no`() {
        val texto = GeneradorInforme.texto(
            informe(
                comidasControladas = listOf(
                    ComidaControlada(hoy, LocalTime.of(14, 0), "Almuerzo", "Pollo con arroz", true),
                    ComidaControlada(hoy, LocalTime.of(21, 0), "Cena", "Pizza", true),
                    ComidaControlada(
                        hoy,
                        LocalTime.of(8, 0),
                        "Desayuno",
                        "Tostada con tomate",
                        false,
                    ),
                    ComidaControlada(hoy, LocalTime.of(11, 0), "Media mañana", null, null),
                ),
            ),
        )

        assertTrue(texto.contains("COMIDAS Y REACCIÓN"))
        assertTrue(texto.contains("Con cambios después:"))
        assertTrue(texto.contains("Pollo con arroz"))
        assertTrue(texto.contains("Pizza"))
        // Las que se toleraron bien también se listan: saber qué no ha
        // dado problemas vale tanto como saber qué sí.
        assertTrue(texto.contains("Se toleraron bien: 1"))
        assertTrue(texto.contains("Tostada con tomate"))
        assertTrue(texto.contains("Sin control pendiente: 1"))
    }

    @Test
    fun `sin comidas controladas no aparece la seccion`() {
        val texto = GeneradorInforme.texto(informe())

        assertFalse(texto.contains("COMIDAS Y REACCIÓN"))
    }

    @Test
    fun `el informe no deja lineas en blanco sueltas`() {
        // Secciones que no aplican: sin enteral ni pesajes no deben dejar
        // huecos donde habrían ido.
        val texto = GeneradorInforme.texto(informe())

        assertFalse(
            texto.contains("\n\n\n"),
        )
    }
}
