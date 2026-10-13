package com.digest.app.notificaciones

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.digest.app.data.DigestRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Programa y cancela las alarmas de aviso de toma.
 *
 * En Android 12+ las alarmas exactas requieren un permiso concedible por el
 * usuario. Pedirlo forma parte del flujo de la app, pero no siempre se concede
 * y hay pacientes que lo deniegan. Por eso aquí se degrada a una alarma
 * inexacta en lugar de dejar al paciente sin avisos: puede llegar unos
 * minutos tarde, pero le llega.
 */
object PlanificadorAlarmas {

    private const val TAG = "PlanificadorAlarmas"

    /** Margen tras la hora programada para el aviso de "se te ha pasado". */
    val MARGEN_RETRASO: Duration = Duration.ofMinutes(20)

    /**
     * Programa los avisos de todas las tomas de un día.
     *
     * Se llama al abrir la app, al modificar un horario y tras un reinicio.
     */
    suspend fun programar(context: Context, repositorio: DigestRepository) {
        val hoy = LocalDate.now()
        val zonasHorarias = avisosDeHoy(context, repositorio, hoy)
        for ((tomaId, instante) in zonasHorarias) {
            programarAviso(context, tomaId, instante, retrasado = false)
            programarAviso(
                context = context,
                tomaId = tomaId,
                instante = instante.plus(MARGEN_RETRASO),
                retrasado = true,
            )
        }
        Log.i(TAG, "Programados ${zonasHorarias.size} avisos hasta $hoy")
    }

    /**
     * Calcula los instantes en que debe saltar cada aviso de hoy.
     *
     * Solo se generan los que aún no han pasado: programar una alarma en el
     * pasado la hace dispararse de inmediato, que es justo lo que hay que
     * evitar al abrir la app a mediodía.
     */
    private suspend fun avisosDeHoy(
        context: Context,
        repositorio: DigestRepository,
        fecha: LocalDate,
    ): List<Pair<Long, LocalDateTime>> {
        val horarios = repositorio.horariosDeHoy(fecha)
        val creadas = repositorio.materializarTomasDelDia(fecha, horarios)
        val ahora = LocalDateTime.now()

        return creadas
            .map { it.id to LocalDateTime.of(it.fecha, it.horaProgramada) }
            .filter { it.second.isAfter(ahora) }
    }

    /** Programa una alarma concreta. [retrasado] cambia el canal del aviso. */
    fun programarAviso(
        context: Context,
        tomaId: Long,
        instante: LocalDateTime,
        retrasado: Boolean,
    ) {
        val alarma = context.getSystemService(AlarmManager::class.java)
        val millis = instante.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = pendingIntent(context, tomaId, retrasado)

        runCatching {
            when {
                puedeProgramarExacta(alarma) ->
                    alarma.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    // Inexacta: Android la repartirá, pero puede retrasarse
                    // unos minutos. Mejor un aviso tarde que ninguno.
                    alarma.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)

                else -> alarma.set(AlarmManager.RTC_WAKEUP, millis, intent)
            }
        }.onFailure {
            // Si incluso la alarma inexacta falla (restricciones del fabricante),
            // la app sigue siendo utilizable: queda el recordatorio al abrirla.
            Log.w(TAG, "No se pudo programar el aviso de la toma $tomaId", it)
        }
    }

    fun cancelar(context: Context, tomaId: Long) {
        val alarma = context.getSystemService(AlarmManager::class.java)
        alarma.cancel(pendingIntent(context, tomaId, retrasado = false))
        alarma.cancel(pendingIntent(context, tomaId, retrasado = true))
    }

    private fun pendingIntent(context: Context, tomaId: Long, retrasado: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            // Cada (toma, momento) necesita su propio identificador: sin esto,
            // el aviso tardío de una toma sustituiría al de su hora.
            if (retrasado) {
                (tomaId.toInt() xor 0x40000000)
            } else {
                tomaId.toInt()
            },
            Intent(context, ReceptorAvisoToma::class.java).apply {
                action = if (retrasado) ACCION_RETRASADA else ACCION_TOMA
                putExtra(AvisosToma.EXTRA_TOMA_ID, tomaId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /**
     * ¿Se pueden usar alarmas exactas?
     *
     * Android 13+ lo expone como permiso; en versiones anteriores basta con
     * comprobar que el servicio exista.
     */
    fun puedeProgramarExacta(alarma: AlarmManager?): Boolean =
        alarma != null && if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarma.canScheduleExactAlarms()
        } else {
            true
        }

    const val ACCION_TOMA = "com.digest.app.AVISO_TOMA"
    const val ACCION_RETRASADA = "com.digest.app.AVISO_TOMA_RETRASADA"
}