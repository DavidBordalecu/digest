package com.digest.app.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.digest.app.data.local.entity.TomaEntity
import com.digest.app.model.EstadoToma
import com.digest.app.model.TipoPresentacion
import com.digest.app.model.ViaAdministracion
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/**
 * Toma programada con todo lo necesario para pintarla: qué medicamento es,
 * en qué horario estaba y en qué estado quedó.
 *
 * Se resuelve con un `JOIN` en lugar de relaciones anidadas de Room porque la
 * pantalla de hoy la lee entera de una vez y así el mapeo es explícito.
 */
data class TomaConDetalle(
    val id: Long,
    val fecha: LocalDate,
    val horaProgramada: LocalTime,
    val estado: EstadoToma,
    val horaReal: LocalTime?,
    val notas: String?,
    val horarioId: Long,
    val notaHorario: String?,
    val medicamentoId: Long,
    val medicamentoNombre: String,
    val dosis: String?,
    val presentacion: TipoPresentacion,
    val via: ViaAdministracion,
    val instrucciones: String?,
    val color: Int,
)

@Dao
interface TomaDao {

    @Transaction
    @Query(
        """
        SELECT
            t.id            AS id,
            t.fecha         AS fecha,
            t.horaProgramada AS horaProgramada,
            t.estado        AS estado,
            t.horaReal      AS horaReal,
            t.notas         AS notas,
            h.id            AS horarioId,
            h.nota          AS notaHorario,
            m.id            AS medicamentoId,
            m.nombre        AS medicamentoNombre,
            m.dosis         AS dosis,
            m.presentacion  AS presentacion,
            m.via           AS via,
            m.instrucciones AS instrucciones,
            m.color         AS color
        FROM tomas t
        JOIN horarios h    ON h.id = t.horarioId
        JOIN medicamentos m ON m.id = h.medicamentoId
        WHERE t.fecha = :fecha
        ORDER BY t.horaProgramada
        """,
    )
    fun observarDelDia(fecha: LocalDate): Flow<List<TomaConDetalle>>

    @Transaction
    @Query(
        """
        SELECT
            t.id            AS id,
            t.fecha         AS fecha,
            t.horaProgramada AS horaProgramada,
            t.estado        AS estado,
            t.horaReal      AS horaReal,
            t.notas         AS notas,
            h.id            AS horarioId,
            h.nota          AS notaHorario,
            m.id            AS medicamentoId,
            m.nombre        AS medicamentoNombre,
            m.dosis         AS dosis,
            m.presentacion  AS presentacion,
            m.via           AS via,
            m.instrucciones AS instrucciones,
            m.color         AS color
        FROM tomas t
        JOIN horarios h    ON h.id = t.horarioId
        JOIN medicamentos m ON m.id = h.medicamentoId
        WHERE t.fecha BETWEEN :desde AND :hasta
        ORDER BY t.fecha DESC, t.horaProgramada DESC
        """,
    )
    fun observarRango(desde: LocalDate, hasta: LocalDate): Flow<List<TomaConDetalle>>

    @Query("SELECT * FROM tomas WHERE id = :id")
    suspend fun obtener(id: Long): TomaEntity?

    @Query("SELECT * FROM tomas WHERE horarioId = :horarioId AND fecha = :fecha LIMIT 1")
    suspend fun obtenerPorHorarioYFecha(horarioId: Long, fecha: LocalDate): TomaEntity?

    /**
     * Tomas de un día que siguen pendientes.
     *
     * Es la entrada de la que parte el planificador de avisos: hay que
     * recorrer todas, no solo las creadas en esta pasada, o un medicamento
     * añadido ayer se quedaría hoy sin alarma.
     */
    @Query("SELECT * FROM tomas WHERE fecha = :fecha AND estado = 'PENDIENTE'")
    suspend fun pendientesDe(fecha: LocalDate): List<TomaEntity>

    /**
     * Inserta ignorando duplicados. Al abrir la app se llama por cada horario
     * del día y no debe fallar porque la toma ya exista.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(toma: TomaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(toma: TomaEntity): Long

    @Update
    suspend fun actualizar(toma: TomaEntity)

    /**
     * Marca la toma como tomada y guarda la hora real, que es lo que permite
     * medir el retraso real de la adherencia.
     */
    @Query(
        """
        UPDATE tomas
        SET estado = 'TOMADA', horaReal = :horaReal, registradoEn = :cuando
        WHERE id = :id
        """,
    )
    suspend fun marcarTomada(
        id: Long,
        horaReal: LocalTime?,
        cuando: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE tomas SET estado = 'OMITIDA' WHERE id = :id")
    suspend fun marcarOmitida(id: Long)

    @Query("UPDATE tomas SET estado = 'PENDIENTE', horaReal = NULL WHERE id = :id")
    suspend fun volverAPendiente(id: Long)

    @Query("DELETE FROM tomas WHERE fecha < :fecha")
    suspend fun borrarAnterioresA(fecha: LocalDate)

    // --- Estadísticas de adherencia ---

    /** Cuántas tomas había y cuántas se tomó, en un rango. Base del % de adherencia. */
    @Query(
        """
        SELECT
            COUNT(*) AS total,
            SUM(CASE WHEN estado = 'TOMADA' THEN 1 ELSE 0 END) AS tomadas
        FROM tomas
        WHERE fecha BETWEEN :desde AND :hasta
        """,
    )
    suspend fun adherencia(desde: LocalDate, hasta: LocalDate): Adherencia

    /** Tomas sin tomar cuya hora ya pasó: lo que el paciente se está saltando. */
    @Query(
        """
        SELECT COUNT(*) FROM tomas
        WHERE estado = 'PENDIENTE' AND fecha = :fecha AND horaProgramada < :ahora
        """,
    )
    suspend fun pendientesVencidas(fecha: LocalDate, ahora: LocalTime): Int
}

/** Resultado agregado de adherencia. [total] es nulo si no hay tomas. */
data class Adherencia(
    val total: Int?,
    val tomadas: Int?,
) {
    val porcentaje: Float
        get() {
            val t = total ?: return 0f
            if (t == 0) return 0f
            return ((tomadas ?: 0).toFloat() / t) * 100f
        }
}