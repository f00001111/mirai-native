plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
}

group = "org.itxtech"
version = "1.9.1"

kotlin {
    sourceSets {
        all {
            languageSettings.enableLanguageFeature("InlineClasses")
            languageSettings.useExperimentalAnnotation("kotlin.Experimental")
        }
    }
}

repositories {
    maven("https://mirrors.huaweicloud.com/repository/maven")
    maven("https://dl.bintray.com/him188moe/mirai")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

dependencies {
    api("org.jetbrains.kotlinx:atomicfu:0.14.4")

    implementation("net.mamoe:mirai-core:1.2.2")
    implementation("net.mamoe:mirai-console:1.0-M4-dev-3")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Name"] = "iTXTech MiraiNative"
        attributes["Revision"] = Runtime.getRuntime().exec("git rev-parse --short HEAD")
            .inputStream.bufferedReader().readText().trim()
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
