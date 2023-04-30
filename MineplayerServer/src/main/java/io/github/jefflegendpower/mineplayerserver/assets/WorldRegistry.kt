package io.github.jefflegendpower.mineplayerserver.assets

import de.leonhard.storage.Json
import de.leonhard.storage.Yaml
import de.leonhard.storage.internal.FlatFile
import io.github.jefflegendpower.mineplayerserver.MineplayerServer

object WorldRegistry {

    private val worlds = mutableListOf<String>()
    fun getWorlds(): List<String> = worlds.toList()

    private val worldsRegistry: FlatFile

    init {
        val dataFolder = MineplayerServer.instance.dataFolder
        if (!dataFolder.exists())
            dataFolder.mkdirs()

        worldsRegistry = Json("worlds", dataFolder.absolutePath)
        worldsRegistry.setDefault("worlds", mutableListOf<String>())
        worldsRegistry.getStringList("worlds")?.let { worlds.addAll(it) }
    }

    fun addWorld(world: String) {
        worlds.add(world)
        worldsRegistry.set("worlds", worlds)
    }

    fun worldRegistered(world: String): Boolean {
        return worlds.contains(world)
    }

    fun removeWorld(world: String) {
        worlds.remove(world)
        worldsRegistry.set("worlds", worlds)
    }

    fun clear() {
        worlds.clear()
        worldsRegistry.set("worlds", worlds)
    }
}