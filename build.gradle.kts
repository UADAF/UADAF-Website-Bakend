import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "UADAF-Website-Bakend"
version = "1.2"

val ktorVersion = "1.0.0-beta-3"

plugins {
    application
    kotlin("jvm") version "1.3.31"
}

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://dl.bintray.com/kotlin/ktor") }
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    maven { url = uri("https://dl.bintray.com/kotlin/exposed")}
    maven { url = uri("http://52.48.142.75/maven") }
    maven { url = uri("https://kotlin.bintray.com/kotlin-js-wrappers") }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "CoreKt"
    applicationName = "UADAF-Website-Bakend"
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-gson:$ktorVersion")
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("com.google.code.gson:gson:2.8.4")
    compile("com.uadaf:uadamlib:1.4.1")
    compile("org.jetbrains.exposed:exposed:0.10.4")
    compile("mysql:mysql-connector-java:6.0.6")
    compile("com.google.guava:guava:25.0-jre")
    compile("io.ktor:ktor-html-builder:$ktorVersion")
    compile("org.jetbrains:kotlin-css-jvm:1.0.0-pre.31-kotlin-1.2.41")

    testCompile(group = "junit", name = "junit", version = "4.12")
}

tasks {
    withType<Jar> {
        manifest {
            attributes(mapOf("Main-Class" to "CoreKt"))
        }
        archiveName = "${application.applicationName}-$version.jar"
        from(configurations.compile.map {
            if (it.isDirectory)
                it
            else zipTree(it)
        })
    }
}
