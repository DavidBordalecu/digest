package com.digest.app.domain

import com.digest.app.data.local.entity.DeposicionEntity
import com.digest.app.data.local.entity.DolorEntity
import com.digest.app.data.local.entity.NutricionEnteralEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.data.local.entity.PesajeEntity
import com.digest.app.model.Condicion
import com.digest.app.model.LocalizacionDolor
import com.digest.app.model.SangreEnHeces
import com.digest.app.model.TipoDolor
import com.digest.app.model.TipoNutricionEnteral
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * No comprueba nada: imprime el informe con datos de ejemplo para poder
 * leerlo tal y como lo verá el médico.
 */
class InformeMuestraTest {

    @Test
    fun `imprime el informe de muestra`() {
        val hoy = LocalDate.of(2026, 10, 14)

        val datos = InformePeriodo(
            desde = hoy.minusDays(6),
            hasta = hoy,
            perfil = PerfilEntity(
                condicion = Condicion.CROHN,
                afectaciones = listOf(
                    com.digest.app.model.Afectacion.ILEITIS,
                    com.digest.app.model.Afectacion.COLITIS_CROHN,
                ),
                fechaDiagnostico = LocalDate.of(2016, 3, 2),
                ultimaConsulta = hoy.minusMonths(7),
                pesoKg = 69.1,
                alturaCm = 180.0,
            ),
            tomas = emptyList(),
            deposiciones = listOf(
                DeposicionEntity(fecha = hoy, hora = LocalTime.of(8, 10), bristol = 4),
                DeposicionEntity(fecha = hoy, hora = LocalTime.of(14, 30), bristol = 6, sangre = SangreEnHeces.LIGERA),
                DeposicionEntity(
                    fecha = hoy.minusDays(1),
                    hora = LocalTime.of(9, 0),
                    bristol = 7,
                    sangre = SangreEnHeces.MODERADA,
                ),
            ),
            dolores = listOf(
                DolorEntity(
                    fecha = hoy,
                    hora = LocalTime.of(13, 0),
                    intensidad = 4,
                    tipo = TipoDolor.FIJO,
                    localizacion = LocalizacionDolor.FOSA_ILIACA_DERECHA,
                ),
                DolorEntity(
                    fecha = hoy,
                    hora = LocalTime.of(20, 0),
                    intensidad = 2,
                    tipo = TipoDolor.COLICO,
                    localizacion = LocalizacionDolor.PERIUMBILICAL,
                ),
            ),
            nutricion = listOf(
                NutricionEnteralEntity(
                    fecha = hoy,
                    hora = LocalTime.of(9, 0),
                    producto = "Nutribiomil",
                    volumenMl = 1000,
                    tipo = TipoNutricionEnteral.PARCIAL,
                ),
            ),
            sintomas = mapOf(
                hoy to listOf(
                    SintomaDia("Diarrea", 4),
                    SintomaDia("Dolor abdominal", 3),
                ),
            ),
            comidas = emptyMap(),
            diario = emptyMap(),
            pesajes = listOf(
                PesajeEntity(fecha = hoy.minusDays(30), pesoKg = 74.2),
                PesajeEntity(fecha = hoy, pesoKg = 69.1),
            ),
            senales = listOf(
                SenalClinica(
                    titulo = "Pérdida de peso",
                    explicacion = "Has perdido un 6,9 % de peso en el último mes.",
                    porque = "Diferencia entre las medias de peso de ambos periodos",
                    nivel = NivelUrgencia.PRONTO,
                ),
            ),
        )

        println("===== INFORME =====")
        println(GeneradorInforme.texto(datos))
        println("===== FIN =====")
    }
}