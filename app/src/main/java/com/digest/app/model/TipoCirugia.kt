package com.digest.app.model

import androidx.annotation.StringRes
import com.digest.app.R

/**
 * Intervenciones quirúrgicas del historial del paciente.
 *
 * Más de la mitad de los pacientes con Crohn pasa por quirófano a lo largo de
 * su vida. Conocer si hay una resección ileocecal, una ileostomía o una
 * colostomía cambia por completo el tránsito intestinal y, con él, las
 * necesidades de hidratación y de dieta que hay que registrar.
 */
enum class TipoCirugia(
    @StringRes val displayName: Int,
    /** Si la intervención altera el tránsito intestinal de forma permanente. */
    val alteraTransito: Boolean = false,
    /** Si deja una ostomía permanente o temporal. */
    val dejaEstomia: Boolean = false,
) {
    RESECCION_ILEOCECAL(R.string.cirugia_ileocecal, alteraTransito = true),
    RESECCION_ILEAL(R.string.cirugia_reseccion_ileal, alteraTransito = true),
    HEMICOLECTOMIA(R.string.cirugia_hemicolectomia, alteraTransito = true),
    COLECTOMIA(R.string.cirugia_colectomia, alteraTransito = true),
    ILEOSTOMIA(R.string.cirugia_ileostomia, alteraTransito = true, dejaEstomia = true),
    COLOSTOMIA(R.string.cirugia_colostomia, alteraTransito = true, dejaEstomia = true),
    DRENAJE_ABSCESO(R.string.cirugia_drenaje_absceso),
    ESTENOPLASTIA(R.string.cirugia_estenoplastia),
    OTRAS(R.string.cirugia_otras),
}