package io.github.jefflegendpower.mineplayerserver.utils

import com.onarandombox.MultiverseCore.MultiverseCore
import de.leonhard.storage.Json
import de.leonhard.storage.internal.FlatFile
import io.github.jefflegendpower.mineplayerserver.MineplayerServer
import org.bukkit.Bukkit
import org.bukkit.World
import java.io.File
import java.io.FileInputStream
import java.util.Properties

object LobbyWorld {

    private var world = "world"

    private val worldsRegistry: FlatFile

    private val mvCore: MultiverseCore = MineplayerServer.instance.mvCore

    init {
        MineplayerServer.instance.dataFolder

        val serverFolder = MineplayerServer.instance.server.worldContainer
        if (!serverFolder.exists())
            serverFolder.mkdirs()

        val serverProps = Properties()
        serverProps.load(FileInputStream(serverFolder.absolutePath + File.separator + "server.properties"))

        worldsRegistry = Json("lobbyworld", serverFolder.absolutePath)

        worldsRegistry.setDefault("multiverse-world", serverProps.getProperty("level-name"))
        worldsRegistry.getString("multiverse-world")?.let { world = it }
    }

    // Will always be non-null because the level world is always loaded
    fun getWorld(): World {
        return mvCore.mvWorldManager.getMVWorld(world).cbWorld ?: return Bukkit.getWorld(world)!!
    }

    fun getSpawnLocation(): org.bukkit.Location {
        return getWorld().spawnLocation
    }
}