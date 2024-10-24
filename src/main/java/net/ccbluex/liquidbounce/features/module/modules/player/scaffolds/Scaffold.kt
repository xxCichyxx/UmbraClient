/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.player.scaffolds

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.MovementUtils.isMovingForward
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timing.DelayTimer
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.ccbluex.liquidbounce.utils.timing.TimeUtils
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockBush
import net.minecraft.block.state.IBlockState
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks.air
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.util.*
import net.minecraft.world.WorldSettings
import net.minecraftforge.event.ForgeEventFactory
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import javax.vecmath.Color3f
import kotlin.math.*

object Scaffold : Module("Scaffold", Category.PLAYER, Keyboard.KEY_X, hideModule = false) {

    /**
     * TOWER MODES & SETTINGS
     */

    // -->

    private val towerMode by Tower.towerModeValues
    private val stopWhenBlockAbove by Tower.stopWhenBlockAboveValues
    private val onJump by Tower.onJumpValues
    private val notOnMove by Tower.notOnMoveValues
    private val jumpMotion by Tower.jumpMotionValues
    private val jumpDelay by Tower.jumpDelayValues
    private val constantMotion by Tower.constantMotionValues
    private val constantMotionJumpGround by Tower.constantMotionJumpGroundValues
    private val constantMotionJumpPacket by Tower.constantMotionJumpPacketValues
    private val triggerMotion by Tower.triggerMotionValues
    private val dragMotion by Tower.dragMotionValues
    private val teleportHeight by Tower.teleportHeightValues
    private val teleportDelay by Tower.teleportDelayValues
    private val teleportGround by Tower.teleportGroundValues
    private val teleportNoMotion by Tower.teleportNoMotionValues

    // <--

    /**
     * SCAFFOLD MODES & SETTINGS
     */

    // -->

    val scaffoldMode by ListValue(
        "ScaffoldMode",
        arrayOf("Normal", "Rewinside", "Expand", "Telly", "GodBridge"),
        "Normal"
    )

    // Expand
    private val omniDirectionalExpand by BoolValue("OmniDirectionalExpand", false) { scaffoldMode == "Expand" }
    private val expandLength by IntegerValue("ExpandLength", 1, 1..6) { scaffoldMode == "Expand" }

    // Placeable delay
    private val placeDelayValue = BoolValue("PlaceDelay", true) { scaffoldMode != "GodBridge" }
    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 0, 0..1000) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minDelay)
        override fun isSupported() = placeDelayValue.isActive()
    }
    private val maxDelay by maxDelayValue

    private val minDelayValue = object : IntegerValue("MinDelay", 0, 0..1000) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxDelay)
        override fun isSupported() = placeDelayValue.isActive() && !maxDelayValue.isMinimal()
    }
    private val minDelay by minDelayValue

    // Extra clicks
    private val extraClicks by BoolValue("DoExtraClicks", false)
    private val simulateDoubleClicking by BoolValue("SimulateDoubleClicking", false) { extraClicks }
    private val extraClickMaxCPSValue: IntegerValue = object : IntegerValue("ExtraClickMaxCPS", 7, 0..50) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(extraClickMinCPS)
        override fun isSupported() = extraClicks
    }
    private val extraClickMaxCPS by extraClickMaxCPSValue

    private val extraClickMinCPS by object : IntegerValue("ExtraClickMinCPS", 3, 0..50) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(extraClickMaxCPS)
        override fun isSupported() = extraClicks && !extraClickMaxCPSValue.isMinimal()
    }

    private val placementAttempt by ListValue(
        "PlacementAttempt",
        arrayOf("Fail", "Independent"),
        "Fail"
    ) { extraClicks }

    // Autoblock
    private val autoBlock by ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
    private val sortByHighestAmount by BoolValue("SortByHighestAmount", false) { autoBlock != "Off" }
    private val earlySwitch by BoolValue("EarlySwitch", false) { autoBlock != "Off" && !sortByHighestAmount }
    private val amountBeforeSwitch by IntegerValue("SlotAmountBeforeSwitch",
        3,
        1..10
    ) { earlySwitch && !sortByHighestAmount }

    // Settings
    private val autoF5 by BoolValue("AutoF5", false, subjective = true)

    // Basic stuff
    private val autojump0 by BoolValue("Jump", false)
    private val automaticjumps by IntegerValue("JumpTicks", 1, 1..450) { autojump0 }
    val sprint by BoolValue("Sprint", false)
    val sprintMode by ListValue("SprintMode", arrayOf("Legit","Vanilla","Packet","Motion","MotionPacket","onGround","OneTick"), "Legit") { sprint }
    val motionx1 by FloatValue("motionX1", 0.94f, 0.70f..1.25f) { sprintMode == "Motion" }
    val motionz1 by FloatValue("motionZ1", 0.94f, 0.70f..1.25f) { sprintMode == "Motion" }
    val motionx2 by FloatValue("motionX2", 0.99f, 0.70f..1.25f) { sprintMode == "Motion" }
    val motionz2 by FloatValue("motionZ2", 0.99f, 0.70f..1.25f) { sprintMode == "Motion" }
    val motion2x1 by FloatValue("motionX", 0.92f, 0.70f..1.25f) { sprintMode == "MotionPacket" }
    val motion2z1 by FloatValue("motionZ", 0.92f, 0.70f..1.25f) { sprintMode == "MotionPacket" }
    private val swing by BoolValue("Swing", true, subjective = true)
    private val down by BoolValue("Down", true) { scaffoldMode !in arrayOf("GodBridge", "Telly") }

    private val ticksUntilRotation: IntegerValue = object : IntegerValue("TicksUntilRotation", 3, 1..5) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceIn(minimum, maximum)
    }

    // GodBridge mode sub-values
    private val waitForRots by BoolValue("WaitForRotations", false) { isGodBridgeEnabled }
    private val useOptimizedPitch by BoolValue("UseOptimizedPitch", false) { isGodBridgeEnabled }
    private val customGodPitch by FloatValue("GodBridgePitch",
        73.5f,
        0f..90f
    ) { isGodBridgeEnabled && !useOptimizedPitch }

    val jumpAutomatically by BoolValue("JumpAutomatically", true) { scaffoldMode == "GodBridge" }
    private val maxBlocksToJump: IntegerValue = object : IntegerValue("MaxBlocksToJump", 4, 1..8) {
        override fun isSupported() = scaffoldMode == "GodBridge" && !jumpAutomatically
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minBlocksToJump.get())
    }

    private val minBlocksToJump: IntegerValue = object : IntegerValue("MinBlocksToJump", 4, 1..8) {
        override fun isSupported() =
            scaffoldMode == "GodBridge" && !jumpAutomatically && !maxBlocksToJump.isMinimal()

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxBlocksToJump.get())
    }

    // Telly mode subvalues
    private val startHorizontally by BoolValue("StartHorizontally", true) { scaffoldMode == "Telly" }
    private val maxHorizontalPlacements: IntegerValue = object : IntegerValue("MaxHorizontalPlacements", 1, 1..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minHorizontalPlacements.get())
    }
    private val minHorizontalPlacements: IntegerValue = object : IntegerValue("MinHorizontalPlacements", 1, 1..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxHorizontalPlacements.get())
    }
    private val maxVerticalPlacements: IntegerValue = object : IntegerValue("MaxVerticalPlacements", 1, 1..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minVerticalPlacements.get())
    }

    private val minVerticalPlacements: IntegerValue = object : IntegerValue("MinVerticalPlacements", 1, 1..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxVerticalPlacements.get())
    }

    private val maxJumpTicks: IntegerValue = object : IntegerValue("MaxJumpTicks", 0, 0..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minJumpTicks.get())
    }
    private val minJumpTicks: IntegerValue = object : IntegerValue("MinJumpTicks", 0, 0..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxJumpTicks.get())
    }

    private val allowClutching by BoolValue("AllowClutching", true) { scaffoldMode !in arrayOf("Telly", "Expand") }
    private val horizontalClutchBlocks: IntegerValue = object : IntegerValue("HorizontalClutchBlocks", 3, 1..5) {
        override fun isSupported() = allowClutching && scaffoldMode !in arrayOf("Telly", "Expand")
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceIn(minimum, maximum)
    }
    private val verticalClutchBlocks: IntegerValue = object : IntegerValue("VerticalClutchBlocks", 2, 1..3) {
        override fun isSupported() = allowClutching && scaffoldMode !in arrayOf("Telly", "Expand")
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceIn(minimum, maximum)
    }

    // Sneak
    private val sneakValue =
        ListValue("Sneak", arrayOf("TicksFixed", "Off"), "Off")
    val sneak by sneakValue
    private val SneakJump by BoolValue("SneakOnlyJump", false) { sneakValue.isSupported() && sneak != "Off" }
    private val blocksToSneakForward by IntegerValue("TicksToSneak", 1, 1..500) { sneakValue.isSupported() && sneak != "Off" }
    private val SneakDistanseForward by IntegerValue("SneakDistanse", 1, 1..200) { sneakValue.isSupported() && sneak != "Off" }
    private val SneakSpeed by FloatValue("SneakSpeed", 0.3f, 0.15f..1.0f) { sneakValue.isSupported() && sneak != "Off" }

    // Eagle
    private val eagleValue =
        ListValue("Eagle", arrayOf("Normal", "Silent", "Off"), "Normal") { scaffoldMode != "GodBridge" }
    val eagle by eagleValue
    private val adjustedSneakSpeed by BoolValue("AdjustedSneakSpeed", true) { eagle == "Silent" }
    private val eagleSpeed by FloatValue("EagleSpeed", 0.3f, 0.3f..1.0f) { eagleValue.isSupported() && eagle != "Off" }
    val eagleSprint by BoolValue("EagleSprint", false) { eagleValue.isSupported() && eagle == "Normal" }
    private val blocksToEagle by IntegerValue("BlocksToEagle", 0, 0..10) { eagleValue.isSupported() && eagle != "Off" }
    private val edgeDistance by FloatValue(
        "EagleEdgeDistance",
        0f,
        0f..0.5f
    ) { eagleValue.isSupported() && eagle != "Off" }

    // Rotation Options
    val rotationMode by ListValue("Rotations", arrayOf("Off", "Normal", "Stabilized", "Offset", "Backward", "Backward2"), "Normal")
    val pitchMode by ListValue("PitchMode", arrayOf("Legit", "Vanilla", "Vanilla2", "Custom"), "Legit")
    val pitchCorrectValue = FloatValue("PitchCorrect", 80.0f, 0.0f.. 90.0f) { pitchMode == "Legit" }
    val pitchCustomValue = FloatValue("PitchStableValue", 90.0f, 0.0f.. 90.0f) { pitchMode == "Custom" }
    val smootherMode by ListValue(
        "SmootherMode",
        arrayOf("Linear", "Relative"),
        "Relative"
    ) { rotationMode != "Off" }
    val silentRotation by BoolValue("SilentRotation", true) { rotationMode != "Off" }
    val simulateShortStop by BoolValue("SimulateShortStop", false) { rotationMode != "Off" }
    val strafe by BoolValue("Strafe", false) { rotationMode != "Off" && silentRotation }
    private val keepRotation by BoolValue("KeepRotation", true) { rotationMode != "Off" && silentRotation }
    private val keepTicks by object : IntegerValue("KeepTicks", 1, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minimum)
        override fun isSupported() = rotationMode != "Off" && scaffoldMode != "Telly" && silentRotation
    }

    // Search options
    val searchMode by ListValue("SearchMode", arrayOf("Area", "Center","FullBlock"), "Area") { scaffoldMode != "GodBridge" }
    private val minDist by FloatValue("MinDist", 0f, 0f..0.2f) { scaffoldMode !in arrayOf("GodBridge", "Telly") }

    // Turn Speed
    val startRotatingSlow by BoolValue("StartRotatingSlow", false) { rotationMode != "Off" }
    val slowDownOnDirectionChange by BoolValue("SlowDownOnDirectionChange", false) { rotationMode != "Off" }
    val useStraightLinePath by BoolValue("UseStraightLinePath", true) { rotationMode != "Off" }
    val maxHorizontalSpeedValue = object : FloatValue("MaxHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalSpeed)
        override fun isSupported() = rotationMode != "Off"
    }
    val maxHorizontalSpeed by maxHorizontalSpeedValue

    val minHorizontalSpeed: Float by object : FloatValue("MinHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalSpeed)
        override fun isSupported() = !maxHorizontalSpeedValue.isMinimal() && rotationMode != "Off"
    }

    val maxVerticalSpeedValue = object : FloatValue("MaxVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minVerticalSpeed)
        override fun isSupported() = rotationMode != "Off"
    }
    val maxVerticalSpeed by maxVerticalSpeedValue

    val minVerticalSpeed: Float by object : FloatValue("MinVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxVerticalSpeed)
        override fun isSupported() = !maxVerticalSpeedValue.isMinimal() && rotationMode != "Off"
    }

    private val angleThresholdUntilReset by FloatValue(
        "AngleThresholdUntilReset",
        5f,
        0.1f..180f
    ) { rotationMode != "Off" && silentRotation }

    val minRotationDifference by FloatValue("MinRotationDifference", 0f, 0f..1f) { rotationMode != "Off" }

    // Zitter
    private val zitterMode by ListValue("Zitter", arrayOf("Off", "Teleport", "Smooth"), "Off")
    private val zitterSpeed by FloatValue("ZitterSpeed", 0.13f, 0.1f..0.3f) { zitterMode == "Teleport" }
    private val zitterStrength by FloatValue("ZitterStrength", 0.05f, 0f..0.2f) { zitterMode == "Teleport" }

    private val maxZitterTicksValue: IntegerValue = object : IntegerValue("MaxZitterTicks", 3, 0..6) {
        override fun isSupported() = zitterMode == "Smooth"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minZitterTicks)
    }
    private val maxZitterTicks by maxZitterTicksValue

    private val minZitterTicksValue: IntegerValue = object : IntegerValue("MinZitterTicks", 2, 0..6) {
        override fun isSupported() = zitterMode == "Smooth" && !maxZitterTicksValue.isMinimal()
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxZitterTicks)
    }
    private val minZitterTicks by minZitterTicksValue

    private val useSneakMidAir by BoolValue("UseSneakMidAir", false) { zitterMode == "Smooth" }

    // Game
    val timer by FloatValue("Timer", 1f, 0.1f..10f)
    private val speedModifier by FloatValue("SpeedModifier", 1f, 0f..2f)
    private val motionModifier by BoolValue("MotionModifier", false)
    private val multiplierGroundForward by FloatValue("MultiplierGroundForward", 5f, 4f..10f) { motionModifier }
    private val multiplierAirForward by FloatValue("MultiplierAirForward", 5f, 4f..10f) { motionModifier }
    private val multiplierGroundStrafe by FloatValue("MultiplierGroundStrafe", 5f, 4f..10f) { motionModifier }
    private val multiplierAirStrafe by FloatValue("MultiplierAirStrafe", 5f, 4f..10f) { motionModifier }
    private val motionMultiplierAirMax by FloatValue("MotionMultiplierAirMax", 1.01f, 0.9f..1.1f) { motionModifier }
    private val motionMultiplierAirMin by FloatValue("MotionMultiplierAirMin", 1f, 0.9f..1.1f) { motionModifier }
    private val motionMultiplierGroundMax by FloatValue("MotionMultiplierGroundMax", 1.13f, 0.9f..1.3f) { motionModifier }
    private val motionMultiplierGroundMin by FloatValue("MotionMultiplierGroundMin", 1.1f, 0.9f..1.3f) { motionModifier }
    private val speedLimiter by BoolValue("SpeedLimiter", false) { !slow }
    private val speedLimit by FloatValue("SpeedLimit", 0.27f, 0.01f..0.3f) { !slow && speedLimiter }
    private val slow by BoolValue("Slow", false)
    private val slowGround by BoolValue("SlowOnlyGround", false) { slow }
    private val slowSpeed by FloatValue("SlowSpeed", 0.98f, 0.2f..1.4f) { slow }

    // Jump Strafe
    private val jumpStrafe by BoolValue("JumpStrafe", false)
    private val maxJumpStraightStrafe: FloatValue = object : FloatValue("MaxStraightStrafe", 0.45f, 0.1f..1f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minJumpStraightStrafe.get())
        override fun isSupported() = jumpStrafe
    }

    private val minJumpStraightStrafe: FloatValue = object : FloatValue("MinStraightStrafe", 0.4f, 0.1f..1f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxJumpStraightStrafe.get())
        override fun isSupported() = jumpStrafe
    }

    private val maxJumpDiagonalStrafe: FloatValue = object : FloatValue("MaxDiagonalStrafe", 0.45f, 0.1f..1f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minJumpDiagonalStrafe.get())
        override fun isSupported() = jumpStrafe
    }

    private val minJumpDiagonalStrafe: FloatValue = object : FloatValue("MinDiagonalStrafe", 0.4f, 0.1f..1f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxJumpDiagonalStrafe.get())
        override fun isSupported() = jumpStrafe
    }

    // Safety
    private val sameY by BoolValue("SameY", false) { scaffoldMode != "GodBridge" }
    private val sameYMode = ListValue("SameYMode", arrayOf("off", "SameY"), "off") { sameY && scaffoldMode != "GodBridge" }
    private val freezeCameraY by BoolValue("FreezeCameraY", false)
    private val jumpOnUserInput by BoolValue("JumpOnUserInput", true) { sameY && scaffoldMode != "GodBridge" }

    private val safeWalkValue = BoolValue("SafeWalk", true) { scaffoldMode != "GodBridge" }
    private val airSafe by BoolValue("AirSafe", false) { safeWalkValue.isActive() }

    // Visuals
    private val mark by BoolValue("Mark", false, subjective = true)
    private val trackCPS by BoolValue("TrackCPS", false, subjective = true)
    private val safetyLines by BoolValue("SafetyLines", false, subjective = true) { isGodBridgeEnabled }

    // Target placement
    var placeRotation: PlaceRotation? = null

    var hasPlacedBlockThisFall = false

    // Launch position
    private var launchY = -999

    val shouldJumpOnInput
        get() = !jumpOnUserInput || !mc.gameSettings.keyBindJump.isKeyDown && mc.thePlayer.posY >= launchY && !mc.thePlayer.onGround

    private val shouldKeepLaunchPosition
        get() = when (sameYMode.value) {
            "SameY" -> shouldJumpOnInput && scaffoldMode != "GodBridge"
            else -> false
        }

    // Zitter
    private var zitterDirection = false

    // Delay
    private val delayTimer = object : DelayTimer(minDelayValue, maxDelayValue, MSTimer()) {
        override fun hasTimePassed() = !placeDelayValue.isActive() || super.hasTimePassed()
    }

    private val zitterTickTimer = TickDelayTimer(minZitterTicksValue, maxZitterTicksValue)

    //Sneak
    private var ticksCouter0 = 0
    private val isSneakEnabled
        get() = sneak != "Off"

    // Eagle
    private var placedBlocksWithoutEagle = 0
    var eagleSneaking = false
    private val isEagleEnabled
        get() = eagle != "Off" && !shouldGoDown && scaffoldMode != "GodBridge"

    // Downwards
    val shouldGoDown
        get() = down && !sameY && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && scaffoldMode !in arrayOf(
            "GodBridge",
            "Telly"
        ) && blocksAmount > 1

    // Current rotation
    private val currRotation
        get() = RotationUtils.currentRotation ?: mc.thePlayer.rotation

    // Extra clicks
    private var extraClick =
        ExtraClickInfo(TimeUtils.randomClickDelay(extraClickMinCPS, extraClickMaxCPS), 0L, 0)

    // GodBridge
    private var blocksPlacedUntilJump = 0

    private val isManualJumpOptionActive
        get() = scaffoldMode == "GodBridge" && !jumpAutomatically

    private var blocksToJump = randomDelay(minBlocksToJump.get(), maxBlocksToJump.get())

    private val isGodBridgeEnabled
        get() = !Tower.isTowering && (scaffoldMode == "GodBridge" || scaffoldMode == "Normal" && rotationMode == "GodBridge")

    private var godBridgeTargetRotation: Rotation? = null

    private val isLookingDiagonally: Boolean
        get() {
            val player = mc.thePlayer ?: return false

            // Round the rotation to the nearest multiple of 45 degrees so that way we check if the player faces diagonally
            val yaw = round(abs(MathHelper.wrapAngleTo180_float(player.rotationYaw)).roundToInt() / 45f) * 45f

            return floatArrayOf(
                45f,
                135f
            ).any { yaw == it } && player.movementInput.moveForward != 0f && player.movementInput.moveStrafe == 0f
        }

    // Telly
    private var offGroundTicks = 0
    private var ticksUntilJump = 0
    private var blocksUntilAxisChange = 0
    private var jumpTicks = randomDelay(minJumpTicks.get(), maxJumpTicks.get())
    private var horizontalPlacements =
        randomDelay(minHorizontalPlacements.get(), maxHorizontalPlacements.get())
    private var verticalPlacements = randomDelay(minVerticalPlacements.get(), maxVerticalPlacements.get())
    private val shouldPlaceHorizontally
        get() = scaffoldMode == "Telly" && MovementUtils.isMoving && (startHorizontally && blocksUntilAxisChange <= horizontalPlacements || !startHorizontally && blocksUntilAxisChange > verticalPlacements)

    // <--

    // Enabling module
    override fun onEnable() {
        val player = mc.thePlayer ?: return

        launchY = player.posY.roundToInt()
        blocksUntilAxisChange = 0
    }

    // Events
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return

        if (mc.playerController.currentGameType == WorldSettings.GameType.SPECTATOR)
            return

        mc.timer.timerSpeed = timer

        // Telly
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0
            ticksUntilJump++
        } else {
            offGroundTicks++
        }

        if (shouldGoDown) {
            mc.gameSettings.keyBindSneak.pressed = false
        }

        if (slow) {
            if (!slowGround || slowGround && mc.thePlayer.onGround) {
                player.motionX *= slowSpeed
                player.motionZ *= slowSpeed
            }
        }

        if (isSneakEnabled){
            ticksCouter0++;
            if(!SneakJump){
                if(MovementUtils.isMovingForward) {
                    if (ticksCouter0 % blocksToSneakForward == 0) {
                        mc.gameSettings.keyBindSneak.pressed = true
                    } else if (ticksCouter0 % SneakDistanseForward == 0) {
                        mc.gameSettings.keyBindSneak.pressed = false
                    }
                }
            } else {
                if(!mc.thePlayer.onGround){
                    mc.gameSettings.keyBindSneak.pressed = true
                } else if (ticksCouter0 % SneakDistanseForward == 0){
                    mc.gameSettings.keyBindSneak.pressed = false
                }
            }
        }

        // Eagle
        if (isEagleEnabled) {
            var dif = 0.5
            val blockPos = BlockPos(player).down()

            for (side in EnumFacing.values()) {
                if (side.axis == EnumFacing.Axis.Y) {
                    continue
                }

                val neighbor = blockPos.offset(side)

                if (isReplaceable(neighbor)) {
                    val calcDif = (if (side.axis == EnumFacing.Axis.Z) {
                        abs(neighbor.z + 0.5 - player.posZ)
                    } else {
                        abs(neighbor.x + 0.5 - player.posX)
                    }) - 0.5

                    if (calcDif < dif) {
                        dif = calcDif
                    }
                }
            }

            if (placedBlocksWithoutEagle >= blocksToEagle) {
                val shouldEagle = isReplaceable(blockPos) || dif < edgeDistance
                if (eagle == "Silent") {
                    if (eagleSneaking != shouldEagle) {
                        sendPacket(
                            C0BPacketEntityAction(
                                player,
                                if (shouldEagle) C0BPacketEntityAction.Action.START_SNEAKING else C0BPacketEntityAction.Action.STOP_SNEAKING
                            )
                        )

                        // Adjust speed when silent sneaking
                        if (adjustedSneakSpeed && shouldEagle) {
                            player.motionX *= eagleSpeed
                            player.motionZ *= eagleSpeed
                        }
                    }

                    eagleSneaking = shouldEagle
                } else {
                    mc.gameSettings.keyBindSneak.pressed = shouldEagle
                    eagleSneaking = shouldEagle
                }
                placedBlocksWithoutEagle = 0
            } else {
                placedBlocksWithoutEagle++
            }
        }

        if (player.onGround) {
            // Still a thing?
            if (scaffoldMode == "Rewinside") {
                MovementUtils.strafe(0.2F)
                player.motionY = 0.0
            }
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        val player = mc.thePlayer

        if (scaffoldMode == "Telly" && player.onGround && MovementUtils.isMoving && currRotation == player.rotation && ticksUntilJump >= jumpTicks) {
            player.tryJump()

            ticksUntilJump = 0
            jumpTicks = randomDelay(minJumpTicks.get(), maxJumpTicks.get())
        }
        if (autojump0 && mc.thePlayer.ticksExisted % automaticjumps == 0 && mc.thePlayer.onGround) {
            player.tryJump()
        }
    }

    @EventTarget
    fun onRotationUpdate(event: RotationUpdateEvent) {
        if (mc.thePlayer.ticksExisted == 1)
            launchY = mc.thePlayer.posY.roundToInt()

        val rotation = RotationUtils.currentRotation

        update()

        val ticks = if (keepRotation) {
            if (scaffoldMode == "Telly") 1 else keepTicks
        } else {
            RotationUtils.resetTicks
        }

        if (isGodBridgeEnabled && rotationMode != "Off") {
            generateGodBridgeRotations(ticks)

            return
        }

        if (rotationMode != "Off" && rotation != null) {
            val placeRotation = this.placeRotation?.rotation ?: rotation

            if (RotationUtils.resetTicks != 0 || keepRotation) {
                setRotation(placeRotation, ticks)
            }
        }
    }

    @EventTarget
    fun onTick(event: GameTickEvent) {
        val target = placeRotation?.placeInfo

        if (extraClicks) {
            val doubleClick = if (simulateDoubleClicking) RandomUtils.nextInt(-1, 1) else 0

            repeat(extraClick.clicks + doubleClick) {
                extraClick.clicks--

                doPlaceAttempt()
            }
        }

        if (target == null) {
            if (placeDelayValue.isActive()) {
                delayTimer.reset()
            }
            return
        }

        val raycastProperly = !(scaffoldMode == "Expand" && expandLength > 1 || shouldGoDown) && rotationMode != "Off"

        performBlockRaytrace(currRotation, mc.playerController.blockReachDistance).let {
            if (rotationMode == "Off" || it != null && it.blockPos == target.blockPos && (!raycastProperly || it.sideHit == target.enumFacing)) {
                val result = if (raycastProperly && it != null) {
                    PlaceInfo(it.blockPos, it.sideHit, it.hitVec)
                } else {
                    target
                }

                place(result)
            }
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (motionModifier) {
            // Determine if player is in the air or on the ground
            val isAir = !mc.thePlayer.onGround

            // Select multipliers based on the player's state
            val forwardMultiplier = if (isAir) {
                multiplierAirForward.coerceIn(motionMultiplierAirMin, motionMultiplierAirMax) // Air forward multiplier
            } else {
                multiplierGroundForward.coerceIn(
                    motionMultiplierGroundMin,
                    motionMultiplierGroundMax
                ) // Ground forward multiplier
            }

            val strafeMultiplier = if (isAir) {
                multiplierAirStrafe.coerceIn(motionMultiplierAirMin, motionMultiplierAirMax) // Air strafe multiplier
            } else {
                multiplierGroundStrafe.coerceIn(
                    motionMultiplierGroundMin,
                    motionMultiplierGroundMax
                ) // Ground strafe multiplier
            }

            // Apply the modified motion
            mc.thePlayer.motionX *= strafeMultiplier // Strafe
            mc.thePlayer.motionZ *= forwardMultiplier // Forward
        }
    }

    @EventTarget
    fun onSneakSlowDown(event: SneakSlowDownEvent) {
        if (isEagleEnabled && eagle == "Normal") {
            event.forward *= eagleSpeed / 0.3f
            event.strafe *= eagleSpeed / 0.3f
        }
        if (sneak == "TicksFixed") {
            event.forward *= SneakSpeed / 0.3f
            event.strafe *= SneakSpeed / 0.3f
        }
    }

    @EventTarget
    fun onCameraUpdate(event: CameraPositionEvent) {
        if (!freezeCameraY || mc.thePlayer.posY < launchY || mc.thePlayer.posY - launchY > 1.5 || launchY == -999)
            return

        event.withY(launchY.toDouble())
    }

    @EventTarget
    fun onMovementInput(event: MovementInputEvent) {
        val player = mc.thePlayer ?: return

        if (!isGodBridgeEnabled || !player.onGround)
            return

        if (waitForRots) {
            godBridgeTargetRotation?.run {
                event.originalInput.sneak = event.originalInput.sneak || rotationDifference(this, currRotation) != 0f
            }
        }

        val simPlayer = SimulatedPlayer.fromClientPlayer(event.originalInput)

        simPlayer.tick()

        if (!simPlayer.onGround || blocksPlacedUntilJump > blocksToJump) {
            event.originalInput.jump = true

            blocksPlacedUntilJump = 0

            blocksToJump = randomDelay(minBlocksToJump.get(), maxBlocksToJump.get())
        }
    }

    fun update() {
        val player = mc.thePlayer ?: return
        val holdingItem = player.heldItem?.item is ItemBlock

        if (!holdingItem && (autoBlock == "Off" || InventoryUtils.findBlockInHotbar() == null)) {
            return
        }

        findBlock(scaffoldMode == "Expand" && expandLength > 1, searchMode == "Area")
    }

    private fun setRotation(rotation: Rotation, ticks: Int) {
        val player = mc.thePlayer ?: return

        if (silentRotation) {
            if (scaffoldMode == "Telly" && MovementUtils.isMoving) {
                if (offGroundTicks < ticksUntilRotation.get() && ticksUntilJump >= jumpTicks) {
                    return
                }
            }

            setTargetRotation(
                rotation,
                ticks,
                strafe,
                turnSpeed = minHorizontalSpeed..maxHorizontalSpeed to minVerticalSpeed..maxVerticalSpeed,
                angleThresholdForReset = angleThresholdUntilReset,
                smootherMode = this.smootherMode,
                simulateShortStop = simulateShortStop,
                startOffSlow = startRotatingSlow,
                slowDownOnDirChange = slowDownOnDirectionChange,
                useStraightLinePath = useStraightLinePath,
                minRotationDifference = minRotationDifference
            )

        } else {
            rotation.toPlayer(player)
        }
    }

    // Search for new target block
    private fun findBlock(expand: Boolean, area: Boolean) {
        val player = mc.thePlayer ?: return

        if (!shouldKeepLaunchPosition)
            launchY = player.posY.roundToInt()

        val blockPosition = if (shouldGoDown) {
            if (player.posY == player.posY.roundToInt() + 0.5) {
                BlockPos(player.posX, player.posY - 0.6, player.posZ)
            } else {
                BlockPos(player.posX, player.posY - 0.6, player.posZ).down()
            }
        } else if (shouldKeepLaunchPosition && launchY <= player.posY) {
            BlockPos(player.posX, launchY - 1.0, player.posZ)
        } else if (player.posY == player.posY.roundToInt() + 0.5) {
            BlockPos(player)
        } else {
            BlockPos(player).down()
        }

        if (!expand && (!isReplaceable(blockPosition) ||
                    search(blockPosition, !shouldGoDown, area, shouldPlaceHorizontally))) {
            return
        }

        if (expand) {
            val yaw = player.rotationYaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z

            repeat(expandLength) {
                if (search(blockPosition.add(x * it, 0, z * it), false, area))
                    return
            }
            return
        }

        val (horizontal, vertical) = if (scaffoldMode == "Telly") {
            5 to 3
        } else if (allowClutching) {
            horizontalClutchBlocks.get() to verticalClutchBlocks.get()
        } else {
            1 to 1
        }

        (-horizontal..horizontal).flatMap { x ->
            (0 downTo -vertical).flatMap { y ->
                (-horizontal..horizontal).map { z ->
                    Vec3i(x, y, z)
                }
            }
        }.sortedBy {
            BlockUtils.getCenterDistance(blockPosition.add(it))
        }.forEach {
            if (canBeClicked(blockPosition.add(it)) ||
                search(blockPosition.add(it), !shouldGoDown, area, shouldPlaceHorizontally)) {
                return
            }
        }
    }

    private fun place(placeInfo: PlaceInfo) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return
        if (!delayTimer.hasTimePassed() || shouldKeepLaunchPosition && launchY - 1 != placeInfo.vec3.yCoord.toInt() && scaffoldMode != "Expand")
            return

        var stack = player.inventoryContainer.getSlot(serverSlot + 36).stack

        //TODO: blacklist more blocks than only bushes
        if (stack == null || stack.item !is ItemBlock || (stack.item as ItemBlock).block is BlockBush || stack.stackSize <= 0 || sortByHighestAmount || earlySwitch) {
            val blockSlot = if (sortByHighestAmount) {
                InventoryUtils.findLargestBlockStackInHotbar() ?: return
            } else if (earlySwitch) {
                InventoryUtils.findBlockStackInHotbarGreaterThan(amountBeforeSwitch) ?: InventoryUtils.findBlockInHotbar() ?: return
            } else {
                InventoryUtils.findBlockInHotbar() ?: return
            }

            when (autoBlock.lowercase()) {
                "off" -> return

                "pick" -> {
                    player.inventory.currentItem = blockSlot - 36
                    mc.playerController.updateController()
                }

                "spoof", "switch" -> serverSlot = blockSlot - 36
            }
            stack = player.inventoryContainer.getSlot(blockSlot).stack
        }

        // Line 437-440
        if ((stack.item as? ItemBlock)?.canPlaceBlockOnSide(
                world,
                placeInfo.blockPos,
                placeInfo.enumFacing,
                player,
                stack
            ) == false
        ) {
            return
        }

        tryToPlaceBlock(stack, placeInfo.blockPos, placeInfo.enumFacing, placeInfo.vec3)

        if (autoBlock == "Switch")
            serverSlot = player.inventory.currentItem

        // Since we violate vanilla slot switch logic if we send the packets now, we arrange them for the next tick
        switchBlockNextTickIfPossible(stack)

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }
    }

    private fun doPlaceAttempt() {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        val stack = player.inventoryContainer.getSlot(serverSlot + 36).stack ?: return

        if (stack.item !is ItemBlock || InventoryUtils.BLOCK_BLACKLIST.contains((stack.item as ItemBlock).block)) {
            return
        }

        val block = stack.item as ItemBlock

        val raytrace = performBlockRaytrace(currRotation, mc.playerController.blockReachDistance) ?: return

        val canPlaceOnUpperFace = block.canPlaceBlockOnSide(
            world, raytrace.blockPos, EnumFacing.UP, player, stack
        )

        val shouldPlace = if (placementAttempt == "Fail") {
            !block.canPlaceBlockOnSide(world, raytrace.blockPos, raytrace.sideHit, player, stack)
        } else {
            if (shouldKeepLaunchPosition) {
                raytrace.blockPos.y == launchY - 1 && !canPlaceOnUpperFace
            } else if (shouldPlaceHorizontally) {
                !canPlaceOnUpperFace
            } else {
                raytrace.blockPos.y <= player.posY.toInt() - 1 && !(raytrace.blockPos.y == player.posY.toInt() - 1 && canPlaceOnUpperFace && raytrace.sideHit == EnumFacing.UP)
            }
        }

        if (!raytrace.typeOfHit.isBlock || !shouldPlace) {
            return
        }

        tryToPlaceBlock(stack, raytrace.blockPos, raytrace.sideHit, raytrace.hitVec, attempt = true)

        // Since we violate vanilla slot switch logic if we send the packets now, we arrange them for the next tick
        switchBlockNextTickIfPossible(stack)

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }
    }

    // Disabling module
    override fun onDisable() {
        val player = mc.thePlayer ?: return
        ticksCouter0 = 0

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
            if (eagleSneaking && player.isSneaking) {
                //sendPacket(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SNEAKING))

                /**
                 * Should prevent false flag by some AntiCheat (Ex: Verus)
                 */
                player.isSneaking = false
            }
        }

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
            mc.gameSettings.keyBindRight.pressed = false
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
            mc.gameSettings.keyBindLeft.pressed = false
        }

        if (autoF5) {
            mc.gameSettings.thirdPersonView = 0
        }

        placeRotation = null
        mc.timer.timerSpeed = 1f

        TickScheduler += {
            serverSlot = player.inventory.currentItem
        }
    }

    // Entity movement event
    @EventTarget
    fun onMove(event: MoveEvent) {
        val player = mc.thePlayer ?: return

        if (!safeWalkValue.isActive() || shouldGoDown) {
            return
        }

        if (airSafe || player.onGround) {
            event.isSafeWalk = true
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (!jumpStrafe) return

        if (event.eventState == EventState.POST) {
            MovementUtils.strafe(if (!isLookingDiagonally) (minJumpStraightStrafe.get()..maxJumpStraightStrafe.get()).random() else (minJumpDiagonalStrafe.get()..maxJumpDiagonalStrafe.get()).random())
        }
    }

    // Visuals
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val player = mc.thePlayer ?: return

        val shouldBother =
            !(shouldGoDown || scaffoldMode == "Expand" && expandLength > 1) && extraClicks && (MovementUtils.isMoving || MovementUtils.speed > 0.03)

        if (shouldBother) {
            currRotation.let {
                performBlockRaytrace(it, mc.playerController.blockReachDistance)?.let { raytrace ->
                    val timePassed = System.currentTimeMillis() - extraClick.lastClick >= extraClick.delay

                    if (raytrace.typeOfHit.isBlock && timePassed) {
                        extraClick = ExtraClickInfo(
                            TimeUtils.randomClickDelay(extraClickMinCPS, extraClickMaxCPS),
                            System.currentTimeMillis(),
                            extraClick.clicks + 1
                        )
                    }
                }
            }
        }

        displaySafetyLinesIfEnabled()

        if (!mark) {
            return
        }

        repeat(if (scaffoldMode == "Expand") expandLength + 1 else 2) {
            val yaw = player.rotationYaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
            val blockPos = BlockPos(
                player.posX + x * it,
                if (shouldKeepLaunchPosition && launchY <= player.posY) launchY - 1.0 else player.posY - (if (player.posY == player.posY + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
                player.posZ + z * it
            )
            val placeInfo = PlaceInfo.get(blockPos)

            if (isReplaceable(blockPos) && placeInfo != null) {
                RenderUtils.drawBlockBox(blockPos, Color(68, 117, 255, 100), false)
                return
            }
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param raycast visible
     * @param area spot
     * @return
     */

    fun search(
        blockPosition: BlockPos,
        raycast: Boolean,
        area: Boolean,
        horizontalOnly: Boolean = false,
    ): Boolean {
        val player = mc.thePlayer ?: return false

        // Check if the block can be replaced
        if (!isReplaceable(blockPosition)) {
            if (autoF5) mc.gameSettings.thirdPersonView = 0
            return false
        } else {
            if (autoF5 && mc.gameSettings.thirdPersonView != 1) mc.gameSettings.thirdPersonView = 1
        }

        val maxReach = mc.playerController.blockReachDistance
        val eyes = player.eyes
        var placeRotation: PlaceRotation? = null
        var currPlaceRotation: PlaceRotation?

        // Loop through neighboring blocks
        for (side in EnumFacing.values().filter { !horizontalOnly || it.axis != EnumFacing.Axis.Y }) {
            val neighbor = blockPosition.offset(side)

            // Check if the neighboring block can be clicked
            if (!canBeClicked(neighbor)) {
                continue
            }

            // FullBlock mode - searching multiple positions on the block
            if (searchMode == "FullBlock") {
                for (x in 0.0..1.0 step 0.5) {
                    for (y in 0.0..1.0 step 0.5) {
                        for (z in 0.0..1.0 step 0.5) {
                            currPlaceRotation = findTargetPlace(blockPosition, neighbor, Vec3(x, y, z), side, eyes, maxReach, raycast)
                                ?: continue
                            placeRotation = compareDifferences(currPlaceRotation, placeRotation)
                        }
                    }
                }
            }

            // Area mode or when GodBridge is enabled
            if (!area || isGodBridgeEnabled) {
                currPlaceRotation =
                    findTargetPlace(blockPosition, neighbor, Vec3(0.5, 0.5, 0.5), side, eyes, maxReach, raycast)
                        ?: continue

                placeRotation = compareDifferences(currPlaceRotation, placeRotation)
            } else {
                // Area search mode (different positions in the block)
                for (x in 0.1..0.9 step 0.2) {
                    for (y in 0.1..0.9 step 0.2) {
                        for (z in 0.1..0.9 step 0.2) {
                            currPlaceRotation =
                                findTargetPlace(blockPosition, neighbor, Vec3(x, y, z), side, eyes, maxReach, raycast)
                                    ?: continue

                            placeRotation = compareDifferences(currPlaceRotation, placeRotation)
                        }
                    }
                }
            }
        }

        // If no valid placement rotation was found, return false
        placeRotation ?: return false

        // Set head rotation if rotation mode is active
        if (rotationMode != "Off" && !isGodBridgeEnabled) {
            // Check if the player is moving
            if (rotationMode == "Backward2" && MovementUtils.isMoving) {
                val correctedRotation = getCorrectedBackwardRotation(player, placeRotation.rotation)
                setRotation(correctedRotation, if (scaffoldMode == "Telly") 1 else keepTicks)
            } else {
                setRotation(placeRotation.rotation, if (scaffoldMode == "Telly") 1 else keepTicks)
            }
        }

        // Set the block placement rotation
        this.placeRotation = placeRotation
        return true
    }
    private fun getCorrectedBackwardRotation(player: EntityPlayer, desiredRotation: Rotation): Rotation {
        // Check if the player is near an edge
        val nearEdge = detectEdge(player)
        var currentYaw = MovementUtils.movingYaw

        // Adjust yaw based on whether the player is near an edge
        if (nearEdge) {
            currentYaw = adjustYawForEdgePlacement(currentYaw, desiredRotation.yaw)
        } else {
            // Standard backward adjustment
            currentYaw -= 180.0f
        }

        return Rotation(currentYaw, desiredRotation.pitch).fixedSensitivity()
    }

    /**
     * Adjust the yaw when the player is near the edge to facilitate block placement.
     */
    private fun adjustYawForEdgePlacement(currentYaw: Float, desiredYaw: Float): Float {
        // Implement logic to smoothly adjust the yaw
        // This could include checking movement direction and the desired block placement angle
        val yawDifference = (desiredYaw - currentYaw + 180) % 360 - 180 // Calculate the difference within -180 to 180 range
        val adjustment = if (yawDifference > 0) 5.0f else -5.0f // Adjust in small increments
        return currentYaw + adjustment // Adjust the yaw smoothly
    }

    fun detectEdge(player: EntityPlayer): Boolean {
        // Check if the player's vertical motion is downward and if they are near an edge
        return player.motionY < -0.15 && isPlayerNearEdge(player)
    }

    fun isPlayerNearEdge(player: EntityPlayer): Boolean {
        val pos = player.position
        val blocksAround = listOf(
            BlockPos(pos.x + 1, pos.y, pos.z), // Right
            BlockPos(pos.x - 1, pos.y, pos.z), // Left
            BlockPos(pos.x, pos.y, pos.z + 1), // Forward
            BlockPos(pos.x, pos.y, pos.z - 1)  // Backward
        )
        // Check if all adjacent blocks are air using the isAir method
        return blocksAround.all { blockPos ->
            mc.theWorld.getBlockState(blockPos).block.isAir(mc.theWorld, blockPos)
        }
    }

    /**
     * For expand scaffold, fixes vector values that should match according to direction vector
     */
    private fun modifyVec(original: Vec3, direction: EnumFacing, pos: Vec3, shouldModify: Boolean): Vec3 {
        if (!shouldModify) {
            return original
        }

        val x = original.xCoord
        val y = original.yCoord
        val z = original.zCoord

        val side = direction.opposite

        return when (side.axis ?: return original) {
            EnumFacing.Axis.Y -> Vec3(x, pos.yCoord + side.directionVec.y.coerceAtLeast(0), z)
            EnumFacing.Axis.X -> Vec3(pos.xCoord + side.directionVec.x.coerceAtLeast(0), y, z)
            EnumFacing.Axis.Z -> Vec3(x, y, pos.zCoord + side.directionVec.z.coerceAtLeast(0))
        }

    }

    private fun findTargetPlace(
        pos: BlockPos, offsetPos: BlockPos, vec3: Vec3, side: EnumFacing, eyes: Vec3, maxReach: Float, raycast: Boolean,
    ): PlaceRotation? {
        val world = mc.theWorld ?: return null

        val vec = (Vec3(pos) + vec3).addVector(
            side.directionVec.x * vec3.xCoord,
            side.directionVec.y * vec3.yCoord,
            side.directionVec.z * vec3.zCoord
        )

        val distance = eyes.distanceTo(vec)

        if (raycast && (distance > maxReach || world.rayTraceBlocks(eyes, vec, false, true, false) != null)) {
            return null
        }

        val diff = vec - eyes

        if (side.axis != EnumFacing.Axis.Y) {
            val dist = abs(if (side.axis == EnumFacing.Axis.Z) diff.zCoord else diff.xCoord)

            if (dist < minDist && scaffoldMode != "Telly") {
                return null
            }
        }

        val player = mc.thePlayer
        var rotation = toRotation(vec, true).fixedSensitivity()
        rotation = when (pitchMode) {
            "Custom" -> Rotation(player.rotationYaw ,pitchCustomValue.get())
            "Vanilla" -> rotation
            "Vanilla2" -> Rotation(player.rotationYaw, rotation.pitch)
            "Legit" -> Rotation(player.rotationYaw, rotation.pitch.coerceAtMost(pitchCorrectValue.get()))
            else -> rotation
        }.fixedSensitivity()
        rotation = when (rotationMode) {
            "Normal" -> rotation
            "Offset" -> Rotation(player.rotationYaw + 15.0f, rotation.pitch)
            "Stabilized" -> Rotation(round(rotation.yaw / 45f) * 45f, rotation.pitch)
            "Backward" -> {
                if (player != null) {
                    var currentYaw = MovementUtils.movingYaw - 180
                    val currentPitch = rotation.pitch
                    val nearEdge = detectEdge(player)
                    if (nearEdge) {
                        currentYaw = adjustYawForPlacement(currentYaw)
                    }
                    val smoothYaw = smoothYawAdjustment(MovementUtils.movingYaw - 180, currentYaw, tickRate = 5)
                    Rotation(smoothYaw, currentPitch).fixedSensitivity()
                } else {
                    rotation
                }
            }
            "Backward2" -> {
                if (player != null) {
                    val smoothYaw = smoothYawAdjustment(MovementUtils.movingYaw - 180,rotation.yaw - 180, tickRate = 2)
                    Rotation(smoothYaw, rotation.pitch).fixedSensitivity()
                } else {
                    rotation
                }
            }
            else -> rotation
        }.fixedSensitivity()

        // If the current rotation already looks at the target block and side, then return right here
        performBlockRaytrace(currRotation, maxReach)?.let { raytrace ->
            if (raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite)) {
                return PlaceRotation(
                    PlaceInfo(
                        raytrace.blockPos, side.opposite, modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
                    ), currRotation
                )
            }
        }

        val raytrace = performBlockRaytrace(rotation, maxReach) ?: return null

        if (raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite)) {
            return PlaceRotation(
                PlaceInfo(
                    raytrace.blockPos, side.opposite, modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
                ), rotation
            )
        }

        return null
    }
    fun adjustYawForPlacement(currentYaw: Float): Float {
        return currentYaw + 5.0f
    }

    // Funkcja płynnego dostosowania yaw za pomocą ticków
    fun smoothYawAdjustment(startYaw: Float, targetYaw: Float, tickRate: Int): Float {
        return startYaw + (targetYaw - startYaw) / tickRate // Płynne dostosowanie yaw
    }

    private fun performBlockRaytrace(rotation: Rotation, maxReach: Float): MovingObjectPosition? {
        val player = mc.thePlayer ?: return null
        val world = mc.theWorld ?: return null

        val eyes = player.eyes
        val rotationVec = getVectorForRotation(rotation)

        val reach = eyes + (rotationVec * maxReach.toDouble())

        return world.rayTraceBlocks(eyes, reach, false, false, true)
    }

    private fun compareDifferences(
        new: PlaceRotation, old: PlaceRotation?, rotation: Rotation = currRotation,
    ): PlaceRotation {
        if (old == null || rotationDifference(
                new.rotation,
                rotation
            ) < rotationDifference(
                old.rotation, rotation
            )
        ) {
            return new
        }

        return old
    }

    private fun switchBlockNextTickIfPossible(stack: ItemStack) {
        val player = mc.thePlayer ?: return

        if (autoBlock in arrayOf("Off", "Switch"))
            return

        val switchAmount = if (earlySwitch) amountBeforeSwitch else 0

        if (stack.stackSize > switchAmount)
            return

        val switchSlot = if (earlySwitch) {
            InventoryUtils.findBlockStackInHotbarGreaterThan(amountBeforeSwitch) ?: InventoryUtils.findBlockInHotbar() ?: return
        } else {
            InventoryUtils.findBlockInHotbar()
        } ?: return

        TickScheduler += {
            if (autoBlock == "Pick") {
                player.inventory.currentItem = switchSlot - 36
                mc.playerController.updateController()
            } else {
                serverSlot = switchSlot - 36
            }
        }
    }

    private fun displaySafetyLinesIfEnabled() {
        if (!safetyLines || !isGodBridgeEnabled) {
            return
        }

        val player = mc.thePlayer ?: return

        // If player is not walking diagonally then continue
        if (round(abs(MathHelper.wrapAngleTo180_float(player.rotationYaw)).roundToInt() / 45f) * 45f !in arrayOf(
                135f,
                45f
            ) || player.movementInput.moveForward == 0f || player.movementInput.moveStrafe != 0f
        ) {
            val (posX, posY, posZ) = player.interpolatedPosition()

            GL11.glPushMatrix()
            GL11.glTranslated(-posX, -posY, -posZ)
            GL11.glLineWidth(5.5f)
            GL11.glDisable(GL11.GL_TEXTURE_2D)

            val (yawX, yawZ) = player.horizontalFacing.directionVec.x * 1.5 to player.horizontalFacing.directionVec.z * 1.5

            // The target rotation will either be the module's placeRotation or a forced rotation (usually that's where the GodBridge mode aims)
            val targetRotation = run {
                val yaw = floatArrayOf(-135f, -45f, 45f, 135f).minByOrNull {
                    abs(
                        RotationUtils.angleDifference(
                            it,
                            MathHelper.wrapAngleTo180_float(currRotation.yaw)
                        )
                    )
                } ?: return

                placeRotation?.rotation ?: Rotation(yaw, 73f)
            }

            // Calculate color based on rotation difference
            val color = getColorForRotationDifference(
                rotationDifference(
                    targetRotation,
                    currRotation
                )
            )

            val main = BlockPos(player).down()

            val pos = if (canBeClicked(main)) {
                main
            } else {
                (-1..1).flatMap { x ->
                    (-1..1).map { z ->
                        val neighbor = main.add(x, 0, z)

                        neighbor to BlockUtils.getCenterDistance(neighbor)
                    }
                }.filter { canBeClicked(it.first) }.minByOrNull { it.second }?.first ?: main
            }.up().getVec()

            for (offset in 0..1) {
                for (i in -1..1 step 2) {
                    for (x1 in 0.25..0.5 step 0.01) {
                        val opposite = offset == 1

                        val (offsetX, offsetZ) = if (opposite) 0.0 to x1 * i else x1 * i to 0.0
                        val (lineX, lineZ) = if (opposite) yawX to 0.0 else 0.0 to yawZ

                        val (x, y, z) = pos.add(Vec3(offsetX, -0.99, offsetZ))

                        GL11.glBegin(GL11.GL_LINES)

                        GL11.glColor3f(color.x, color.y, color.z)
                        GL11.glVertex3d(x - lineX, y + 0.5, z - lineZ)
                        GL11.glVertex3d(x + lineX, y + 0.5, z + lineZ)

                        GL11.glEnd()
                    }
                }
            }
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glPopMatrix()
        }
    }

    private fun getColorForRotationDifference(rotationDifference: Float): Color3f {
        val maxDifferenceForGreen = 10.0f
        val maxDifferenceForYellow = 40.0f

        val interpolationFactor = when {
            rotationDifference <= maxDifferenceForGreen -> 0.0f
            rotationDifference <= maxDifferenceForYellow -> (rotationDifference - maxDifferenceForGreen) / (maxDifferenceForYellow - maxDifferenceForGreen)
            else -> 1.0f
        }

        val green = 1.0f - interpolationFactor
        val blue = 0.0f

        return Color3f(interpolationFactor, green, blue)
    }

    private fun updatePlacedBlocksForTelly() {
        if (blocksUntilAxisChange > horizontalPlacements + verticalPlacements) {
            blocksUntilAxisChange = 0

            horizontalPlacements =
                randomDelay(minHorizontalPlacements.get(), maxHorizontalPlacements.get())
            verticalPlacements =
                randomDelay(minVerticalPlacements.get(), maxVerticalPlacements.get())
            return
        }

        blocksUntilAxisChange++
    }

    private fun tryToPlaceBlock(
        stack: ItemStack,
        clickPos: BlockPos,
        side: EnumFacing,
        hitVec: Vec3,
        attempt: Boolean = false,
    ): Boolean {
        val thePlayer = mc.thePlayer ?: return false

        val prevSize = stack.stackSize

        val clickedSuccessfully = thePlayer.onPlayerRightClick(clickPos, side, hitVec, stack)

        if (clickedSuccessfully) {
            if (!attempt) {
                delayTimer.reset()

                if (thePlayer.onGround) {
                    thePlayer.motionX *= speedModifier
                    thePlayer.motionZ *= speedModifier
                }
            }

            if (swing) thePlayer.swingItem()
            else sendPacket(C0APacketAnimation())

            if (isManualJumpOptionActive)
                blocksPlacedUntilJump++

            updatePlacedBlocksForTelly()

            if (stack.stackSize <= 0) {
                thePlayer.inventory.mainInventory[serverSlot] = null
                ForgeEventFactory.onPlayerDestroyItem(thePlayer, stack)
            } else if (stack.stackSize != prevSize || mc.playerController.isInCreativeMode)
                mc.entityRenderer.itemRenderer.resetEquippedProgress()

        } else {
            if (thePlayer.sendUseItem(stack))
                mc.entityRenderer.itemRenderer.resetEquippedProgress2()
        }

        return clickedSuccessfully
    }

    fun handleMovementOptions(input: MovementInput) {
        val player = mc.thePlayer ?: return

        if (!state) {
            return
        }

        if (!slow && speedLimiter && MovementUtils.speed > speedLimit) {
            input.moveStrafe = 0f
            input.moveForward = 0f
            return
        }

        when (zitterMode.lowercase()) {
            "off" -> {
                return
            }

            "smooth" -> {
                val notOnGround = !player.onGround || !player.isCollidedVertically

                if (player.onGround) {
                    input.sneak = eagleSneaking || GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
                }

                if (input.jump || mc.gameSettings.keyBindJump.isKeyDown || notOnGround) {
                    zitterTickTimer.reset()

                    if (useSneakMidAir) {
                        input.sneak = true
                    }

                    if (!notOnGround && !input.jump) {
                        // Attempt to move against the direction
                        input.moveStrafe = if (zitterDirection) 1f else -1f
                    } else {
                        input.moveStrafe = 0f
                    }

                    zitterDirection = !zitterDirection

                    // Recreate input in case the user was indeed pressing inputs
                    if (mc.gameSettings.keyBindLeft.isKeyDown) {
                        input.moveStrafe++
                    }

                    if (mc.gameSettings.keyBindRight.isKeyDown) {
                        input.moveStrafe--
                    }
                    return
                }

                if (zitterTickTimer.hasTimePassed()) {
                    zitterDirection = !zitterDirection
                    zitterTickTimer.reset()
                } else {
                    zitterTickTimer.update()
                }

                if (zitterDirection) {
                    input.moveStrafe = -1f
                } else {
                    input.moveStrafe = 1f
                }
            }

            "teleport" -> {
                MovementUtils.strafe(zitterSpeed)
                val yaw = (player.rotationYaw + if (zitterDirection) 90.0 else -90.0).toRadians()
                player.motionX -= sin(yaw) * zitterStrength
                player.motionZ += cos(yaw) * zitterStrength
                zitterDirection = !zitterDirection
            }
        }
    }

    private var isOnRightSide = false

    /**
     * God-bridge rotation generation method from Nextgen
     *
     * Credits to @opZywl
     */
    private fun generateGodBridgeRotations(ticks: Int) {
        val player = mc.thePlayer ?: return

        val direction = MovementUtils.direction.toDegreesF() + 180f

        val movingYaw = round(direction / 45) * 45
        val isMovingStraight = movingYaw % 90 == 0f

        if (!MovementUtils.isMoving) {
            placeRotation?.run {
                val axisMovement = floor(this.rotation.yaw / 90) * 90

                val yaw = axisMovement + 45
                val pitch = 75f

                setRotation(Rotation(yaw, pitch), ticks)
                return
            }
        }

        val rotation = if (isMovingStraight) {
            if (player.onGround) {
                isOnRightSide = floor(player.posX + cos(movingYaw.toRadians()) * 0.5) != floor(player.posX) ||
                        floor(player.posZ + sin(movingYaw.toRadians()) * 0.5) != floor(player.posZ)

                val posInDirection = BlockPos(player.positionVector.offset(EnumFacing.fromAngle(movingYaw.toDouble()),
                    0.6
                )
                )

                val isLeaningOffBlock = getBlock(player.position.down()) == air
                val nextBlockIsAir = getBlock(posInDirection.down()) == air

                if (isLeaningOffBlock && nextBlockIsAir) {
                    isOnRightSide = !isOnRightSide
                }
            }

            Rotation(movingYaw + if (isOnRightSide) 45f else -45f, if (useOptimizedPitch) 73.5f else customGodPitch)
        } else {
            Rotation(movingYaw, 75.6f)
        }.fixedSensitivity()

        godBridgeTargetRotation = rotation

        setRotation(rotation, ticks)
    }

    /**
     * Returns the amount of blocks
     */
    val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue
                val item = stack.item
                if (item is ItemBlock) {
                    val block = item.block
                    val heldItem = mc.thePlayer.heldItem
                    if (heldItem != null && heldItem == stack || block !in InventoryUtils.BLOCK_BLACKLIST && block !is BlockBush) {
                        amount += stack.stackSize
                    }
                }
            }
            return amount
        }

    override val tag
        get() = if (towerMode != "None") ("$scaffoldMode | $towerMode") else scaffoldMode

    data class ExtraClickInfo(val delay: Int, val lastClick: Long, var clicks: Int)
}
