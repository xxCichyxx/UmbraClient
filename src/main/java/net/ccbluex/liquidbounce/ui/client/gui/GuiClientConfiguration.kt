/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.UmbraClient.clientTitle
import net.ccbluex.liquidbounce.UmbraClient.background
import net.ccbluex.liquidbounce.features.module.modules.client.HUDModule.guiColor
import net.ccbluex.liquidbounce.file.FileManager.backgroundImageFile
import net.ccbluex.liquidbounce.file.FileManager.backgroundShaderFile
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.handler.lang.LanguageManager
import net.ccbluex.liquidbounce.handler.lang.translationMenu
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.Background
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.IconUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBloom
import net.ccbluex.liquidbounce.utils.render.shader.shaders.BackgroundShader
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.client.config.GuiSlider
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.Display
import java.awt.Color
import java.nio.file.Files

class GuiClientConfiguration(val prevGui: GuiScreen) : GuiScreen() {

    companion object {
        var enabledClientTitle = true
        var enabledCustomBackground = true
        var particles = true
        var stylisedAlts = true
        var unformattedAlts = false
        var altsLength = 16

        fun updateClientWindow() {
            if (enabledClientTitle) {
                // Set LiquidBounce title
                Display.setTitle(clientTitle)
                // Update favicon
                IconUtils.getFavicon()?.let { icons ->
                    Display.setIcon(icons)
                }
            } else {
                // Set original title
                Display.setTitle("Minecraft 1.8.9")
                // Update favicon
                mc.setWindowIcon()
            }
        }

    }

    private lateinit var languageButton: GuiButton

    private lateinit var backgroundButton: GuiButton
    private lateinit var backgroundGlowButton: GuiButton
    private lateinit var particlesButton: GuiButton
    private lateinit var altsModeButton: GuiButton
    private lateinit var unformattedAltsButton: GuiButton
    private lateinit var altsSlider: GuiSlider

    private lateinit var titleButton: GuiButton

    override fun initGui() {
        buttonList.run {
            clear()

            // Title button
            // Location > 1st row
            add(GuiButton(5, width / 2 - 100, height / 4 + 25, "Client title (${if (enabledClientTitle) "On" else "Off"})").also { titleButton = it })
            add(GuiButton(8, width / 2 - 100, height / 4 + 50, "Language (${LanguageManager.overrideLanguage.ifBlank { "Game" }})").also { languageButton = it })

            // Background configuration buttons
            // Button location > 2nd row
            add(GuiButton(0, width / 2 - 100, height / 4 + 25 + 75, "Enabled (${if (enabledCustomBackground) "On" else "Off"})").also { backgroundButton = it })
            add(GuiButton(1, width / 2 - 100, height / 4 + 25 + 75 + 25, "Glows (${if (BackgroundShader.glowOutline) "On" else "Off"})").also { backgroundGlowButton = it })
            add(GuiButton(2, width / 2 - 100, height / 4 + 25 + 75 + 50, "Particles (${if (particles) "On" else "Off"})").also { particlesButton = it })
            add(GuiButton(3, width / 2 - 100, height / 4 + 25 + 75 + 38 * 2, 98, 20, "Change wallpaper"))
            add(GuiButton(4, width / 2 + 2, height / 4 + 25 + 75 + 38 * 2, 98, 20, "Reset wallpaper"))

            // AltManager configuration buttons
            // Location > 3rd row
            add(GuiButton(7, width / 2 - 100, height / 4 + 50 + 185, "Random alts mode (${if (stylisedAlts) "Stylised" else "Legacy"})").also { altsModeButton = it })
            add(GuiSlider(-1, width / 2 - 100, height / 4 + 210 + 50, 200, 20, "${if (stylisedAlts && unformattedAlts) "Random alt max" else "Random alt"} length (", ")", 6.0, 16.0, altsLength.toDouble(), false, true) {
                altsLength = it.valueInt
            }.also { altsSlider = it })
            add(GuiButton(6, width / 2 - 100, height / 4 + 235 + 50, "Unformatted alt names (${if (unformattedAlts) "On" else "Off"})").also {
                it.enabled = stylisedAlts
                unformattedAltsButton = it
            })

            // Back button
            add(GuiButton(9, width / 2 - 100, height / 4 + 25 + 28 * 11, "Back"))
        }
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> {
                enabledCustomBackground = !enabledCustomBackground
                backgroundButton.displayString = "Enabled (${if (enabledCustomBackground) "On" else "Off"})"
            }
            1 -> {
                BackgroundShader.glowOutline = !BackgroundShader.glowOutline
                backgroundGlowButton.displayString = "Glows (${if (BackgroundShader.glowOutline) "On" else "Off"})"
            }
            2 -> {
                particles = !particles
                particlesButton.displayString = "Particles (${if (particles) "On" else "Off"})"
            }
            5 -> {
                enabledClientTitle = !enabledClientTitle
                titleButton.displayString = "Client title (${if (enabledClientTitle) "On" else "Off"})"
                updateClientWindow()
            }
            6 -> {
                unformattedAlts = !unformattedAlts
                unformattedAltsButton.displayString = "Unformatted alt names (${if (unformattedAlts) "On" else "Off"})"
                altsSlider.dispString = "${if (unformattedAlts) "Max random alt" else "Random alt"} length ("
                altsSlider.updateSlider()
            }
            7 -> {
                stylisedAlts = !stylisedAlts
                altsModeButton.displayString = "Random alts mode (${if (stylisedAlts) "Stylised" else "Legacy"})"
                altsSlider.dispString = "${if (stylisedAlts && unformattedAlts) "Max random alt" else "Random alt"} length ("
                altsSlider.updateSlider()
                unformattedAltsButton.enabled = stylisedAlts
            }
            3 -> {
                val file = MiscUtils.openFileChooser() ?: return

                if (file.isDirectory)
                    return

                // Delete old files
                background = null
                if (backgroundImageFile.exists()) backgroundImageFile.deleteRecursively()
                if (backgroundShaderFile.exists()) backgroundShaderFile.deleteRecursively()

                // Copy new file
                val fileExtension = file.extension

                try {
                    val destFile =  when (fileExtension) {
                        "png" -> backgroundImageFile
                        "frag", "glsl", "shader" -> backgroundShaderFile
                        else -> {
                            MiscUtils.showErrorPopup("Error", "Invalid file extension: $fileExtension")
                            return
                        }
                    }

                    Files.copy(file.toPath(), destFile.outputStream())

                    // Load new background
                    try {
                        background = Background.createBackground(destFile)
                    } catch (e: IllegalArgumentException) {
                        background = null
                        if (backgroundImageFile.exists()) backgroundImageFile.deleteRecursively()
                        if (backgroundShaderFile.exists()) backgroundShaderFile.deleteRecursively()

                        MiscUtils.showErrorPopup("Error", "Invalid file extension: $fileExtension")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    MiscUtils.showErrorPopup("Error", "Exception class: " + e.javaClass.name + "\nMessage: " + e.message)

                    background = null
                    if (backgroundImageFile.exists()) backgroundImageFile.deleteRecursively()
                    if (backgroundShaderFile.exists()) backgroundShaderFile.deleteRecursively()
                }
            }
            4 -> {
                background = null
                if (backgroundImageFile.exists()) backgroundImageFile.deleteRecursively()
                if (backgroundShaderFile.exists()) backgroundShaderFile.deleteRecursively()
            }
            8 -> {
                val languageIndex = LanguageManager.knownLanguages.indexOf(LanguageManager.overrideLanguage)

                // If the language is not found, set it to the first language
                if (languageIndex == -1) {
                    LanguageManager.overrideLanguage = LanguageManager.knownLanguages.first()
                } else {
                    // If the language is the last one, set it to blank
                    if (languageIndex == LanguageManager.knownLanguages.size - 1) {
                        LanguageManager.overrideLanguage = ""
                    } else {
                        // Otherwise, set it to the next language
                        LanguageManager.overrideLanguage = LanguageManager.knownLanguages[languageIndex + 1]
                    }
                }

                initGui()
            }
            9 -> mc.displayGuiScreen(prevGui)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        Fonts.fontBold180.drawCenteredString(
            translationMenu("configuration"), width / 2F, height / 8F + 5F,
            4673984, true)

        Fonts.font40.drawString("Window", width / 2F - 98F, height / 4F + 15F,
            0xFFFFFF, true)

        Fonts.font40.drawString("Background", width / 2F - 98F, height / 4F + 90F,
            0xFFFFFF, true)
        Fonts.font35.drawString("Supported background types: (.png, .frag, .glsl)", width / 2F - 98F, height / 4F + 125 + 25 * 3,
            0xFFFFFF, true)

        Fonts.font40.drawString(translationMenu("altManager"), width / 2F - 98F, height / 4F + 225F,
            0xFFFFFF, true)

        drawBloom(mouseX - 5, mouseY - 5, 10, 10, 16, Color(guiColor))

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui)
            return
        }

        super.keyTyped(typedChar, keyCode)
    }

    override fun onGuiClosed() {
        saveConfig(valuesConfig)
        super.onGuiClosed()
    }
}