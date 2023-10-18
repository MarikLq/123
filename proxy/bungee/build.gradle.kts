val mavenGroup: String by rootProject
val buildVersion: String by rootProject

val bungeeVersion: String by project

plugins {
    id("su.plo.voice.relocate")
}

group = "$mavenGroup.bungee"

repositories {
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    compileOnly("net.md-5:bungeecord-api:$bungeeVersion")
    compileOnly("org.bstats:bstats-bungeecord:${rootProject.libs.versions.bstats.get()}")
    compileOnly("su.plo.slib:bungee:${rootProject.libs.versions.crosslib.get()}")

    compileOnly(project(":proxy:common"))
    compileOnly(rootProject.libs.netty)

    // shadow projects
    listOf(
        project(":api:common"),
        project(":api:proxy"),
        project(":api:server-common"),
        project(":proxy:common"),
        project(":server-common"),
        project(":common"),
        project(":protocol")
    ).forEach {
        shadow(it) {
            isTransitive = false
        }
    }
    // shadow external deps
    shadow(kotlin("stdlib-jdk8"))
    shadow(rootProject.libs.kotlinx.coroutines)
    shadow(rootProject.libs.kotlinx.coroutines.jdk8)
    shadow(rootProject.libs.kotlinx.json)

    shadow(rootProject.libs.guice) {
        exclude("com.google.guava")
    }

    shadow(rootProject.libs.opus.concentus)
    shadow(rootProject.libs.config)
    shadow(rootProject.libs.crowdin.lib) {
        isTransitive = false
    }
    shadow("org.bstats:bstats-bungeecord:${rootProject.libs.versions.bstats.get()}")
    shadow("su.plo.slib:bungee:${rootProject.libs.versions.crosslib.get()}") {
        isTransitive = false
    }
}

tasks {
    processResources {
        filesMatching("bungee.yml") {
            expand(
                mutableMapOf(
                    "version" to version
                )
            )
        }
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())

        archiveBaseName.set("PlasmoVoice-BungeeCord")
        archiveAppendix.set("")
        archiveClassifier.set("")

        relocate("su.plo.crowdin", "su.plo.voice.libs.crowdin")
        relocate("org.bstats", "su.plo.voice.libs.bstats")

        relocate("org.concentus", "su.plo.voice.libs.concentus")

        relocate("com.google.inject", "su.plo.voice.libs.google.inject")
        relocate("org.aopalliance", "su.plo.voice.libs.aopalliance")
        relocate("javax.inject", "su.plo.voice.libs.javax.inject")

        dependencies {
            exclude(dependency("net.java.dev.jna:jna"))
            exclude(dependency("org.slf4j:slf4j-api"))
            exclude(dependency("org.jetbrains:annotations"))

            exclude("su/plo/opus/*")
            exclude("natives/opus/**/*")

            exclude("DebugProbesKt.bin")
            exclude("META-INF/**")
        }
    }

    build {
        dependsOn.add(shadowJar)

        doLast {
            shadowJar.get().archiveFile.get().asFile
                .copyTo(rootProject.buildDir.resolve("libs/${shadowJar.get().archiveFile.get().asFile.name}"), true)
        }
    }

    jar {
        archiveClassifier.set("dev")
        dependsOn.add(shadowJar)
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    }
}
