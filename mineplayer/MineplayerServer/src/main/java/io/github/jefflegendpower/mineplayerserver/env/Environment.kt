package io.github.jefflegendpower.mineplayerserver.env

import com.google.gson.JsonObject
import io.github.jefflegendpower.mineplayerserver.env.types.EnvType
import org.bukkit.entity.Player

class Environment(private val envType: EnvType, private val player: Player) {

    val open = false

    // TODO: add props to reset method (might not be needed tbh)
    fun reset(): JsonObject {
        return envType.reset()
    }

    fun step(): JsonObject {
        return envType.step()
    }

    fun close(): JsonObject {
        return envType.close()
    }
}