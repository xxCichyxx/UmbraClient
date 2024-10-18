/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.RenderWings
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

object Wings : Module("Wings", Category.CLIENT, hideModule = false) {
    init {
        state = true
    }
    private val onlyThirdPerson by BoolValue("OnlyThirdPerson", true)
    val colorType by ListValue("Color Type", arrayOf("Custom", "Chroma", "None"), "Custom")
    val customRed by IntegerValue("Red", 90, 0.. 255) { colorType == "Custom" }
    val customGreen by IntegerValue("Green", 10, 0.. 255) { colorType == "Custom" }
    val customBlue by IntegerValue("Blue", 200, 0.. 255) { colorType == "Custom" }
    val wingStyle by ListValue("Wing Style", arrayOf("Dragon", "Simple"), "Dragon")

    @EventTarget
    fun onRenderPlayer(event: Render3DEvent) {
        if (onlyThirdPerson && mc.gameSettings.thirdPersonView == 0) return
        val renderWings = RenderWings()
        renderWings.renderWings(event.partialTicks)
    }
}