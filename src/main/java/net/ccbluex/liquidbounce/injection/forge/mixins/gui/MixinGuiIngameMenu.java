/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.UmbraClient;
import net.ccbluex.liquidbounce.ui.client.gui.GuiTheme;
import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameMenu.class)
public abstract class MixinGuiIngameMenu extends MixinGuiScreen {

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo callbackInfo) {
        if (!mc.isIntegratedServerRunning()) {
            final GuiButton disconnectButton = buttonList.get(0);
            disconnectButton.xPosition = width / 2 + 2;
            disconnectButton.width = 98;
            disconnectButton.height = 20;
            this.buttonList.add(new GuiButton(1068,this.width / 2 - 100,this.height / 4 + 128 + 24,"Switcher"));
            this.buttonList.add(new GuiButton(1078, this.width / 2 - 100, this.height / 4 + 128 + 48, "Key Bind Manager"));
            this.buttonList.add(new GuiButton(16578, this.width / 2 - 100, this.height / 4 + 128, "Client Color"));
            buttonList.add(new GuiButton(1337, width / 2 - 100, height / 4 + 120 - 16, 98, 20, "Reconnect"));
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void actionPerformed(GuiButton button, CallbackInfo callbackInfo) {

        if(button.id == 16578) {
            mc.displayGuiScreen(new GuiTheme());
        }

        if (button.id == 1337) {
            mc.theWorld.sendQuittingDisconnectingPacket();
            ServerUtils.INSTANCE.connectToLastServer();
        }

        if (button.id == 1078) {
            mc.displayGuiScreen(UmbraClient.INSTANCE.getKeyBindManager());
        }

        if (button.id == 1068) {
            mc.displayGuiScreen(new GuiMultiplayer((GuiScreen) (Object) this));
        }
    }
}