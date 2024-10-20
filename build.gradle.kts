plugins {
    kotlin("jvm") version "2.0.20"
}

group = "org.vorpal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
    testImplementation("io.kotest:kotest-property:5.6.2")
    testImplementation("io.kotest:kotest-assertions-core:5.6.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}