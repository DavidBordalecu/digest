package com.digest.app.data

import com.digest.app.data.local.ComidaDao
import com.digest.app.data.local.DeposicionDao
import com.digest.app.data.local.DiarioDao
import com.digest.app.data.local.DolorDao
import com.digest.app.data.local.HorarioDao
import com.digest.app.data.local.ManifestacionPerianalDao
import com.digest.app.data.local.MedicamentoDao
import com.digest.app.data.local.NutricionEnteralDao
import com.digest.app.data.local.PerfilDao
import com.digest.app.data.local.PesajeDao
import com.digest.app.data.local.SintomaDao
import com.digest.app.data.local.TomaDao
import com.digest.app.data.local.entity.ComidaEntity
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
import com.digest.app.model.EstadoToma
import com.digest.app.model.TipoSintoma
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/**
 * Punto único de acceso a los datos.
 *
 * Concentra aquí las reglas que no son de una tabla concreta, sobre todo
 * [materializarTomasDelDia]: al abrir la app se generan las tomas de hoy a
 * partir de los horarios, de modo que la pantalla y las alarmas trabajen
 * siempre con las mismas filas.
 */
class DigestRepository(
    private val perfilDao: PerfilDao,
    private val medicamentoDao: MedicamentoDao,
    private val horarioDao: HorarioDao,
    private val tomaDao: TomaDao,
    private val comidaDao: ComidaDao,
    private val deposicionDao: DeposicionDao,
    private val dolorDao: DolorDao,
    private val sintomaDao: SintomaDao,
    private val diarioDao: DiarioDao,
    private val pesajeDao: PesajeDao,
    private val nutricionDao: NutricionEnteralDao,
    private val perianalDao: ManifestacionPerianalDao,
) {

    // --- Perfil ---

    fun observarPerfil(): Flow<PerfilEntity?> = perfilDao.observar()

    suspend fun obtenerPerfil(): PerfilEntity? = perfilDao.obtener()

    suspend fun guardarPerfil(perfil: PerfilEntity) = perfilDao.guardar(perfil)

    suspend fun perfilConfigurado(): Boolean = perfilDao.existe()

    // --- Medicación ---

    fun observarMedicamentos(): Flow<List<MedicamentoEntity>> = medicamentoDao.observarActivos()

    fun buscarMedicamentos(texto: String): Flow<List<MedicamentoEntity>> =
        medicamentoDao.buscar(MedicamentoEntity.normalizar(texto))

    fun observarHorarios(medicamentoId: Long): Flow<List<HorarioEntity>> =
        horarioDao.observarDeMedicamento(medicamentoId)

    suspend fun guardarMedicamento(medicamento: MedicamentoEntity): Long =
        medicamentoDao.insertar(medicamento)

    suspend fun obtenerMedicamento(id: Long): MedicamentoEntity? =
        medicamentoDao.obtener(id)

    /** Nombre legible de un medicamento, para los avisos. */
    suspend fun nombreMedicamento(id: Long?): String? = id?.let { obtenerMedicamento(it)?.nombre }

    suspend fun actualizarMedicamento(medicamento: MedicamentoEntity) =
        medicamentoDao.actualizar(medicamento)

    suspend fun desactivarMedicamento(id: Long) = medicamentoDao.desactivar(id)

    suspend fun guardarHorario(horario: HorarioEntity): Long = horarioDao.insertar(horario)

    suspend fun actualizarHorario(horario: HorarioEntity) = horarioDao.actualizar(horario)

    suspend fun borrarHorario(id: Long) = horarioDao.borrarPorId(id)

    suspend fun obtenerHorario(id: Long): HorarioEntity? = horarioDao.obtener(id)

    /** Todos los horarios de hoy, para programar las alarmas. */
    suspend fun horariosDeHoy(fecha: LocalDate = LocalDate.now()): List<HorarioEntity> =
        horarioDao.deHoy(bitDelDia(fecha))

    private fun bitDelDia(fecha: LocalDate): Int = 1 shl (fecha.dayOfWeek.value - 1)

    // --- Tomas ---

    fun observarTomas(fecha: LocalDate): Flow<List<com.digest.app.data.local.TomaConDetalle>> =
        tomaDao.observarDelDia(fecha)

    suspend fun marcarTomada(tomaId: Long) =
        tomaDao.marcarTomada(tomaId, LocalTime.now())

    suspend fun marcarOmitida(tomaId: Long) = tomaDao.marcarOmitida(tomaId)

    suspend fun volverAPendiente(tomaId: Long) = tomaDao.volverAPendiente(tomaId)

    /**
     * Crea las tomas de [fecha] que aún no existen.
     *
     * Es idempotente: se puede llamar cada vez que se abre la app. Los
     * horarios con días concretos solo generan toma los días que toca, y los
     * de "todos los días" generan uno por día.
     */
    suspend fun materializarTomasDelDia(
        fecha: LocalDate = LocalDate.now(),
        horarios: List<HorarioEntity>,
    ): List<TomaEntity> {
        val creadas = mutableListOf<TomaEntity>()
        for (horario in horarios) {
            if (!horario.aplicaEn(fecha.dayOfWeek)) continue
            val existente = tomaDao.obtenerPorHorarioYFecha(horario.id, fecha)
            if (existente != null) continue
            val toma = TomaEntity(
                horarioId = horario.id,
                fecha = fecha,
                horaProgramada = horario.hora,
                estado = EstadoToma.PENDIENTE,
            )
            val id = tomaDao.insertarSiNoExiste(toma)
            if (id != -1L) creadas += toma.copy(id = id)
        }
        return creadas
    }

    suspend fun obtenerToma(tomaId: Long): TomaEntity? = tomaDao.obtener(tomaId)

    suspend fun adherencia(desde: LocalDate, hasta: LocalDate) = tomaDao.adherencia(desde, hasta)

    suspend fun pendientesVencidas(
        fecha: LocalDate = LocalDate.now(),
        ahora: LocalTime = LocalTime.now(),
    ): Int = tomaDao.pendientesVencidas(fecha, ahora)

    // --- Comidas ---

    fun observarComidas(fecha: LocalDate): Flow<List<ComidaEntity>> =
        comidaDao.observarDelDia(fecha)

    suspend fun guardarComida(comida: ComidaEntity): Long = comidaDao.insertar(comida)

    suspend fun borrarComida(id: Long) = comidaDao.borrar(id)

    // --- Deposiciones ---

    fun observarDeposiciones(fecha: LocalDate): Flow<List<DeposicionEntity>> =
        deposicionDao.observarDelDia(fecha)

    suspend fun guardarDeposicion(deposicion: DeposicionEntity): Long =
        deposicionDao.insertar(deposicion)

    // --- Dolor ---

    fun observarDolores(fecha: LocalDate): Flow<List<DolorEntity>> = dolorDao.observarDelDia(fecha)

    suspend fun guardarDolor(dolor: DolorEntity): Long = dolorDao.insertar(dolor)

    // --- Síntomas ---

    fun observarSintomas(fecha: LocalDate): Flow<List<SintomaEntity>> =
        sintomaDao.observarDelDia(fecha)

    suspend fun alternarSintoma(fecha: LocalDate, tipo: TipoSintoma, gravedad: Int) {
        val actual = sintomaDao.obtener(fecha, tipo)
        if (actual == null) {
            sintomaDao.guardar(
                SintomaEntity(fecha = fecha, tipo = tipo, gravedad = gravedad),
            )
        } else {
            sintomaDao.quitar(fecha, tipo)
        }
    }

    suspend fun marcarSintoma(fecha: LocalDate, tipo: TipoSintoma, gravedad: Int) =
        sintomaDao.guardar(SintomaEntity(fecha = fecha, tipo = tipo, gravedad = gravedad))

    suspend fun quitarSintoma(fecha: LocalDate, tipo: TipoSintoma) =
        sintomaDao.quitar(fecha, tipo)

    // --- Diario ---

    fun observarDiario(fecha: LocalDate): Flow<DiarioEntity?> = diarioDao.observar(fecha)

    suspend fun guardarDiario(diario: DiarioEntity) = diarioDao.guardar(diario)

    /** Actualiza solo los campos indicados, sin pisar el resto del día. */
    suspend fun actualizarDiario(
        fecha: LocalDate,
        transform: (DiarioEntity) -> DiarioEntity,
    ) {
        val actual = diarioDao.obtener(fecha) ?: DiarioEntity(fecha = fecha)
        diarioDao.guardar(transform(actual))
    }

    // --- Pesajes ---

    fun observarPesajes(): Flow<List<PesajeEntity>> = pesajeDao.observarUltimos()

    suspend fun guardarPesaje(pesaje: PesajeEntity): Long = pesajeDao.guardar(pesaje)

    // --- Nutrición enteral ---

    fun observarNutricion(fecha: LocalDate): Flow<List<NutricionEnteralEntity>> =
        nutricionDao.observarDelDia(fecha)

    fun volumenNutricion(fecha: LocalDate): Flow<Int> = nutricionDao.volumenDelDia(fecha)

    suspend fun guardarNutricion(registro: NutricionEnteralEntity): Long =
        nutricionDao.insertar(registro)

    // --- Manifestaciones perianales ---

    fun observarPerianales(fecha: LocalDate): Flow<List<ManifestacionPerianalEntity>> =
        perianalDao.observarDelDia(fecha)

    suspend fun guardarPerianal(manifestacion: ManifestacionPerianalEntity): Long =
        perianalDao.insertar(manifestacion)
}