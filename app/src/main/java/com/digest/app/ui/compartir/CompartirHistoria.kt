package com.digest.app.ui.compartir

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.digest.app.R
import com.digest.app.data.DigestRepository
import com.digest.app.domain.GeneradorInforme
import com.digest.app.ui.diario.PeriodoInforme
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Comparte la historia entera con el médico en un solo envío.
 *
 * Antes de esto había dos acciones separadas y ninguna servía: una
 * compartía el texto del informe y otra los informes adjuntos. El
 * paciente acababa enviando un correo con un PDF de texto y otro con los
 * análisis sueltos, o peor, acababa enviando solo una de las dos cosas y que el
 * médico se quedara sin media historia.
 *
 * Aquí se juntan: el resumen escrito y todos los informes van en el mismo
 * envío. Es lo que el médico espera recibir y lo que evita tener que
 * acordarse de qué era lo que faltaba.
 */
object CompartirHistoria {

    private val FECHA: DateTimeFormatter =
        DateTimeFormatter.ofPattern("ddMMyyyy", Locale.Builder().setLanguage("es").build())

    /**
     * Prepara y abre el menú de compartir.
     *
     * @param periodo días que cubre el resumen. Los informes adjuntos se
     *   mandan siempre todos. Da igual el periodo: no son datos del
     *   periodo, son documentos de la historia entera.
     */
    suspend fun compartir(
        contexto: Context,
        repositorio: DigestRepository,
        periodo: PeriodoInforme,
    ) {
        val hasta = LocalDate.now()
        val desde = hasta.minusDays((periodo.dias - 1).toLong())

        val informe = repositorio.prepararInforme(desde, hasta)
        val resumen = GeneradorInforme.texto(informe)

        val adjuntos = repositorio.documentos()
            .mapNotNull { documento ->
                val archivo = File(contexto.filesDir, "documentos/${documento.nombreArchivo}")
                if (archivo.exists()) documento to archivo else null
            }

        abrirMenu(contexto, resumen, desde, hasta, adjuntos)
    }

    private fun abrirMenu(
        contexto: Context,
        resumen: String,
        desde: LocalDate,
        hasta: LocalDate,
        adjuntos: List<Pair<com.digest.app.data.local.entity.DocumentoEntity, File>>,
    ) {
        val asunto = contexto.getString(R.string.historia_asunto, desde.format(FECHA), hasta.format(FECHA))

        // Sin adjuntos se manda texto plano y no varios archivos: algunas
        // apps de correo y mensajería manejan mal ACTION_SEND_MULTIPLE, y
        // sin archivos que mandar no aporta nada usarlo.
        if (adjuntos.isEmpty()) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, asunto)
                putExtra(Intent.EXTRA_TEXT, resumen)
            }
            contexto.startActivity(
                Intent.createChooser(intent, contexto.getString(R.string.historia_compartir))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }

        val resumenUri = escribirResumen(contexto, resumen, desde, hasta)

        val archivos = buildList {
            add(resumenUri)
            adjuntos.forEach { (_, archivo) ->
                add(uriDe(contexto, archivo))
            }
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(archivos))
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // `clipData` además de `EXTRA_STREAM`: el permiso de lectura
            // se propaga a la app receptora a través del ClipData, y solo
            // con el extra hay versiones de Android que lo pierden y la
            // app de destino abre un archivo que no puede leer.
            clipData = ClipData.newUri(
                contexto.contentResolver,
                asunto,
                archivos.first(),
            ).also { datos ->
                archivos.drop(1).forEach { datos.addItem(ClipData.Item(it)) }
            }
        }

        contexto.startActivity(
            Intent.createChooser(intent, contexto.getString(R.string.historia_compartir))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /**
     * Escribe el resumen en un archivo para poder adjuntarlo.
     *
     * Va al directorio de caché y no al de documentos porque es una copia
     * desechable: se regenera cada vez que se comparte y no tiene sentido
     * guardarla. Android la limpia solo cuando hace falta.
     */
    private fun escribirResumen(
        contexto: Context,
        resumen: String,
        desde: LocalDate,
        hasta: LocalDate,
    ): Uri {
        val carpeta = File(contexto.cacheDir, "informes").apply { mkdirs() }
        val archivo = File(carpeta, "resumen-digest_${desde.format(FECHA)}_$hasta.txt")

        archivo.writeText(resumen)

        return uriDe(contexto, archivo)
    }

    private fun uriDe(contexto: Context, archivo: File): Uri =
        FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.documentos",
            archivo,
        )
}