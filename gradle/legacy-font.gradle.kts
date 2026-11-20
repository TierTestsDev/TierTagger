import groovy.json.JsonSlurper
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.zip.ZipFile
import javax.imageio.ImageIO

// 1.8.9 fonts cannot load bitmap glyphs, so the gamemode icons from the modern icon font are packed into
// HD unicode font pages and given a width in glyph_sizes.bin, patched from the game's own copy at build time.
// Font mods such as Argentum read glyph_sizes.bin themselves, which is why the file has to ship.

val iconFont = rootDir.resolve("../src/main/resources/assets/tiertag/font/icons.json")
val textures = rootDir.resolve("../src/main/resources/assets/tiertag/textures")
val output = layout.buildDirectory.dir("generated/legacyFont")
val gameJars = configurations.getByName("compileClasspath")
    .filter { it.name.endsWith(".jar") && ("minecraft" in it.name || "forge" in it.name) }

val legacyFont = tasks.register("legacyFont") {
    inputs.file(iconFont)
    inputs.dir(textures)
    inputs.files(gameJars)
    outputs.dir(output)
    doLast {
        val sizes = gameJars.files.firstNotNullOf { jar ->
            ZipFile(jar).use { zip ->
                zip.getEntry("assets/minecraft/font/glyph_sizes.bin")?.let { zip.getInputStream(it).readBytes() }
            }
        }
        val cell = 32
        val pages = sortedMapOf<Int, BufferedImage>()
        @Suppress("UNCHECKED_CAST")
        val providers = (JsonSlurper().parse(iconFont) as Map<String, Any>)["providers"] as List<Map<String, Any>>
        for (provider in providers.filter { it["height"] == 8 }) {
            val code = (provider["chars"] as List<*>)[0].toString()[0].code
            sizes[code] = 0x0F
            val icon = ImageIO.read(textures.resolve(provider["file"].toString().substringAfter(':')))
            val page = pages.getOrPut(code shr 8) { BufferedImage(16 * cell, 16 * cell, BufferedImage.TYPE_INT_ARGB) }
            val graphics = page.createGraphics()
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            graphics.drawImage(icon, (code and 0xF) * cell, (code shr 4 and 0xF) * cell, cell, cell, null)
            graphics.dispose()
        }

        val root = output.get().asFile
        root.deleteRecursively()
        root.resolve("assets/minecraft/font").apply { mkdirs() }.resolve("glyph_sizes.bin").writeBytes(sizes)
        val pageDir = root.resolve("assets/minecraft/textures/font").apply { mkdirs() }
        pages.forEach { (page, image) -> ImageIO.write(image, "png", pageDir.resolve("unicode_page_%02x.png".format(page))) }
    }
}

the<SourceSetContainer>()["main"].resources.srcDir(legacyFont)
