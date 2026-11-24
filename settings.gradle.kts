rootProject.name = "surf-seasonal-events"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/releases/") { name = "maven-public" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.api.gradle.settings") version "+"
}

include("surf-seasonal-events-api")
include("surf-seasonal-events-core")
include("surf-seasonal-events-core:surf-seasonal-events-core-client")
include("surf-seasonal-events-paper")
include("surf-seasonal-events-microservice")
