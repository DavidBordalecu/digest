package com.digest.app.notificaciones

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * Comprobación y solicitud de los permisos que sostienen los avisos.
 *
 * Se separan porque son dos cosas distintas: el permiso de notificar lo pide
 * el usuario con un diálogo, y el de alarma exacta solo se concede desde los
 * ajustes del sistema.
 */
object Permisos {

    /** El permiso de notificaciones solo existe a partir de Android 13. */
    val soportaPermisoDeNotificaciones: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Sin este permiso en Android 13+ no suena ningún aviso. */
    fun avisosActivados(contexto: Context): Boolean =
        NotificationManagerCompat.from(contexto).areNotificationsEnabled()

    /**
     * ¿Se pueden programar alarmas exactas?
     *
     * Android 12 (API 31) introdujo el permiso y lo deja sin conceder por
     * defecto, de modo que hay que enviar al paciente a los ajustes.
     */
    fun tieneAlarmasExactas(contexto: Context): Boolean {
        val alarma = contexto.getSystemService(AlarmManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarma.canScheduleExactAlarms()
        } else {
            true
        }
    }
}