plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.vscodemobile"
    compileSdk = 34
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.vscodemobile"
        minSdk = 26

        // NAO subir isto para 29+. Medido no aparelho (Android 16 / SDK 36):
        //
        //   targetSdk 34 -> untrusted_app       -> exec do filesDir: Permission denied (rc=126)
        //   targetSdk 29 -> untrusted_app_29    -> exec do filesDir: Permission denied (rc=126)
        //   targetSdk 28 -> untrusted_app_27    -> exec do filesDir: OK
        //
        // A partir de API 29 o dominio SELinux perde `execute` sobre
        // app_data_file, e TODO o userland (bash, coreutils, apt) deixa de
        // rodar. O proprio Termux mantem targetSdk 28 por causa disso.
        // Compilar contra 34 continua valido; so o alvo muda.
        targetSdk = 28
        versionCode = 3
        versionName = "1.2.0"

        // Foco em celular ARM64
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    // Chave de debug versionada no repo, e nao a `~/.android/debug.keystore`
    // de cada maquina. Sem isso, cada run do CI gera um keystore novo e cada
    // APK sai com assinatura diferente — o que impede instalar um build novo
    // por cima do anterior (INSTALL_FAILED_UPDATE_INCOMPATIBLE) e obriga a
    // desinstalar, levando junto os dados do app e o prefixo do Termux.
    // Keystore de debug nao e segredo.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // O app nao vai para a Play Store, entao release usa a mesma chave
            // de debug: assim o APK de release e o de teste trocam entre si.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    externalNativeBuild {
        ndkBuild {
            path("src/main/jni/Android.mk")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("com.google.android.material:material:1.12.0")

    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
