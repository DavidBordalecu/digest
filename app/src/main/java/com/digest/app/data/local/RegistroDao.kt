package com.digest.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digest.app.data.local.entity.ComidaEntity
import com.digest.app.data.local.entity.DeposicionEntity
import com.digest.app.data.local.entity.DolorEntity
import com.digest.app.data.local.entity.SintomaEntity
import com.digest.app.model.TipoSintoma
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ComidaDao {

    @Query("SELECT * FROM comidas WHERE fecha = :fecha ORDER BY hora")
    fun observarDelDia(fecha: LocalDate): Flow<List<ComidaEntity>>

    @Query("SELECT * FROM comidas WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC, hora")
    fun observarRango(desde: LocalDate, hasta: LocalDate): Flow<List<ComidaEntity>>

    @Insert
    suspend fun insertar(comida: ComidaEntity): Long

    @Update
    suspend fun actualizar(comida: ComidaEntity)

    @Query("DELETE FROM comidas WHERE id = :id")
    suspend fun borrar(id: Long)
}

@Dao
interface DeposicionDao {

    @Query("SELECT * FROM deposiciones WHERE fecha = :fecha ORDER BY hora")
    fun observarDelDia(fecha: LocalDate): Flow<List<DeposicionEntity>>

    @Query("SELECT * FROM deposiciones WHERE fecha = :fecha ORDER BY hora")
    suspend fun delDia(fecha: LocalDate): List<DeposicionEntity>

    @Query("SELECT * FROM deposiciones WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha, hora")
    fun observarRango(desde: LocalDate, hasta: LocalDate): Flow<List<DeposicionEntity>>

    @Query("SELECT COUNT(*) FROM deposiciones WHERE fecha = :fecha")
    fun contarDelDia(fecha: LocalDate): Flow<Int>

    @Query("SELECT COUNT(*) FROM deposiciones WHERE fecha BETWEEN :desde AND :hasta")
    suspend fun contarRango(desde: LocalDate, hasta: LocalDate): Int

    /** Media de Bristol por día: la serie que dibuja la curva de tránsito. */
    @Query(
        """
        SELECT fecha, AVG(bristol) AS media, COUNT(*) AS numero
        FROM deposiciones
        WHERE fecha BETWEEN :desde AND :hasta
        GROUP BY fecha ORDER BY fecha
        """,
    )
    fun mediaBristolPorDia(desde: LocalDate, hasta: LocalDate): Flow<List<BristolPorDia>>

    @Insert
    suspend fun insertar(deposicion: DeposicionEntity): Long

    @Update
    suspend fun actualizar(deposicion: DeposicionEntity)

    @Query("DELETE FROM deposiciones WHERE id = :id")
    suspend fun borrar(id: Long)
}

data class BristolPorDia(val fecha: LocalDate, val media: Double, val numero: Int)

@Dao
interface DolorDao {

    @Query("SELECT * FROM dolores WHERE fecha = :fecha ORDER BY hora")
    fun observarDelDia(fecha: LocalDate): Flow<List<DolorEntity>>

    @Query("SELECT * FROM dolores WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha, hora")
    fun observarRango(desde: LocalDate, hasta: LocalDate): Flow<List<DolorEntity>>

    /** Intensidad media diaria, para ver si el dolor está mejorando o no. */
    @Query(
        """
        SELECT fecha, AVG(intensidad) AS media, MAX(intensidad) AS maxima
        FROM dolores
        WHERE fecha BETWEEN :desde AND :hasta
        GROUP BY fecha ORDER BY fecha
        """,
    )
    fun intensidadPorDia(desde: LocalDate, hasta: LocalDate): Flow<List<DolorPorDia>>

    @Insert
    suspend fun insertar(dolor: DolorEntity): Long

    @Update
    suspend fun actualizar(dolor: DolorEntity)

    @Query("DELETE FROM dolores WHERE id = :id")
    suspend fun borrar(id: Long)
}

data class DolorPorDia(val fecha: LocalDate, val media: Double, val maxima: Int)

@Dao
interface SintomaDao {

    /**
     * Fechas en las que se registró un síntoma, de la más reciente a la más
     * antigua. La racha de días consecutivos se calcula en Kotlin a partir de
     * esta lista, que es más sencilla de probar que una consulta con
     * aritmética de fechas en SQL.
     */
    @Query("SELECT fecha FROM sintomas WHERE tipo = :tipo AND fecha <= :hoy ORDER BY fecha DESC")
    suspend fun fechasDeSintoma(tipo: TipoSintoma, hoy: LocalDate): List<LocalDate>

    @Query("SELECT * FROM sintomas WHERE fecha = :fecha ORDER BY tipo")
    fun observarDelDia(fecha: LocalDate): Flow<List<SintomaEntity>>

    @Query("SELECT * FROM sintomas WHERE fecha = :fecha ORDER BY tipo")
    suspend fun de(fecha: LocalDate): List<SintomaEntity>

    @Query("SELECT * FROM sintomas WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha")
    fun observarRango(desde: LocalDate, hasta: LocalDate): Flow<List<SintomaEntity>>

    /**
     * Síntomas con los días en que aparecieron. Es la base de las estadísticas:
     * "diarrea 12 de los últimos 30 días".
     */
    @Query(
        """
        SELECT tipo, COUNT(*) AS veces, AVG(gravedad) AS gravedadMedia
        FROM sintomas
        WHERE fecha BETWEEN :desde AND :hasta
        GROUP BY tipo
        ORDER BY veces DESC
        """,
    )
    fun frecuenciaPorTipo(desde: LocalDate, hasta: LocalDate): Flow<List<FrecuenciaSintoma>>

    @Query("SELECT * FROM sintomas WHERE fecha = :fecha AND tipo = :tipo LIMIT 1")
    suspend fun obtener(fecha: LocalDate, tipo: TipoSintoma): SintomaEntity?

    /** Sobrescribe el registro del día en lugar de duplicarlo. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(sintoma: SintomaEntity): Long

    @Query("DELETE FROM sintomas WHERE fecha = :fecha AND tipo = :tipo")
    suspend fun quitar(fecha: LocalDate, tipo: TipoSintoma)
}

data class FrecuenciaSintoma(
    val tipo: TipoSintoma,
    val veces: Int,
    val gravedadMedia: Double,
)