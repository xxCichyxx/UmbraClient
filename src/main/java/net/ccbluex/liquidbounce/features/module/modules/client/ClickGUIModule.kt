package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.UmbraClient.clickGui
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.BlackStyle
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.input.Keyboard

object ClickGUIModule : Module("ClickGUI", Category.CLIENT, Keyboard.KEY_RSHIFT, canBeEnabled = false) {

    // Dodajemy opcję wyboru stylu
    private val style by object : ListValue("Style", arrayOf("Black", "Panel"), "Black") {
        override fun onChanged(oldValue: String, newValue: String) = updateStyle()
    }

    var scale by FloatValue("Scale", 0.8f, 0.5f..1.5f)
    val maxElements by IntegerValue("MaxElements", 15, 1..30)
    val fadeSpeed by FloatValue("FadeSpeed", 1f, 0.5f..4f)
    val scrolls by BoolValue("Scrolls", true)
    val spacedModules by BoolValue("SpacedModules", false)
    val panelsForcedInBoundaries by BoolValue("PanelsForcedInBoundaries", false)

    override fun onEnable() {
        updateStyle()
        mc.displayGuiScreen(clickGui)
    }

    // Aktualizacja stylu w zależności od wybranej opcji
    private fun updateStyle() {
        clickGui.style = when (style) {
            "Black" -> BlackStyle
            else -> return
        }
    }
}
