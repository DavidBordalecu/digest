package com.digest.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digest.app.data.local.entity.CirugiaEntity
import com.digest.app.data.local.entity.PerfilEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilDao {

    @Query("SELECT * FROM perfil WHERE id = 1")
    fun observar(): Flow<PerfilEntity?>

    @Query("SELECT * FROM perfil WHERE id = 1")
    suspend fun obtener(): PerfilEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(perfil: PerfilEntity)

    @Update
    suspend fun actualizar(perfil: PerfilEntity)

    @Query("UPDATE perfil SET pesoKg = :peso, actualizadoEn = :cuando WHERE id = 1")
    suspend fun actualizarPeso(peso: Double?, cuando: Long = System.currentTimeMillis())

    @Query("UPDATE perfil SET alturaCm = :altura, actualizadoEn = :cuando WHERE id = 1")
    suspend fun actualizarAltura(altura: Double?, cuando: Long = System.currentTimeMillis())

    @Query("SELECT EXISTS(SELECT 1 FROM perfil WHERE id = 1)")
    suspend fun existe(): Boolean

    /** ¿Hay ya una configuración guardada? Determina si se muestra el alta inicial. */
    @Query("SELECT COUNT(*) FROM perfil")
    fun contar(): Flow<Int>
}

@Dao
interface CirugiaDao {

    @Query("SELECT * FROM cirugias ORDER BY fecha DESC, id DESC")
    fun observarTodas(): Flow<List<CirugiaEntity>>

    @Insert
    suspend fun insertar(cirugia: CirugiaEntity): Long

    @Update
    suspend fun actualizar(cirugia: CirugiaEntity)

    @Delete
    suspend fun borrar(cirugia: CirugiaEntity)

    @Query("DELETE FROM cirugias WHERE id = :id")
    suspend fun borrarPorId(id: Long)

    /**
     * Comprueba si el historial quirúrgico del paciente incluye alguna
     * intervención que altere el tránsito. Si es así, la app avisa de la
     * hidratación y ofrece el registro de ostomía.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM cirugias
            WHERE tipo IN ('ILEOSTOMIA', 'COLOSTOMIA', 'RESECCION_ILEOCECAL',
                           'RESECCION_ILEAL', 'HEMICOLECTOMIA', 'COLECTOMIA')
        )
        """,
    )
    fun observarAlteraTransito(): Flow<Boolean>
}