/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.SuperKnockback
import net.ccbluex.liquidbounce.features.module.modules.`fun`.Derp
import net.ccbluex.liquidbounce.features.module.modules.player.scaffolds.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.player.scaffolds.Scaffold.motion2x1
import net.ccbluex.liquidbounce.features.module.modules.player.scaffolds.Scaffold.motion2z1
import net.ccbluex.liquidbounce.features.module.modules.player.scaffolds.Scaffold.motionx1
import net.ccbluex.liquidbounce.features.module.modules.player.scaffolds.Scaffold.motionx2
import net.ccbluex.liquidbounce.features.module.modules.player.scaffolds.Scaffold.motionz1
import net.ccbluex.liquidbounce.features.module.modules.player.scaffolds.Scaffold.motionz2
import net.ccbluex.liquidbounce.features.module.modules.player.scaffolds.Scaffold.sprintMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.rotationData
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.potion.Potion
import net.minecraft.util.MovementInput
import kotlin.math.abs

object Sprint : Module("Sprint", Category.MOVEMENT, gameDetecting = false, hideModule = false) {
    val mode by ListValue("Mode", arrayOf("Legit", "Vanilla"), "Vanilla")

    val onlyOnSprintPress by BoolValue("OnlyOnSprintPress", false)
    val alwaysCorrect by BoolValue("AlwaysCorrectSprint", false)

    val allDirections by BoolValue("AllDirections", true) { mode == "Vanilla" }
    val onlyForward by BoolValue("OnlyForward", false) { mode == "Vanilla" && allDirections }
    val jumpDirections by BoolValue("JumpDirections", false) { mode == "Vanilla" && allDirections }

    private val allDirectionsLimitSpeed by FloatValue("AllDirectionsLimitSpeed", 1f, 0.75f..1f)
    { mode == "Vanilla" && allDirections }
    private val allDirectionsLimitSpeedGround by BoolValue("AllDirectionsLimitSpeedOnlyGround", true)
    { mode == "Vanilla" && allDirections }

    // Nowa opcja: sprint działa tylko na ziemi w trybie allDirections
    val sprintOnGroundOnly by BoolValue("SprintOnGroundOnly", true) { mode == "Vanilla" && allDirections }

    private val blindness by BoolValue("Blindness", true) { mode == "Vanilla" }
    private val usingItem by BoolValue("UsingItem", false) { mode == "Vanilla" }
    private val inventory by BoolValue("Inventory", false) { mode == "Vanilla" }
    private val food by BoolValue("Food", true) { mode == "Vanilla" }

    private val checkServerSide by BoolValue("CheckServerSide", false) { mode == "Vanilla" }
    private val checkServerSideGround by BoolValue("CheckServerSideOnlyGround", false)
    { mode == "Vanilla" && checkServerSide }
    private val noPackets by BoolValue("NoPackets", false) { mode == "Vanilla" }

    private var isSprinting = false
    var tickCounter: Int = 0 // Zmienna do liczenia ticków
    var tickInterval: Int = 80 // Co ile ticków sprint zostanie wyłączony na moment
    var disableSprintForOneTick: Boolean = false // Flaga, żeby wyłączyć sprint na 1 tick

    override val tag
        get() = mode

    fun correctSprintState(movementInput: MovementInput, isUsingItem: Boolean) {
        val player = mc.thePlayer ?: return

        if (SuperKnockback.breakSprint()) {
            player.isSprinting = false
            return
        }

        if ((onlyOnSprintPress || !handleEvents()) && !player.isSprinting && !mc.gameSettings.keyBindSprint.isKeyDown && !SuperKnockback.startSprint() && !isSprinting)
            return

        if (Derp.handleEvents()){
            if (isMoving && player.onGround && Derp.noSprint) {
                player.isSprinting = false
                isSprinting = false
                return
            } else if (isMoving && player.onGround && !Derp.noSprint) {
                return
            }
        }

        if (Scaffold.handleEvents()) {
            when (sprintMode) {
                "Off" -> {
                    player.isSprinting = false
                    isSprinting = false
                    return
                }
                "Vanilla" -> {
                    if (isMoving && player.onGround && Scaffold.sprint) {
                        player.isSprinting = true
                        isSprinting = true
                    } else if (!isMoving || !Scaffold.sprint) {
                        player.isSprinting = false
                        isSprinting = false
                    }
                    return
                }
                "OneTick" -> {
                    tickCounter++;
                    if (tickCounter % tickInterval == 0) {
                        disableSprintForOneTick = true; // Włącz flagę, żeby wyłączyć sprint na 1 tick
                    }

                    // Jeśli flaga disableSprintForOneTick jest ustawiona, wyłącz sprint na 1 tick
                    if (disableSprintForOneTick) {
                        player.isSprinting = false; // Wyłącz sprint
                        isSprinting = false;
                        disableSprintForOneTick = false; // Resetuj flagę po jednym ticku
                    } else if (isMoving && player.onGround && Scaffold.sprint) {
                        player.isSprinting = true; // Włącz sprint
                        isSprinting = true;
                    } else if (!isMoving || !Scaffold.sprint) {
                        player.isSprinting = false; // Jeśli nie spełniasz warunków, sprint jest wyłączony
                        isSprinting = false;
                        tickCounter = 0;
                    }
                    return
                }
                "Packet" -> {
                    if (isMoving && !player.isSprinting && Scaffold.sprint) {
                        sendSprintPacket(true)
                        player.isSprinting = true
                    } else if (!isMoving || !Scaffold.sprint) {
                        sendSprintPacket(false)
                        player.isSprinting = false
                    }
                }
                "Motion" -> {
                    if (isMoving && player.onGround && Scaffold.sprint) {
                        player.isSprinting = true;
                        isSprinting = true;
                        mc.thePlayer.motionX *= motionx1;
                        mc.thePlayer.motionZ *= motionz1;

                    } else if (!isMoving || !Scaffold.sprint) {
                        player.isSprinting = false;
                        isSprinting = false;
                        mc.thePlayer.motionX *= motionx2;
                        mc.thePlayer.motionZ *= motionz2;
                    }
                }
                "MotionPacket" -> {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionX *= motion2x1
                        mc.thePlayer.motionZ *= motion2z1
                    }
                }
                "onGround" -> {
                    if (mc.thePlayer.onGround && Scaffold.sprint) {
                        player.isSprinting = true
                        isSprinting = true
                    } else if (!mc.thePlayer.onGround){
                        player.isSprinting = false
                        isSprinting = false
                    }
                }
            }
        }

        if (handleEvents() || alwaysCorrect) {
            player.isSprinting = !shouldStopSprinting(movementInput, isUsingItem)
            isSprinting = player.isSprinting

            if (player.isSprinting && allDirections && mode != "Legit") {
                if (sprintOnGroundOnly && !player.onGround) {
                    player.isSprinting = false
                    isSprinting = false
                    return
                }

                if (onlyForward && movementInput.moveStrafe != 0f) {
                    player.isSprinting = false
                    isSprinting = false
                    return
                }

                if (!allDirectionsLimitSpeedGround || player.onGround) {
                    player.motionX *= allDirectionsLimitSpeed
                    player.motionZ *= allDirectionsLimitSpeed
                }
            }
        }
    }
    private fun sendSprintPacket(sprinting: Boolean) {
        if (sprinting) {
            mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
        } else {
            mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
        }
    }

    private fun shouldStopSprinting(movementInput: MovementInput, isUsingItem: Boolean): Boolean {
        val player = mc.thePlayer ?: return false

        val isLegitModeActive = mode == "Legit"

        val modifiedForward = if (currentRotation != null && rotationData?.strict == true) {
            player.movementInput.moveForward
        } else {
            movementInput.moveForward
        }

        if (!isMoving) {
            return true
        }

        if (player.isCollidedHorizontally) {
            return true
        }

        if ((blindness || isLegitModeActive) && player.isPotionActive(Potion.blindness) && !player.isSprinting) {
            return true
        }

        if ((food || isLegitModeActive) && !(player.foodStats.foodLevel > 6f || player.capabilities.allowFlying)) {
            return true
        }

        if ((usingItem || isLegitModeActive) && !NoSlow.handleEvents() && isUsingItem) {
            return true
        }

        if ((inventory || isLegitModeActive) && serverOpenInventory) {
            return true
        }

        if (isLegitModeActive) {
            return modifiedForward < 0.8
        }

        if (allDirections) {
            // Sprawdź, czy onlyForward jest aktywne i ogranicz sprint do przodu/tyłu
            if (onlyForward && movementInput.moveStrafe != 0f) {
                return true
            }

            // Sprawdź, czy sprint powinien działać tylko na podłodze
            if (sprintOnGroundOnly && !player.onGround) {
                return true
            }

            return false
        }

        val threshold = if ((!usingItem || NoSlow.handleEvents()) && isUsingItem) 0.2 else 0.8
        val playerForwardInput = player.movementInput.moveForward

        if (!checkServerSide) {
            return if (currentRotation != null) {
                abs(playerForwardInput) < threshold || playerForwardInput < 0 && modifiedForward < threshold
            } else {
                playerForwardInput < threshold
            }
        }

        if (checkServerSideGround && !player.onGround) {
            return currentRotation == null && modifiedForward < threshold
        }

        return modifiedForward < threshold
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mode == "Legit") {
            return
        }
        val packet = event.packet
        if (sprintMode.equals("MotionPacket")) {
            if (packet is C03PacketPlayer) {
                if (mc.thePlayer.onGround && mc.thePlayer.ticksExisted % 2 == 0) {
                    packet.onGround = false
                    packet.y += 0.035
                }
            }
        }
        if (packet !is C0BPacketEntityAction || !noPackets || event.isCancelled) {
            return
        }
        if (packet.action == C0BPacketEntityAction.Action.STOP_SPRINTING || packet.action == C0BPacketEntityAction.Action.START_SPRINTING) {
            event.cancelEvent()
        }
    }
}
