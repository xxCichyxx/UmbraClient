/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.inventory.attackDamage
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK

object AutoWeapon : Module("AutoWeapon", Category.COMBAT, subjective = true, hideModule = false) {

    private val onlySword by BoolValue("OnlySword", false)

    private val spoof by BoolValue("SpoofItem", false)
        private val spoofTicks by IntegerValue("SpoofTicks", 10, 1..20) { spoof }

    private var attackEnemy = false

    private var ticks = 0

    @EventTarget
    fun onAttack(event: AttackEvent) {
        attackEnemy = true
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is C02PacketUseEntity && event.packet.action == ATTACK && attackEnemy) {
            attackEnemy = false

            // Find the best weapon in hotbar (#Kotlin Style)
            val (slot, _) = (0..8)
                .map { it to mc.thePlayer.inventory.getStackInSlot(it) }
                .filter { it.second != null && ((onlySword && it.second.item is ItemSword)
                        || (!onlySword && (it.second.item is ItemSword || it.second.item is ItemTool))) }
                .maxByOrNull { it.second.attackDamage } ?: return

            if (slot == mc.thePlayer.inventory.currentItem) // If in hand no need to swap
                return

            // Switch to best weapon
            if (spoof) {
                serverSlot = slot
                ticks = spoofTicks
            } else {
                mc.thePlayer.inventory.currentItem = slot
                mc.playerController.updateController()
            }

            // Resend attack packet
            sendPacket(event.packet)
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onUpdate(update: UpdateEvent) {
        // Switch back to old item after some time
        if (ticks > 0) {
            if (ticks == 1)
                serverSlot = mc.thePlayer.inventory.currentItem

            ticks--
        }
    }
}