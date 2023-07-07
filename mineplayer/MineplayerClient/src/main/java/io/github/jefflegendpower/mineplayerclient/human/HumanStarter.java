package io.github.jefflegendpower.mineplayerclient.human;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
import io.github.jefflegendpower.mineplayerclient.human.screen.HumanStartScreen;
import io.github.jefflegendpower.mineplayerclient.utils.StringByteUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.client.gui.screen.ConnectScreen.connect;

public class HumanStarter {

    private final Identifier startEnvIdentifier = MineplayerClient.mineplayerIdentifier("env_init");
    private AtomicBoolean started = new AtomicBoolean(false);

    public boolean start(String startMessage) {
        try {
            JsonObject envstartMessage = new Gson().fromJson(startMessage, JsonObject.class);
            JsonObject body = connectToServer(envstartMessage);

            ClientPlayNetworking.registerGlobalReceiver(startEnvIdentifier, this::startReceiver);

            // Timeout after 10 seconds
            int timeout = 0;
            boolean connected = false;
            while (!connected && timeout < 30) {
                Thread.sleep(1000);
                try {
                    ClientPlayNetworking.send(startEnvIdentifier, generateStartPayload(
                            body.get("env_type").getAsString(),
                            body.get("props").getAsJsonObject()));
                    connected = true;
                } catch (IllegalStateException e) {
                    // This is thrown when the client is not connected to a server
                    // We will just try again
                }
                timeout++;
            }
            if (!connected)
                throw new RuntimeException("Failed to connect to server");

            while (!started.get()) {
                Thread.sleep(100);
            }

            started.set(false);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public JsonObject generateClientStartMessage(boolean success) {
        JsonObject startMessage = new JsonObject();
        startMessage.addProperty("context", "start");
        JsonObject body = new JsonObject();
        body.addProperty("status", success ? "success" : "failure");
        startMessage.add("body", body);
        return startMessage;
    }

    private void startReceiver(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Gson gson = new Gson();
        String message = StringByteUtils.dataToString(buf);
        JsonObject envstartResponse = gson.fromJson(message, JsonObject.class);
        if (!envstartResponse.get("context").getAsString().equals("start"))
            throw new RuntimeException("Invalid context for start message, got: " + envstartResponse.get("context").getAsString());
        JsonObject body = envstartResponse.getAsJsonObject("body");
        if (body.get("status").getAsString().equals("success")) {
            System.out.println("Server successfully initialized environment");
        } else {
            if (body.has("reason"))
                throw new RuntimeException("Server failed to initialize environment, reason: " + body.get("reason").getAsString());
            else
                throw new RuntimeException("Server failed to initialize environment, no reason provided");
        }

        started.set(true);
        ClientPlayNetworking.unregisterGlobalReceiver(startEnvIdentifier);
    }

    // returns the body of the start message
    private JsonObject connectToServer(JsonObject startMessage) {
        JsonObject body = startMessage.getAsJsonObject("body");

        if (!startMessage.get("context").getAsString().equals("start"))
            throw new RuntimeException("Invalid context for start message, got: " + startMessage.get("context").getAsString());

        String address = body.get("address").getAsString();
        int port = body.get("port").getAsInt();

        if (MinecraftClient.getInstance().getNetworkHandler() != null &&
                MinecraftClient.getInstance().getNetworkHandler().getConnection() != null) {

            MinecraftClient.getInstance().getNetworkHandler().getConnection().disconnect(
                    MutableText.of(new LiteralTextContent("Disconnected by Mineplayer")));
        }

        AtomicBoolean connected = new AtomicBoolean(false);

        for (int attempt = 0; attempt < 3; attempt++) {
            MineplayerClient.runOnMainThread(() -> {
                connect(
                        MinecraftClient.getInstance().currentScreen,
                        MinecraftClient.getInstance(),
                        new ServerAddress(address, port),
                        new ServerInfo("MineplayerServer", address + ":" + port, false));

                connected.set(true);
            });

            for (int timeout = 0; timeout < 30; timeout++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (MinecraftClient.getInstance().getNetworkHandler() != null &&
                        MinecraftClient.getInstance().getNetworkHandler().getConnection() != null) {
                    break;
                }
            }

            if (MinecraftClient.getInstance().getNetworkHandler() != null &&
                    MinecraftClient.getInstance().getNetworkHandler().getConnection() != null) {
                break;
            } else MineplayerClient.runOnMainThread(() -> MinecraftClient.getInstance().disconnect());
        }

        while (!connected.get()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return body;
    }

    private PacketByteBuf generateStartPayload(String envType, JsonObject props) {
        JsonObject serverEnvStart = new JsonObject();
        JsonObject serverEnvStartBody = new JsonObject();
        serverEnvStart.addProperty("context", "start");
        serverEnvStartBody.addProperty("env_type", envType);
        serverEnvStartBody.add("props", props);
        serverEnvStart.add("body", serverEnvStartBody);

        PacketByteBuf byteBuf = PacketByteBufs.create();

        StringByteUtils.writeString(byteBuf, new Gson().toJson(serverEnvStart));

        return byteBuf;
    }
}
