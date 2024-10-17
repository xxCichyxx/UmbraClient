/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.RotationSetEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.extensions.prevRotation
import net.ccbluex.liquidbounce.utils.extensions.rotation
import org.lwjgl.input.Keyboard

object FreeLook : Module("FreeLook", Category.VISUAL, Keyboard.KEY_LMENU) {

    // The module's rotations
    private var currRotation = Rotation.ZERO
    private var prevRotation = currRotation

    // The player's rotations
    private var savedCurrRotation = Rotation.ZERO
    private var savedPrevRotation = Rotation.ZERO

    // Zmienna do przechowywania stanu widoku
    private var originalPerspective: Int = 0 // 0: First Person, 1: Third Person (Front), 2: Third Person (Back)
    private var wasInFirstPerson: Boolean = false

    override fun onEnable() {
        mc.thePlayer?.run {
            originalPerspective = mc.gameSettings.thirdPersonView
            wasInFirstPerson = (originalPerspective == 0)

            mc.gameSettings.thirdPersonView = 1

            currRotation = rotation
            prevRotation = prevRotation
        }
    }
    override fun onDisable() {
        // Przywróć poprzedni stan widoku
        mc.gameSettings.thirdPersonView = if (wasInFirstPerson) 0 else originalPerspective

        // Przywróć oryginalne rotacje
        restoreOriginalRotation()
    }

    @EventTarget
    fun onRotationSet(event: RotationSetEvent) {
        if (mc.gameSettings.thirdPersonView != 0) {
            event.cancelEvent()
        }

        prevRotation = currRotation
        currRotation += Rotation(event.yawDiff, -event.pitchDiff)

        currRotation.withLimitedPitch()
    }

    fun useModifiedRotation() {
        val player = mc.thePlayer ?: return

        savedCurrRotation = player.rotation
        savedPrevRotation = player.prevRotation

        if (!handleEvents())
            return

        player.rotation = currRotation
        player.prevRotation = prevRotation
    }
    fun restoreOriginalRotation() {
        val player = mc.thePlayer ?: return

        if (mc.gameSettings.thirdPersonView == 0) {
            savedCurrRotation = player.rotation
            savedPrevRotation = player.prevRotation
            return
        }

        if (!handleEvents())
            return

        player.rotation = savedCurrRotation
        player.prevRotation = savedPrevRotation
    }
}