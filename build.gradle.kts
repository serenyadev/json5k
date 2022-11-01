import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("org.jetbrains.kotlinx.kover") version "0.6.1"
    id("org.jetbrains.dokka") version "1.7.20"
    `maven-publish`
    `java-library`
    signing
}

group = "io.github.xn32"
version = "0.2.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.4.1")
}

tasks.test {
    useJUnitPlatform()
}

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("json5k")
                description.set("JSON5 library for Kotlin")
                url.set("https://github.com/xn32/json5k")

                scm {
                    url.set("https://github.com/xn32/json5k")
                    connection.set("scm:git:git://github.com/xn32/json5k.git")
                    developerConnection.set("scm:git:ssh://git@github.com/xn32/json5k.git")
                }

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            url = if (isReleaseVersion) {
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            } else {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            }

            credentials {
                username = sonatypeUsername ?: ""
                password = sonatypePassword ?: ""
            }
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

signing {
    isRequired = isReleaseVersion && gradle.taskGraph.hasTask("publish")
    sign(publishing.publications["mavenJava"])
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

tasks.withType<DokkaTask>() {
    val githubRepo = "https://github.com/xn32/json5k"
    val footerMsg = "<a href='$githubRepo'>json5k on GitHub</a>"

    dokkaSourceSets {
        configureEach {
            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/kotlinx.serialization/"))
            }

            includes.from("dokka/index.md")

            sourceLink {
                val gitVersion = if (isReleaseVersion) {
                    "v$version"
                } else {
                    "main"
                }

                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("$githubRepo/blob/$gitVersion/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
        }
    }

    outputDirectory.set(buildDir.resolve("dokka"))
    suppressInheritedMembers.set(true)

    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """
                {
                    "footerMessage": "$footerMsg",
                    "customStyleSheets": [ "${file("dokka/custom.css")}" ]
                }
            """.trimIndent()
        )
    )
}
