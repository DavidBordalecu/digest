package com.digest.app.notificaciones

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.digest.app.MainActivity
import com.digest.app.R

/**
 * Creación y publicación de los avisos de toma.
 *
 * El canal se crea en [crearCanales] al arrancar la app, antes de que pueda
 * dispararse ninguna alarma, porque en Android 13+ notificar sin canal previo
 * hace que la notificación no se muestre.
 */
object AvisosToma {

    const val CANAL_PENDIENTE = "avisos_toma_pendiente"
    const val CANAL_URGENTE = "avisos_toma_urgente"

    /** Acciones disponibles directamente sobre la notificación. */
    const val ACCION_TOMADA = "com.digest.app.MARCAR_TOMADA"
    const val ACCION_OMITIDA = "com.digest.app.MARCAR_OMITIDA"

    const val EXTRA_TOMA_ID = "toma_id"
    const val EXTRA_TITULO = "titulo"

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val gestor = context.getSystemService(NotificationManager::class.java)

        // Aviso normal: aparece a la hora de la toma.
        val pendiente = NotificationChannel(
            CANAL_PENDIENTE,
            context.getString(R.string.canal_pendiente),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.canal_pendiente_desc)
            enableVibration(true)
            setShowBadge(true)
        }

        // Aviso de toma pasada: más insistente, porque ya se ha saltado el horario.
        val urgente = NotificationChannel(
            CANAL_URGENTE,
            context.getString(R.string.canal_urgente),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.canal_urgente_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
        }

        gestor.createNotificationChannel(pendiente)
        gestor.createNotificationChannel(urgente)
    }

    /**
     * Publica el aviso de una toma.
     *
     * @param retrasada si la hora ya pasó con margen, se usa el canal
     *   más insistente: es la diferencia entre un aviso a tiempo y uno tardío.
     *   El aviso llega con otro identificador para que el paciente no vea
     *   duplicados si salta el aviso a su hora mientras el tardío sigue vivo.
     */
    fun mostrar(
        context: Context,
        tomaId: Long,
        titulo: String,
        cuerpo: String,
        retrasada: Boolean = false,
    ) {
        val canal = if (retrasada) CANAL_URGENTE else CANAL_PENDIENTE
        val id = if (retrasada) {
            (tomaId + DESPLAZAMIENTO_RETRASADA).toInt()
        } else {
            tomaId.toInt()
        }

        val abrirApp = PendingIntent.getActivity(
            context,
            tomaId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TOMA_ID, tomaId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val marcarTomada = accion(
            context,
            ACCION_TOMADA,
            tomaId,
            R.string.accion_tomar,
        )
        val marcarOmitida = accion(
            context,
            ACCION_OMITIDA,
            tomaId,
            R.string.accion_omitir,
        )

        val notificacion: Notification = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_pastilla)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(abrirApp)
            .addAction(R.drawable.ic_check, context.getString(R.string.accion_tomar), marcarTomada)
            .addAction(R.drawable.ic_close, context.getString(R.string.accion_omitir), marcarOmitida)
            .setAutoCancel(true)
            .build()

        // Comprobar permisos evita el SecurityException en Android 13+.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        NotificationManagerCompat.from(context).notify(id, notificacion)
    }

    private fun accion(
        context: Context,
        accion: String,
        tomaId: Long,
        tituloRes: Int,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        (accion.hashCode() + tomaId.toInt()),
        Intent(context, ReceptorAccionToma::class.java).apply {
            this.action = accion
            putExtra(EXTRA_TOMA_ID, tomaId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Quita el aviso de una toma, tanto el de la hora como el tardío. */
    fun cancelar(context: Context, tomaId: Long) {
        val gestor = NotificationManagerCompat.from(context)
        gestor.cancel(tomaId.toInt())
        gestor.cancel((tomaId + DESPLAZAMIENTO_RETRASADA).toInt())
    }

    /** Separa el identificador del aviso normal del aviso tardío. */
    private const val DESPLAZAMIENTO_RETRASADA = 1_000_000L
}