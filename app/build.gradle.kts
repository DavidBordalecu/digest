import java.util.Properties

/**
 * Credenciales de firma.
 *
 * Se leen de `keystore.properties`, que está fuera de git, para que la
 * contraseña no acabe en el historial del repositorio.
 */
val credencialesFirma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
    namespace = "com.digest.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.digest.app"
        minSdk = 26
        targetSdk = 36
        // La versión va dentro de la app y no solo en el nombre del archivo: es
        // la que ve el paciente en ajustes y la que usa Play para ordenar
        // las actualizaciones. Un `versionCode` repetido impide subir.
        versionCode = 5
        versionName = "1.5"
    }

    signingConfigs {
        create("firma") {
            // Sin credenciales se usa la firma de depuración y el proyecto
            // sigue compilando, para que un clon recién bajado funcione.
            if (credencialesFirma.isNotEmpty()) {
                storeFile = rootProject.file(credencialesFirma.getProperty("digest.storeFile"))
                storePassword = credencialesFirma.getProperty("digest.storePassword")
                keyAlias = credencialesFirma.getProperty("digest.keyAlias")
                keyPassword = credencialesFirma.getProperty("digest.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 reduce el APK y ofusca el código. Las reglas por defecto de
            // AndroidX ya cubren Room y Compose, que son los puntos
            // sensibles de esta app.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("firma")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        // Las anotaciones de Android (p. ej. @StringRes) se aplican también al
        // campo, no solo al parámetro: evita los avisos de KT-73255.
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

// Exporta el esquema de la base de datos para control de migraciones
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Base de datos local (Room)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Preferencias y trabajo en segundo plano
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.work.runtime.ktx)

  // Tests
  testImplementation(libs.androidx.room.testing)
}
