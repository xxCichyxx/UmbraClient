/*
 * UmbraClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/xxCichyxx/UmbraClient
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.extensions.getFullName
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0BPacketAnimation
import net.minecraft.network.play.server.S13PacketDestroyEntities
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.network.play.server.S20PacketEntityProperties

object AntiBot : Module("AntiBot", Category.CLIENT, hideModule = false) {

    private val tab by BoolValue("Tab", false)
    private val tabMode by ListValue("TabMode", arrayOf("Equals", "Contains"), "Contains") { tab }

    private val entityID by BoolValue("EntityID", false)
    private val invalidUUID by BoolValue("InvalidUUID", false)
    private val color by BoolValue("Color", false)

    private val livingTime by BoolValue("LivingTime", false)
    private val livingTimeTicks by IntegerValue("LivingTimeTicks", 40, 1..200) { livingTime }

    private val capabilities by BoolValue("Capabilities", false)
    private val ground by BoolValue("Ground", false)
    private val air by BoolValue("Air", true)
    private val invalidGround by BoolValue("InvalidGround", false)
    private val swing by BoolValue("Swing", false)
    private val health by BoolValue("Health", false)
    private val derp by BoolValue("Derp", true)
    private val wasInvisible by BoolValue("WasInvisible", false)
    private val armor by BoolValue("Armor", false)
    private val ping by BoolValue("Ping", false)
    private val needHit by BoolValue("NeedHit", false)
    private val duplicateInWorld by BoolValue("DuplicateInWorld", false)
    private val duplicateInTab by BoolValue("DuplicateInTab", false)
    private val properties by BoolValue("Properties", true)

    private val alwaysInRadius by BoolValue("AlwaysInRadius", false)
    private val alwaysRadius by FloatValue("AlwaysInRadiusBlocks", 20f, 5f..30f) { alwaysInRadius }

    private val groundList = mutableSetOf<Int>()
    private val airList = mutableSetOf<Int>()
    private val invalidGroundList = mutableMapOf<Int, Int>()
    private val swingList = mutableSetOf<Int>()
    private val invisibleList = mutableListOf<Int>()
    private val propertiesList = mutableSetOf<Int>()
    private val hitList = mutableSetOf<Int>()
    private val notAlwaysInRadiusList = mutableSetOf<Int>()
    private val worldPlayerNames = mutableSetOf<String>()
    private val worldDuplicateNames = mutableSetOf<String>()
    private val tabPlayerNames = mutableSetOf<String>()
    private val tabDuplicateNames = mutableSetOf<String>()

    fun isBot(entity: EntityLivingBase): Boolean {
        // Check if entity is a player
        if (entity !is EntityPlayer)
            return false

        // Check if anti bot is enabled
        if (!handleEvents())
            return false

        // Anti Bot checks
        if (color && "§" !in entity.displayName.formattedText.replace("§r", ""))
            return true

        if (livingTime && entity.ticksExisted < livingTimeTicks)
            return true

        if (ground && entity.entityId !in groundList)
            return true

        if (air && entity.entityId !in airList)
            return true

        if (swing && entity.entityId !in swingList)
            return true

        if (health && (entity.health > 20F || entity.health < 0F))
            return true

        if (entityID && (entity.entityId >= 1000000000 || entity.entityId <= -1))
            return true

        if (derp && (entity.rotationPitch > 90F || entity.rotationPitch < -90F))
            return true

        if (wasInvisible && entity.entityId in invisibleList)
            return true

        if (properties && entity.entityId !in propertiesList)
            return true

        if (armor) {
            if (entity.inventory.armorInventory[0] == null && entity.inventory.armorInventory[1] == null &&
                entity.inventory.armorInventory[2] == null && entity.inventory.armorInventory[3] == null)
                return true
        }

        if (ping) {
            if (mc.netHandler.getPlayerInfo(entity.uniqueID)?.responseTime == 0 ||
                mc.netHandler.getPlayerInfo(entity.uniqueID)?.responseTime == null)
                return true
        }

        if (invalidUUID && mc.netHandler.getPlayerInfo(entity.uniqueID) == null) {
            return true
        }

        if (capabilities && (entity.isSpectator || entity.capabilities.isFlying || entity.capabilities.allowFlying
                    || entity.capabilities.disableDamage || entity.capabilities.isCreativeMode))
            return true

        if (needHit && entity.entityId !in hitList)
            return true

        if (invalidGround && invalidGroundList.getOrDefault(entity.entityId, 0) >= 10)
            return true

        if (tab) {
            val equals = tabMode == "Equals"
            val targetName = stripColor(entity.displayName.formattedText)

            val shouldReturn = mc.netHandler.playerInfoMap.any { networkPlayerInfo ->
                val networkName = stripColor(networkPlayerInfo.getFullName())
                if (equals) {
                    targetName == networkName
                } else {
                    networkName in targetName
                }
            }
            return !shouldReturn
        }

        if (duplicateInWorld) {
            for (player in mc.theWorld.playerEntities.filterNotNull()) {
                val playerName = player.name

                if (worldPlayerNames.contains(playerName)) {
                    worldDuplicateNames.add(playerName)
                } else {
                    worldPlayerNames.add(playerName)
                }
            }

            if (worldDuplicateNames.isNotEmpty()) {
                val duplicateCount = worldDuplicateNames.size
                if (mc.theWorld.playerEntities.count { it.name in worldDuplicateNames } > duplicateCount) {
                    return true
                }
            }
        }

        if (duplicateInTab) {
            for (networkPlayerInfo in mc.netHandler.playerInfoMap.filterNotNull()) {
                val playerName = stripColor(networkPlayerInfo.getFullName())

                if (tabPlayerNames.contains(playerName)) {
                    tabDuplicateNames.add(playerName)
                } else {
                    tabPlayerNames.add(playerName)
                }
            }

            if (tabDuplicateNames.isNotEmpty()) {
                val duplicateCount = tabDuplicateNames.size
                if (mc.netHandler.playerInfoMap.count { stripColor(it.getFullName()) in tabDuplicateNames } > duplicateCount) {
                    return true
                }
            }
        }

        if (alwaysInRadius && entity.entityId !in notAlwaysInRadiusList)
            return true

        return entity.name.isEmpty() || entity.name == mc.thePlayer.name
    }

    @EventTarget(ignoreCondition = true)
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return

        val packet = event.packet

        if (packet is S14PacketEntity) {
            val entity = packet.getEntity(mc.theWorld)

            if (entity is EntityPlayer) {
                if (entity.onGround && entity.entityId !in groundList)
                    groundList += entity.entityId

                if (!entity.onGround && entity.entityId !in airList)
                    airList += entity.entityId

                if (entity.onGround) {
                    if (entity.fallDistance > 0.0 || entity.posY == entity.prevPosY || !entity.isCollidedVertically) {
                        invalidGroundList.putIfAbsent(entity.entityId,
                            invalidGroundList.getOrDefault(entity.entityId, 0) + 1
                        )
                    }
                } else {
                    val currentVL = invalidGroundList.getOrDefault(entity.entityId, 0)

                    if (currentVL > 0) {
                        invalidGroundList.putIfAbsent(entity.entityId, currentVL - 1)
                    } else {
                        invalidGroundList.remove(entity.entityId)
                    }
                }

                if ((entity.isInvisible || entity.isInvisibleToPlayer(mc.thePlayer)) && entity.entityId !in invisibleList)
                    invisibleList += entity.entityId

                if (alwaysInRadius) {
                    val distance = mc.thePlayer.getDistanceToEntity(entity)

                    if (distance < alwaysRadius) {
                        if (entity.entityId in notAlwaysInRadiusList) {
                            notAlwaysInRadiusList.remove(entity.entityId)
                        }
                    } else {
                        if (entity.entityId !in notAlwaysInRadiusList) {
                            notAlwaysInRadiusList.add(entity.entityId)
                        }
                    }
                }
            }
        }

        if (packet is S0BPacketAnimation) {
            val entity = mc.theWorld.getEntityByID(packet.entityID)

            if (entity != null && entity is EntityLivingBase && packet.animationType == 0
                && entity.entityId !in swingList)
                swingList += entity.entityId
        }

        if (packet is S20PacketEntityProperties) {
            propertiesList += packet.entityId
        }

        if (packet is S13PacketDestroyEntities) {
            for (entityID in packet.entityIDs) {
                // Remove [entityID] from every list upon deletion
                groundList -= entityID
                airList -= entityID
                invalidGroundList -= entityID
                swingList -= entityID
                invisibleList -= entityID
                notAlwaysInRadiusList -= entityID
                propertiesList -= entityID
            }
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onAttack(e: AttackEvent) {
        val entity = e.targetEntity

        if (entity != null && entity is EntityLivingBase && entity.entityId !in hitList)
            hitList += entity.entityId
    }

    @EventTarget(ignoreCondition = true)
    fun onWorld(event: WorldEvent) {
        clearAll()
    }

    private fun clearAll() {
        hitList.clear()
        swingList.clear()
        groundList.clear()
        invalidGroundList.clear()
        invisibleList.clear()
        notAlwaysInRadiusList.clear()
        worldPlayerNames.clear()
        worldDuplicateNames.clear()
        tabPlayerNames.clear()
        tabDuplicateNames.clear()
    }

}