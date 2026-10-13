package com.digest.app.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.digest.app.DigestApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gestiona las acciones "Tomada" y "Omitir" de la notificación.
 *
 * Que se pueda responder desde el aviso sin abrir la app es importante: el
 * paciente que está en la calle o comiendo no va a buscar el icono para
 * confirmar la toma.
 */
class ReceptorAccionToma : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tomaId = intent.getLongExtra(AvisosToma.EXTRA_TOMA_ID, -1L)
        if (tomaId <= 0) return

        val marcada = when (intent.action) {
            AvisosToma.ACCION_TOMADA -> true
            AvisosToma.ACCION_OMITIDA -> false
            else -> return
        }

        val pendiente = goAsync()
        val repositorio = (context.applicationContext as DigestApplication).repositorio

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (marcada) {
                    repositorio.marcarTomada(tomaId)
                } else {
                    repositorio.marcarOmitida(tomaId)
                }
                // El aviso ya no tiene sentido: el paciente ha respondido.
                AvisosToma.cancelar(context, tomaId)
            } catch (e: Exception) {
                Log.e(TAG, "Error registrando la acción sobre la toma $tomaId", e)
            } finally {
                pendiente.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ReceptorAccionToma"
    }
}

/**
 * Reprograma los avisos tras un reinicio del móvil.
 *
 * Android borra todas las alarmas al apagar: sin este receptor, un paciente
 * que reinicia el teléfono se queda sin avisos de medicación, que es
 * precisamente cuando más los necesita.
 */
class ReceptorReinicio : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendiente = goAsync()
        val repositorio = (context.applicationContext as DigestApplication).repositorio
        val app = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                AvisosToma.crearCanales(app)
                PlanificadorAlarmas.programar(app, repositorio)
            } catch (e: Exception) {
                Log.e(TAG, "Error reprogramando avisos tras el reinicio", e)
            } finally {
                pendiente.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ReceptorReinicio"
    }
}