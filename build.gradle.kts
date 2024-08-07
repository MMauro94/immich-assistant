plugins {
    kotlin("jvm") version "2.0.10"
}

group = "dev.mmauro"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("com.github.ajalt.mordant:mordant:2.7.2")
    implementation("com.github.ajalt.mordant:mordant-coroutines:2.7.2")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.github.seratch:kotliquery:1.9.0")

    val kotestVersion = "5.9.1"
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

tasks.register<Jar>("uberJar") {
    archiveClassifier.set("uber")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    }) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    manifest {
        attributes["Main-Class"] = "dev.mmauro.immichassistant.MainCommandKt"
    }
}
