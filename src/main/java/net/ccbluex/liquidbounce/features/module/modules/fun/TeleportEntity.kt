package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.entity.Entity
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer

object TeleportEntity : Module("TeleportEntity", Category.FUN) {

    val basicTypePrefix = "FakeTeleport-"

    private val fakeTeleportVillagers = BoolValue("${basicTypePrefix}Villagers", false)
    private val fakeTeleportPlayers = BoolValue("${basicTypePrefix}Players", false)
    private val fakeTeleportMobs = BoolValue("${basicTypePrefix}Mob", false)
    private val fakeTeleportAnimals = BoolValue("${basicTypePrefix}Animals", false)
    private val fakeTeleportNPCBlocksmc = BoolValue("${basicTypePrefix}BlocksmcNPC", false)

    // Opóźnienia dla różnych typów bytów
    private val teleportDelayVillager = FloatValue("${basicTypePrefix}VillagerDelay", 0.5f, 0.1f..3.0f)
    private val teleportDelayPlayer = FloatValue("${basicTypePrefix}PlayerDelay", 0.5f, 0.1f..3.0f)
    private val teleportDelayBlocksmcNPC = FloatValue("${basicTypePrefix}BlocksmcNPCDelay", 0.5f, 0.1f..3.0f)
    private val teleportDelayMob = FloatValue("${basicTypePrefix}MobDelay", 0.5f, 0.1f..3.0f)
    private val teleportDelayAnimal = FloatValue("${basicTypePrefix}AnimalDelay", 0.5f, 0.1f..3.0f)

    // Kolejki dla każdego typu bytu
    private val villagerQueue = mutableListOf<EntityVillager>()
    private val playerQueue = mutableListOf<EntityPlayer>()
    private val mobQueue = mutableListOf<EntityMob>()
    private val animalQueue = mutableListOf<EntityAnimal>()

    // Ostatnie czasy teleportacji
    private var lastVillagerTeleportTime = 0L
    private var lastPlayerTeleportTime = 0L
    private var lastMobTeleportTime = 0L
    private var lastAnimalTeleportTime = 0L

    private var wasDead = false // Flaga do śledzenia stanu śmierci gracza

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer
        val currentTime = System.currentTimeMillis()

        // Jeśli gracz jest martwy, czyścimy kolejki i ustawiamy flagę
        if (player == null || player.isDead) {
            clearAllQueues()
            wasDead = true
            return
        }

        // Jeśli gracz był martwy, a teraz zmartwychwstał, resetujemy flagę i kontynuujemy teleportację
        if (wasDead && !player.isDead) {
            wasDead = false
        }

        // Dodajemy byty do odpowiednich kolejek
        addEntitiesToQueues()

        // Teleportacja bytów z kolejkami i opóźnieniami
        if (villagerQueue.isNotEmpty() && currentTime - lastVillagerTeleportTime >= (teleportDelayVillager.get() * 1000).toLong()) {
            val entity = villagerQueue.removeAt(0)
            handleTeleportVillager(entity)
            villagerQueue.add(entity)
            lastVillagerTeleportTime = currentTime
        }

        if (playerQueue.isNotEmpty() && currentTime - lastPlayerTeleportTime >= (teleportDelayPlayer.get() * 1000).toLong()) {
            val entity = playerQueue.removeAt(0)
            handleTeleportPlayers(entity)
            playerQueue.add(entity)
            lastPlayerTeleportTime = currentTime
        }

        if (playerQueue.isNotEmpty() && currentTime - lastPlayerTeleportTime >= (teleportDelayBlocksmcNPC.get() * 1000).toLong()) {
            val entity = playerQueue.removeAt(0)
            handleTeleportPlayers2(entity)
            playerQueue.add(entity)
            lastPlayerTeleportTime = currentTime
        }

        if (mobQueue.isNotEmpty() && currentTime - lastMobTeleportTime >= (teleportDelayMob.get() * 1000).toLong()) {
            val entity = mobQueue.removeAt(0)
            handleTeleportMobs(entity)
            mobQueue.add(entity)
            lastMobTeleportTime = currentTime
        }

        if (animalQueue.isNotEmpty() && currentTime - lastAnimalTeleportTime >= (teleportDelayAnimal.get() * 1000).toLong()) {
            val entity = animalQueue.removeAt(0)
            handleTeleportAnimals(entity)
            animalQueue.add(entity)
            lastAnimalTeleportTime = currentTime
        }
    }

    // Funkcja dodająca byty do odpowiednich kolejek
    private fun addEntitiesToQueues() {
        val player = mc.thePlayer ?: return

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity != player) {
                when {
                    entity is EntityVillager && fakeTeleportVillagers.get() && !villagerQueue.contains(entity) -> villagerQueue.add(entity)
                    entity is EntityPlayer && fakeTeleportPlayers.get() && !playerQueue.contains(entity) -> playerQueue.add(entity)
                    entity is EntityPlayer && fakeTeleportNPCBlocksmc.get() && mc.netHandler.getPlayerInfo(entity.uniqueID) == null && !playerQueue.contains(entity) -> playerQueue.add(entity)
                    entity is EntityMob && fakeTeleportMobs.get() && !mobQueue.contains(entity) -> mobQueue.add(entity)
                    entity is EntityAnimal && fakeTeleportAnimals.get() && !animalQueue.contains(entity) -> animalQueue.add(entity)
                }
            }
        }
    }

    // Funkcje teleportujące poszczególne typy bytów
    private fun handleTeleportPlayers(entity: EntityPlayer) {
        entity.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
    }

    private fun handleTeleportPlayers2(entity: EntityPlayer) {
        if (mc.netHandler.getPlayerInfo(entity.uniqueID) == null) {
            entity.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
        }
    }

    private fun handleTeleportAnimals(entity: EntityAnimal) {
        entity.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
    }

    private fun handleTeleportVillager(entity: EntityVillager) {
        entity.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
    }

    private fun handleTeleportMobs(entity: EntityMob) {
        entity.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
    }

    // Czyszczenie wszystkich kolejek
    private fun clearAllQueues() {
        villagerQueue.clear()
        playerQueue.clear()
        mobQueue.clear()
        animalQueue.clear()
    }
}