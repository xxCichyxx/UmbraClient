/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.RotationUtils.coerceBodyPoint
import net.ccbluex.liquidbounce.utils.RotationUtils.isFaced
import net.ccbluex.liquidbounce.utils.RotationUtils.limitAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomClickDelay
import net.ccbluex.liquidbounce.value.*
import net.minecraft.entity.Entity
import java.util.*
import kotlin.math.atan

object LegitAura : Module("LegitAura", Category.GHOST, hideModule = false) {

    // New Reach Mode
    private val reachMode by ListValue("ReachMode", arrayOf("Normal", "Reach", "Legit"), "Normal")

    // CPS - Attack speed
    private val cpsMode by ListValue("CPSMode", arrayOf("DragClick", "ButterFly", "Stabilized", "Itter", "Legit", "Burst", "Random"), "Legit")
    private val maxCPSValue = object : IntegerValue("MaxCPS", 8, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minCPS)

        override fun onChanged(oldValue: Int, newValue: Int) {
            attackDelay = calculateAttackDelay()
        }
    }

    private val maxCPS by maxCPSValue

    private val minCPS: Int by object : IntegerValue("MinCPS", 5, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxCPS)

        override fun onChanged(oldValue: Int, newValue: Int) {
            attackDelay = calculateAttackDelay()
        }
    }


    private val horizontalAim by BoolValue("HorizontalAim", true)
    private val verticalAim by BoolValue("VerticalAim", true)
    private val raycast by BoolValue("Raycast", true)
    private val attackRange by FloatValue("AttackRange", 4.0F, 1F..8F)
    private val scanRange by FloatValue("ScanRange", 4.4F, 1F..8F)
    private val startRotatingSlow by BoolValue("StartRotatingSlow", true) { horizontalAim || verticalAim }
    private val slowDownOnDirectionChange by BoolValue("SlowDownOnDirectionChange", false) { horizontalAim || verticalAim }
    private val useStraightLinePath by BoolValue("UseStraightLinePath", true) { horizontalAim || verticalAim }
    private val turnSpeed by FloatValue("TurnSpeed", 10f, 1F..180F) { horizontalAim || verticalAim }
    private val inViewTurnSpeed by FloatValue("InViewTurnSpeed", 35f, 1f..180f) { horizontalAim || verticalAim }
    private val predictClientMovement by IntegerValue("PredictClientMovement", 2, 0..5)
    private val predictEnemyPosition by FloatValue("PredictEnemyPosition", 1.5f, -1f..2f)
    private val highestBodyPointToTargetValue: ListValue = object : ListValue("HighestBodyPointToTarget",
        arrayOf("Head", "Body", "Feet"),
        "Head"
    ) {
        override fun isSupported() = verticalAim

        override fun onChange(oldValue: String, newValue: String): String {
            val newPoint = RotationUtils.BodyPoint.fromString(newValue)
            val lowestPoint = RotationUtils.BodyPoint.fromString(lowestBodyPointToTarget)
            val coercedPoint = coerceBodyPoint(newPoint, lowestPoint, RotationUtils.BodyPoint.HEAD)
            return coercedPoint.name
        }
    }
    private val highestBodyPointToTarget by highestBodyPointToTargetValue

    private val lowestBodyPointToTargetValue: ListValue = object : ListValue("LowestBodyPointToTarget",
        arrayOf("Head", "Body", "Feet"),
        "Feet"
    ) {
        override fun isSupported() = verticalAim

        override fun onChange(oldValue: String, newValue: String): String {
            val newPoint = RotationUtils.BodyPoint.fromString(newValue)
            val highestPoint = RotationUtils.BodyPoint.fromString(highestBodyPointToTarget)
            val coercedPoint = coerceBodyPoint(newPoint, RotationUtils.BodyPoint.FEET, highestPoint)
            return coercedPoint.name
        }
    }

    private val lowestBodyPointToTarget by lowestBodyPointToTargetValue

    private val maxHorizontalBodySearch: FloatValue = object : FloatValue("MaxHorizontalBodySearch", 1f, 0f..1f) {
        override fun isSupported() = horizontalAim

        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalBodySearch.get())
    }

    private val minHorizontalBodySearch: FloatValue = object : FloatValue("MinHorizontalBodySearch", 0f, 0f..1f) {
        override fun isSupported() = horizontalAim

        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalBodySearch.get())
    }

    private val minRotationDifference by FloatValue("MinRotationDifference", 0f, 0f..2f) { verticalAim || horizontalAim }
    private val lock by BoolValue("Lock", true) { horizontalAim || verticalAim }
    private val onClick by BoolValue("OnClick", false) { horizontalAim || verticalAim }
    private val jitter by BoolValue("Jitter", false)
    private val yawJitterMultiplier by FloatValue("JitterYawMultiplier", 1f, 0.1f..2.5f) {jitter}
    private val pitchJitterMultiplier by FloatValue("JitterPitchMultiplier", 1f, 0.1f..2.5f) {jitter}
    private val center by BoolValue("Center", false)
    private val headLock by BoolValue("Headlock", false) { center && lock }
    private val headLockBlockHeight by FloatValue("HeadBlockHeight", -1f, -2f..0f) { headLock && center && lock }
    private val breakBlocks by BoolValue("BreakBlocks", true)

    private val clickTimer = MSTimer()

    var itterStep: Int = 1 // Krok zwiększenia CPS w trybie Itter
    var itterDelay: Long = 1000 // Opóźnienie między krokami w ms
    var dropAmount: Int = 20 // Liczba kliknięć w jednym dropie
    var dropInterval: Long = 100 // Czas trwania dropu w ms

    // Attack delay
    private var attackDelay = 0
    private var currentCPS = minCPS

    private fun calculateAttackDelay(): Int {
        return when (cpsMode) {
            "DragClick" -> randomClickDelay(20, maxCPS) // 20 kliknięć z dropem
            "ButterFly" -> randomClickDelay(minCPS, maxCPS) // Klikanie Butterfly
            "Stabilized" -> (1000 / ((minCPS + maxCPS) / 2)) // Stabilizowane CPS
            "Itter" -> itterCPS() // Iteracyjne CPS
            "Legit" -> (1200 / (maxCPS + 3)) // Klikanie Legit
            "Burst" -> burstClickDelay(dropAmount, dropInterval)
            "Random" -> randomClickDelay(5, 50) // Nowy tryb: Random clicks
            else -> 1000 / minCPS // Domyślna wartość
        }
    }
    private fun burstClickDelay(dropAmount: Int, dropInterval: Long): Int {
        val delay = 1000 / dropAmount // Opóźnienie na kliknięcie w trybie Burst

        // Wykonanie kliknięć w burst
        for (i in 1..dropAmount) {
            Thread.sleep(dropInterval)
        }

        return delay
    }

    // Nowa metoda dla trybu "Random"
    private fun randomClickDelay(minCPS: Int, maxCPS: Int): Int {
        val randomCPS = (minCPS..maxCPS).random() // Losowa liczba kliknięć na sekundę
        return 1000 / randomCPS // Obliczamy opóźnienie na podstawie losowej CPS
    }

    private fun itterCPS(): Int {
        // Zwiększ CPS o krok (itterStep)
        currentCPS += itterStep
        if (currentCPS > maxCPS) {
            currentCPS = minCPS // Reset do minimalnej CPS, jeśli przekroczono maxCPS
        }

        // Opóźnienie przed kolejnym kliknięciem
        Thread.sleep(itterDelay)

        return 1000 / currentCPS // Oblicz opóźnienie na podstawie aktualnego CPS
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.POST) return

        val thePlayer = mc.thePlayer ?: return
        val theWorld = mc.theWorld ?: return

        // Clicking delay
        if (mc.gameSettings.keyBindAttack.isKeyDown) clickTimer.reset()

        if (onClick && (clickTimer.hasTimePassed(150) || (!mc.gameSettings.keyBindAttack.isKeyDown && AutoClicker.handleEvents()))) return

        val random = Random()

        var shouldReturn = false
        // Search for the best enemy to target
        val entity = theWorld.loadedEntityList.filter {
            isSelected(it, true) &&
                    thePlayer.canEntityBeSeen(it) &&
                    thePlayer.getDistanceToEntityBox(it) <= scanRange &&
                    rotationDifference(it) <= 180 // Use FOV for rotation
        }.minByOrNull { thePlayer.getDistanceToEntityBox(it) } ?: return

        val blockReachDistance = when (reachMode) {
            "Normal" -> attackRange.toDouble()
            "Reach" -> (if (Reach.handleEvents()) Reach.combatReach else 3f).toDouble()
            "Legit" -> 3.0 // Use a more legit reach value for this mode
            else -> 3.0
        }

        // Opóźnienie ataku
        if (clickTimer.hasTimePassed(attackDelay)) {
            if (raycast) {
                if (isFaced(entity, blockReachDistance)) {
                    thePlayer.swingItem()
                    mc.playerController.attackEntity(thePlayer, entity)
                    clickTimer.reset()
                }
            } else {
                // Jeśli Raycast jest wyłączony, wykonuje atak bez względu na to, czy gracz jest naprowadzony
                thePlayer.swingItem()
                mc.playerController.attackEntity(thePlayer, entity)
                clickTimer.reset()
            }
        }

        // Check if the entity is within scan range
        if (thePlayer.getDistanceToEntityBox(entity) > scanRange) return

        Backtrack.runWithNearestTrackedDistance(entity) {
            shouldReturn = !findRotation(entity, random)
        }

        if (shouldReturn) {
            return
        }

        // Jitter
        if (jitter) {
            if (random.nextBoolean()) {
                thePlayer.fixedSensitivityYaw += ((random.nextGaussian() - 0.5f) * yawJitterMultiplier).toFloat()
            }

            if (random.nextBoolean()) {
                thePlayer.fixedSensitivityPitch += ((random.nextGaussian() - 0.5f) * pitchJitterMultiplier).toFloat()
            }
        }
    }

    private fun findRotation(entity: Entity, random: Random): Boolean {
        val player = mc.thePlayer ?: return false
        if (mc.playerController.isHittingBlock && breakBlocks) {
            return true
        }

        val (predictX, predictY, predictZ) = entity.currPos.subtract(entity.prevPos)
            .times(2 + predictEnemyPosition.toDouble())

        val boundingBox = entity.hitBox.offset(predictX, predictY, predictZ)
        val (currPos, oldPos) = player.currPos to player.prevPos

        val simPlayer = SimulatedPlayer.fromClientPlayer(player.movementInput)

        repeat(predictClientMovement + 1) {
            simPlayer.tick()
        }

        player.setPosAndPrevPos(simPlayer.pos)

        val playerRotation = player.rotation

        val destinationRotation = if (center) {
            toRotation(boundingBox.center, true)
        } else {
            searchCenter(boundingBox,
                outborder = false,
                random = false,
                predict = true,
                lookRange = scanRange,
                attackRange = if (Reach.handleEvents()) Reach.combatReach else 3f,
                bodyPoints = listOf(highestBodyPointToTarget, lowestBodyPointToTarget),
                horizontalSearch = minHorizontalBodySearch.get()..maxHorizontalBodySearch.get(),
            )
        }

        if (destinationRotation == null) {
            player.setPosAndPrevPos(currPos, oldPos)
            return false
        }

        // look headLockBlockHeight higher
        if (headLock && center && lock) {
            val distance = player.getDistanceToEntityBox(entity)
            val playerEyeHeight = player.eyeHeight
            val blockHeight = headLockBlockHeight

            // Calculate the pitch offset needed to shift the view one block up
            val pitchOffset = Math.toDegrees(atan((blockHeight + playerEyeHeight) / distance)).toFloat()

            destinationRotation.pitch -= pitchOffset
        }

        // Figure out the best turn speed suitable for the distance and configured turn speed
        val rotationDiff = rotationDifference(playerRotation, destinationRotation)

        // is enemy visible to player on screen. Fov is about to be right with that you can actually see on the screen. Still not 100% accurate, but it is fast check.
        val supposedTurnSpeed = if (rotationDiff < mc.gameSettings.fovSetting) {
            inViewTurnSpeed
        } else {
            turnSpeed
        }

        val gaussian = random.nextGaussian()

        val realisticTurnSpeed = rotationDiff * ((supposedTurnSpeed + (gaussian - 0.5)) / 180)

        val rotation = limitAngleChange(player.rotation,
            destinationRotation,
            realisticTurnSpeed.toFloat(),
            startOffSlow = startRotatingSlow,
            slowOnDirChange = slowDownOnDirectionChange,
            useStraightLinePath = useStraightLinePath,
            minRotationDifference = minRotationDifference
        )

       rotation.toPlayer(player, horizontalAim, verticalAim)

        player.setPosAndPrevPos(currPos, oldPos)

        return true
    }
}