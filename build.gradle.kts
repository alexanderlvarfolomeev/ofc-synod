plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "ru.varfolomeev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
    implementation("com.typesafe.akka:akka-actor_3:2.8.2")
    implementation("com.typesafe.akka:akka-slf4j_3:2.8.2")
    implementation("ch.qos.logback:logback-classic:1.4.6")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("ru.varfolomeev.MainKt")
}