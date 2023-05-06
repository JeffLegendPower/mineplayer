package io.github.jefflegendpower.mineplayerserver.env

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.github.jefflegendpower.mineplayerserver.MineplayerServer
import io.github.jefflegendpower.mineplayerserver.env.types.EnvType
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket
import net.minecraft.resources.ResourceLocation
import org.bukkit.entity.Player
import java.util.*

object EnvironmentFactory {

    private val registered = false

    private val envs = mutableMapOf<UUID, Environment>()

    // map of string and class of envtype so i can instantiate it and pass props
    private val registeredEnvTypes = mutableMapOf<String, Class<out EnvType>>()

    fun registerEnvType(id: String, envType: Class<out EnvType>) {
        registeredEnvTypes[id] = envType
    }

    fun register(plugin: MineplayerServer) {
        if (registered) {
            throw RuntimeException("EnvironmentFactory already registered")
        }

        val protocolManager = ProtocolLibrary.getProtocolManager()

        protocolManager.addPacketListener(object : PacketAdapter(
            plugin,
            ListenerPriority.NORMAL,
            PacketType.Play.Client.CUSTOM_PAYLOAD
        ) {
            override fun onPacketReceiving(event: PacketEvent) {
                if (event.packet.type !== PacketType.Play.Client.CUSTOM_PAYLOAD) {
                    return
                }

                val packet = ServerboundCustomPayloadPacket::class.java.cast(event.packet.handle)
                val player = event.player

                val returnBlock = when (packet.identifier) {
                    ResourceLocation(plugin.namespace, "env_init") -> init(packet.data, player)
                    ResourceLocation(plugin.namespace, "env_reset") -> reset(packet.data, player)
                    ResourceLocation(plugin.namespace, "env_step") -> step(packet.data, player)
                    ResourceLocation(plugin.namespace, "env_close") -> close(packet.data, player)
                    else -> {
                        if (packet.identifier.namespace != plugin.namespace) {
                            return
                        }
                        val response = JsonObject()
                        val responseBody = JsonObject()
                        response.addProperty("context", "error")
                        responseBody.addProperty("status", "unknown")
                        responseBody.addProperty("reason", "unknown packet identifier, got ${packet.identifier}")
                        response.add("body", responseBody)
                        val byteBuf = FriendlyByteBuf(Unpooled.buffer())
                        writeString(byteBuf, Gson().toJson(response))

                        byteBuf
                    }
                }

                val returnPacket = PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD,
                    ClientboundCustomPayloadPacket(packet.identifier, returnBlock))
                protocolManager.sendServerPacket(player, returnPacket)
            }
        })
    }

    private fun init(data: FriendlyByteBuf, player: Player): FriendlyByteBuf {
        try {
            val message = dataToString((data))
            println(message)
            val json = Gson().fromJson(message, JsonObject::class.java)
            val body = json.get("body") as JsonObject
            val envTypeJson = body.get("env_type").asString
            val props = body.get("props").asJsonObject

            val envType = createInstance(registeredEnvTypes[envTypeJson]!!, player, props)

            val environment = Environment(envType, player)
            envs[player.uniqueId] = environment

            val response = JsonObject()
            val responseBody = JsonObject()
            response.addProperty("context", "init")
            responseBody.addProperty("status", "success")
            response.add("body", responseBody)
            val byteBuf = FriendlyByteBuf(Unpooled.buffer())
            writeString(byteBuf, Gson().toJson(response))
            return byteBuf

        } catch (e: Exception) {
            val response = JsonObject()
            val responseBody = JsonObject()
            response.addProperty("context", "init")
            responseBody.addProperty("status", "error")
            responseBody.addProperty("reason", e.message)
            response.add("body", responseBody)
            val byteBuf = FriendlyByteBuf(Unpooled.buffer())
            writeString(byteBuf, Gson().toJson(response))
            e.printStackTrace()
            return byteBuf
        }
    }

    private fun reset(data: FriendlyByteBuf, player: Player): FriendlyByteBuf {
        try {
            val environment = envs[player.uniqueId] ?: throw RuntimeException("Environment not found")

            val response = JsonObject()
            val responseBody = environment.reset()

            response.addProperty("context", "reset")
            response.add("body", responseBody)
            val byteBuf = FriendlyByteBuf(Unpooled.buffer())
            writeString(byteBuf, Gson().toJson(response))
            return byteBuf
        } catch (e: Exception) {
            val response = JsonObject()
            val responseBody = JsonObject()
            response.addProperty("context", "reset")
            responseBody.addProperty("status", "error")
            responseBody.addProperty("reason", e.message)
            response.add("body", responseBody)
            val byteBuf = FriendlyByteBuf(Unpooled.buffer())
            writeString(byteBuf, Gson().toJson(response))
            e.printStackTrace()
            return byteBuf
        }
    }

    private fun step(data: FriendlyByteBuf, player: Player): FriendlyByteBuf {
        try {
            val environment = envs[player.uniqueId] ?: throw RuntimeException("Environment not found")

            val response = JsonObject()
            val responseBody = environment.step()

            response.addProperty("context", "step")
            response.add("body", responseBody)
            val byteBuf = FriendlyByteBuf(Unpooled.buffer())
            writeString(byteBuf, Gson().toJson(response))
            return byteBuf
        } catch (e: Exception) {
            val response = JsonObject()
            val responseBody = JsonObject()
            response.addProperty("context", "step")
            responseBody.addProperty("status", "error")
            responseBody.addProperty("reason", e.message)
            response.add("body", responseBody)
            val byteBuf = FriendlyByteBuf(Unpooled.buffer())
            writeString(byteBuf, Gson().toJson(response))
            e.printStackTrace()
            return byteBuf
        }
    }

    private fun close(data: FriendlyByteBuf, player: Player): FriendlyByteBuf {
        try {
            val environment = envs[player.uniqueId] ?: throw RuntimeException("Environment not found")

            val response = JsonObject()
            val responseBody = environment.close()

            envs.remove(player.uniqueId)

            response.addProperty("context", "close")
            response.add("body", responseBody)

            val byteBuf = FriendlyByteBuf(Unpooled.buffer())
            writeString(byteBuf, Gson().toJson(response))
            return byteBuf
        } catch (e: Exception) {
            val response = JsonObject()
            val responseBody = JsonObject()
            response.addProperty("context", "close")
            responseBody.addProperty("status", "error")
            responseBody.addProperty("reason", e.message)
            response.add("body", responseBody)
            val byteBuf = FriendlyByteBuf(Unpooled.buffer())
            writeString(byteBuf, Gson().toJson(response))
            e.printStackTrace()
            return byteBuf
        }
    }

    private fun createInstance(clazz: Class<out EnvType>, vararg args: Any?): EnvType {
        val constructor = clazz.constructors.firstOrNull {
            it.parameterTypes.size == args.size
        } ?: throw IllegalArgumentException("No matching constructor found")

        return constructor.newInstance(*args) as EnvType
    }

    // ONLY WORKS ONCE PER BYTE BUFFER
    private fun dataToString(data: FriendlyByteBuf): String {
        return String(data.readByteArray(), Charsets.UTF_8)
    }

    private fun writeString(byteBuf: FriendlyByteBuf, data: String) {
        byteBuf.writeByteArray(data.toByteArray(Charsets.UTF_8))
    }
}