package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.UmbraClient.CLIENT_NAME
import net.ccbluex.liquidbounce.UmbraClient.clientVersionText
import net.ccbluex.liquidbounce.features.module.modules.client.HUDModule.guiColor
import net.ccbluex.liquidbounce.ui.client.gui.button.ImageButton
import net.ccbluex.liquidbounce.ui.client.gui.button.QuitButton
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.Fonts.minecraftFont
import net.ccbluex.liquidbounce.utils.APIConnecter.canConnect
import net.ccbluex.liquidbounce.utils.APIConnecter.checkBugs
import net.ccbluex.liquidbounce.utils.APIConnecter.checkChangelogs
import net.ccbluex.liquidbounce.utils.APIConnecter.checkStatus
import net.ccbluex.liquidbounce.utils.APIConnecter.isLatest
import net.ccbluex.liquidbounce.utils.APIConnecter.loadDonors
import net.ccbluex.liquidbounce.utils.APIConnecter.loadPictures
import net.ccbluex.liquidbounce.utils.GitUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBloom
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawShadowRect
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.GuiModList
import java.awt.Color
import java.util.*

class GuiMainMenu : GuiScreen(), GuiYesNoCallback {
    private var logo: ResourceLocation? = null
    private lateinit var btnSinglePlayer: GuiButton
    private lateinit var btnMultiplayer: GuiButton
    private lateinit var btnClientOptions: GuiButton
    private lateinit var btnThemes: GuiButton
    private lateinit var btnServerStatus: GuiButton
    private lateinit var btnAltManager: GuiButton
    private lateinit var btnCommitInfo: ImageButton
    private lateinit var btnMinecraftOptions: ImageButton
    private lateinit var btnLanguage: ImageButton
    private lateinit var btnQuit: QuitButton

    override fun initGui() {
        logo = ResourceLocation("umbraclient/mainmenu/logo.png")
        val yPos = height - 30  // Pozycja dolna dla obrazków
        val buttonWidth = 133   // Szerokość przycisków
        val buttonHeight = 20
        val buttonSpacing = 8  // Odstęp między przyciskami


        // Ustawienie przycisków "Single Player", "Multi Player"
        btnSinglePlayer = GuiButton(0, width / 2 - buttonWidth - buttonSpacing / 2, height / 2, buttonWidth, buttonHeight, "SINGLE PLAYER")
        btnMultiplayer = GuiButton(1, width / 2 + buttonSpacing / 2, height / 2, buttonWidth, buttonHeight, "MULTIPLAYER")

        // Ustalamy Y dla nowych przycisków
        val additionalButtonY = height / 2 + buttonHeight + buttonSpacing  // Ustalamy Y dla nowych przycisków

        // Nowe przyciski: "THEMES", "SCRIPTS", "SERVER STATUS", "ALT MANAGER"

        btnThemes = GuiButton(3, width / 2 - buttonWidth - buttonSpacing / 2, additionalButtonY, buttonWidth, buttonHeight, "THEMES")
        btnServerStatus = GuiButton(5, width / 2 - buttonWidth - buttonSpacing / 2, additionalButtonY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight, "SERVER STATUS")
        btnAltManager = GuiButton(6, width / 2 + buttonSpacing / 2, additionalButtonY, buttonWidth, buttonHeight, "ALT MANAGER")

        // Przycisk "OPTIONS" pod innymi przyciskami
        btnClientOptions = GuiButton(2, width / 2 + buttonSpacing / 2, additionalButtonY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight, "MINECRAFT SETTINGS")

        btnCommitInfo = ImageButton(
            "COMMIT INFO",
            ResourceLocation("umbraclient/mainmenu/github.png"),
            width / 2 - 23,
            yPos
        )
        btnMinecraftOptions = ImageButton(
            "OPTIONS",
            ResourceLocation("umbraclient/mainmenu/cog.png"),
            width / 2 - 5,
            yPos
        )
        btnLanguage = ImageButton(
            "LANGUAGE",
            ResourceLocation("umbraclient/mainmenu/globe.png"),
            width / 2 + 13,
            yPos
        )
        btnQuit = QuitButton(width - 25, 10)

        // Dodanie przycisków do listy
        buttonList.addAll(listOf(btnSinglePlayer, btnMultiplayer, btnClientOptions, btnThemes, btnServerStatus, btnAltManager))
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int) {
        buttonList.forEach { guiButton ->
            if (guiButton.mousePressed(mc, mouseX, mouseY)) {
                actionPerformed(guiButton)
            }

            when {
                btnQuit.hoverFade > 0 -> mc.shutdown()
                btnMinecraftOptions.hoverFade > 0 -> mc.displayGuiScreen(GuiInfo(this))
                btnLanguage.hoverFade > 0 -> mc.displayGuiScreen(GuiLanguage(this, mc.gameSettings, mc.languageManager))
                btnCommitInfo.hoverFade > 0 -> mc.displayGuiScreen(GuiCommitInfo())
            }
        }
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(GuiSelectWorld(this))
            1 -> mc.displayGuiScreen(GuiMultiplayer(this))
            2 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            3 -> mc.displayGuiScreen(GuiTheme())
            5 -> mc.displayGuiScreen(GuiServerStatus(this))
            6 -> mc.displayGuiScreen(GuiAltManager(this))
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        GlStateManager.pushMatrix()
        drawShadowRect(
            (width / 2 - 150).toFloat(),
            (height / 2 - 80).toFloat(),
            (width / 2 + 150).toFloat(),
            (height / 2 + 95).toFloat(),
            15F,
            Color(44, 43, 43, 100).rgb.toFloat().toInt()
        )

        GlStateManager.disableAlpha()
        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.color(1.0f, 1.0f, 1.0f)
        mc.textureManager.bindTexture(logo)
        drawModalRectWithCustomSizedTexture(width / 2 - 65, height / 2 - 120, 0f, 0f, 130, 130, 130f, 130f)
        minecraftFont.drawStringWithShadow(
            CLIENT_NAME,
            (width - 10f - minecraftFont.getStringWidth(CLIENT_NAME)),
            (height - 30f),
            Color(255, 255, 255, 140).rgb
        )
        minecraftFont.drawStringWithShadow(
            "Twój Aktualny Build to $clientVersionText",
            (width - 10f - minecraftFont.getStringWidth("Twój Aktualny Build to $clientVersionText")),
            (height - 18f),
            Color(255, 255, 255, 140).rgb
        )

        GlStateManager.color(1f, 1f, 1f, 1f)
        Fonts.fontSmall.drawCenteredStringWithoutShadow(
            "Comunity BlockBypass YT",
            width.toFloat() / 2, height.toFloat() / 2 - 20, Color(255, 255, 255, 100).rgb
        )

        listOf(btnSinglePlayer, btnMultiplayer, btnClientOptions).forEach {
            it.drawButton(mc, mouseX, mouseY)
        }
        listOf(btnCommitInfo, btnMinecraftOptions, btnLanguage, btnQuit).forEach {
            it.drawButton(mouseX, mouseY)
        }

        Fonts.font35.drawString(
            ((CLIENT_NAME + "(" + GitUtils.gitBranch) + "/" + GitUtils.gitInfo.getProperty("git.commit.id.abbrev")) + ") | Minecraft 1.8.9",
            10,
            (this.height - 20).toFloat().toInt(),
            Color(255, 255, 255, 100).rgb
        )

        drawBloom(mouseX - 5, mouseY - 5, 10, 10, 16, Color(guiColor))

        GlStateManager.popMatrix()

        super.drawScreen(mouseX, mouseY, partialTicks)
    }
}
