/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue

object CustomModel : Module("CustomModel", Category.RENDER, hideModule = false) {
    val mode by ListValue("Mode", arrayOf("Imposter", "Rabbit", "Freddy", "None"), "Imposter")

    val rotatePlayer by  BoolValue("RotatePlayer", false)

    override val tag: String
        get() = mode
}