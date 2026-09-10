plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

if (file("google-services.json").exists() || rootProject.file("google-services.json").exists()) {
    pluginManager.apply("com.google.gms.google-services")
}

android {
    val signingStoreFile = providers.environmentVariable("SDO_KEYSTORE_FILE").orNull
    val signingStorePassword = providers.environmentVariable("SDO_KEYSTORE_PASSWORD").orNull
    val signingKeyAlias = providers.environmentVariable("SDO_KEY_ALIAS").orNull
    val signingKeyPassword = providers.environmentVariable("SDO_KEY_PASSWORD").orNull

    namespace = "com.kinderman.sdo"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.kinderman.sdo"
        minSdk = 30
        targetSdk = 37
        versionCode = providers.gradleProperty("releaseCode").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("releaseVersion").orNull ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }

    signingConfigs {
        if (
            signingStoreFile != null &&
            signingStorePassword != null &&
            signingKeyAlias != null &&
            signingKeyPassword != null
        ) {
            create("release") {
                storeFile = file(signingStoreFile)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Firebase/Google Sign-In recognizes the production certificate SHAs. CI debug
            // artifacts therefore use the same keystore while local builds retain the
            // standard debug certificate when protected credentials are unavailable.
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        release {
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
