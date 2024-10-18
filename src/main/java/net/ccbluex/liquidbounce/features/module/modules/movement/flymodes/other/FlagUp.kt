/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object FlagUp : FlyMode("FlagUp") {
	override fun onUpdate() {
		val player = mc.thePlayer ?: return
		val (x, y, z) = player

		// Wysyłamy pakiety tylko z modyfikacją osi Y
		sendPackets(
			C04PacketPlayerPosition(
				x,
				y + (if (mc.gameSettings.keyBindJump.isKeyDown) 1.5624 else 0.00000001) - if (mc.gameSettings.keyBindSneak.isKeyDown) 0.0624 else 0.00000002,
				z,
				true
			),
			C04PacketPlayerPosition(
				x,
				y - 6969,  // Duże przesunięcie w dół, aby "resetować" pozycję
				z,
				true
			)
		)

		// Ustawiamy motionY na większą wartość, aby gracz faktycznie ruszył się w górę
		player.motionY = 1.0 // To zapewnia faktyczne ruch w górę

		// Przesuwamy gracza lekko w górę
		player.setPosition(x, y + 0.0001, z)
	}
}
