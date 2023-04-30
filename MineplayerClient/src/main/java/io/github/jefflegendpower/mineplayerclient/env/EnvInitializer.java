package io.github.jefflegendpower.mineplayerclient.env;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
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

import static net.minecraft.client.gui.screen.ConnectScreen.connect;

public class EnvInitializer implements EnvContextHandler {

    private final Identifier initEnvIdentifier = MineplayerClient.mineplayerIdentifier("env_init");
    private AtomicBoolean initialized = new AtomicBoolean(false);

    // Note that this function is blocking and will only respond once the server has responded
    public boolean initialize(String initMessage) {
        try {
            Gson gson = new Gson();
            JsonObject envInitMessage = gson.fromJson(initMessage, JsonObject.class);
            JsonObject body = connectToServer(envInitMessage);

            ClientPlayNetworking.registerGlobalReceiver(initEnvIdentifier, this::initReceiver);

            // Timeout after 10 seconds
            int timeout = 0;
            boolean connected = false;
            while (!connected && timeout < 30) {
                Thread.sleep(1000);
                try {
                    ClientPlayNetworking.send(initEnvIdentifier, generateInitPayload(
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

            while (!initialized.get()) {
                Thread.sleep(100);
            }

            initialized.set(false);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String generateClientInitMessage(boolean success) {
        JsonObject initMessage = new JsonObject();
        initMessage.addProperty("context", "init");
        JsonObject body = new JsonObject();
        body.addProperty("status", success ? "success" : "failure");
        initMessage.add("body", body);
        return initMessage.toString();
    }

    private void initReceiver(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Gson gson = new Gson();
        String message = StringByteUtils.dataToString(buf);
        JsonObject envInitResponse = gson.fromJson(message, JsonObject.class);
        if (!envInitResponse.get("context").getAsString().equals("init"))
            throw new RuntimeException("Invalid context for init message, got: " + envInitResponse.get("context").getAsString());
        JsonObject body = envInitResponse.getAsJsonObject("body");
        if (body.get("status").getAsString().equals("success")) {
            System.out.println("Server successfully initialized environment");
        } else {
            if (body.has("reason"))
                throw new RuntimeException("Server failed to initialize environment, reason: " + body.get("reason").getAsString());
            else
                throw new RuntimeException("Server failed to initialize environment, no reason provided");
        }

        initialized.set(true);
        ClientPlayNetworking.unregisterGlobalReceiver(initEnvIdentifier);
    }

    // returns the body of the init message
    private JsonObject connectToServer(JsonObject initMessage) {
        JsonObject body = initMessage.getAsJsonObject("body");

        if (!initMessage.get("context").getAsString().equals("init"))
            throw new RuntimeException("Invalid context for init message, got: " + initMessage.get("context").getAsString());

        String address = body.get("address").getAsString();
        int port = body.get("port").getAsInt();

        if (MinecraftClient.getInstance().getNetworkHandler() != null &&
                MinecraftClient.getInstance().getNetworkHandler().getConnection() != null) {

            MinecraftClient.getInstance().getNetworkHandler().getConnection().disconnect(
                    MutableText.of(new LiteralTextContent("Disconnected by Mineplayer")));
        }

        AtomicBoolean connected = new AtomicBoolean(false);



        MineplayerClient.runOnMainThread(() -> {
            connect(
                    MinecraftClient.getInstance().currentScreen,
                    MinecraftClient.getInstance(),
                    new ServerAddress(address, port),
                    new ServerInfo("MineplayerServer", address + ":" + port, false));
            connected.set(true);
        });

        while (!connected.get()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return body;
    }

    private PacketByteBuf generateInitPayload(String envType, JsonObject props) {
        JsonObject serverEnvInit = new JsonObject();
        JsonObject serverEnvInitBody = new JsonObject();
        serverEnvInit.addProperty("context", "init");
        serverEnvInitBody.addProperty("env_type", envType);
        serverEnvInitBody.add("props", props);
        serverEnvInit.add("body", serverEnvInitBody);

        PacketByteBuf byteBuf = PacketByteBufs.create();

        StringByteUtils.writeString(byteBuf, new Gson().toJson(serverEnvInit));

        return byteBuf;
    }
}
