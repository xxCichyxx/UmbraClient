/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customoldAirStrafe
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customoldAirTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customoldAirTimerTick
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customoldGroundStrafe
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customoldGroundTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customoldY
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.notOnConsuming
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.notOnFalling
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.notOnVoid
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.stopY
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.minecraft.item.ItemBucketMilk
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion

object CustomOld : SpeedMode("CustomOld") {

    override fun onMotion() {
        val player = mc.thePlayer ?: return
        val heldItem = player.heldItem

        val fallingPlayer = FallingPlayer()
        if (notOnVoid && fallingPlayer.findCollision(500) == null
            || notOnFalling && player.fallDistance > 2.5f
            || notOnConsuming && player.isUsingItem
            && (heldItem.item is ItemFood
                    || heldItem.item is ItemPotion
                    || heldItem.item is ItemBucketMilk)
        ) {

            if (player.onGround) player.tryJump()
            mc.timer.timerSpeed = 1f
            return
        }

        if (isMoving) {
            if (player.onGround) {
                if (customoldGroundStrafe > 0) {
                    strafe(customoldGroundStrafe)
                }

                mc.timer.timerSpeed = customoldGroundTimer
                player.motionY = customoldY.toDouble()
            } else {
                if (customoldAirStrafe > 0) {
                    strafe(customoldAirStrafe)
                }

                if (player.ticksExisted % customoldAirTimerTick == 0) {
                    mc.timer.timerSpeed = customoldAirTimer
                } else {
                    mc.timer.timerSpeed = 1f
                }
            }
        }
    }

    override fun onEnable() {
        val player = mc.thePlayer ?: return

        if (Speed.resetXZ) player.stopXZ()
        if (Speed.resetY) player.stopY()

        super.onEnable()
    }
}