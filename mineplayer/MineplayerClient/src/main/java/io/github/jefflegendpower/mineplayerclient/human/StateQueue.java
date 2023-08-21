package io.github.jefflegendpower.mineplayerclient.human;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
import io.github.jefflegendpower.mineplayerclient.state.Action;
import io.github.jefflegendpower.mineplayerclient.state.Observation;
import io.github.jefflegendpower.mineplayerclient.utils.StringByteUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class StateQueue {

    private static final Identifier stepEnvIdentifier = MineplayerClient.mineplayerIdentifier("env_step");
    private static final AtomicBoolean checked = new AtomicBoolean(true);

    private static final AtomicBoolean active = new AtomicBoolean(false);
    private static final Queue<State> queue = new ConcurrentLinkedQueue<>();

    private static boolean initialized = false;
    private static final AtomicInteger terminationKey = new AtomicInteger(-1);
    private static AtomicReference<Observation> observation = new AtomicReference<>();
    private static Runnable onTermination;

    public static void initialize(int terminationKey,
                                  Supplier<Observation> getObservation,
                                  Supplier<Action> getAction,
                                  @Nullable Runnable onTerminate) {
        if (initialized) return;
        onTermination = onTerminate;

        StateQueue.terminationKey.set(terminationKey);

        ClientPlayNetworking.registerGlobalReceiver(stepEnvIdentifier,
                StateQueue::stepReceiver);

        ClientTickEvents.START_CLIENT_TICK.register((client) -> CompletableFuture.runAsync(() -> {
            if (!active.get()) return;
            observation.set(getObservation.get());
        }));

        ClientTickEvents.END_CLIENT_TICK.register((client) -> CompletableFuture.runAsync(() -> {

            if (!active.get() || observation == null) return;
            Action action = getAction.get();
            queue.add(new State(observation.get(), action));

            if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), terminationKey)) {
                active.set(false);
                if (onTermination != null) {
                    onTermination.run();
                }
            }

            if (!checked.get()) return;
            checked.set(false);
            ClientPlayNetworking.send(stepEnvIdentifier, generateStepPayload());
        }));

        initialized = true;
    }

    private static PacketByteBuf generateStepPayload() {
        JsonObject serverEnvStep = new JsonObject();
        JsonObject serverEnvStepBody = new JsonObject();
        serverEnvStep.addProperty("context", "step");
        serverEnvStep.add("body", serverEnvStepBody);

        net.minecraft.network.PacketByteBuf buf = PacketByteBufs.create();
        StringByteUtils.writeString(buf, new Gson().toJson(serverEnvStep));
        return buf;
    }

    private static void stepReceiver(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
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

        if (body.get("terminated").getAsBoolean()) {
            active.set(false);
            if (onTermination != null) {
                onTermination.run();
            }
        }

        checked.set(true);
//        ClientPlayNetworking.unregisterGlobalReceiver(stepEnvIdentifier);
    }

    public static void start() {
        active.set(true);
    }

    public static void stop() {
        active.set(false);
    }

    public static State poll() {
        return queue.poll();
    }

    public static int size() {
        return queue.size();
    }

    public record State(Observation observation, Action action) {}
}
