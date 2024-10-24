/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.silentAddBaseSpeedValue
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.silentBaseSpeedValue
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.silentPacket
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.silentPacketReset
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.silentPacketValue
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.silentlowtimer
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.silentlowtimervalue
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.silenttimer
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.silenttimervalue
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.minecraft.network.play.client.C03PacketPlayer

object SilentTimer : SpeedMode("SilentTimer") {

    private var lastPacketTime = 0L // Timestamp for packet sending
    private var currentSpeed = 0.0 // Current speed adjustment

    override fun onUpdate() {
        if (isMoving) {
            if (silenttimer) {
                mc.timer.timerSpeed = silenttimervalue
            }
            currentSpeed = (silentBaseSpeedValue + silentAddBaseSpeedValue).toDouble()

            if (silentlowtimer) {
                currentSpeed *= (1.0 - silentlowtimervalue)
            }

            // Apply movement speed to the player
            mc.thePlayer.motionX *= currentSpeed
            mc.thePlayer.motionZ *= currentSpeed

            // Apply gravity effect (pulling the player down)
            mc.thePlayer.motionY = -0.1 // Adjust this value as needed for the desired effect

            // Handle packet sending based on the specified interval
            if (silentPacket && System.currentTimeMillis() - lastPacketTime >= silentPacketReset) {
                sendPackets()
                lastPacketTime = System.currentTimeMillis()
            }
        }
    }

    private fun sendPackets() {
        for (i in 0 until silentPacketValue) {
            mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer(mc.thePlayer.onGround))
        }
    }

    override fun onEnable() {
        lastPacketTime = 0L // Reset packet time when enabling
        currentSpeed = 0.0  // Reset current speed
    }

    override fun onDisable() {
        // Reset motion to normal speed when disabling
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        mc.thePlayer.motionY = 0.0 // Reset vertical motion
    }
}
