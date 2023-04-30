package io.github.jefflegendpower.mineplayerserver.env

import com.google.gson.JsonObject

class EnvInitializer : EnvContextHandler {
    fun initialize(message: JsonObject): Boolean {
        if (message["context"].asString != "init")
            throw RuntimeException("Invalid context for env init: " + message["context"].asString)

        return true
    }
}