package com.digest.app.ui.diario

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Abre el menú de compartir de Android con el informe dentro.
 *
 * Se usa el `Intent` del sistema en lugar de escribir el archivo a mano
 * porque así el paciente puede mandarlo por correo, mensajería o
 * guardarlo donde prefiera, sin que la app tenga que inventar donde.
 */
@Composable
fun CompartirInforme(informe: InformeParaCompartir) {
    val contexto = LocalContext.current

    LaunchedEffect(informe.nombreArchivo) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = informe.tipoMime
            putExtra(Intent.EXTRA_SUBJECT, "Informe de seguimiento — Digest")
            putExtra(Intent.EXTRA_TEXT, informe.contenido)
        }

        runCatching {
            contexto.startActivity(
                Intent.createChooser(intent, "Compartir informe con…")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}