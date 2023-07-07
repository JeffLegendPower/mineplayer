package io.github.jefflegendpower.mineplayerclient.human;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
import io.github.jefflegendpower.mineplayerclient.human.screen.HumanResetScreen;
import io.github.jefflegendpower.mineplayerclient.human.screen.HumanStartScreen;
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
import java.util.concurrent.atomic.AtomicInteger;

public class HumanResetter {

    private PrintWriter out;

    public HumanResetter(PrintWriter out) {
        this.out = out;
    }

    private final Identifier resetEnvIdentifier = MineplayerClient.mineplayerIdentifier("env_reset");

    private AtomicBoolean reset = new AtomicBoolean(false);

    public boolean reset(boolean firstTime) {
        try {
            ClientPlayNetworking.registerGlobalReceiver(resetEnvIdentifier, this::resetReceiver);

            ClientPlayNetworking.send(resetEnvIdentifier, generateResetPayload());

            while (!reset.get()) {
                Thread.sleep(100);
            }

            reset.set(false);

            // TODO add gui to confirm start/stop stuff
            AtomicInteger responded = new AtomicInteger(0);
            MineplayerClient.runOnMainThread(() -> {
                if (firstTime) {
                    MinecraftClient.getInstance().setScreen(new HumanStartScreen(
                            () -> responded.set(1),
                            () -> responded.set(2)
                    ));
                } else {
                    MinecraftClient.getInstance().setScreen(new HumanResetScreen(
                            () -> responded.set(1),
                            () -> responded.set(2)
                    ));
                }
            });

            while (responded.get() == 0)
                Thread.sleep(100);

            if (responded.get() == 1) {
                out.println(envClientResponse(true));
                StateQueue.start();
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("context", "reset");
                JsonObject body = new JsonObject();
                body.addProperty("status", "user_cancelled");
                response.add("body", body);
                out.println(response);
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            out.println(envClientResponse(false));
            return false;
        }
    }

    private void resetReceiver(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Gson gson = new Gson();
        String message = StringByteUtils.dataToString(buf);
        JsonObject envResetResponse = gson.fromJson(message, JsonObject.class);
        if (!envResetResponse.get("context").getAsString().equals("reset"))
            throw new RuntimeException("Invalid context for reset message, got: " + envResetResponse.get("context").getAsString());
        JsonObject body = envResetResponse.getAsJsonObject("body");
        if (body.get("status").getAsString().equals("success")) {
            System.out.println("Server successfully reset environment");
        } else {
            if (body.has("reason"))
                throw new RuntimeException("Server failed to reset environment, reason: " + body.get("reason").getAsString());
            else
                throw new RuntimeException("Server failed to reset environment, no reason provided");
        }


        reset.set(true);
        ClientPlayNetworking.unregisterGlobalReceiver(resetEnvIdentifier);
    }

    private PacketByteBuf generateResetPayload() {
        JsonObject serverEnvReset = new JsonObject();
        JsonObject serverEnvResetBody = new JsonObject();
        serverEnvReset.addProperty("context", "reset");
        serverEnvReset.add("body", serverEnvResetBody);

        PacketByteBuf byteBuf = PacketByteBufs.create();
        StringByteUtils.writeString(byteBuf, new Gson().toJson(serverEnvReset));
        return byteBuf;
    }

    private JsonObject envClientResponse(boolean success) {
        JsonObject clientEnvReset = new JsonObject();
        JsonObject clientEnvResetBody = new JsonObject();
        clientEnvReset.addProperty("context", "reset");
        clientEnvResetBody.addProperty("status", success ? "success" : "failure");

        clientEnvReset.add("body", clientEnvResetBody);

        return clientEnvReset;
    }
}