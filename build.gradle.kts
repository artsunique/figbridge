plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "com.artsunique.figbridge"
version = "1.0.1"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1")
    }

    // HTTP client for Figma API
    implementation("io.ktor:ktor-client-core:3.1.1")
    implementation("io.ktor:ktor-client-cio:3.1.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.1")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines (Swing dispatcher for UI updates)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("junit:junit:4.13.2")
    testImplementation("io.ktor:ktor-client-mock:3.1.1")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.artsunique.figbridge"
        name = "FigBridge"
        version = project.version.toString()
        description = "Generate Tailwind CSS or Custom CSS/HTML code from Figma designs"

        ideaVersion {
            sinceBuild = "251"
            untilBuild = "261.*"
        }

        vendor {
            name = "arts-unique"
            url = "https://arts-unique.com"
        }
    }

    buildSearchableOptions = false

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
