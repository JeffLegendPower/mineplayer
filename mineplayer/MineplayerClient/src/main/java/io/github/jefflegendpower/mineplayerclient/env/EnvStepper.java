package io.github.jefflegendpower.mineplayerclient.env;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
import io.github.jefflegendpower.mineplayerclient.utils.StringByteUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.main.Main;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class EnvStepper implements EnvContextHandler {

    private PrintWriter out;
    private OutputStream outputStream;
    private Supplier<Observation> getObservation;

    public EnvStepper(PrintWriter out, OutputStream outputStream, Supplier<Observation> getObservation) {
        this.out = out;
        this.outputStream = outputStream;
        this.getObservation = getObservation;
    }

    private final Identifier stepEnvIdentifier = MineplayerClient.mineplayerIdentifier("env_step");

    private AtomicBoolean step = new AtomicBoolean(false);

    private AtomicBoolean terminated = new AtomicBoolean(false);
    private AtomicDouble reward = new AtomicDouble(0.0);

    public boolean step(String stepMessage) {
        try {
            Gson gson = new Gson();
            JsonObject envStepMessage = gson.fromJson(stepMessage, JsonObject.class);

            JsonObject body = envStepMessage.getAsJsonObject("body");

            JsonArray keyToggles = body.getAsJsonArray("key_toggles");
            JsonArray mouseToggles = body.getAsJsonArray("mouse_toggles");
            JsonObject mouseMovement = body.getAsJsonObject("mouse_move");
            ClientPlayNetworking.registerGlobalReceiver(stepEnvIdentifier, this::stepReceiver);

            MineplayerClient.runOnMainThread(() -> {
                for (int i = 0; i < keyToggles.size(); i++) {
                    int key_id = keyToggles.get(i).getAsInt();
                    MineplayerClient.getVirtualKeyboard().toggleKeyState(key_id);
                }

                for (int i = 0; i < mouseToggles.size(); i++) {
                    int button_id = mouseToggles.get(i).getAsInt();
                    MineplayerClient.getVirtualMouse().toggleButtonState(button_id);
                }

                int mouseMoveX = mouseMovement.get("x").getAsInt();
                int mouseMoveY = mouseMovement.get("y").getAsInt();
                MineplayerClient.getVirtualMouse().moveMouse(mouseMoveX, mouseMoveY);
                ClientPlayNetworking.send(stepEnvIdentifier, generateStepPayload());
            });

            while (!step.get()) {
                Thread.sleep(20); // refresh 50fps
            }

            step.set(false);
            out.println(envClientResponse(true, terminated.get(), reward.get()));
//            byte[] frame = getObservation.get().getFrame();
//            outputStream.write(frame, 0, frame.length);
//            outputStream.flush();

            String frame = getObservation.get().getFrameBase64();
            out.println(frame);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            out.println(envClientResponse(false, true, 0));
            return false;
        }
    }

    private void stepReceiver(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Gson gson = new Gson();
        String message = StringByteUtils.dataToString(buf);
        JsonObject envStepResponse = gson.fromJson(message, JsonObject.class);
        if (!envStepResponse.get("context").getAsString().equals("step"))
            throw new RuntimeException("Invalid context for step message, got: " + envStepResponse.get("context").getAsString());
        JsonObject body = envStepResponse.getAsJsonObject("body");
        if (body.get("status").getAsString().equals("success")) {
            System.out.println("Server successfully executed step");
        } else {
            if (body.has("reason"))
                throw new RuntimeException("Server failed to execute step, reason: " + body.get("reason").getAsString());
            else
                throw new RuntimeException("Server failed to execute step, no reason provided");
        }

        terminated.set(body.get("terminated").getAsBoolean());
        reward.set(body.get("reward").getAsDouble());

        step.set(true);
        ClientPlayNetworking.unregisterGlobalReceiver(stepEnvIdentifier);
    }

    private PacketByteBuf generateStepPayload() {
        JsonObject serverEnvStep = new JsonObject();
        JsonObject serverEnvStepBody = new JsonObject();
        serverEnvStep.addProperty("context", "step");
        serverEnvStep.add("body", serverEnvStepBody);

        PacketByteBuf buf = PacketByteBufs.create();
        StringByteUtils.writeString(buf, new Gson().toJson(serverEnvStep));
        return buf;
    }

    private JsonObject envClientResponse(boolean success, boolean terminated, double reward) {
        JsonObject clientEnvStep = new JsonObject();
        JsonObject clientEnvStepBody = new JsonObject();
        clientEnvStep.addProperty("context", "step");
        clientEnvStepBody.addProperty("status", success ? "success" : "failure");

        if (success) {
            // the window is handled by the python package using openCV and OBS
            Observation obs = getObservation.get();
            clientEnvStepBody.add("observation", obs.toJson());
            clientEnvStepBody.addProperty("terminated", terminated);
            clientEnvStepBody.addProperty("reward", reward);
        }

        clientEnvStep.add("body", clientEnvStepBody);
        return clientEnvStep;
    }
}
