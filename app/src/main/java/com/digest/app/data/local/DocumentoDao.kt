package com.digest.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.digest.app.data.local.entity.DocumentoEntity
import com.digest.app.model.TipoDocumento
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DocumentoDao {

    @Query("SELECT * FROM documentos ORDER BY fechaDocumento IS NULL, fechaDocumento DESC, creadoEn DESC")
    fun observarTodos(): Flow<List<DocumentoEntity>>

    @Query("SELECT * FROM documentos WHERE id = :id LIMIT 1")
    suspend fun porId(id: Long): DocumentoEntity?

    @Query("SELECT * FROM documentos ORDER BY creadoEn DESC")
    suspend fun todos(): List<DocumentoEntity>

    /** Última prueba de un tipo, que es lo que se quiere saber de un vistazo. */
    @Query(
        """
        SELECT * FROM documentos
        WHERE tipo = :tipo AND fechaDocumento IS NOT NULL
        ORDER BY fechaDocumento DESC LIMIT 1
        """,
    )
    suspend fun ultimoDeTipo(tipo: TipoDocumento): DocumentoEntity?

    /** Fecha de la última colonoscopia, para los avisos de revisión. */
    @Query(
        """
        SELECT MAX(fechaDocumento) FROM documentos
        WHERE tipo = :tipo AND fechaDocumento IS NOT NULL
        """,
    )
    fun ultimaFechaDeTipo(tipo: TipoDocumento): Flow<LocalDate?>

    @Insert
    suspend fun insertar(documento: DocumentoEntity): Long

    @Update
    suspend fun actualizar(documento: DocumentoEntity)

    @Delete
    suspend fun borrar(documento: DocumentoEntity)
}