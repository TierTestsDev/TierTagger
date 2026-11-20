import java.util.Properties

plugins {
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("me.modmuss50.mod-publish-plugin") version "2.2.1"
}

val shared = Properties().apply {
    rootDir.resolve("../stonecutter.properties.toml").readLines()
        .map { it.trim() }
        .filter { it.startsWith("mod.") && "=" in it }
        .forEach { line -> setProperty(line.substringBefore("=").trim(), line.substringAfter("=").trim().trim('"')) }
}
val modId: String = shared.getProperty("mod.id")
val modVersion: String = shared.getProperty("mod.version")

version = "$modVersion+1.8.9"
base.archivesName = "$modId-forge"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(8)
}

sourceSets.main {
    java.srcDir("../core/src/main/java")
    output.setResourcesDir(java.classesDirectory)
}

loom {
    runConfigs {
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    processResources {
        inputs.property("version", modVersion)
        inputs.property("modid", modId)
        filesMatching("mcmod.info") { expand("version" to modVersion, "modid" to modId) }
    }

    jar {
        from(rootDir.resolve("../LICENSE")) { rename { "LICENSE_$modId" } }
        destinationDirectory = layout.buildDirectory.dir("devlibs")
    }
}

publishMods {
    val notes = rootDir.resolve("../CHANGELOG.md").readLines()
        .dropWhile { it != "## $modVersion" }.drop(1)
        .takeWhile { !it.startsWith("## ") }
        .joinToString("\n").trim()

    file = tasks.remapJar.flatMap { it.archiveFile }
    displayName = "Tier Tagger $modVersion"
    version = modVersion
    changelog = notes
    type = STABLE
    modLoaders.add("forge")
    dryRun = providers.environmentVariable("MODRINTH_TOKEN").orNull == null

    modrinth {
        projectId = "bTYmcyQz"
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add("1.8.9")
    }
}
