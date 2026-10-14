package com.digest.app.data

import com.digest.app.data.local.CirugiaDao
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
import com.digest.app.data.local.entity.CirugiaEntity
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
import com.digest.app.domain.AlertasClinicas
import com.digest.app.domain.ContextoClinico
import com.digest.app.domain.DeposicionDia
import com.digest.app.domain.InformePeriodo
import com.digest.app.domain.SintomaDia
import com.digest.app.domain.nombreLegible
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
    private val cirugiaDao: CirugiaDao,
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

    fun observarCirugias(): Flow<List<CirugiaEntity>> = cirugiaDao.observarTodas()

    suspend fun guardarCirugia(cirugia: CirugiaEntity): Long = cirugiaDao.insertar(cirugia)

    // --- Medicación ---

    fun observarMedicamentos(): Flow<List<MedicamentoEntity>> = medicamentoDao.observarActivos()

    fun buscarMedicamentos(texto: String): Flow<List<MedicamentoEntity>> =
        medicamentoDao.buscar(MedicamentoEntity.normalizar(texto))

    fun observarHorarios(medicamentoId: Long): Flow<List<HorarioEntity>> =
        horarioDao.observarDeMedicamento(medicamentoId)

    fun observarTodosLosHorarios(): Flow<List<HorarioEntity>> = horarioDao.observarTodos()

    suspend fun guardarMedicamento(medicamento: MedicamentoEntity): Long =
        medicamentoDao.insertar(conNombreNormalizado(medicamento))

    /**
     * Recalcula la copia normalizada del nombre antes de guardar.
     *
     * Kotlin no vuelve a evaluar los valores por defecto del constructor al
     * usar `copy()`: si el paciente corrige el nombre del fármaco se
     * quedaría la copia antigua y la búsqueda dejaría de encontrarlo.
     * Centralizarlo aquí evita que cada pantalla tenga que acordarse.
     */
    private fun conNombreNormalizado(medicamento: MedicamentoEntity) = medicamento.copy(
        nombreNormalizado = MedicamentoEntity.normalizar(medicamento.nombre),
    )

    suspend fun obtenerMedicamento(id: Long): MedicamentoEntity? =
        medicamentoDao.obtener(id)

    /** Nombre legible de un medicamento, para los avisos. */
    suspend fun nombreMedicamento(id: Long?): String? = id?.let { obtenerMedicamento(it)?.nombre }

    suspend fun actualizarMedicamento(medicamento: MedicamentoEntity) =
        medicamentoDao.actualizar(conNombreNormalizado(medicamento))

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

    /** Tomas de [fecha] que siguen sin tomar: la lista que hay que avisar. */
    suspend fun tomasPendientesDe(fecha: LocalDate = LocalDate.now()): List<TomaEntity> =
        tomaDao.pendientesDe(fecha)

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

    /** Comidas de días previos, para rellenar rápido lo que ya se comió. */
    suspend fun alimentosRecientes(antesDe: LocalDate): List<String> =
        comidaDao.alimentosRecientes(antesDe)

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

    // --- Contexto clínico para las señales de alarma ---
    //
    // Estas consultas no salen de la interfaz: son las que alimentan
    // AlertasClinicas. Se mantienen aquí para que el evaluador no tenga que
    // saber nada de Room.

    suspend fun deposicionesDe(fecha: LocalDate): List<DeposicionEntity> =
        deposicionDao.delDia(fecha)

    suspend fun deposicionesEntre(desde: LocalDate, hasta: LocalDate): Int =
        deposicionDao.contarRango(desde, hasta)

    /** Síntomas de un día, leídos frescos (no desde el flujo reactivo). */
    suspend fun sintomasDe(fecha: LocalDate): List<SintomaEntity> =
        sintomaDao.de(fecha)

    suspend fun diarioDe(fecha: LocalDate): DiarioEntity? = diarioDao.obtener(fecha)

    suspend fun manifestacionesPerianalesEntre(desde: LocalDate, hasta: LocalDate): Int =
        perianalDao.contarRango(desde, hasta)

    suspend fun fechasDeSintoma(tipo: TipoSintoma, hasta: LocalDate): List<LocalDate> =
        sintomaDao.fechasDeSintoma(tipo, hasta)

    /**
     * Días consecutivos que lleva presente cada síntoma hasta [hoy].
     *
     * Solo cuenta hacia atrás desde hoy: un síntoma que dejó de marcarse ayer
     * tiene racha cero, que es lo que espera el paciente al dejar de marcar
     * que se le ha pasado.
     */
    suspend fun rachasDeSintomas(
        tipos: Set<TipoSintoma>,
        hoy: LocalDate,
    ): Map<TipoSintoma, Int> {
        val resultado = mutableMapOf<TipoSintoma, Int>()
        for (tipo in tipos) {
            val fechas = fechasDeSintoma(tipo, hoy)
            if (fechas.isEmpty() || fechas.first() != hoy) continue

            var racha = 0
            var esperada = hoy
            for (fecha in fechas) {
                if (fecha != esperada) break
                racha++
                esperada = esperada.minusDays(1)
            }
            if (racha > 0) resultado[tipo] = racha
        }
        return resultado
    }

    /**
     * Pérdida de peso acumulada en los últimos [meses] meses, en porcentaje.
     *
     * Compara el peso medio reciente con el de hace `meses`. Se usan medias
     * y no valores sueltos porque el peso fluctúa a diario por la retención
     * de líquidos: comparar un único peso produce falsos positivos.
     */
    suspend fun perdidaDePesoPorcentaje(meses: Long = 3): Float? {
        val hoy = LocalDate.now()
        val desde = hoy.minusMonths(meses)
        val antiguos = pesajeDao.entre(desde, hoy.minusMonths(1))
        val recientes = pesajeDao.entre(hoy.minusMonths(1), hoy)

        if (antiguos.isEmpty() || recientes.isEmpty()) return null

        val antes = antiguos.map { it.pesoKg }.average()
        val ahora = recientes.map { it.pesoKg }.average()
        if (antes <= 0) return null

        val perdida = ((antes - ahora) / antes * 100).toFloat()
        // Solo interesan las caídas: ganar peso no es una señal de alarma.
        return if (perdida > 0f) perdida else null
    }

    suspend fun guardarConsulta(fecha: LocalDate) {
        val perfil = obtenerPerfil() ?: return
        guardarPerfil(perfil.copy(ultimaConsulta = fecha, actualizadoEn = System.currentTimeMillis()))
    }

    suspend fun guardarIntervaloConsulta(meses: Int) {
        val perfil = obtenerPerfil() ?: return
        guardarPerfil(perfil.copy(intervaloConsultaMeses = meses.coerceIn(1, 36)))
    }

    // --- Informe para el médico ---

    /**
     * Reúne los datos de un periodo en la forma que necesita el informe.
     *
     * Va al final del repositorio a propósito: es una lectura transversal a
     * casi todas las tablas, no un dato más del dominio.
     */
    suspend fun prepararInforme(desde: LocalDate, hasta: LocalDate): InformePeriodo {
        val perfil = obtenerPerfil()
        val deposiciones = deposicionDao.entre(desde, hasta)

        // Los síntomas se agrupan por día recorriendo cada tipo: así no
        // hace falta una consulta por fecha, que serían una por día del
        // periodo.
        val sintomasPorDia = mutableMapOf<LocalDate, MutableList<SintomaDia>>()
        for (tipo in TipoSintoma.entries) {
            for (fecha in fechasDeSintoma(tipo, hasta)) {
                if (fecha < desde) continue
                val gravedad = sintomaDao.obtener(fecha, tipo)?.gravedad ?: continue
                sintomasPorDia.getOrPut(fecha) { mutableListOf() }
                    .add(SintomaDia(tipo.nombreLegible, gravedad))
            }
        }

        return InformePeriodo(
            desde = desde,
            hasta = hasta,
            perfil = perfil,
            tomas = tomaDao.observarRangoSincrono(desde, hasta),
            deposiciones = deposiciones,
            dolores = dolorDao.entre(desde, hasta),
            nutricion = nutricionDao.entre(desde, hasta),
            sintomas = sintomasPorDia,
            comidas = comidaDao.entre(desde, hasta).groupBy { it.fecha },
            diario = diarioDao.entre(desde, hasta).associateBy { it.fecha },
            pesajes = pesajeDao.entre(desde, hasta),
            senales = construirContextoClinico(perfil, deposiciones, hasta)
                ?.let { AlertasClinicas.evaluar(it) }
                .orEmpty(),
        )
    }

    /**
     * Monta el contexto que consumen las señales de alarma.
     *
     * Vive aquí para que la pantalla de hoy y el informe partan de la
     * misma construcción: si se duplicara, el médico recibiría unos datos y
     * el paciente vería otros.
     */
    suspend fun construirContextoClinico(
        perfil: PerfilEntity?,
        deposiciones: List<DeposicionEntity>?,
        hoy: LocalDate,
    ): ContextoClinico? {
        val p = perfil ?: return null
        val sintomasHoy = sintomaDao.de(hoy)
        val deposicionesHoy = deposiciones?.filter { it.fecha == hoy }
            ?: deposicionDao.delDia(hoy)

        return ContextoClinico(
            condicion = p.condicion,
            diasDesdeUltimaConsulta = p.ultimaConsulta?.let {
                java.time.temporal.ChronoUnit.DAYS.between(it, hoy).toInt()
            },
            intervaloConsultaMeses = p.intervaloConsultaMeses,
            sintomasHoy = sintomasHoy.associate { it.tipo to it.gravedad },
            diasConSintoma = rachasDeSintomas(sintomasHoy.map { it.tipo }.toSet(), hoy),
            deposicionesHoy = deposicionesHoy.map { DeposicionDia(it.bristol, it.sangre) },
            deposicionesUltimosSieteDias = deposicionDao.contarRango(hoy.minusDays(6), hoy),
            perdidaPesoPorcentaje = perdidaDePesoPorcentaje(),
            sensacion = diarioDao.obtener(hoy)?.sensacion,
            manifestacionPerianalReciente = perianalDao.contarRango(
                hoy.minusDays(10),
                hoy,
            ) > 0,
        )
    }
}