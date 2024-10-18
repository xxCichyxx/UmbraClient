/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue

object Derp : Module("Derp", Category.FUN, subjective = true, hideModule = false) {

    private val headless by BoolValue("Headless", false)
    private val spinny by BoolValue("Spinny", false)
    private val increment by FloatValue("Increment", 1F, 0F..50F) { spinny }
    private val movementMode by ListValue("Mode", arrayOf("None", "Legit", "Silent"), "Legit")
    val noSprint by BoolValue("NoSprint", false)

    private var currentSpin = 0F

    val rotation: Rotation
        get() {
            val rot = Rotation(mc.thePlayer.rotationYaw + nextFloat(-180f, 180f), nextFloat(-90f, 90f))

            if (headless) {
                rot.pitch = 180F
            }

            if (spinny) {
                currentSpin += increment
                rot.yaw = currentSpin
            }

            rot.fixedSensitivity()
            return rot
        }

    // This method handles player movement
    fun onUpdate() {
        if (mc.thePlayer != null) {
            // Sprawdzenie trybu
            when (movementMode) {
                "None" -> {
                    // W trybie "None" nic nie zmieniamy, gracz porusza się normalnie
                    return // Zatrzymujemy dalsze przetwarzanie w tym cyklu
                }
                "Legit" -> {
                    // Legit movement: poruszanie w kierunku, w którym gracz patrzy
                    handleMovementLegit()
                }
                "Silent" -> {
                    // Silent movement: gracz wydaje się stać w miejscu
                    handleMovementSilent()
                }
            }
        }
    }

    private fun handleMovementLegit() {
        val currentYaw = mc.thePlayer.rotationYaw
        val forward = if (mc.gameSettings.keyBindForward.isKeyDown) 1 else 0
        val backward = if (mc.gameSettings.keyBindBack.isKeyDown) 1 else 0
        val strafeLeft = if (mc.gameSettings.keyBindLeft.isKeyDown) 1 else 0
        val strafeRight = if (mc.gameSettings.keyBindRight.isKeyDown) 1 else 0

        // Obliczenie ruchu w zależności od kierunku patrzenia
        val motionX = Math.sin(Math.toRadians(currentYaw.toDouble())) * (forward - backward) +
                Math.cos(Math.toRadians(currentYaw.toDouble())) * (strafeLeft - strafeRight)

        val motionZ = Math.cos(Math.toRadians(currentYaw.toDouble())) * (forward - backward) -
                Math.sin(Math.toRadians(currentYaw.toDouble())) * (strafeLeft - strafeRight)

        mc.thePlayer.motionX = motionX
        mc.thePlayer.motionZ = motionZ

        // Klucz do poruszania się do tyłu, gdy gracz idzie do przodu, a postać jest tyłem
        if (forward > 0 && currentYaw > 90 && currentYaw < 270) {
            // Jeśli gracz idzie do przodu i patrzy tyłem
            mc.gameSettings.keyBindBack.pressed = true // Używamy `pressed`, aby symulować klawisz
        } else {
            mc.gameSettings.keyBindBack.pressed = false // Przywróć normalne sterowanie
        }

        // Resetujemy klawisze strafe, jeśli nie są w użyciu
        if (strafeLeft > 0) {
            mc.gameSettings.keyBindRight.pressed = false
        }
        if (strafeRight > 0) {
            mc.gameSettings.keyBindLeft.pressed = false
        }
    }

    private fun handleMovementSilent() {
        // Silent movement: gracz wydaje się stać w miejscu
        val currentYaw = mc.thePlayer.rotationYaw
        val forward = if (mc.gameSettings.keyBindForward.isKeyDown) 1 else 0
        val backward = if (mc.gameSettings.keyBindBack.isKeyDown) 1 else 0
        val strafeLeft = if (mc.gameSettings.keyBindLeft.isKeyDown) 1 else 0
        val strafeRight = if (mc.gameSettings.keyBindRight.isKeyDown) 1 else 0

        val moveDirection = forward + backward
        val strafeDirection = strafeLeft + strafeRight

        // Zmiana yaw, ale bez zmiany motionX i motionZ
        if (moveDirection != 0 || strafeDirection != 0) {
            val newYaw = currentYaw + (if (forward > 0) 0 else if (backward > 0) 180 else 0) +
                    (if (strafeLeft > 0) -90 else if (strafeRight > 0) 90 else 0)

            mc.thePlayer.rotationYaw = newYaw
        }
    }
}