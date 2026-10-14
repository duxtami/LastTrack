import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    api(project(":core:model"))
    // XmlPull is part of the Android platform at runtime; the API jar is only needed to compile.
    compileOnly(libs.xmlpull)
    testImplementation(libs.xmlpull)
    testImplementation(libs.kxml2)
    testImplementation(libs.junit)
}
