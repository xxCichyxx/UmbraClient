/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.UmbraClient.CLIENT_AUTHOR
import net.ccbluex.liquidbounce.UmbraClient.CLIENT_NAME
import net.ccbluex.liquidbounce.UmbraClient.clientCommit
import net.ccbluex.liquidbounce.UmbraClient.clientVersionText
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura.blockStatus
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.ClientThemesUtils.getColor
import net.ccbluex.liquidbounce.utils.ClientThemesUtils.getGradientColor
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect2
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.item.ItemSword
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import kotlin.random.Random

/**
 * CustomHUD text element
 *
 * Allows to draw custom text
 */
@ElementInfo(name = "Text")
class Text(x: Double = 10.0, y: Double = 10.0, scale: Float = 1F, side: Side = Side.default(), private val index: Int = 0) : Element(x, y, scale, side) {

    companion object {

        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
        val HOUR_FORMAT = SimpleDateFormat("HH:mm")

        val DECIMAL_FORMAT = DecimalFormat("0.00")

        /**
         * Create default element
         */
        fun defaultClient2(): Text {
            val text = Text(x = 4.0, y = 4.0, scale = 1.60F)

            text.displayString = "Umbra Client"
            text.shadow = true
            text.textColorMode == "Theme"

            return text
        }

        fun defaultBPS(): Text {
            val text = Text(x = 4.0, y = 290.0, scale = 1.80F)

            text.displayString = "BPS: %bps%"
            text.shadow = true
            text.textColorMode == "Theme"

            return text
        }

    }


    private var displayString by TextValue("DisplayText", "")
    private val roundedRectRadius by FloatValue("Rounded-Radius", 3F, 0F..5F)

    private val textColorMode by ListValue("Text-Color", arrayOf("Custom", "ADHD", "Rainbow", "Gradient", "Theme","Theme2"), "Theme2")

    private var alpha by IntegerValue("Alpha", 255, 0..255) { textColorMode != "Rainbow" }
    private var red by IntegerValue("Red", 255, 0..255) { textColorMode == "Custom" && alpha > 0 }
    private var green by IntegerValue("Green", 255, 0..255) { textColorMode == "Custom" && alpha > 0 }
    private var blue by IntegerValue("Blue", 255, 0..255) { textColorMode == "Custom" && alpha > 0 }

    private val backgroundAlpha by IntegerValue("BackgroundAlpha", 0, 0..255)
    private val backgroundRed by IntegerValue("BackgroundRed", 0, 0..255) { backgroundAlpha > 0 }
    private val backgroundGreen by IntegerValue("BackgroundGreen", 0, 0..255) { backgroundAlpha > 0 }
    private val backgroundBlue by IntegerValue("BackgroundBlue", 0, 0..255) { backgroundAlpha > 0 }

    private val gradientTextSpeed by FloatValue("Text-Gradient-Speed", 1f, 0.5f..10f) { textColorMode == "Gradient" }
    private val themeTextSpeed by FloatValue("Text-Theme-Speed", 1f, 0.5f..10f) { textColorMode == "Theme2" }

    // TODO: Make Color picker to fix this mess :/
    private val gradientTextRed1 by FloatValue("Text-Gradient-R1", 255f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextGreen1 by FloatValue("Text-Gradient-G1", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextBlue1 by FloatValue("Text-Gradient-B1", 0f, 0f..255f) { textColorMode == "Gradient" }

    private val gradientTextRed2 by FloatValue("Text-Gradient-R2", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextGreen2 by FloatValue("Text-Gradient-G2", 255f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextBlue2 by FloatValue("Text-Gradient-B2", 0f, 0f..255f) { textColorMode == "Gradient" }

    private val gradientTextRed3 by FloatValue("Text-Gradient-R3", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextGreen3 by FloatValue("Text-Gradient-G3", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextBlue3 by FloatValue("Text-Gradient-B3", 255f, 0f..255f) { textColorMode == "Gradient" }

    private val gradientTextRed4 by FloatValue("Text-Gradient-R4", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextGreen4 by FloatValue("Text-Gradient-G4", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextBlue4 by FloatValue("Text-Gradient-B4", 0f, 0f..255f) { textColorMode == "Gradient" }

    private val rainbowX by FloatValue("Rainbow-X", -1000F, -2000F..2000F) { textColorMode == "Rainbow" }
    private val rainbowY by FloatValue("Rainbow-Y", -1000F, -2000F..2000F) { textColorMode == "Rainbow" }
    private val gradientX by FloatValue("Gradient-X", -500F, -2000F..2000F) { textColorMode == "Gradient" }
    private val gradientY by FloatValue("Gradient-Y", -1500F, -2000F..2000F) { textColorMode == "Gradient" }
    private val themeX by FloatValue("Theme-X", -500F, -2000F..2000F) { textColorMode == "Theme2" }
    private val themeY by FloatValue("Theme-Y", -1500F, -2000F..2000F) { textColorMode == "Theme2" }

    private var shadow by BoolValue("Shadow", true)
    private val font by FontValue("Font", Fonts.font40)

    private var editMode = false
    private var editTicks = 0
    private var prevClick = 0L

    private var displayText = display

    private val display: String
        get() {
            val textContent = if (displayString.isEmpty() && !editMode)
                "Text Element"
            else
                displayString


            return multiReplace(textContent)
        }

    private var color: Color
        get() = Color(red, green, blue, alpha)
        set(value) {
            red = value.red
            green = value.green
            blue = value.blue
            alpha = value.alpha
        }

    fun getReplacement(str: String): Any? {
        val thePlayer = mc.thePlayer

        if (thePlayer != null) {
            when (str.lowercase()) {
                "x" -> return DECIMAL_FORMAT.format(thePlayer.posX)
                "y" -> return DECIMAL_FORMAT.format(thePlayer.posY)
                "z" -> return DECIMAL_FORMAT.format(thePlayer.posZ)
                "xdp" -> return thePlayer.posX
                "ydp" -> return thePlayer.posY
                "zdp" -> return thePlayer.posZ
                "velocity" -> return DECIMAL_FORMAT.format(speed)
                "ping" -> return thePlayer.getPing()
                "health" -> return DECIMAL_FORMAT.format(thePlayer.health)
                "maxhealth" -> return DECIMAL_FORMAT.format(thePlayer.maxHealth)
                "yaw" -> return DECIMAL_FORMAT.format(thePlayer.rotationYaw)
                "pitch" -> return DECIMAL_FORMAT.format(thePlayer.rotationPitch)
                "yawint" -> return DECIMAL_FORMAT.format(thePlayer.rotationYaw).toInt()
                "pitchint" -> return DECIMAL_FORMAT.format(thePlayer.rotationPitch).toInt()
                "food" -> return thePlayer.foodStats.foodLevel
                "onground" -> return thePlayer.onGround
                "tbalance", "timerbalance" -> return "${TimerBalanceUtils.getBalance()}ms"
                "block", "blocking" -> return (thePlayer.heldItem?.item is ItemSword && (blockStatus || thePlayer.isUsingItem || thePlayer.isBlocking))
                "sneak", "sneaking" -> return (thePlayer.isSneaking || mc.gameSettings.keyBindSneak.isKeyDown)
                "sprint", "sprinting" -> return (thePlayer.serverSprintState || thePlayer.isSprinting || mc.gameSettings.keyBindSprint.isKeyDown)
                "inventory", "inv" -> return mc.currentScreen is GuiInventory || mc.currentScreen is GuiContainer
                "serverslot" -> return serverSlot
                "clientslot" -> return thePlayer.inventory?.currentItem
                "bps", "blockpersecond" -> return DECIMAL_FORMAT.format(BPSUtils.getBPS())
            }
        }

        return when (str.lowercase()) {
            "username" -> mc.session.username
            "clientname" -> CLIENT_NAME
            "clientversion" -> clientVersionText
            "clientcommit" -> clientCommit
            "clientauthor", "clientcreator" -> CLIENT_AUTHOR
            "fps" -> Minecraft.getDebugFPS()
            "date" -> DATE_FORMAT.format(System.currentTimeMillis())
            "time" -> HOUR_FORMAT.format(System.currentTimeMillis())
            "serverip" -> ServerUtils.remoteIp
            "cps", "lcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.LEFT)
            "mcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.MIDDLE)
            "rcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.RIGHT)
            "pps_sent" -> return PPSCounter.getPPS(PPSCounter.PacketType.SEND)
            "pps_received" -> return PPSCounter.getPPS(PPSCounter.PacketType.RECEIVED)
            else -> null // Null = don't replace
        }
    }

    private fun multiReplace(str: String): String {
        var lastPercent = -1
        val result = StringBuilder()
        for (i in str.indices) {
            if (str[i] == '%') {
                if (lastPercent != -1) {
                    if (lastPercent + 1 != i) {
                        val replacement = getReplacement(str.substring(lastPercent + 1, i))

                        if (replacement != null) {
                            result.append(replacement)
                            lastPercent = -1
                            continue
                        }
                    }
                    result.append(str, lastPercent, i)
                }
                lastPercent = i
            } else if (lastPercent == -1) {
                result.append(str[i])
            }
        }

        if (lastPercent != -1) {
            result.append(str, lastPercent, str.length)
        }

        if (textColorMode == "Theme") {
            val themeTextColor = getColor(index).rgb
        }

        return result.toString()
    }

    fun getRandomBrightColor(alpha: Int): Color {
        val red = Random.nextInt(128, 256) // Upewnij się, że czerwień jest w zakresie od 128 do 255
        val green = Random.nextInt(128, 256) // Upewnij się, że zieleń jest w zakresie od 128 do 255
        val blue = Random.nextInt(128, 256) // Upewnij się, że niebieski jest w zakresie od 128 do 255
        return Color(red, green, blue, alpha)
    }

    /**
     * Draw element
     */
    override fun drawElement(): Border {
        val rainbow = textColorMode == "Rainbow"
        val gradient = textColorMode == "Gradient"
        val random = textColorMode == "ADHD"
        val theme = if (textColorMode == "Theme") getColor(index).rgb else color.rgb
        val theme2 = textColorMode == "Theme2"

        val randomColor = getRandomBrightColor(255).rgb

        val colorToUse = when {
            random -> randomColor
            else -> color.rgb
        }

        if (backgroundAlpha > 0) {
            drawRoundedRect2(
                -2F,
                -2F,
                font.getStringWidth(displayText) + 2F,
                font.FONT_HEIGHT + 0F,
                Color(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha),
                roundedRectRadius
            )
        }

        val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
        val gradientX = if (gradientX == 0f) 0f else 1f / gradientX
        val gradientY = if (gradientY == 0f) 0f else 1f / gradientY

        GradientFontShader.begin(
            textColorMode == "Gradient",
            gradientX,
            gradientY,
            floatArrayOf(
                gradientTextRed1 / 255.0f,
                gradientTextGreen1 / 255.0f,
                gradientTextBlue1 / 255.0f,
                1.0f
            ),
            floatArrayOf(
                gradientTextRed2 / 255.0f,
                gradientTextGreen2 / 255.0f,
                gradientTextBlue2 / 255.0f,
                1.0f
            ),
            floatArrayOf(
                gradientTextRed3 / 255.0f,
                gradientTextGreen3 / 255.0f,
                gradientTextBlue3 / 255.0f,
                1.0f
            ),
            floatArrayOf(
                gradientTextRed4 / 255.0f,
                gradientTextGreen4 / 255.0f,
                gradientTextBlue4 / 255.0f,
                1.0f
            ),
            gradientTextSpeed,
            gradientOffset
        ).use {
            RainbowFontShader.begin(
                rainbow,
                if (rainbowX == 0f) 0f else 1f / rainbowX,
                if (rainbowY == 0f) 0f else 1f / rainbowY,
                System.currentTimeMillis() % 10000 / 10000F
            ).use {
                font.drawString(
                    displayText, 0F, 0F, if (rainbow)
                        0 else if (gradient) 0 else color.rgb, shadow
                )

                if (editMode && mc.currentScreen is GuiHudDesigner && editTicks <= 40) {
                    font.drawString(
                        "_", font.getStringWidth(displayText) + 2F,
                        0F, if (rainbow) ColorUtils.rainbow(400000000L).rgb else if (gradient) 0 else color.rgb, shadow
                    )
                }
            }
        }

        // Render Theme2 Gradient
        if (textColorMode == "Theme2") {
            val (color1, color2) = getGradientColor()
            val themeOffset = System.currentTimeMillis() % 10000 / 10000F
            val themeX = if (themeX == 0f) 0f else 1f / themeX
            val themeY = if (themeY == 0f) 0f else 1f / themeY

            GradientFontShader.begin(
                true,
                themeX,
                themeY,
                floatArrayOf(
                    color1.red / 255.0f,
                    color1.green / 255.0f,
                    color1.blue / 255.0f,
                    1.0f
                ),
                floatArrayOf(
                    color2.red / 255.0f,
                    color2.green / 255.0f,
                    color2.blue / 255.0f,
                    1.0f
                ),
                floatArrayOf(
                    color1.red / 255.0f,
                    color1.green / 255.0f,
                    color1.blue / 255.0f,
                    1.0f
                ),
                floatArrayOf(
                    color2.red / 255.0f,
                    color2.green / 255.0f,
                    color2.blue / 255.0f,
                    1.0f
                ),
                themeTextSpeed,
                themeOffset
            ).use {
                font.drawString(displayText, 0F, 0F, color.rgb, shadow)

                if (editMode && mc.currentScreen is GuiHudDesigner && editTicks <= 40) {
                    font.drawString(
                        "_", font.getStringWidth(displayText) + 2F,
                        0F, color.rgb, shadow
                    )
                }
            }
        }
        if (editMode && mc.currentScreen !is GuiHudDesigner) {
            editMode = false
            updateElement()
        }
        if(textColorMode == "Theme") {
            font.drawString(displayText, 0F, 0F, theme, shadow)
        }
        if(textColorMode == "ADHD") {
            font.drawString(displayText, 0F, 0F, colorToUse, shadow)
        }

        return Border(-2F, -2F, font.getStringWidth(displayText) + 2F, font.FONT_HEIGHT.toFloat())
    }

    override fun updateElement() {
        editTicks += 5
        if (editTicks > 80) editTicks = 0

        displayText = if (editMode) displayString else display
    }

    override fun handleMouseClick(x: Double, y: Double, mouseButton: Int) {
        if (isInBorder(x, y) && mouseButton == 0) {
            if (System.currentTimeMillis() - prevClick <= 250L)
                editMode = true

            prevClick = System.currentTimeMillis()
        } else {
            editMode = false
        }
    }

    override fun handleKey(c: Char, keyCode: Int) {
        if (editMode && mc.currentScreen is GuiHudDesigner) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (displayString.isNotEmpty())
                    displayString = displayString.dropLast(1)

                updateElement()
                return
            }

            if (ColorUtils.isAllowedCharacter(c) || c == '§')
                displayString += c

            updateElement()
        }
    }
}
