import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "UADAF-Website-Bakend"
version = "1.4.0"

val ktorVersion = "2.1.0"

plugins {
    application
    kotlin("jvm") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://dl.bintray.com/kotlin/ktor") }
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    maven { url = uri("https://dl.bintray.com/kotlin/exposed")}
    maven { url = uri("http://176.124.213.115/maven"); isAllowInsecureProtocol = true }
    maven { url = uri("https://kotlin.bintray.com/kotlin-js-wrappers") }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "bakend.CoreKt"
    applicationName = "UADAF-Website-Bakend"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.uadaf:uadamlib:1.4.1")
    implementation("org.jetbrains.exposed:exposed:0.10.4")
    implementation("mysql:mysql-connector-java:8.0.30")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css-jvm:1.0.0-pre.381")
    testImplementation(group = "junit", name = "junit", version = "4.12")
}
