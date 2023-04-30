package io.github.jefflegendpower.mineplayerserver.env.types

import com.google.gson.JsonObject
import org.bukkit.entity.Player

abstract class EnvType(val id: String, val player: Player) {

    /**
     * Initialize the environment for the player
     * @param props The properties to initialize the environment with (if any)
     * @return The return message to send to the client
     */
    abstract fun reset(props: JsonObject = JsonObject()): JsonObject

    /**
     * Step the environment for the player
     * @param props the properties to step the environment with (if any)
     * @return The return message to send to the client (including whether environment is terminated, reward, etc)
     */
    abstract fun step(props: JsonObject = JsonObject()): JsonObject


    /**
     * Close the environment for the player
     * @param props the properties to close the environment with (if any)
     * @return The return message to send to the client
     */
    abstract fun close(props: JsonObject = JsonObject()): JsonObject
}