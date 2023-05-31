package io.github.jefflegendpower.mineplayerserver.env.types.treechop

import com.google.gson.JsonObject
import io.github.jefflegendpower.mineplayerserver.MineplayerServer
import io.github.jefflegendpower.mineplayerserver.assets.WorldRegistry
import io.github.jefflegendpower.mineplayerserver.env.types.EnvType
import io.github.jefflegendpower.mineplayerserver.utils.LobbyWorld
import io.github.jefflegendpower.mineplayerserver.utils.runOnMainThread
import io.github.jefflegendpower.mineplayerserver.utils.supplyFromMainThread
import io.github.jefflegendpower.mineplayerserver.utils.unzip
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import java.io.File
import java.io.FileInputStream
import java.util.*

class TreeChop(player: Player, props: JsonObject) : EnvType("treechop", player) {

    private val uid = UUID.randomUUID()
    private val worldName = "treechop_$uid"
    private val worldManager = MineplayerServer.instance.mvCore.mvWorldManager
    private var terminated = true
    private var closed = false
    private var logGoal = props.get("log_goal").asInt

    override fun bootstrap() {
        val targetDir = MineplayerServer.instance.server.worldContainer

//        MineplayerServer.instance.saveResource("treechop_template", true)
        val treechopTemplate = supplyFromMainThread {
            MineplayerServer.instance.saveResource("treechop_template.zip", true)
            return@supplyFromMainThread File(MineplayerServer.instance.dataFolder, "treechop_template.zip")
        }

        if (File(targetDir, "treechop_template").exists())
            File(targetDir, "treechop_template").deleteRecursively()

        unzip(FileInputStream(treechopTemplate), targetDir.toPath())

        runOnMainThread {
            try {
                MineplayerServer.instance.mvCore.mvWorldManager.addWorld(
                    "treechop_template", World.Environment.NORMAL,
                    null, null, null, null, true)
            } catch (e: Exception) {
                // World already exists so ignore
            }
        }
    }

    override fun reset(props: JsonObject): JsonObject {
        try {
            val returnMessage = JsonObject()

            val result = supplyFromMainThread {

                if (worldManager.getMVWorld(worldName) != null)
                    delAndRemoveWorldSafe(worldName, player)

                worldManager.cloneWorld(
                    "treechop_template",
                    worldName
                ).let { if (!it) throw RuntimeException("Failed to clone world") }
                WorldRegistry.addWorld(worldName)

                worldManager.loadWorld(worldName).let { if (!it) throw RuntimeException("Failed to load world $worldName") }
                val world = worldManager.getMVWorld(worldName) ?: throw RuntimeException("World $worldName not found")

                val spawnLocation = world.spawnLocation
                spawnLocation.set(-215.4, 64.0, 71.1)

                return@supplyFromMainThread player.teleportAsync(spawnLocation)
            }

            result.join()

            returnMessage.addProperty("status", "success")
            terminated = false
            return returnMessage
        } catch (e: Exception) {
            e.printStackTrace()
            val returnMessage = JsonObject()
            returnMessage.addProperty("status", "error")
            returnMessage.addProperty("reason", e.message)
            return returnMessage
        }

    }

    override fun step(props: JsonObject): JsonObject {
        try {
            if (terminated) throw RuntimeException("Environment is terminated")
            if (closed) throw RuntimeException("Environment is closed")

            val returnMessage = JsonObject()

            val reward = rewardFunc()
            val shouldTerminate = shouldTerminate()

            if (shouldTerminate) terminated = true

            returnMessage.addProperty("status", "success")
            returnMessage.addProperty("reward", reward)
            returnMessage.addProperty("terminated", shouldTerminate)
            return returnMessage
        } catch (e: Exception) {
            e.printStackTrace()
            val returnMessage = JsonObject()
            returnMessage.addProperty("status", "error")
            returnMessage.addProperty("reason", e.message)
            return returnMessage
        }
    }

    override fun close(props: JsonObject): JsonObject {
        try {
            if (closed) throw RuntimeException("Environment is already closed")

            val returnMessage = JsonObject()

            delAndRemoveWorldSafe(worldName, player)

            returnMessage.addProperty("status", "success")

            terminated = false
            closed = true

            return returnMessage


        } catch (e: Exception) {
            e.printStackTrace()
            val returnMessage = JsonObject()
            returnMessage.addProperty("status", "error")
            returnMessage.addProperty("reason", e.message)
            return returnMessage
        }
    }

    private fun delAndRemoveWorldSafe(worldName: String, player: Player) {
        runOnMainThread {
            LobbyWorld.getSpawnLocation().let { player.teleportAsync(it) }
            worldManager.deleteWorld(worldName, true)
        }
        WorldRegistry.removeWorld(worldName)
    }

    private var oldWoodInInv = 0

    private fun rewardFunc(): Double {
        val woodInInv = supplyFromMainThread {
            var woodInInv = 0
            player.inventory.forEach { item ->
                if (item != null && isLog(item.type)) {
                    woodInInv += item.amount
                }
            }
            return@supplyFromMainThread woodInInv
        }

        val reward = (woodInInv - oldWoodInInv).toDouble()
        oldWoodInInv = woodInInv
        return reward
    }

    private fun shouldTerminate(): Boolean {
        val woodInInv = supplyFromMainThread {
            var woodInInv = 0
            player.inventory.forEach { item ->
                if (item != null && isLog(item.type)) {
                    woodInInv += item.amount
                }
            }
            return@supplyFromMainThread woodInInv
        }
        return woodInInv >= logGoal
    }

    private fun isLog(material: Material): Boolean {
        return when (material) {
            Material.OAK_LOG,
            Material.SPRUCE_LOG,
            Material.BIRCH_LOG,
            Material.JUNGLE_LOG,
            Material.ACACIA_LOG,
            Material.DARK_OAK_LOG,
            Material.CRIMSON_STEM,
            Material.WARPED_STEM -> true
            else -> false
        }
    }
}