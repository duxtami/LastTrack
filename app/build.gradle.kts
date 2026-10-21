import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun getSigningProperty(vararg keys: String): String? {
    for (key in keys) {
        val prop = (project.findProperty(key) as? String)
            ?: keystoreProperties.getProperty(key)
            ?: System.getenv(key)
        if (!prop.isNullOrBlank()) {
            return prop
        }
    }
    return null
}

val releaseStoreFile = getSigningProperty("RELEASE_STORE_FILE", "storeFile", "KEYSTORE_FILE", "KEYSTORE_PATH")
val releaseStorePassword = getSigningProperty("RELEASE_STORE_PASSWORD", "storePassword", "KEYSTORE_PASSWORD")
val releaseKeyAlias = getSigningProperty("RELEASE_KEY_ALIAS", "keyAlias", "KEY_ALIAS")
val releaseKeyPassword = getSigningProperty("RELEASE_KEY_PASSWORD", "keyPassword", "KEY_PASSWORD")

val releaseKeyFile = releaseStoreFile?.let { path ->
    val f = java.io.File(path)
    if (f.isAbsolute) {
        f
    } else {
        val rootFile = rootProject.file(path)
        if (rootFile.exists()) rootFile else file(path)
    }
}

val hasReleaseSigning = releaseKeyFile != null &&
        releaseKeyFile.exists() &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

if (!releaseStoreFile.isNullOrBlank() && !hasReleaseSigning) {
    logger.warn("Release signing configuration incomplete or file not found: path=$releaseStoreFile, resolved=${releaseKeyFile?.absolutePath}, exists=${releaseKeyFile?.exists()}")
}

android {
    namespace = "LasTrack.duxtami.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "LasTrack.duxtami.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        maybeCreate("release").apply {
            if (hasReleaseSigning) {
                storeFile = releaseKeyFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:parser"))
    implementation(project(":core:map"))
    implementation(project(":map:google"))
    implementation(project(":map:mapbox"))
    implementation(project(":map:arcgis"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.osmdroid)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
