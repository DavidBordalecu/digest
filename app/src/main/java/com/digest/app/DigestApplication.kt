package com.digest.app

import android.app.Application
import com.digest.app.data.AlmacenDocumentos
import com.digest.app.data.DigestRepository
import com.digest.app.data.local.DigestDatabase
import com.digest.app.notificaciones.AvisosComida
import com.digest.app.notificaciones.AvisosToma

/**
 * Punto de entrada de la aplicación.
 *
 * Mantiene una única instancia de la base de datos y del repositorio. Se usa
 * inyección manual en lugar de Hilt: el grafo es pequeño y así se ahorra una
 * capa de generación de código en cada compilación.
 */
class DigestApplication : Application() {

    lateinit var repositorio: DigestRepository

    /** Copia y guarda los informes que el paciente adjunta. */
    val almacenDocumentos: AlmacenDocumentos by lazy { AlmacenDocumentos(this) }

    private val baseDatos by lazy { DigestDatabase.obtener(this) }

    override fun onCreate() {
        super.onCreate()

        repositorio = DigestRepository(
            perfilDao = baseDatos.perfilDao(),
            cirugiaDao = baseDatos.cirugiaDao(),
            medicamentoDao = baseDatos.medicamentoDao(),
            horarioDao = baseDatos.horarioDao(),
            tomaDao = baseDatos.tomaDao(),
            comidaDao = baseDatos.comidaDao(),
            deposicionDao = baseDatos.deposicionDao(),
            dolorDao = baseDatos.dolorDao(),
            sintomaDao = baseDatos.sintomaDao(),
            diarioDao = baseDatos.diarioDao(),
            pesajeDao = baseDatos.pesajeDao(),
            nutricionDao = baseDatos.nutricionEnteralDao(),
            perianalDao = baseDatos.manifestacionPerianalDao(),
            controlComidaDao = baseDatos.controlComidaDao(),
            documentoDao = baseDatos.documentoDao(),
        )

        // Los canales deben existir antes de que salte la primera alarma.
        AvisosToma.crearCanales(this)
        AvisosComida.crearCanal(this)
    }
}