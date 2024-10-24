/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.exploit.disablermodes.other.BasicDisabler.basicTypePrefix
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.hypixel.HypixelHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave.IntaveHop14
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave.IntaveTimer14
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spartan.SpartanYPort
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreBHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreOnGround
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanGround288
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanLowHop
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.input.Keyboard

object Speed : Module("Speed", Category.MOVEMENT, Keyboard.KEY_V, hideModule = false) {

    private val speedModes = arrayOf(

        // NCP
        NCPBHop,
        NCPFHop,
        SNCPBHop,
        NCPHop,
        NCPYPort,
        UNCPHop,
        UNCPHopNew,

        // AAC
        AACHop3313,
        AACHop350,
        AACHop4,
        AACHop5,

        // Spartan
        SpartanYPort,

        // Spectre
        SpectreLowHop,
        SpectreBHop,
        SpectreOnGround,

        // Verus
        VerusHop,
        VerusFHop,
        VerusLowHop,
        VerusLowHopNew,

        // Vulcan
        VulcanHop,
        VulcanLowHop,
        VulcanGround288,

        // Matrix
        OldMatrixHop,
        MatrixHop,
        MatrixSlowHop,

        // Intave
        IntaveHop14,
        IntaveTimer14,

        // Server specific
        TeleportCubeCraft,
        HypixelHop,

        // Other
        Boost,
        Frame,
        MiJump,
        OnGround,
        SlowHop,
        Legit,
        CustomOld,
        SilentTimer,
    )

    /**
     * Old/Deprecated Modes
     */
    private val deprecatedMode = arrayOf(
        TeleportCubeCraft,

        OldMatrixHop,

        VerusLowHop,

        SpectreLowHop, SpectreBHop, SpectreOnGround,

        AACHop3313, AACHop350, AACHop4,

        NCPBHop, NCPFHop, SNCPBHop, NCPHop, NCPYPort,
    )

    private val showDeprecatedValue = object : BoolValue("DeprecatedMode", true) {
        override fun onUpdate(value: Boolean) {
            mode.changeValue(modesList.first { it !in deprecatedMode }.modeName)
            mode.updateValues(modesList.filter { value || it !in deprecatedMode }.map { it.modeName }.toTypedArray())
        }
    }

    private val showDeprecated by showDeprecatedValue

    private var modesList = speedModes

    val mode = ListValue("Mode", modesList.map { it.modeName }.toTypedArray(), "NCPBHop")

    // CustomOld Speed
    val customoldY by FloatValue("CustomY", 0.42f, 0f..4f) { mode.get() == "CustomOld" }
    val customoldGroundStrafe by FloatValue("CustomGroundStrafe", 1.6f, 0f..2f) { mode.get() == "CustomOld" }
    val customoldAirStrafe by FloatValue("CustomAirStrafe", 0f, 0f..2f) { mode.get() == "CustomOld" }
    val customoldGroundTimer by FloatValue("CustomGroundTimer", 1f, 0.1f..2f) { mode.get() == "CustomOld" }
    val customoldAirTimerTick by IntegerValue("CustomAirTimerTick", 5, 1..20) { mode.get() == "CustomOld" }
    val customoldAirTimer by FloatValue("CustomAirTimer", 1f, 0.1f..2f) { mode.get() == "CustomOld" }
    val resetXZ by BoolValue("ResetXZ", false) { mode.get() == "CustomOld" }
    val resetY by BoolValue("ResetY", false) { mode.get() == "CustomOld" }
    val notOnConsuming by BoolValue("NotOnConsuming", false) { mode.get() == "CustomOld" }
    val notOnFalling by BoolValue("NotOnFalling", false) { mode.get() == "CustomOld" }
    val notOnVoid by BoolValue("NotOnVoid", true) { mode.get() == "CustomOld" }

    // SilentTimer
    val silentprefix = "Silent-"
    val silentPacket by BoolValue("${silentprefix}Packet", false) { mode.get() == "SilentTimer" }
    val silentPacketValue by IntegerValue("Packet-Value", 20, 0..20) { mode.get() == "SilentTimer" && silentPacket}
    val silentPacketReset by IntegerValue("Packet-ResetDelay", 1000, 0..10000) { mode.get() == "SilentTimer" && silentPacket}
    val silenttimer by BoolValue("${silentprefix}Timer", false) { mode.get() == "SilentTimer" }
    val silenttimervalue by FloatValue("${silentprefix}TimerValue", 0.1f,0.1f..1.0f) { mode.get() == "SilentTimer" && silenttimer}
    val silentlowtimer by BoolValue("${silentprefix}LowTimer", false) { mode.get() == "SilentTimer" && silenttimer}
    val silentlowtimervalue by FloatValue("${silentprefix}LowTimerValue", 0.1f,0.1f..1.0f) { mode.get() == "SilentTimer" && silentlowtimer && silenttimer}
    val silentbasespeed by BoolValue("${silentprefix}BaseSpeed", false) { mode.get() == "SilentTimer" }
    val silentBaseSpeedValue by FloatValue("${silentprefix}BaseSpeedValue", 1f,0.1f..1f) { mode.get() == "SilentTimer" && silentbasespeed}
    val silentAddBaseSpeedValue by FloatValue("${silentprefix}AddBaseSpeedValue", 0.1f,0.1f..1f) { mode.get() == "SilentTimer" && silentbasespeed}


    // TeleportCubecraft Speed
    val cubecraftPortLength by FloatValue("CubeCraft-PortLength", 1f, 0.1f..2f) { mode.get() == "TeleportCubeCraft" }

    // IntaveHop14 Speed
    val boost by BoolValue("Boost", true) { mode.get() == "IntaveHop14" }
    val strafeStrength by FloatValue("StrafeStrength", 0.29f, 0.1f..0.29f) { mode.get() == "IntaveHop14" }
    val groundTimer by FloatValue("GroundTimer", 0.5f, 0.1f..5f) { mode.get() == "IntaveHop14" }
    val airTimer by FloatValue("AirTimer", 1.09f, 0.1f..5f) { mode.get() == "IntaveHop14" }

    // UNCPHopNew Speed
    private val pullDown by BoolValue("PullDown", true) { mode.get() == "UNCPHopNew" }
    val onTick by IntegerValue("OnTick", 5, 5..9) { pullDown && mode.get() == "UNCPHopNew" }
    val onHurt by BoolValue("OnHurt", true) { pullDown && mode.get() == "UNCPHopNew" }
    val shouldBoost by BoolValue("ShouldBoost", true) { mode.get() == "UNCPHopNew" }
    val timerBoost by BoolValue("TimerBoost", true) { mode.get() == "UNCPHopNew" }
    val damageBoost by BoolValue("DamageBoost", true) { mode.get() == "UNCPHopNew" }
    val lowHop by BoolValue("LowHop", true) { mode.get() == "UNCPHopNew" }
    val airStrafe by BoolValue("AirStrafe", true) { mode.get() == "UNCPHopNew" }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.isSneaking)
            return

        if (isMoving && !sprintManually)
            thePlayer.isSprinting = true

        modeModule.onUpdate()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.isSneaking || event.eventState != EventState.PRE)
            return

        if (isMoving && !sprintManually)
            thePlayer.isSprinting = true

        modeModule.onMotion()
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (mc.thePlayer?.isSneaking == true)
            return

        modeModule.onMove(event)
    }

    @EventTarget
    fun onTick(event: GameTickEvent) {
        if (mc.thePlayer?.isSneaking == true)
            return

        modeModule.onTick()
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (mc.thePlayer?.isSneaking == true)
            return

        modeModule.onStrafe()
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (mc.thePlayer?.isSneaking == true)
            return

        modeModule.onJump(event)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer?.isSneaking == true)
            return

        modeModule.onPacket(event)
    }

    override fun onEnable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1f

        modeModule.onEnable()
    }

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1f
        mc.thePlayer.speedInAir = 0.02f

        modeModule.onDisable()
    }

    override val tag
        get() = mode.get()

    private val modeModule
        get() = speedModes.find { it.modeName == mode.get() }!!

    private val sprintManually
        // Maybe there are more but for now there's the Legit mode.get().
        get() = modeModule in arrayOf(Legit)
}