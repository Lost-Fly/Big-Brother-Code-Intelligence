
val kotlin_version: String by project
val logback_version: String by project
val mongo_version: String by project

plugins {
    kotlin("jvm") version "2.0.20"
    id("io.ktor.plugin") version "3.0.0"
    kotlin("plugin.serialization") version "1.4.0"
}



group = "com.brother.big"
version = "0.0.1"

application {
    mainClass.set("com.brother.big.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("BigBrother.jar")
    }
}

tasks {
    shadowJar {
        archiveFileName.set("BigBrother111.jar")
    }
}


repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
}

dependencies {
    // Ktor core dependencies (updated to 2.0.20)
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-auto-head-response-jvm:2.3.12")
    implementation("io.ktor:ktor-server-resources-jvm:2.3.12")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.12")
    implementation("io.ktor:ktor-server-default-headers-jvm:2.3.12")
    implementation("io.ktor:ktor-server-openapi:2.3.12")
    implementation("io.ktor:ktor-server-swagger-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-jackson-jvm:2.3.12")
    implementation("com.google.code.gson:gson:2.11.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")

    // MongoDB driver dependencies
    implementation("org.mongodb:mongodb-driver-core:$mongo_version")
    implementation("org.mongodb:mongodb-driver-sync:$mongo_version")
    implementation("org.mongodb:bson:$mongo_version")

//    // Task scheduling dependencies
//    implementation("io.github.flaxoos:ktor-server-task-scheduling-core-jvm:1.2.10")
//    implementation("io.github.flaxoos:ktor-server-task-scheduling-redis-jvm:1.2.10")
//    implementation("io.github.flaxoos:ktor-server-task-scheduling-mongodb-jvm:1.2.10")
//    implementation("io.github.flaxoos:ktor-server-task-scheduling-jdbc-jvm:1.2.10")

    // Ktor server engine version aligned with modern version
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml:2.3.12")

    // JGit dependencies
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:6.7.0.202309050840-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.gpg.bc:6.7.0.202309050840-r")

    // Coroutines with updated versions aligned for better compatibility
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0") // Современная версия
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.9.0")

    // Matching the Kotlin runtime version
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")

    // Update Ktor client dependencies to the correct version
    implementation("io.ktor:ktor-client-core-jvm:2.3.12")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
}
