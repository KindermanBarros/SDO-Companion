import java.util.Properties

plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
    id("org.jetbrains.kotlinx.kover")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

ktlint {
    android.set(true)
}

detekt {
    buildUponDefaultConfig = true
}

kover {
    reports {
        filters {
            excludes {
                packages("*.screen", "*.components")
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

if (
    file("google-services.json").exists() ||
    file("src/dev/google-services.json").exists() ||
    file("src/prod/google-services.json").exists() ||
    rootProject.file("google-services.json").exists()
) {
    pluginManager.apply("com.google.gms.google-services")
}

afterEvaluate {
    tasks.configureEach {
        if (name.contains("Dev") && name.endsWith("GoogleServices") && !file("src/dev/google-services.json").exists()) {
            enabled = false
        }
        if (
            name.contains("Prod") &&
            name.endsWith("GoogleServices") &&
            !file("src/prod/google-services.json").exists() &&
            !file("google-services.json").exists()
        ) {
            enabled = false
        }
    }
}

android {
    val localProperties = Properties().apply {
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { load(it) }
        }
    }

    fun propertyOrEnv(key: String, altKey: String? = null): String? =
        providers.environmentVariable(key).orNull
            ?: providers.gradleProperty(key).orNull
            ?: localProperties.getProperty(key)
            ?: altKey?.let {
                providers.environmentVariable(it).orNull
                    ?: providers.gradleProperty(it).orNull
                    ?: localProperties.getProperty(it)
            }

    val signingStorePath = propertyOrEnv("SDO_KEYSTORE_FILE", "KEYSTORE_FILE")
    val resolvedStoreFile = signingStorePath?.let { path ->
        val candidate = file(path)
        if (candidate.isFile) candidate
        else rootProject.file(path).takeIf { it.isFile }
    } ?: rootProject.file("firebase/sdo-companion-release.jks").takeIf { it.isFile }

    val signingStorePassword = propertyOrEnv("SDO_KEYSTORE_PASSWORD", "KEYSTORE_PASSWORD")
    val signingKeyAlias = propertyOrEnv("SDO_KEY_ALIAS", "KEY_ALIAS")
    val signingKeyPassword = propertyOrEnv("SDO_KEY_PASSWORD", "KEY_PASSWORD")

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
    buildFeatures { compose = true; buildConfig = true; resValues = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }

    signingConfigs {
        if (
            resolvedStoreFile != null &&
            signingStorePassword != null &&
            signingKeyAlias != null &&
            signingKeyPassword != null
        ) {
            create("release") {
                storeFile = resolvedStoreFile
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release")
        }
    }

    val devTestEmail = propertyOrEnv("SDO_DEV_TEST_EMAIL").orEmpty()
    val devTestPassword = propertyOrEnv("SDO_DEV_TEST_PASSWORD").orEmpty()

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "SDO Companion (Dev)")
            buildConfigField("String", "ENVIRONMENT", "\"dev\"")
            buildConfigField("boolean", "IS_DEV_ENV", "true")
            if (devTestEmail.isNotBlank()) {
                testInstrumentationRunnerArguments["sdoDevTestEmail"] = devTestEmail
            }
            if (devTestPassword.isNotBlank()) {
                testInstrumentationRunnerArguments["sdoDevTestPassword"] = devTestPassword
            }
        }
        create("prod") {
            dimension = "env"
            resValue("string", "app_name", "SDO Companion")
            buildConfigField("String", "ENVIRONMENT", "\"prod\"")
            buildConfigField("boolean", "IS_DEV_ENV", "false")
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.gson)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
