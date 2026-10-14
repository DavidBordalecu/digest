package com.digest.app.notificaciones

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.digest.app.MainActivity
import com.digest.app.R

/**
 * Aviso que pregunta si la comida ha sentado bien.
 *
 * Va en su propio canal, y con importancia más baja que el de la
 * medicación, a propósito: un aviso de toma y un control de comida en el
 * mismo canal hacen que Android agrupe los dos y el paciente los ignore
 * juntos. La medicación no puede competir por la atención con un
 * questionnaire, y si compiten, gana siempre la primera.
 */
object AvisosComida {

    const val CANAL_CONTROL = "avisos_control_comida"

    /** Respuesta: no ha notado nada. Cierra el control y no se insiste. */
    const val ACCION_SIN_CAMBIOS = "com.digest.app.SIN_CAMBIOS_COMIDA"

    const val EXTRA_CONTROL_ID = "control_id"
    const val EXTRA_COMIDA = "comida"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val canal = NotificationChannel(
            CANAL_CONTROL,
            context.getString(R.string.canal_control_comida),
            // Baja: molesta si el paciente está comiendo o conduciendo, y la
            // pregunta puede esperar a que esté en casa.
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.canal_control_comida_desc)
            enableVibration(false)
            setShowBadge(false)
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(canal)
    }

    /**
     * Publica la pregunta.
     *
     * @param comida lo que se comió, para que la pregunta sea concreta: «¿has
     *   notado algo desde el pollo con arroz?» es mucho más fácil de
     *   responder que una pregunta genérica.
     */
    fun mostrar(context: Context, controlId: Long, comida: String?) {
        val pregunta = comida
            ?.takeIf { it.isNotBlank() }
            ?.let { context.getString(R.string.control_comida_con_comida, it) }
            ?: context.getString(R.string.control_comida)

        val abrirApp = PendingIntent.getActivity(
            context,
            controlId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AvisosComida.EXTRA_CONTROL_ID, controlId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // «No, nada» se responde sin abrir la app: es la respuesta más
        // frecuente y no puede pedir más de lo que ya se está haciendo.
        val sinCambios = PendingIntent.getBroadcast(
            context,
            controlId.toInt(),
            Intent(context, ReceptorAccionComida::class.java).apply {
                action = ACCION_SIN_CAMBIOS
                putExtra(EXTRA_CONTROL_ID, controlId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificacion: Notification = NotificationCompat.Builder(context, CANAL_CONTROL)
            .setSmallIcon(R.drawable.ic_pastilla)
            .setContentTitle(context.getString(R.string.control_comida_titulo))
            .setContentText(pregunta)
            .setStyle(NotificationCompat.BigTextStyle().bigText(pregunta))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(abrirApp)
            .addAction(
                R.drawable.ic_check,
                context.getString(R.string.accion_sin_cambios),
                sinCambios,
            )
            .setAutoCancel(true)
            .build()

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.i(TAG, "Sin permiso de avisos: el control se verá dentro de la app")
            return
        }

        NotificationManagerCompat.from(context).notify(controlId.toInt(), notificacion)
    }

    fun cancelar(context: Context, controlId: Long) {
        NotificationManagerCompat.from(context).cancel(controlId.toInt())
    }

    private const val TAG = "AvisosComida"
}