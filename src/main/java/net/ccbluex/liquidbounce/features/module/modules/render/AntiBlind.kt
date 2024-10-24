/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue

object AntiBlind : Module("AntiBlind", Category.RENDER, gameDetecting = false, hideModule = false) {
    init {
        state = true
    }
    val confusionEffect by BoolValue("Confusion", true)
    val pumpkinEffect by BoolValue("Pumpkin", true)
    val fireEffect by FloatValue("FireAlpha", 0.1f, 0f..1f)
    val bossHealth by BoolValue("BossHealth", true)
}