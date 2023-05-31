package io.github.jefflegendpower.mineplayerserver

import com.onarandombox.MultiverseCore.MultiverseCore
import io.github.jefflegendpower.mineplayerserver.assets.WorldRegistry
import io.github.jefflegendpower.mineplayerserver.env.EnvironmentFactory
import io.github.jefflegendpower.mineplayerserver.env.types.treechop.TreeChop
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class MineplayerServer : JavaPlugin() {

    val namespace = "mineplayer"

    lateinit var mvCore: MultiverseCore

    override fun onEnable() {
        // Plugin startup logic
        instance = this

        mvCore = Bukkit.getServer().pluginManager.getPlugin("Multiverse-Core") as MultiverseCore
        EnvironmentFactory.registerEnvType("treechop", TreeChop::class.java)
        EnvironmentFactory.register(this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
        // Delete all environment worlds
        WorldRegistry.getWorlds().forEach { world ->
            mvCore.mvWorldManager.deleteWorld(world, true)
            WorldRegistry.removeWorld(world)
        }
    }

    companion object {
        lateinit var instance: MineplayerServer
            private set
    }
}