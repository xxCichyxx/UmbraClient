/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.features.module.modules.client.HUDModule.guiColor
import net.ccbluex.liquidbounce.utils.APIConnecter
import net.ccbluex.liquidbounce.utils.APIConnecter.donate
import net.ccbluex.liquidbounce.utils.URLComponent
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBloom
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import java.awt.Color

class GuiInfo(private val prevGui: GuiScreen) : GuiScreen() {

    override fun initGui() {
        val yOffset = height / 4 + 20
        val buttonWidth = 200
        val buttonHeight = 20

        val buttons = listOf(
            GuiButton(1, width / 2 - buttonWidth / 2, yOffset + buttonHeight * 1, "Open Website"),
            GuiButton(2, width / 2 - buttonWidth / 2, yOffset + buttonHeight * 2 + 10, "Join Discord Server"),
            GuiButton(3, width / 2 - buttonWidth / 2, yOffset + buttonHeight * 3 + 20, "Client Configuration"),
            GuiButton(4, width / 2 - buttonWidth / 2, yOffset + buttonHeight * 4 + 30, "Donate Now"),
            GuiButton(5, width / 2 - buttonWidth / 2, yOffset + buttonHeight * 5 + 40, "Done")
        )

        buttonList.addAll(buttons)

        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()

        drawBloom(mouseX - 5, mouseY - 5, 10, 10, 16, Color(guiColor))

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(prevGui)
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            1 -> MiscUtils.showURL(URLComponent.WEBSITE)
            2 -> MiscUtils.showURL(APIConnecter.discord)
            3 -> mc.displayGuiScreen(GuiClientConfiguration(this))
            4 -> MiscUtils.showURL(donate)
            5 -> mc.displayGuiScreen(prevGui)
        }
    }
}
