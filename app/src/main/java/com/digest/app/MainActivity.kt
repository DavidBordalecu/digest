package com.digest.app

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.digest.app.notificaciones.Permisos
import com.digest.app.theme.DigestTheme
import com.digest.app.ui.App

class MainActivity : ComponentActivity() {

    private val pedirNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        pedirPermisoDeAvisos()

        setContent {
            DigestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    App()
                }
            }
        }
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