pluginManagement {
    plugins {
        id("me.modmuss50.mod-publish-plugin") version "2.2.1"
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.10"
    id("dev.kikugie.loom-back-compat") version "0.4.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "tiertagger"

include("core")

stonecutter {
    create(rootProject) {
        fun mc(version: String, vararg loaders: String) {
            for (loader in loaders) version("$version-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        mc("1.21", "fabric")
        mc("1.21.2", "fabric")
        mc("1.21.4", "fabric")
        mc("1.21.5", "fabric")
        mc("1.21.6", "fabric")
        mc("1.21.9", "fabric")
        mc("1.21.11", "fabric")
        mc("26.1", "fabric")
        mc("26.2", "fabric")
        mc("26.3", "fabric")
        vcsVersion = "26.3-fabric"
    }
}
