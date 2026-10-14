package com.digest.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.digest.app.notificaciones.AvisosComida
import com.digest.app.notificaciones.Permisos
import com.digest.app.theme.DigestTheme
import com.digest.app.ui.App

class MainActivity : ComponentActivity() {

    private val pedirNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /**
     * Contador que sube cada vez que se abre la app desde el aviso de un
     * control de comida.
     *
     * No se guarda el identificador del control en un sitio al que Compose
     * pueda leer, sino que se cuenta. La pantalla de Hoy necesita enterarse
     * de que hay que ir allí, y con un contador basta: cada toque en el
     * aviso es un evento nuevo, y si se abre dos veces seguidas también
     * tiene que saltar dos veces.
     */
    private var controlAbierto by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        pedirPermisoDeAvisos()
        registrarControlSiVieneDeAviso(intent)

        setContent {
            DigestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    App(controlAbierto = controlAbierto)
                }
            }
        }
    }

    /**
     * La app ya estaba abierta cuando se toca el aviso: sin esto Android
     * reutiliza la actividad existente y el toque no haría nada.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        registrarControlSiVieneDeAviso(intent)
    }

    /** Si el aviso trae un control, se anota para llevar al paciente a Hoy. */
    private fun registrarControlSiVieneDeAviso(intent: Intent?) {
        if (intent?.hasExtra(AvisosComida.EXTRA_CONTROL_ID) != true) return
        controlAbierto++
    }

    /**
     * Pide el permiso de notificaciones nada más entrar.
     *
     * Sin él la app perdería su función principal. Solo se pide en
     * dispositivos con Android 13 o superior, donde es obligatorio, y el
     * sistema ya no vuelve a mostrar el diálogo si el usuario lo rechaza:
     * en ese caso la app avisa con un aviso en pantalla y enlaza a Ajustes.
     */
    private fun pedirPermisoDeAvisos() {
        if (!Permisos.soportaPermisoDeNotificaciones) return
        if (!Permisos.avisosActivados(this)) return

        val concedido = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        if (!concedido) {
            pedirNotificaciones.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}