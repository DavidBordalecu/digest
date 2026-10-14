package com.digest.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.digest.app.data.local.entity.CirugiaEntity
import com.digest.app.data.local.entity.ComidaEntity
import com.digest.app.data.local.entity.ControlComidaEntity
import com.digest.app.data.local.entity.DeposicionEntity
import com.digest.app.data.local.entity.DiarioEntity
import com.digest.app.data.local.entity.DolorEntity
import com.digest.app.data.local.entity.HorarioEntity
import com.digest.app.data.local.entity.ManifestacionPerianalEntity
import com.digest.app.data.local.entity.MedicamentoEntity
import com.digest.app.data.local.entity.NutricionEnteralEntity
import com.digest.app.data.local.entity.PerfilEntity
import com.digest.app.data.local.entity.PesajeEntity
import com.digest.app.data.local.entity.SintomaEntity
import com.digest.app.data.local.entity.TomaEntity

/**
 * Base de datos local de Digest.
 *
 * Todo vive en el dispositivo: los datos de salud no salen del móvil salvo
 * que el paciente los exporte explícitamente, y la app sigue siendo
 * utilizable sin conexión, algo importante para un paciente que está malo.
 */
@Database(
    entities = [
        PerfilEntity::class,
        CirugiaEntity::class,
        MedicamentoEntity::class,
        HorarioEntity::class,
        TomaEntity::class,
        ComidaEntity::class,
        DeposicionEntity::class,
        DolorEntity::class,
        SintomaEntity::class,
        DiarioEntity::class,
        PesajeEntity::class,
        NutricionEnteralEntity::class,
        ManifestacionPerianalEntity::class,
        ControlComidaEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DigestDatabase : RoomDatabase() {

    abstract fun perfilDao(): PerfilDao
    abstract fun cirugiaDao(): CirugiaDao
    abstract fun medicamentoDao(): MedicamentoDao
    abstract fun horarioDao(): HorarioDao
    abstract fun tomaDao(): TomaDao
    abstract fun comidaDao(): ComidaDao
    abstract fun deposicionDao(): DeposicionDao
    abstract fun dolorDao(): DolorDao
    abstract fun sintomaDao(): SintomaDao
    abstract fun diarioDao(): DiarioDao
    abstract fun pesajeDao(): PesajeDao
    abstract fun nutricionEnteralDao(): NutricionEnteralDao
    abstract fun manifestacionPerianalDao(): ManifestacionPerianalDao
    abstract fun controlComidaDao(): ControlComidaDao

    companion object {
        private const val NOMBRE = "digest.db"

        @Volatile
        private var instancia: DigestDatabase? = null

        fun obtener(context: Context): DigestDatabase =
            instancia ?: synchronized(this) {
                instancia ?: construir(context.applicationContext).also { instancia = it }
            }

        private fun construir(context: Context): DigestDatabase =
            Room.databaseBuilder(context, DigestDatabase::class.java, NOMBRE)
                // WAL permite leer mientras se escribe: el registro de una toma
                // desde el aviso no se solapa con la lectura de la pantalla de hoy.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                .build()

        /**
         * Paso 1 → 2: fecha de la última consulta e intervalo de revisión.
         *
         * `ultimaConsulta` se deja a null a propósito: el paciente aún no ha
         * dicho cuándo fue, y poner una fecha inventada haría que la app
         * avisara de una revisión que no toca.
         */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE perfil ADD COLUMN ultimaConsulta INTEGER DEFAULT NULL",
                )
                db.execSQL(
                    "ALTER TABLE perfil ADD COLUMN intervaloConsultaMeses INTEGER NOT NULL DEFAULT 6",
                )
            }
        }

        /**
         * Paso 2 → 3: control de tolerancia de las comidas.
         *
         * Se crea vacía y con `huboCambios` a nulo, que significa «sin
         * responder». No se rellenan controles para las comidas ya
         * registradas: preguntar ahora por una comida de hace tres días no
         * tiene sentido y daría datos inventados.
         */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS control_comidas (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "comidaId INTEGER NOT NULL, " +
                        "fecha INTEGER NOT NULL, " +
                        "horaComida INTEGER NOT NULL, " +
                        "programadoPara INTEGER NOT NULL, " +
                        "respondidoEn INTEGER, " +
                        "huboCambios INTEGER, " +
                        "registradoEn INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_control_comidas_comidaId " +
                        "ON control_comidas (comidaId)",
                )
            }
        }
    }
}