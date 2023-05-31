package io.github.jefflegendpower.mineplayerclient.env;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
import io.github.jefflegendpower.mineplayerclient.utils.StringByteUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

public class EnvCloser implements EnvContextHandler {

    private PrintWriter out;
    private Runnable disconnectTCP;

    public EnvCloser(PrintWriter out, Runnable disconnectTCP) {
        this.out = out;
        this.disconnectTCP = disconnectTCP;
    }

    private final Identifier closeEnvIdentifier = MineplayerClient.mineplayerIdentifier("env_close");

    private AtomicBoolean closed = new AtomicBoolean(false);

    // The env client will close the connection once the response message is sent
    public boolean close(String closeMessage) {
        try {
            MineplayerClient.getVirtualKeyboard().clearKeys();
            MineplayerClient.getVirtualMouse().resetMouse();

            Gson gson = new Gson();

            JsonObject envCloseMessage = gson.fromJson(closeMessage, JsonObject.class);
            if (!envCloseMessage.get("context").getAsString().equals("close"))
                throw new RuntimeException("Invalid context for env close: " + envCloseMessage.get("context").getAsString());

            ClientPlayNetworking.registerGlobalReceiver(closeEnvIdentifier, this::closeReceiver);

            ClientPlayNetworking.send(closeEnvIdentifier, generateClosePayload());

            while (!closed.get()) {
                Thread.sleep(100);
            }

            closed.set(false);
            System.out.println("1a");
            out.println(envClientResponse(true));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            out.println(envClientResponse(false));
            return false;
        } finally {
            try {
                disconnectTCP.run();
                MinecraftClient.getInstance().stop();
            } catch (Exception ignored) {}
        }
    }

    public void closeReceiver(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Gson gson = new Gson();
        String message = StringByteUtils.dataToString(buf);
        JsonObject envCloseResponse = gson.fromJson(message, JsonObject.class);
        if (!envCloseResponse.get("context").getAsString().equals("close"))
            throw new RuntimeException("Invalid context for close message, got: " + envCloseResponse.get("context").getAsString());

        JsonObject body = envCloseResponse.getAsJsonObject("body");

        if (body.get("status").getAsString().equals("success")) {
            System.out.println("Server successfully closed environment");
        } else {
            if (body.has("reason"))
                throw new RuntimeException("Server failed to close, reason: " + body.get("reason").getAsString());
            else
                throw new RuntimeException("Server failed to close, no reason provided");
        }

        closed.set(true);
        ClientPlayNetworking.unregisterGlobalReceiver(closeEnvIdentifier);
    }

    private PacketByteBuf generateClosePayload() {
        JsonObject serverEnvClose = new JsonObject();
        JsonObject serverEnvCloseBody = new JsonObject();
        serverEnvClose.addProperty("context", "close");
        serverEnvClose.add("body", serverEnvCloseBody);

        PacketByteBuf buf = PacketByteBufs.create();
        StringByteUtils.writeString(buf, new Gson().toJson(serverEnvClose));
        return buf;
    }

    private JsonObject envClientResponse(boolean success) {
        JsonObject envClientResponse = new JsonObject();
        JsonObject envClientResponseBody = new JsonObject();
        envClientResponse.addProperty("context", "close");
        envClientResponseBody.addProperty("status", success ? "success" : "failure");
        envClientResponse.add("body", envClientResponseBody);
        return envClientResponse;
    }
}
