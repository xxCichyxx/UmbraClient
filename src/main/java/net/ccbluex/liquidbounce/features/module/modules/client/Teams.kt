/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemArmor

object Teams : Module("Teams", Category.CLIENT, gameDetecting = false, hideModule = false) {

    private val scoreboard by BoolValue("ScoreboardTeam", true)
    private val color by BoolValue("Color", true)
    private val gommeSW by BoolValue("GommeSW", false)
    private val armorValue by BoolValue("ArmorColor", false)
    private val armorIndexValue = IntegerValue("ArmorIndex", 3, 0.. 3) { armorValue }

    /**
     * Check if [entity] is in your own team using scoreboard, name color or team prefix
     */
    fun isInYourTeam(entity: EntityLivingBase): Boolean {
        val thePlayer = mc.thePlayer ?: return false

        if (scoreboard && thePlayer.team != null && entity.team != null &&
                thePlayer.team.isSameTeam(entity.team))
            return true

        val displayName = thePlayer.displayName

        if (gommeSW && displayName != null && entity.displayName != null) {
            val targetName = entity.displayName.formattedText.replace("§r", "")
            val clientName = displayName.formattedText.replace("§r", "")
            if (targetName.startsWith("T") && clientName.startsWith("T"))
                if (targetName[1].isDigit() && clientName[1].isDigit())
                    return targetName[1] == clientName[1]
        }
        if (armorValue) {
            val entityPlayer = entity as EntityPlayer
            if (mc.thePlayer.inventory.armorInventory[armorIndexValue.get()] != null && entityPlayer.inventory.armorInventory[armorIndexValue.get()] != null) {
                val myHead = mc.thePlayer.inventory.armorInventory[armorIndexValue.get()]
                val myItemArmor = myHead!!.item!! as ItemArmor


                val entityHead = entityPlayer.inventory.armorInventory[armorIndexValue.get()]
                var entityItemArmor = myHead.item!! as ItemArmor

                if (myItemArmor.getColor(myHead) == entityItemArmor.getColor(entityHead!!)) {
                    return true
                }
            }
        }

        if (color && displayName != null && entity.displayName != null) {
            val targetName = entity.displayName.formattedText.replace("§r", "")
            val clientName = displayName.formattedText.replace("§r", "")
            return targetName.startsWith("§${clientName[1]}")
        }

        return false
    }

}
