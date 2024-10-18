/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac.*
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.blocksmc.BlocksMC
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.blocksmc.BlocksMC2
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel.BoostHypixel
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel.FreeHypixel
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel.Hypixel
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp.NCP
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp.OldNCP
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other.*
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan.BugSpartan
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan.Spartan
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan.Spartan2
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vanilla.DefaultVanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vanilla.SmoothVanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vanilla.Vanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.verus.Verus
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.verus.VerusGlide
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan.Vulcan
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan.VulcanGhost
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan.VulcanOld
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.stop
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import org.lwjgl.input.Keyboard
import java.awt.Color

object Flight : Module("Fly", Category.MOVEMENT, Keyboard.KEY_F, hideModule = false) {
    private val flyModes = arrayOf(
        Vanilla, SmoothVanilla, DefaultVanilla,

        // NCP
        NCP, OldNCP,

        // AAC
        AAC1910, AAC305, AAC316, AAC3312, AAC3312Glide, AAC3313,

        // CubeCraft
        CubeCraft,

        // Hypixel
        Hypixel, BoostHypixel, FreeHypixel,

        // Other server specific flys
        NeruxVace, Minesucht, BlocksMC, BlocksMC2,

        // Spartan
        Spartan, Spartan2, BugSpartan,

        // Vulcan
        Vulcan, VulcanOld, VulcanGhost,

        // Verus
        Verus, VerusGlide,

        // Other anti-cheats
        MineSecure, HawkEye, HAC, WatchCat,

        // Other
        Jetpack, KeepAlive, Collide, Jump, Flag, FlagUp, Fireball
    )

    /**
     * Old/Deprecated Modes
     */
    private val deprecatedMode = arrayOf(
        Spartan, Spartan2, BugSpartan,

        MineSecure, HawkEye, HAC, WatchCat, NeruxVace, Minesucht,

        BlocksMC, BlocksMC2,

        Hypixel, BoostHypixel, FreeHypixel,

        NCP, OldNCP,

        AAC1910, AAC305, AAC316, AAC3312, AAC3312Glide, AAC3313,

        CubeCraft
    )

    private val showDeprecatedValue: BoolValue = object : BoolValue("DeprecatedMode", true) {
        override fun onUpdate(value: Boolean) {
            mode.changeValue(modesList.first { it !in deprecatedMode }.modeName)
            mode.updateValues(modesList.filter { value || it !in deprecatedMode }.map { it.modeName }.toTypedArray())
        }
    }

    private val showDeprecated by showDeprecatedValue

    private var modesList = flyModes

    val mode = ListValue("Mode", modesList.map { it.modeName }.toTypedArray(), "Vanilla")
    val smoothValue by BoolValue("Smooth", false) { mode.get() == "DefaultVanilla" }
    val speedValue by FloatValue("Speed", 2f, 0f.. 5f) { mode.get() == "DefaultVanilla" }
    val vspeedValue by FloatValue("Vertical", 2f, 0f..5f) { mode.get() == "DefaultVanilla" }
    val kickBypassValue by BoolValue("KickBypass", false) { mode.get() == "DefaultVanilla" }
    val kickBypassModeValue by ListValue("KickBypassMode", arrayOf("Motion", "Packet"), "Packet") {  kickBypassValue }
    val kickBypassMotionSpeedValue by FloatValue("KickBypass-MotionSpeed", 0.0626F, 0.05F..0.1F) { kickBypassModeValue == "Motion" && kickBypassValue }
    val noClipValue by BoolValue("NoClip", false) { mode.get() == "DefaultVanilla" }
    val spoofValue by BoolValue("SpoofGround", false) { mode.get() == "DefaultVanilla" }

    val vanillaSpeed by FloatValue("VanillaSpeed", 2f, 0f..10f, subjective = true)
    { mode.get() in arrayOf("Vanilla", "KeepAlive", "MineSecure", "BugSpartan") }
    private val vanillaKickBypass by BoolValue("VanillaKickBypass", false, subjective = true)
    { mode.get() in arrayOf("Vanilla", "SmoothVanilla") }
    val ncpMotion by FloatValue("NCPMotion", 0f, 0f..1f) { mode.get() == "NCP" }

    // AAC
    val aacSpeed by FloatValue("AAC1.9.10-Speed", 0.3f, 0f..1f) { mode.get() == "AAC1.9.10" }
    val aacFast by BoolValue("AAC3.0.5-Fast", true) { mode.get() == "AAC3.0.5" }
    val aacMotion by FloatValue("AAC3.3.12-Motion", 10f, 0.1f..10f) { mode.get() == "AAC3.3.12" }
    val aacMotion2 by FloatValue("AAC3.3.13-Motion", 10f, 0.1f..10f) { mode.get() == "AAC3.3.13" }

    // Hypixel
    val hypixelBoost by BoolValue("Hypixel-Boost", true) { mode.get() == "Hypixel" }
    val hypixelBoostDelay by IntegerValue("Hypixel-BoostDelay", 1200, 50..2000)
    { mode.get() == "Hypixel" && hypixelBoost }
    val hypixelBoostTimer by FloatValue("Hypixel-BoostTimer", 1f, 0.1f..5f)
    { mode.get() == "Hypixel" && hypixelBoost }

    // Other
    val neruxVaceTicks by IntegerValue("NeruxVace-Ticks", 6, 2..20) { mode.get() == "NeruxVace" }

    // Verus
    val damage by BoolValue("Damage", false) { mode.get() == "Verus" }
    val timerSlow by BoolValue("TimerSlow", true) { mode.get() == "Verus" }
    val boostTicksValue by IntegerValue("BoostTicks", 20, 1..30) { mode.get() == "Verus" }
    val boostMotion by FloatValue("BoostMotion", 6.5f, 1f..9.85f) { mode.get() == "Verus" }
    val yBoost by FloatValue("YBoost", 0.42f, 0f..10f) { mode.get() == "Verus" }

    // BlocksMC
    val stable by BoolValue("Stable", false) { mode.get() == "BlocksMC" || mode.get() == "BlocksMC2" }
    val timerSlowed by BoolValue("TimerSlowed", true) { mode.get() == "BlocksMC" || mode.get() == "BlocksMC2" }
    val boostSpeed by FloatValue("BoostSpeed", 6f, 1f..15f) { mode.get() == "BlocksMC" || mode.get() == "BlocksMC2" }
    val extraBoost by FloatValue("ExtraSpeed", 1f, 0.0F..2f) { mode.get() == "BlocksMC" || mode.get() == "BlocksMC2" }
    val stopOnLanding by BoolValue("StopOnLanding", true) { mode.get() == "BlocksMC" || mode.get() == "BlocksMC2" }
    val stopOnNoMove by BoolValue("StopOnNoMove", false) { mode.get() == "BlocksMC" || mode.get() == "BlocksMC2" }
    val debugFly by BoolValue("Debug", false) { mode.get() == "BlocksMC" || mode.get() == "BlocksMC2" }

    // Fireball
    val rotations by BoolValue("Rotations", true) { mode.get() == "Fireball" }
    val pitchMode by ListValue("PitchMode", arrayOf("Custom", "Smart"), "Custom") { mode.get() == "Fireball" }
    val rotationPitch by FloatValue("Pitch", 90f,0f..90f) { pitchMode != "Smart" && mode.get() == "Fireball" }
    val invertYaw by BoolValue("InvertYaw", true) { pitchMode != "Smart" && mode.get() == "Fireball" }

    val autoFireball by ListValue("AutoFireball", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof") { mode.get() == "Fireball" }
    val swing by BoolValue("Swing", true) { mode.get() == "Fireball" }
    val fireballTry by IntegerValue("MaxFireballTry", 1, 0..2) { mode.get() == "Fireball" }
    val fireBallThrowMode by ListValue("FireballThrow", arrayOf("Normal", "Edge"), "Normal") { mode.get() == "Fireball" }
    val edgeThreshold by FloatValue("EdgeThreshold", 1.05f,1f..2f) { fireBallThrowMode == "Edge" && mode.get() == "Fireball" }

    val smootherMode by ListValue("SmootherMode", arrayOf("Linear", "Relative"), "Relative") { rotations && mode.get() == "Fireball" }
    val keepRotation by BoolValue("KeepRotation", true) { rotations && mode.get() == "Fireball" }
    val keepTicks by object : IntegerValue("KeepTicks", 1, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minimum)
        override fun isSupported() = rotations && keepRotation && mode.get() == "Fireball"
    }

    val simulateShortStop by BoolValue("SimulateShortStop", false) {  rotations && mode.get() == "Fireball" }
    val startFirstRotationSlow by BoolValue("StartFirstRotationSlow", false) { rotations && mode.get() == "Fireball" }

    val maxHorizontalSpeed: FloatValue = object : FloatValue("MaxHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalSpeed.get())
        override fun isSupported() = rotations && mode.get() == "Fireball"
    }

    val minHorizontalSpeed: FloatValue = object : FloatValue("MinHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalSpeed.get())
        override fun isSupported() = rotations && mode.get() == "Fireball"
    }

    val maxVerticalSpeed: FloatValue = object : FloatValue("MaxVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minVerticalSpeed.get())
        override fun isSupported() = rotations && mode.get() == "Fireball"
    }

    val minVerticalSpeed: FloatValue = object : FloatValue("MinVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxVerticalSpeed.get())
        override fun isSupported() = rotations && mode.get() == "Fireball"
    }

    val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f) { rotations && mode.get() == "Fireball" }

    val minRotationDifference by FloatValue("MinRotationDifference", 0f, 0f..1f) { rotations && mode.get() == "Fireball" }

    val autoJump by BoolValue("AutoJump", true) { mode.get() == "Fireball" }

    // Visuals
    private val mark by BoolValue("Mark", true, subjective = true)

    var wasFired = false
    var firePosition: BlockPos ?= null

    var jumpY = 0.0

    var startY = 0.0
        private set

    private val groundTimer = MSTimer()
    private var wasFlying = false

    override fun onEnable() {
        val thePlayer = mc.thePlayer ?: return

        startY = thePlayer.posY
        jumpY = thePlayer.posY
        wasFlying = mc.thePlayer.capabilities.isFlying

        modeModule.onEnable()
    }

    override fun onDisable() {
        val thePlayer = mc.thePlayer ?: return

        if (!mode.get().startsWith("AAC") && mode.get() != "Hypixel" && mode.get() != "VerusGlide"
            && mode.get() != "SmoothVanilla" && mode.get() != "Vanilla" && mode.get() != "Rewinside"
            && mode.get() != "Fireball" && mode.get() != "Collide" && mode.get() != "Jump") {

            if (mode.get() == "CubeCraft") thePlayer.stopXZ()
            else thePlayer.stop()
        }

        wasFired = false
        firePosition = null
        serverSlot = thePlayer.inventory.currentItem
        thePlayer.capabilities.isFlying = wasFlying
        mc.timer.timerSpeed = 1f
        thePlayer.speedInAir = 0.02f

        modeModule.onDisable()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        modeModule.onUpdate()
    }

    @EventTarget
    fun onTick(event: GameTickEvent) {
        if (mode.get() == "Fireball" && wasFired) {
            WaitTickUtils.scheduleTicks(2) {
                Flight.state = false
            }
        }

        modeModule.onTick()
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!mark || mode.get() == "Vanilla" || mode.get() == "SmoothVanilla")
            return

        val y = startY + 2.0 + (if (mode.get() == "BoostHypixel") 0.42 else 0.0)
        drawPlatform(
            y,
            if (mc.thePlayer.entityBoundingBox.maxY < y) Color(0, 255, 0, 90) else Color(255, 0, 0, 90),
            1.0
        )

        modeModule.onRender3D(event)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        mc.thePlayer ?: return

        modeModule.onPacket(event)
    }

    @EventTarget
    fun onBB(event: BlockBBEvent) {
        mc.thePlayer ?: return

        modeModule.onBB(event)
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        modeModule.onJump(event)
    }

    @EventTarget
    fun onStep(event: StepEvent) {
        modeModule.onStep(event)
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        modeModule.onMotion(event)
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        modeModule.onMove(event)
    }

    fun handleVanillaKickBypass() {
        if (!vanillaKickBypass || !groundTimer.hasTimePassed(1000)) return
        val ground = calculateGround() + 0.5
        run {
            var posY = mc.thePlayer.posY
            while (posY > ground) {
                sendPacket(C04PacketPlayerPosition(mc.thePlayer.posX, posY, mc.thePlayer.posZ, true))
                if (posY - 8.0 < ground) break // Prevent next step
                posY -= 8.0
            }
        }
        sendPacket(C04PacketPlayerPosition(mc.thePlayer.posX, ground, mc.thePlayer.posZ, true))
        var posY = ground
        while (posY < mc.thePlayer.posY) {
            sendPacket(C04PacketPlayerPosition(mc.thePlayer.posX, posY, mc.thePlayer.posZ, true))
            if (posY + 8.0 > mc.thePlayer.posY) break // Prevent next step
            posY += 8.0
        }
        sendPacket(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true))
        groundTimer.reset()
    }

    // TODO: Make better and faster calculation lol
    private fun calculateGround(): Double {
        val playerBoundingBox = mc.thePlayer.entityBoundingBox
        var blockHeight = 0.05
        var ground = mc.thePlayer.posY
        while (ground > 0.0) {
            val customBox = AxisAlignedBB.fromBounds(
                playerBoundingBox.maxX,
                ground + blockHeight,
                playerBoundingBox.maxZ,
                playerBoundingBox.minX,
                ground,
                playerBoundingBox.minZ
            )
            if (mc.theWorld.checkBlockCollision(customBox)) {
                if (blockHeight <= 0.05) return ground + blockHeight
                ground += blockHeight
                blockHeight = 0.05
            }
            ground -= blockHeight
        }
        return 0.0
    }

    override val tag
        get() = mode.get()

    private val modeModule
        get() = flyModes.find { it.modeName == mode.get() }!!
}