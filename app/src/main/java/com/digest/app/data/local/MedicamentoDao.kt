package com.digest.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digest.app.data.local.entity.HorarioEntity
import com.digest.app.data.local.entity.MedicamentoEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface MedicamentoDao {

    @Query("SELECT * FROM medicamentos WHERE activo = 1 ORDER BY nombre COLLATE NOCASE")
    fun observarActivos(): Flow<List<MedicamentoEntity>>

    @Query("SELECT * FROM medicamentos ORDER BY activo DESC, nombre COLLATE NOCASE")
    fun observarTodos(): Flow<List<MedicamentoEntity>>

    /**
     * Autocompletado del catálogo. Busca sobre [nombreNormalizado], que ya
     * viene sin acentos ni mayúsculas, así que el paciente puede escribir
     * "mesalazina" y encontrar "Mesalazina" o "MESALAZINA 2,5 g".
     */
    @Query(
        """
        SELECT * FROM medicamentos
        WHERE activo = 1
          AND (:texto = '' OR nombreNormalizado LIKE '%' || :texto || '%')
        ORDER BY
            CASE WHEN nombreNormalizado LIKE :texto || '%' THEN 0 ELSE 1 END,
            nombre COLLATE NOCASE
        LIMIT :limite
        """,
    )
    fun buscar(texto: String, limite: Int = 8): Flow<List<MedicamentoEntity>>

    @Query("SELECT * FROM medicamentos WHERE id = :id")
    suspend fun obtener(id: Long): MedicamentoEntity?

    @Query("SELECT * FROM medicamentos WHERE id = :id")
    fun observar(id: Long): Flow<MedicamentoEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(medicamento: MedicamentoEntity): Long

    @Update
    suspend fun actualizar(medicamento: MedicamentoEntity)

    /**
     * Baja lógica en lugar de borrar: los registros históricos de tomas
     * deben seguir siendo válidos aunque el paciente deje el fármaco.
     */
    @Query("UPDATE medicamentos SET activo = 0 WHERE id = :id")
    suspend fun desactivar(id: Long)

    @Query("UPDATE medicamentos SET activo = 1 WHERE id = :id")
    suspend fun reactivar(id: Long)

    @Query("DELETE FROM medicamentos WHERE id = :id")
    suspend fun borrarDefinitivo(id: Long)
}

@Dao
interface HorarioDao {

    @Query("SELECT * FROM horarios WHERE medicamentoId = :medicamentoId ORDER BY hora")
    fun observarDeMedicamento(medicamentoId: Long): Flow<List<HorarioEntity>>

    @Query("SELECT * FROM horarios ORDER BY hora")
    fun observarTodos(): Flow<List<HorarioEntity>>

    /** Horarios que tocan hoy. Se usa al abrir la app para crear las tomas del día. */
    @Query(
        """
        SELECT * FROM horarios
        WHERE diasSemana = 0 OR (diasSemana & :bitDia) != 0
        ORDER BY hora
        """,
    )
    fun observarDeHoy(bitDia: Int): Flow<List<HorarioEntity>>

    /** Horarios que tocan hoy, para materializar las tomas al abrir la app. */
    @Query(
        """
        SELECT * FROM horarios
        WHERE diasSemana = 0 OR (diasSemana & :bitDia) != 0
        ORDER BY hora
        """,
    )
    suspend fun deHoy(bitDia: Int): List<HorarioEntity>

    @Query("SELECT * FROM horarios WHERE id = :id")
    suspend fun obtener(id: Long): HorarioEntity?

    @Insert
    suspend fun insertar(horario: HorarioEntity): Long

    @Update
    suspend fun actualizar(horario: HorarioEntity)

    @Delete
    suspend fun borrar(horario: HorarioEntity)

    @Query("DELETE FROM horarios WHERE id = :id")
    suspend fun borrarPorId(id: Long)
}