/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import kotlin.math.sqrt

object OmegaCraft : FlyMode("OmegaCraft") {

    private var field23700 = 0
    private var field23701 = 0
    private var field23702 = 0.0
    private val field23703 = doubleArrayOf(0.0, 0.25, 0.5, 0.75, 1.0)

    override fun onEnable() {
        mc.thePlayer!!.motionX = 0.0
        mc.thePlayer!!.motionZ = 0.0
        mc.thePlayer!!.motionY = 0.0
        field23701 = 0
        field23700 = -1
        updateFlyHeight()
    }

    override fun onDisable() {
        MovementUtils.strafe(0.2f)
        if (mc.thePlayer!!.motionY > 0.03) {
            mc.thePlayer!!.motionY = -0.0784
        }
    }

    @EventTarget
    fun onWorldLoad(event: WorldEvent) {
        updateFlyHeight()
    }

    private fun updateFlyHeight() {
        field23701 = 0
        field23700 = -1
        var closestValue = field23703[0]
        val yOffset = mc.thePlayer!!.posY - mc.thePlayer!!.posY.toInt()

        for (i in 1 until field23703.size) {
            val difference = field23703[i] - yOffset
            if (difference < yOffset - closestValue) {
                closestValue = field23703[i]
            }
        }

        field23702 = mc.thePlayer!!.posY.toInt() + closestValue
        mc.thePlayer!!.setPosition(mc.thePlayer!!.posX, field23702, mc.thePlayer!!.posZ)
    }

    @EventTarget
    override fun onMove(event: MoveEvent) {
        field23700++
        if (field23701 > 0) field23701--

        event.y = 0.0
        if (field23700 != 1) {
            if (field23700 > 1) {
                mc.thePlayer!!.setPosition(mc.thePlayer!!.posX, field23702, mc.thePlayer!!.posZ)
                val currentSpeed = sqrt(mc.thePlayer!!.motionX * mc.thePlayer!!.motionX + mc.thePlayer!!.motionZ * mc.thePlayer!!.motionZ)
                val speed = if (!mc.gameSettings.keyBindSneak.isKeyDown) {
                    (0.405 + currentSpeed * 0.02).toFloat()
                } else {
                    0.25f
                }
                MovementUtils.strafe(speed)
                field23700 = 0
            }
        } else {
            if (mc.gameSettings.keyBindJump.isKeyDown && field23701 == 0) {
                event.y = 0.5
                field23702 += event.y
                field23701 = 3
                field23700 = 0
            }

            val speed = if (!mc.gameSettings.keyBindSneak.isKeyDown) 0.6f else 0.25f
            MovementUtils.strafe(speed)
        }

        // Zatrzymywanie gracza, gdy nie porusza się
        if (!isMoving) {
            event.x = 0.0
            event.z = 0.0
            mc.thePlayer!!.motionX = 0.0
            mc.thePlayer!!.motionZ = 0.0
            mc.thePlayer!!.motionY = 0.0 // Opcjonalnie, możesz również zatrzymać ruch w pionie
        }
    }

    @EventTarget
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            var closestValue = field23703[0]
            val yOffset = packet.y - packet.y.toInt()

            for (i in 1 until field23703.size) {
                val difference = field23703[i] - yOffset
                if (difference < yOffset - closestValue) {
                    closestValue = field23703[i]
                }
            }

            field23702 = packet.y.toInt() + closestValue
        }
    }
}
