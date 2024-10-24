/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.ui.client.gui.GuiMainMenu
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerAddress
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.network.NetHandlerLoginClient
import net.minecraft.network.EnumConnectionState
import net.minecraft.network.NetworkManager
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.login.client.C00PacketLoginStart
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.net.InetAddress

@SideOnly(Side.CLIENT)
object ServerUtils : MinecraftInstance() {
    var serverData: ServerData? = null
    private val sessionTimer: MSTimer = MSTimer()

    @JvmOverloads
    fun connectToLastServer(noGLContext: Boolean = false) {
        if (serverData == null) return

        if (noGLContext) {
            Thread {
                // Code ported from GuiConnecting.connect
                // Used in AutoAccount's ReconnectDelay.
                // You cannot do this in the normal way because of required OpenGL context in current thread.
                // When you delay a call, it gets run in a new TimerThread.

                val serverAddress = ServerAddress.fromString(serverData!!.serverIP)
                mc.theWorld = null
                mc.setServerData(serverData)

                val inetAddress = InetAddress.getByName(serverAddress.ip)
                val networkManager = NetworkManager.createNetworkManagerAndConnect(
                    inetAddress,
                    serverAddress.port,
                    mc.gameSettings.isUsingNativeTransport
                )
                networkManager.netHandler = NetHandlerLoginClient(networkManager, mc, GuiMainMenu())

                networkManager.sendPacket(
                    C00Handshake(47, serverAddress.ip, serverAddress.port, EnumConnectionState.LOGIN, true)
                )

                networkManager.sendPacket(
                    C00PacketLoginStart(mc.session.profile)
                )
            }.start()
        } else mc.displayGuiScreen(GuiConnecting(GuiMultiplayer(GuiMainMenu()), mc, serverData))
    }

    val remoteIp: String
        get() {
            var serverIp = "Singleplayer"

            // This can throw NPE during LB startup, if an element has server ip in it
            if (mc.theWorld?.isRemote == true) {
                val serverData = mc.currentServerData
                if (serverData != null) serverIp = serverData.serverIP
            }

            return serverIp
        }

    fun formatSessionTime(): String {
        if (System.currentTimeMillis() - sessionTimer.time < 0L) sessionTimer.reset()

        val realTime =
            (System.currentTimeMillis() - sessionTimer.time).toInt() / 1000
        val hours = realTime / 3600
        val seconds = (realTime % 3600) % 60
        val minutes = (realTime % 3600) / 60

        return hours.toString() + "h " + minutes + "m " + seconds + "s"
    }
    fun isHypixelLobby(): Boolean {
        if (mc.theWorld == null) return false

        val target = "CLICK TO PLAY"
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity.name.startsWith("§e§l")) {
                if (entity.name == "§e§l$target") {
                    return true
                }
            }
        }
        return false
    }

    fun isHypixelDomain(s1: String): Boolean {
        var chars = 0
        val str = "www.hypixel.net"

        for (c in str.toCharArray()) {
            if (s1.contains(c.toString())) chars++
        }

        return chars == str.length
    }
}