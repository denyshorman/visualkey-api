group = "visualkey"
version = "0.0.0"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.graalvm.native)
}

application {
    mainClass.set("visualkey.MainKt")
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(23)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

ktor {
    fatJar {
        archiveFileName.set("api.jar")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.batik.transcoder)
    implementation(libs.batik.codec)
    implementation(libs.web3j.core)
}

graalvmNative {
    binaries {
        named("main") {
            verbose.set(true)
            imageName.set("api")

            buildArgs.add("--install-exit-handlers")
            buildArgs.add("--exact-reachability-metadata")
        }
    }
}
