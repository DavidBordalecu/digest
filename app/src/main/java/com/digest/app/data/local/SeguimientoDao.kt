package com.digest.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digest.app.data.local.entity.DiarioEntity
import com.digest.app.data.local.entity.ManifestacionPerianalEntity
import com.digest.app.data.local.entity.NutricionEnteralEntity
import com.digest.app.data.local.entity.PesajeEntity
import com.digest.app.model.TipoSintoma
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DiarioDao {

    @Query("SELECT * FROM diario WHERE fecha = :fecha")
    suspend fun obtener(fecha: LocalDate): DiarioEntity?

    @Query("SELECT * FROM diario WHERE fecha = :fecha")
    fun observar(fecha: LocalDate): Flow<DiarioEntity?>

    @Query("SELECT * FROM diario WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC")
    fun observarRango(desde: LocalDate, hasta: LocalDate): Flow<List<DiarioEntity>>

    @Query("SELECT * FROM diario WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha")
    suspend fun entre(desde: LocalDate, hasta: LocalDate): List<DiarioEntity>

    /** El diario es una fila por fecha: al guardar, se replaces. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(diario: DiarioEntity)

    @Query("DELETE FROM diario WHERE fecha = :fecha")
    suspend fun borrar(fecha: LocalDate)
}

@Dao
interface PesajeDao {

    @Query("SELECT * FROM pesajes ORDER BY fecha DESC LIMIT 1")
    fun observarUltimo(): Flow<PesajeEntity?>

    @Query("SELECT * FROM pesajes ORDER BY fecha DESC LIMIT :limite")
    fun observarUltimos(limite: Int = 90): Flow<List<PesajeEntity>>

    /** Serie para la curva: peso medio de cada mes. */
    @Query(
        """
        SELECT strftime('%Y-%m', fecha) AS mes, AVG(pesoKg) AS media
        FROM pesajes
        WHERE fecha BETWEEN :desde AND :hasta
        GROUP BY mes ORDER BY mes
        """,
    )
    fun mediaMensual(desde: LocalDate, hasta: LocalDate): Flow<List<PesoMensual>>

    @Query("SELECT * FROM pesajes WHERE fecha = :fecha LIMIT 1")
    suspend fun obtenerPorFecha(fecha: LocalDate): PesajeEntity?

    /** Pesajes de un rango, para comparar periodos y calcular la pérdida. */
    @Query("SELECT * FROM pesajes WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha")
    suspend fun entre(desde: LocalDate, hasta: LocalDate): List<PesajeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(pesaje: PesajeEntity): Long

    @Query("DELETE FROM pesajes WHERE id = :id")
    suspend fun borrar(id: Long)
}

data class PesoMensual(val mes: String, val media: Double)

@Dao
interface NutricionEnteralDao {

    @Query("SELECT * FROM nutricion_enteral WHERE fecha = :fecha ORDER BY hora")
    fun observarDelDia(fecha: LocalDate): Flow<List<NutricionEnteralEntity>>

    @Query("SELECT * FROM nutricion_enteral WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha, hora")
    fun observarRango(desde: LocalDate, hasta: LocalDate): Flow<List<NutricionEnteralEntity>>

    /** Total de mililitros del día: es como se contabiliza la nutrición enteral. */
    @Query("SELECT COALESCE(SUM(volumenMl), 0) FROM nutricion_enteral WHERE fecha = :fecha")
    fun volumenDelDia(fecha: LocalDate): Flow<Int>

    @Query("SELECT COALESCE(SUM(volumenMl), 0) FROM nutricion_enteral WHERE fecha BETWEEN :desde AND :hasta")
    fun volumenRango(desde: LocalDate, hasta: LocalDate): Flow<Int>

    @Insert
    suspend fun insertar(registro: NutricionEnteralEntity): Long

    @Update
    suspend fun actualizar(registro: NutricionEnteralEntity)

    @Query("DELETE FROM nutricion_enteral WHERE id = :id")
    suspend fun borrar(id: Long)
}

@Dao
interface ManifestacionPerianalDao {

    @Query("SELECT * FROM manifestaciones_perianales WHERE fecha = :fecha ORDER BY tipo")
    fun observarDelDia(fecha: LocalDate): Flow<List<ManifestacionPerianalEntity>>

    @Query("SELECT * FROM manifestaciones_perianales WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC")
    fun observarRango(desde: LocalDate, hasta: LocalDate): Flow<List<ManifestacionPerianalEntity>>

    /** ¿Ha habido alguna manifestación perianal en el periodo? */
    @Query("SELECT COUNT(*) FROM manifestaciones_perianales WHERE fecha BETWEEN :desde AND :hasta")
    suspend fun contarRango(desde: LocalDate, hasta: LocalDate): Int

    @Query("SELECT COUNT(*) FROM manifestaciones_perianales WHERE tipo = :tipo")
    suspend fun contarDeTipo(tipo: TipoSintoma): Int

    @Insert
    suspend fun insertar(manifestacion: ManifestacionPerianalEntity): Long

    @Update
    suspend fun actualizar(manifestacion: ManifestacionPerianalEntity)

    @Query("DELETE FROM manifestaciones_perianales WHERE id = :id")
    suspend fun borrar(id: Long)
}