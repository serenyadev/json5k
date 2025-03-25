import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("multiplatform") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    id("org.jetbrains.kotlinx.kover") version "0.7.1"
    id("org.jetbrains.dokka") version "1.8.20"
    `maven-publish`
    signing
}

group = "io.github.xn32"
version = "0.5.2"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }

        testRuns.configureEach {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }

    js(IR) {
        nodejs()
    }

    mingwX64()
    linuxX64()

    macosX64()
    macosArm64()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("json5k")
            description.set("JSON5 library for Kotlin")
            url.set("https://github.com/xn32/json5k")

            scm {
                url.set("https://github.com/xn32/json5k")
                connection.set("scm:git:git://github.com/xn32/json5k.git")
                developerConnection.set("scm:git:ssh://git@github.com/xn32/json5k.git")
            }

            developers {
                developer {
                    id.set("xn32")
                    url.set("https://github.com/xn32")
                }
            }

            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
        }
    }

    repositories {
        val username = "sapphoCompanyUsername".let { System.getenv(it) ?: findProperty(it) }?.toString()
        val password = "sapphoCompanyPassword".let { System.getenv(it) ?: findProperty(it) }?.toString()
        if (username != null && password != null) {
            maven("https://maven.is-immensely.gay/${if(isReleaseVersion) "releases" else "nightly"}") {
                name = "sapphoCompany"
                credentials {
                    this.username = username
                    this.password = password
                }
            }
        } else {
            println("Sappho Company credentials not present.")
        }
    }
}

//signing {
//    isRequired = isReleaseVersion && gradle.taskGraph.hasTask("publish")
//    val signingKey: String? by project
//    val signingPassword: String? by project
//    useInMemoryPgpKeys(signingKey, signingPassword)
//    sign(publishing.publications)
//}

tasks.withType<DokkaTask>().configureEach {
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

                localDirectory.set(file("src"))
                remoteUrl.set(URL("$githubRepo/blob/$gitVersion/src"))
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
