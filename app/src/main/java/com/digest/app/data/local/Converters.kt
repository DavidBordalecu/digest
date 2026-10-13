package com.digest.app.data.local

import androidx.room.TypeConverter
import com.digest.app.model.Afectacion
import com.digest.app.model.Condicion
import com.digest.app.model.EstadoBrote
import com.digest.app.model.EstadoToma
import com.digest.app.model.GrupoFarmaco
import com.digest.app.model.LocalizacionDolor
import com.digest.app.model.NivelHidratacion
import com.digest.app.model.NivelMejora
import com.digest.app.model.SangreEnHeces
import com.digest.app.model.SensacionGeneral
import com.digest.app.model.TipoCirugia
import com.digest.app.model.TipoComida
import com.digest.app.model.TipoDieta
import com.digest.app.model.TipoDolor
import com.digest.app.model.TipoNutricionEnteral
import com.digest.app.model.TipoPresentacion
import com.digest.app.model.TipoSintoma
import com.digest.app.model.ViaAdministracion
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Resuelve un enum por su nombre devolviendo `null` si ya no existe.
 *
 * Los enums se guardan por nombre y no por ordinal: reordenar las constantes
 * no invalida los datos ya guardados, y una constante renombrada deja de
 * leerse como `null` en lugar de romper la consulta entera.
 */
private inline fun <reified T : Enum<T>> enumFromName(value: String?): T? =
    value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

/**
 * Conversores de Room para los tipos que no son primitivos.
 *
 * Room exige un conversor con tipo concreto por cada tipo de campo, así que
 * los enums se emparejan de dos en dos delegando en [enumFromName].
 */
class Converters {

    // --- Fechas y horas ---
    @TypeConverter
    fun fechaToLong(valor: LocalDate?): Long? = valor?.toEpochDay()

    @TypeConverter
    fun longToFecha(valor: Long?): LocalDate? = valor?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun horaToLong(valor: LocalTime?): Long? = valor?.toSecondOfDay()?.toLong()

    @TypeConverter
    fun longToHora(valor: Long?): LocalTime? = valor?.let(LocalTime::ofSecondOfDay)

    @TypeConverter
    fun instantToLong(valor: Instant?): Long? = valor?.toEpochMilli()

    @TypeConverter
    fun longToInstant(valor: Long?): Instant? = valor?.let(Instant::ofEpochMilli)

    // --- Listas de enums ---
    // Devuelven cadena vacía en lugar de `null` cuando la lista está vacía:
    // las columnas de lista son NOT NULL e insertar `null` provocaría un fallo.
    @TypeConverter
    fun listaToString(valores: List<String>?): String? = valores?.joinToString(SEPARADOR)

    @TypeConverter
    fun stringToLista(valor: String?): List<String>? =
        valor?.split(SEPARADOR)?.filter { it.isNotBlank() }

    /** El paciente puede tener más de una zona afectada (p. ej. íleo + perianal). */
    @TypeConverter
    fun afectacionesToString(valores: List<Afectacion>?): String? =
        valores?.joinToString(SEPARADOR) { it.name }

    @TypeConverter
    fun stringToAfectaciones(valor: String?): List<Afectacion> =
        valor?.split(SEPARADOR)?.mapNotNull { enumFromName<Afectacion>(it) } ?: emptyList()

    // --- Enums ---
    @TypeConverter fun condicionToString(v: Condicion?): String? = v?.name
    @TypeConverter fun stringToCondicion(v: String?): Condicion? = enumFromName(v)

    @TypeConverter fun afectacionToString(v: Afectacion?): String? = v?.name
    @TypeConverter fun stringToAfectacion(v: String?): Afectacion? = enumFromName(v)

    @TypeConverter fun tipoSintomaToString(v: TipoSintoma?): String? = v?.name
    @TypeConverter fun stringToTipoSintoma(v: String?): TipoSintoma? = enumFromName(v)

    @TypeConverter fun tipoCirugiaToString(v: TipoCirugia?): String? = v?.name
    @TypeConverter fun stringToTipoCirugia(v: String?): TipoCirugia? = enumFromName(v)

    @TypeConverter fun tipoPresentacionToString(v: TipoPresentacion?): String? = v?.name
    @TypeConverter fun stringToTipoPresentacion(v: String?): TipoPresentacion? = enumFromName(v)

    @TypeConverter fun viaToString(v: ViaAdministracion?): String? = v?.name
    @TypeConverter fun stringToVia(v: String?): ViaAdministracion? = enumFromName(v)

    @TypeConverter fun estadoTomaToString(v: EstadoToma?): String? = v?.name
    @TypeConverter fun stringToEstadoToma(v: String?): EstadoToma? = enumFromName(v)

    @TypeConverter fun grupoToString(v: GrupoFarmaco?): String? = v?.name
    @TypeConverter fun stringToGrupo(v: String?): GrupoFarmaco? = enumFromName(v)

    @TypeConverter fun tipoComidaToString(v: TipoComida?): String? = v?.name
    @TypeConverter fun stringToTipoComida(v: String?): TipoComida? = enumFromName(v)

    @TypeConverter fun tipoNutricionToString(v: TipoNutricionEnteral?): String? = v?.name
    @TypeConverter fun stringToTipoNutricion(v: String?): TipoNutricionEnteral? = enumFromName(v)

    @TypeConverter fun nivelHidratacionToString(v: NivelHidratacion?): String? = v?.name
    @TypeConverter fun stringToNivelHidratacion(v: String?): NivelHidratacion? = enumFromName(v)

    @TypeConverter fun nivelMejoraToString(v: NivelMejora?): String? = v?.name
    @TypeConverter fun stringToNivelMejora(v: String?): NivelMejora? = enumFromName(v)

    @TypeConverter fun sensacionToString(v: SensacionGeneral?): String? = v?.name
    @TypeConverter fun stringToSensacion(v: String?): SensacionGeneral? = enumFromName(v)

    @TypeConverter fun estadoBroteToString(v: EstadoBrote?): String? = v?.name
    @TypeConverter fun stringToEstadoBrote(v: String?): EstadoBrote? = enumFromName(v)

    @TypeConverter fun tipoDietaToString(v: TipoDieta?): String? = v?.name
    @TypeConverter fun stringToTipoDieta(v: String?): TipoDieta? = enumFromName(v)

    @TypeConverter fun tipoDolorToString(v: TipoDolor?): String? = v?.name
    @TypeConverter fun stringToTipoDolor(v: String?): TipoDolor? = enumFromName(v)

    @TypeConverter fun localizacionToString(v: LocalizacionDolor?): String? = v?.name
    @TypeConverter fun stringToLocalizacion(v: String?): LocalizacionDolor? = enumFromName(v)

    @TypeConverter fun sangreToString(v: SangreEnHeces?): String? = v?.name
    @TypeConverter fun stringToSangre(v: String?): SangreEnHeces? = enumFromName(v)

    private companion object {
        /** Separador de listas. No aparece en nombres de enum. */
        const val SEPARADOR = "|"
    }
}