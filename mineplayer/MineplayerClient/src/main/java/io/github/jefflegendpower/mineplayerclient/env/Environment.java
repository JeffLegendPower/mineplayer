package io.github.jefflegendpower.mineplayerclient.env;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
import io.github.jefflegendpower.mineplayerclient.inputs.VideoInput;
import io.github.jefflegendpower.mineplayerclient.state.Observation;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class Environment {

    private boolean active = false;

    private EnvInitializer envInitializer;
    private EnvResetter envResetter;
    private EnvStepper envStepper;
    private EnvCloser envCloser;

    private PrintWriter out;

    private VideoInput videoInput;

    public Environment(PrintWriter out, OutputStream outputStream, Runnable disconnectTCP) {
        envInitializer = new EnvInitializer();
        envResetter = new EnvResetter(out, outputStream, this::getObservation);
        envStepper = new EnvStepper(out, outputStream, this::getObservation);
        envCloser = new EnvCloser(out, disconnectTCP);
        this.out = out;
    }

    public boolean initialize(String initMessage) {
        try {
            Gson gson = new Gson();
            boolean success = envInitializer.initialize(initMessage);
            JsonObject init = gson.fromJson(initMessage, JsonObject.class);
            JsonObject body = init.getAsJsonObject("body");

            int width = body.get("window_width").getAsInt();
            int height = body.get("window_height").getAsInt();

            videoInput = new VideoInput(width, height);

            MineplayerClient.runOnMainThread(() -> videoInput.initialize());

            out.println(envInitializer.generateClientInitMessage(success));

            active = success;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            out.println(envInitializer.generateClientInitMessage(false));
            return false;
        }
    }

    public boolean close(String closeMessage) {
        boolean closed = envCloser.close(closeMessage);
        MineplayerClient.runOnMainThread(() -> {
            videoInput.cleanup();
        });
        active = false;
        return closed;
    }

    // NOTE blocking call
    public boolean handleMessage(String message) {
        try {
            Gson gson = new Gson();
            JsonObject messageObject = gson.fromJson(message, JsonObject.class);
            String context = messageObject.get("context").getAsString();

            switch (context) {
                case "init":
                    return initialize(message);
                case "reset":
                    return envResetter.reset(message);
                case "step":
                    return envStepper.step(message);
                case "close":
                    return close(message);
                default:
                    throw new RuntimeException("Invalid context: " + context);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Observation getObservation() {
        return Observation.getObservation(videoInput, active);
    }

    public boolean isActive() {
        return active;
    }
}
