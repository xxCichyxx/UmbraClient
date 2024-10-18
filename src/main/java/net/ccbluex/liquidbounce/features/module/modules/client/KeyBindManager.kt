/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.ui.client.keybind.KeyBindManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiSelectWorld
import org.lwjgl.input.Keyboard

object KeyBindManager : Module("KeyBindManager", Category.CLIENT, Keyboard.KEY_RMENU, canBeEnabled = false) {
    override fun onEnable() {
        mc.displayGuiScreen(KeyBindManager)
    }
}