package com.digest.app.notificaciones

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.digest.app.DigestApplication
import com.digest.app.data.DigestRepository
import com.digest.app.data.local.entity.ControlComidaEntity
import com.digest.app.domain.ControlComida
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Programa la pregunta de control posterior a cada comida.
 *
 * Va aparte de [PlanificadorAlarmas] porque el motivo es distinto: los
 * avisos de medicación no admiten retraso, estos sí. Un control de comida
 * llegar tres o cuatro horas tarde no sirve de nada, y llegar puntual
 * tampoco importa tanto. Se reaprovecha el mismo tipo de alarma por no
 * escribir una segunda implementación, pero no se intenta la exacta si no
 * hay permiso: no está en juego que el paciente no tome su medicina.
 */
object PlanificadorControlComida {

    private const val TAG = "PlanificadorControlComida"

    private const val ACCION_PREGUNTAR = "com.digest.app.PREGUNTAR_POSTPRANDIAL"

    /**
     * Crea el control de una comida y programa su aviso.
     *
     * Se llama al guardar la comida. El control queda en la base aunque el
     * aviso no llegue —sin permiso de notificaciones, o con las alarmas
     * bloqueadas por el fabricante— porque la tarjeta de la app lo muestra
     * igualmente.
     */
    suspend fun registrarYProgramar(
        context: Context,
        comidaId: Long,
        fecha: LocalDate,
        horaComida: LocalTime,
    ) {
        val pregunta = ControlComida.momentoDe(horaComida)
        val control = ControlComidaEntity(
            comidaId = comidaId,
            fecha = fecha,
            horaComida = horaComida,
            programadoPara = pregunta,
        )

        val id = (context.applicationContext as DigestApplication)
            .repositorio
            .guardarControlComida(control)

        programar(context, id, fecha, pregunta)
        Log.i(TAG, "Control $id programado para $pregunta")
    }

    /**
     * Reprograma los controles que ya tocaban y siguen sin respuesta.
     *
     * Se llama al abrir la app. Sin esto, un paciente que abrió la app
     * antes de la hora de la pregunta y la cerró se quedaría sin ella
     * hasta el siguiente arranque.
     */
    suspend fun reprogramarPendientes(context: Context, repositorio: DigestRepository) {
        val hoy = LocalDate.now()
        val pendientes = repositorio.controlesPendientes(hoy)
        val ahora = LocalTime.now()

        for (control in pendientes) {
            // Solo los que ya han vencido: los demás mantienen su alarma.
            if (!ControlComida.tocaPreguntar(control.horaComida, ahora)) continue

            AvisosComida.mostrar(
                context,
                control.id,
                repositorio.textoDeComida(control.comidaId),
            )
        }
    }

    fun programar(context: Context, controlId: Long, fecha: LocalDate, instante: LocalTime) {
        val alarma = context.getSystemService(AlarmManager::class.java)
        val millis = LocalDateTime.of(fecha, instante)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        runCatching {
            if (PlanificadorAlarmas.puedeProgramarExacta(alarma)) {
                alarma.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent(context, controlId))
            } else {
                alarma.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent(context, controlId))
            }
        }.onFailure {
            Log.w(TAG, "No se pudo programar el control $controlId", it)
        }
    }

    fun cancelar(context: Context, controlId: Long) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context, controlId))
        AvisosComida.cancelar(context, controlId)
    }

    private fun pendingIntent(context: Context, controlId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            controlId.toInt(),
            Intent(context, ReceptorAvisoComida::class.java).apply {
                action = ACCION_PREGUNTAR
                putExtra(AvisosComida.EXTRA_CONTROL_ID, controlId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

/** Publica la pregunta cuando salta la alarma. */
class ReceptorAvisoComida : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val controlId = intent.getLongExtra(AvisosComida.EXTRA_CONTROL_ID, -1L)
        if (controlId <= 0) return

        val pendiente = goAsync()
        val app = context.applicationContext
        val repositorio = (app as DigestApplication).repositorio

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val comida = repositorio.textoDeComida(controlId)
                AvisosComida.mostrar(app, controlId, comida)
            } catch (e: Exception) {
                Log.e(TAG, "Error mostrando el control $controlId", e)
            } finally {
                pendiente.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ReceptorAvisoComida"
    }
}

/**
 * Respuesta «No he notado nada» desde la notificación.
 *
 * Cierra el control y no vuelve a preguntar. Es la respuesta mayoritaria y
 * tiene que poder darse en un toque, sin abrir la app.
 */
class ReceptorAccionComida : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val controlId = intent.getLongExtra(AvisosComida.EXTRA_CONTROL_ID, -1L)
        if (controlId <= 0) return
        if (intent.action != AvisosComida.ACCION_SIN_CAMBIOS) return

        val pendiente = goAsync()
        val repositorio = (context.applicationContext as DigestApplication).repositorio

        CoroutineScope(Dispatchers.IO).launch {
            try {
                repositorio.responderControl(controlId, huboCambios = false)
                PlanificadorControlComida.cancelar(context, controlId)
            } catch (e: Exception) {
                Log.e(TAG, "Error respondiendo el control $controlId", e)
            } finally {
                pendiente.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ReceptorAccionComida"
    }
}