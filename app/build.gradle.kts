plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.elkhobaraa.lab"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.elkhobaraa.lab"
        minSdk = 26
        targetSdk = 34
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    // Optional fixed signing key (written by CI from the SIGNING_KEYSTORE_B64 secret) so app
    // updates install over the old version without losing data. Falls back to the default debug key.
    val ks = file("signing.keystore")
    if (ks.exists()) {
        signingConfigs {
            getByName("debug") {
                storeFile = ks
                storePassword = System.getenv("SIGNING_PASSWORD") ?: "android"
                keyAlias = System.getenv("SIGNING_ALIAS") ?: "androiddebugkey"
                keyPassword = System.getenv("SIGNING_PASSWORD") ?: "android"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
