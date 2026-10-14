package com.digest.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.digest.app.data.local.entity.ControlComidaEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ControlComidaDao {

    /**
     * Un solo control por comida.
     *
     * Reprogramar la pregunta para la misma comida sustituye la anterior en
     * lugar de duplicarla: sin esto, abrir la app varias veces dejaría
     * cuatro avisos de la misma comida.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(control: ControlComidaEntity): Long

    /** Controles sin responder, para mostrar la tarjeta dentro de la app. */
    @Query(
        """
        SELECT * FROM control_comidas
        WHERE fecha = :fecha AND huboCambios IS NULL
        ORDER BY programadoPara
        """,
    )
    fun observarPendientes(fecha: LocalDate): Flow<List<ControlComidaEntity>>

    /** Controles ya respondidos del día, para el resumen. */
    @Query(
        """
        SELECT * FROM control_comidas
        WHERE fecha = :fecha AND huboCambios IS NOT NULL
        ORDER BY programadoPara
        """,
    )
    fun observarRespondidos(fecha: LocalDate): Flow<List<ControlComidaEntity>>

    @Query(
        """
        SELECT * FROM control_comidas
        WHERE fecha BETWEEN :desde AND :hasta
        ORDER BY fecha, programadoPara
        """,
    )
    suspend fun entre(desde: LocalDate, hasta: LocalDate): List<ControlComidaEntity>

    @Query("SELECT * FROM control_comidas WHERE comidaId = :comidaId LIMIT 1")
    suspend fun deComida(comidaId: Long): ControlComidaEntity?

    @Query(
        """
        SELECT * FROM control_comidas
        WHERE huboCambios IS NULL AND programadoPara <= :hora AND fecha <= :fecha
        ORDER BY fecha, programadoPara
        """,
    )
    suspend fun vencidos(fecha: LocalDate, hora: String): List<ControlComidaEntity>

    /** Registra la respuesta y da el control por cerrado. */
    @Query(
        """
        UPDATE control_comidas
        SET huboCambios = :huboCambios, respondidoEn = :hora
        WHERE id = :id
        """,
    )
    suspend fun responder(id: Long, huboCambios: Boolean, hora: String)

    /** Un mismo control no puede quedarse pendiente para siempre. */
    @Query("DELETE FROM control_comidas WHERE fecha < :fecha")
    suspend fun borrarAntiguos(fecha: LocalDate)

    @Query("DELETE FROM control_comidas WHERE comidaId = :comidaId")
    suspend fun borrarDeComida(comidaId: Long)
}