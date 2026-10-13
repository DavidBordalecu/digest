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
 * Recibe el disparo de la alarma y publica el aviso de la toma.
 *
 * VaWorkManager a propósito: el aviso tiene que salir puntual y el paciente no
 * puede permitirse esperar al work manager. Lo que sí delega en él es el
 * acceso a la base de datos, que se hace fuera del hilo principal.
 */
class ReceptorAvisoToma : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tomaId = intent.getLongExtra(AvisosToma.EXTRA_TOMA_ID, -1L)
        if (tomaId <= 0) return

        val retrasado = intent.action == PlanificadorAlarmas.ACCION_RETRASADA

        // onReceive puede ejecutarse en el hilo principal:Room no se permite.
        val pendiente = goAsync()
        val repositorio = (context.applicationContext as DigestApplication).repositorio

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val toma = repositorio.obtenerToma(tomaId)
                if (toma == null) {
                    Log.w(TAG, "La toma $tomaId ya no existe")
                    return@launch
                }

                // Si el paciente ya la tomó, el aviso sobraría.
                if (toma.estado != com.digest.app.model.EstadoToma.PENDIENTE) {
                    AvisosToma.cancelar(context, tomaId)
                    return@launch
                }

                val horario = repositorio.obtenerHorario(toma.horarioId)
                val nombre = repositorio.nombreMedicamento(horario?.medicamentoId)
                val dosis = horario?.dosis

                AvisosToma.mostrar(
                    context = context,
                    tomaId = tomaId,
                    titulo = context.getString(
                        com.digest.app.R.string.aviso_titulo,
                        nombre ?: context.getString(com.digest.app.R.string.medicamento_sin_nombre),
                    ),
                    cuerpo = context.getString(
                        com.digest.app.R.string.aviso_cuerpo,
                        toma.horaProgramada.toString().take(5),
                        // La dosis entra como sufijo ya separado: si no la
                        // hay, no debe quedar un punto suelto al final.
                        dosis?.let { " · $it" }.orEmpty(),
                    ),
                    retrasada = retrasado,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error publicando el aviso de la toma $tomaId", e)
            } finally {
                pendiente.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ReceptorAvisoToma"
    }
}