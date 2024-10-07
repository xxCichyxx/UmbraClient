/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.ui.font

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.UmbraClient.CLIENT_CLOUD
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import net.ccbluex.liquidbounce.file.FileManager.fontsDir
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.URLComponent.FONTS
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.download
import net.minecraft.client.gui.FontRenderer
import java.awt.Font
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import kotlin.io.path.inputStream

object Fonts : MinecraftInstance() {

    @FontDetails(fontName = "Minecraft Font")
    val minecraftFont: FontRenderer = mc.fontRendererObj

    @FontDetails(fontName = "Roboto Medium", fontSize = 15)
    lateinit var font15: GameFontRenderer

    @FontDetails(fontName = "Roboto Medium", fontSize = 20)
    lateinit var font20: GameFontRenderer

    @FontDetails(fontName = "Roboto Medium", fontSize = 24)
    lateinit var fontTiny: GameFontRenderer

    @FontDetails(fontName = "Roboto Medium", fontSize = 30)
    lateinit var fontSmall: GameFontRenderer

    @FontDetails(fontName = "Roboto Medium", fontSize = 35)
    lateinit var font35: GameFontRenderer

    @FontDetails(fontName = "Roboto Medium", fontSize = 40)
    lateinit var font40: GameFontRenderer

    @FontDetails(fontName = "Roboto Medium", fontSize = 72)
    lateinit var font72: GameFontRenderer

    @FontDetails(fontName = "Roboto Bold", fontSize = 180)
    lateinit var fontBold180: GameFontRenderer

    @FontDetails(fontName = "SFUI Medium", fontSize = 35)
    lateinit var fontSFUI35: GameFontRenderer

    @FontDetails(fontName = "SFUI Medium", fontSize = 40)
    lateinit var fontSFUI40: GameFontRenderer

    @FontDetails(fontName = "Aqua Icons", fontSize = 35)
    lateinit var fontIcons35: GameFontRenderer

    @FontDetails(fontName = "XD Icons", fontSize = 85)
    lateinit var fontIconXD85: GameFontRenderer

    @FontDetails(fontName = "Novo Angular Icons", fontSize = 85)
    lateinit var fontNovoAngularIcon85: GameFontRenderer

    private val CUSTOM_FONT_RENDERERS = hashMapOf<FontInfo, FontRenderer>()

    fun loadFonts() {
        val l = System.currentTimeMillis()
        LOGGER.info("Loading Fonts.")

        downloadFonts()
        // roboto
        fontBold180 = GameFontRenderer(getFont("Roboto-Bold.ttf", 180))
        fontSmall = GameFontRenderer(getFont("Roboto-Medium.ttf", 30))
        fontTiny = GameFontRenderer(getFont("Roboto-Medium.ttf", 24))
        font15 = GameFontRenderer(getFont("Roboto-Medium.ttf", 15))
        font20 = GameFontRenderer(getFont("Roboto-Medium.ttf", 20))
        font35 = GameFontRenderer(getFont("Roboto-Medium.ttf", 35))
        font40 = GameFontRenderer(getFont("Roboto-Medium.ttf", 40))
        font72 = GameFontRenderer(getFont("Roboto-Medium.ttf", 72))
        // sfui
        fontSFUI35 = GameFontRenderer(getFont("sfui.ttf", 35))
        fontSFUI40 = GameFontRenderer(getFont("sfui.ttf", 40))
        // others
        fontIcons35 = GameFontRenderer(getFont("aquaIcons.ttf", 35))
        fontIconXD85 = GameFontRenderer(getFont("iconxd.ttf", 85))
        fontNovoAngularIcon85 = GameFontRenderer(getFont("novoangular.ttf", 85))

        try {
            CUSTOM_FONT_RENDERERS.clear()
            val fontsFile = File(fontsDir, "fonts.json")
            if (fontsFile.exists()) {
                val jsonElement = JsonParser().parse(fontsFile.bufferedReader())
                if (jsonElement is JsonNull) return
                val jsonArray = jsonElement as JsonArray
                for (element in jsonArray) {
                    if (element is JsonNull) return
                    val fontObject = element as JsonObject
                    val font = getFont(fontObject["fontFile"].asString, fontObject["fontSize"].asInt)
                    CUSTOM_FONT_RENDERERS[FontInfo(font)] = GameFontRenderer(font)
                }
            } else {
                fontsFile.createNewFile()

                fontsFile.writeText(PRETTY_GSON.toJson(JsonArray()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        LOGGER.info("Loaded Fonts. (" + (System.currentTimeMillis() - l) + "ms)")
    }

    private fun downloadFonts() {
        try {
            val outputFile = File(fontsDir, "roboto.zip")
            if (!outputFile.exists()) {
                LOGGER.info("Downloading fonts...")
                download("$CLIENT_CLOUD/fonts/Roboto.zip", outputFile)
                LOGGER.info("Extract fonts...")
                extractZip(outputFile.path, fontsDir.path)
            }
            val fontZipFile = File(fontsDir, "font.zip")
            if (!fontZipFile.exists()) {
                LOGGER.info("Downloading additional fonts...")
                download("${FONTS}/Font.zip", fontZipFile)
                LOGGER.info("Extracting additional fonts...")
                extractZip(fontZipFile.path, fontsDir.path)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getFontRenderer(name: String, size: Int): FontRenderer {
        for (field in Fonts::class.java.declaredFields) {
            try {
                field.isAccessible = true
                val obj = field[null]
                if (obj is FontRenderer) {
                    val fontDetails = field.getAnnotation(FontDetails::class.java)
                    if (fontDetails.fontName == name && fontDetails.fontSize == size) return obj
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        return CUSTOM_FONT_RENDERERS.getOrDefault(FontInfo(name, size), minecraftFont)
    }

    fun getFontDetails(fontRenderer: FontRenderer): FontInfo? {
        for (field in Fonts::class.java.declaredFields) {
            try {
                field.isAccessible = true
                val obj = field[null]
                if (obj == fontRenderer) {
                    val fontDetails = field.getAnnotation(FontDetails::class.java)
                    return FontInfo(fontDetails.fontName, fontDetails.fontSize)
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        for ((key, value) in CUSTOM_FONT_RENDERERS) {
            if (value === fontRenderer) return key
        }
        return null
    }

    val fonts: List<FontRenderer>
        get() {
            val fonts = mutableListOf<FontRenderer>()
            for (fontField in Fonts::class.java.declaredFields) {
                try {
                    fontField.isAccessible = true
                    val fontObj = fontField[null]
                    if (fontObj is FontRenderer) fonts += fontObj
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
            fonts += CUSTOM_FONT_RENDERERS.values
            return fonts
        }

    private fun getFont(fontName: String, size: Int) =
        try {
            val inputStream = File(fontsDir, fontName).inputStream()
            var awtClientFont = Font.createFont(Font.TRUETYPE_FONT, inputStream)
            awtClientFont = awtClientFont.deriveFont(Font.PLAIN, size.toFloat())
            inputStream.close()
            awtClientFont
        } catch (e: Exception) {
            e.printStackTrace()
            Font("default", Font.PLAIN, size)
        }

    private fun extractZip(zipFile: String, outputFolder: String) {
        val buffer = ByteArray(1024)
        try {
            val folder = File(outputFolder)
            if (!folder.exists()) folder.mkdir()
            val zipInputStream = ZipInputStream(Paths.get(zipFile).inputStream())
            var zipEntry = zipInputStream.nextEntry
            while (zipEntry != null) {
                val newFile = File(outputFolder + File.separator + zipEntry.name)
                File(newFile.parent).mkdirs()
                val fileOutputStream = newFile.outputStream()
                var i: Int
                while (zipInputStream.read(buffer).also { i = it } > 0) fileOutputStream.write(buffer, 0, i)
                fileOutputStream.close()
                zipEntry = zipInputStream.nextEntry
            }
            zipInputStream.closeEntry()
            zipInputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    class FontInfo(val name: String?, val fontSize: Int) {

        constructor(font: Font) : this(font.name, font.size)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            val fontInfo = other as FontInfo

            return fontSize == fontInfo.fontSize && name == fontInfo.name
        }

        override fun hashCode(): Int {
            var result = name?.hashCode() ?: 0
            result = 31 * result + fontSize
            return result
        }
    }
}