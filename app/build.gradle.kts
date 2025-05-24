plugins {
    application
    kotlin("jvm") version "1.9.22"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit)

    implementation(libs.guava)
    implementation("com.rabbitmq:amqp-client:5.20.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "org.example.ChatClientKt"
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("fat")

    manifest {
        attributes["Main-Class"] = "org.example.ChatClientKt"
    }

    from(sourceSets.main.get().output)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
