// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // KSP : traite les annotations Room a la compilation pour generer
    // le code d'acces a la base de donnees. Equivalent conceptuel de
    // l'annotation processor MapStruct cote Spring Boot.
    alias(libs.plugins.ksp)
}

android {
    namespace = "ht.mbds.calebtoussaint.trailgo"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "ht.mbds.calebtoussaint.trailgo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ---- Reseau : appels vers l'API Spring Boot ----
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // ---- Coroutines : appels asynchrones sans bloquer l'interface ----
    implementation(libs.coroutines.android)

    // ---- Room : cache local, fonctionnement hors ligne ----
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ---- Navigation entre les ecrans ----
    implementation(libs.navigation.compose)

    // ---- Cartographie : OSMDroid (OpenStreetMap), sans cle ni compte ----
    implementation(libs.osmdroid)
    implementation(libs.play.services.location)

    // ---- Stockage securise du jeton JWT ----
    implementation(libs.security.crypto)

    // ---- Synchronisation differee en arriere-plan ----
    implementation(libs.work.runtime.ktx)

    // ---- Chargement des images distantes ----
    implementation(libs.coil.compose)
}