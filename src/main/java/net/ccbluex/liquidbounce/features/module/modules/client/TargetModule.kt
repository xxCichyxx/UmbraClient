/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.BoolValue

object TargetModule : Module("Target", Category.CLIENT, defaultInArray = false, gameDetecting = false, hideModule = true, canBeEnabled = false) {
    var playerValue by BoolValue("Player", true)
    var animalValue by BoolValue("Animal", true)
    var mobValue by BoolValue("Mob", true)
    var invisibleValue by BoolValue("Invisible", true)
    var deadValue by BoolValue("Dead", true)

    override fun handleEvents() = true
}