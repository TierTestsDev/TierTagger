plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
}

val modId = sc.properties["mod.id"]
val modVersion = sc.properties["mod.version"]

version = "$modVersion+${sc.current.version}"
base.archivesName = modId

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

sourceSets.main {
    java.srcDir(rootProject.file("core/src/main/java"))
}

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${sc.properties["deps.fabric_loader"]}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties["deps.fabric_api"]}")
    modCompileOnly("maven.modrinth:modmenu:${sc.properties["deps.modmenu"]}")
}

if (sc.current.parsed >= "1.21.4") {
    fabricApi {
        configureTests {
            createSourceSet = true
            modId = "tiertag-gametest"
            enableGameTests = false
            enableClientGameTests = true
            eula = true
        }
    }
}

loom {
    runConfigs.all {
        preferGradleTask = true
        runDirectory = rootProject.file("run")
    }
}

java {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
}

tasks {
    processResources {
        val props = mapOf(
            "id" to modId,
            "name" to sc.properties["mod.name"],
            "version" to modVersion,
            "minecraft" to sc.properties["mod.mc_compat"],
            "loader" to sc.properties["deps.fabric_loader"],
            "java" to requiredJava.majorVersion,
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
        filesMatching("*.mixins.json") { expand(props) }
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    withType<Jar>().configureEach {
        val licenseName = "LICENSE_" + sc.properties["mod.id"]
        from(rootProject.file("LICENSE")) { rename { licenseName } }
    }

    register<Copy>("collectJar") {
        group = "build"
        description = "Copies the release jar to build/libs/<mod version>/ in the root project."
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
    }
}

publishMods {
    val releases = sc.properties.rawOrNull("mod", "mc_releases")?.asList().orEmpty().map { it.toString() }
    val notes = rootProject.file("CHANGELOG.md").readLines()
        .dropWhile { it != "## $modVersion" }.drop(1)
        .takeWhile { !it.startsWith("## ") }
        .joinToString("\n").trim()

    file = loomx.modJar.flatMap { it.archiveFile }
    displayName = "Tier Tagger $modVersion"
    version = modVersion
    changelog = notes
    type = STABLE
    modLoaders.add("fabric")
    dryRun = providers.environmentVariable("MODRINTH_TOKEN").orNull == null

    modrinth {
        projectId = "bTYmcyQz"
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(releases)
        requires("fabric-api")
        optional("modmenu")
    }
}
