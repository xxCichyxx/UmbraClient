/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.minecraft.init.Blocks.air

object AutoBreak : Module("AutoBreak", Category.PLAYER, subjective = true, gameDetecting = false) {

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.objectMouseOver == null || mc.objectMouseOver.blockPos == null || mc.theWorld == null)
            return

        mc.gameSettings.keyBindAttack.pressed = getBlock(mc.objectMouseOver.blockPos) != air
    }

    override fun onDisable() {
        if (!mc.gameSettings.keyBindAttack.pressed)
            mc.gameSettings.keyBindAttack.pressed = false
    }
}
