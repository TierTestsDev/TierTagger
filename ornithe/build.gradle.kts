plugins {
    id("fabric-loom") version "1.18-SNAPSHOT"
    id("ploceus") version "1.18-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "2.2.1"
}

val shared = rootDir.resolve("../stonecutter.properties.toml").readLines()
    .map { it.trim() }
    .filter { it.startsWith("mod.") && "=" in it }
    .associate { it.substringBefore("=").trim() to it.substringAfter("=").trim().trim('"') }
val modId = shared.getValue("mod.id")
val modVersion = shared.getValue("mod.version")
val minecraftVersion = property("minecraft_version").toString()
val oslVersion = property("osl_version").toString()

version = "$modVersion+$minecraftVersion"
base.archivesName = "$modId-ornithe"

sourceSets.main {
    java.srcDir("../core/src/main/java")
}

val oneconfig: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
}

configurations.named("oneconfigCompileClasspath") {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
}

apply(from = rootDir.resolve("../gradle/legacy-font.gradle.kts"))

repositories {
    maven("https://repo.polyfrost.org/releases") { name = "Polyfrost" }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(ploceus.featherMappings(property("feather_build").toString()))
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    ploceus.dependOsl(oslVersion)


    // OneConfig needs Java 21, so its page is compiled on its own and only loaded when OneConfig is installed.
    "oneconfigCompileOnly"("org.polyfrost.oneconfig:config:${property("oneconfig_version")}") { isTransitive = false }
    "oneconfigCompileOnly"("org.polyfrost.oneconfig:config-impl:${property("oneconfig_version")}") { isTransitive = false }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 8
    }

    processResources {
        val props = mapOf("version" to modVersion, "id" to modId)
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
    }

    named<JavaCompile>("compileOneconfigJava") {
        options.release = 21
    }

    jar {
        from(oneconfig.output)
        val licenseName = "LICENSE_$modId"
        from(rootDir.resolve("../LICENSE")) { rename { licenseName } }
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
    modLoaders.add("ornithe")
    dryRun = providers.environmentVariable("MODRINTH_TOKEN").orNull == null

    modrinth {
        projectId = "bTYmcyQz"
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add(minecraftVersion)
        requires("osl")
    }
}
