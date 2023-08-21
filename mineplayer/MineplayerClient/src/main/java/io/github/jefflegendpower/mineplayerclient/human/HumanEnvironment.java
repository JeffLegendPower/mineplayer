package io.github.jefflegendpower.mineplayerclient.human;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
import io.github.jefflegendpower.mineplayerclient.inputs.VideoInput;
import io.github.jefflegendpower.mineplayerclient.state.Action;
import io.github.jefflegendpower.mineplayerclient.state.Observation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.stat.Stat;
import org.lwjgl.glfw.GLFW;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HumanEnvironment {

    private AtomicBoolean active = new AtomicBoolean(false);
    private boolean firstTime = true;

    private PrintWriter out;
    private OutputStream outputStream;
    private Runnable disconnectTCP;

    private HumanStarter humanStarter;
    private HumanResetter humanResetter;

    private VideoInput videoInput;

    private AtomicBoolean stateSenderComplete = new AtomicBoolean(false);

    private Runnable stateSender = () -> {
        JsonObject startMessage = new JsonObject();
        startMessage.addProperty("context", "state");
        JsonObject startBody = new JsonObject();
        startBody.addProperty("stage", "start");
        startMessage.add("body", startBody);
        out.println(startMessage);

        while (active.get() || StateQueue.size() > 0) {
            sendState();
        }

        JsonObject endMessage = new JsonObject();
        endMessage.addProperty("context", "state");
        JsonObject endBody = new JsonObject();
        endBody.addProperty("stage", "end");
        endMessage.add("body", endBody);
        out.println(endMessage);
        stateSenderComplete.set(true);
    };

    public HumanEnvironment(PrintWriter out, OutputStream outputStream, Runnable disconnectTCP) {
        this.out = out;
        this.outputStream = outputStream;
        this.disconnectTCP = disconnectTCP;
        this.humanStarter = new HumanStarter();
        this.humanResetter = new HumanResetter(out);
    }

    public void start(String startMessage) {
         try {
             boolean success = humanStarter.start(startMessage);

             JsonObject init = new Gson().fromJson(startMessage, JsonObject.class);
             JsonObject body = init.getAsJsonObject("body");

             int width = body.get("window_width").getAsInt();
             int height = body.get("window_height").getAsInt();
             videoInput = new VideoInput(width, height);

             MineplayerClient.runOnMainThread(() -> videoInput.initialize());

             StateQueue.initialize(GLFW.GLFW_KEY_ESCAPE,
                     () -> Observation.getObservation(videoInput, active.get()),
                     Action::getAction,
                     () -> {
                 active.set(false);
                 CompletableFuture.runAsync(() -> {
//                     while (stateSender.isAlive()) {
                     while (!stateSenderComplete.get()) {
                         try {
                             Thread.sleep(100);
                         } catch (InterruptedException e) {
                             e.printStackTrace();
                         }
                     }

                     active.set(true);
                     boolean success2 = humanResetter.reset(firstTime);

                     System.out.println("start statesender 1");
                     stateSenderComplete.set(false);
                     CompletableFuture.runAsync(stateSender).thenRun(() -> stateSenderComplete.set(true));
//                     stateSender.start();
                     System.out.println("end statesender 1");

                     firstTime = false;
                     if (!success2) MinecraftClient.getInstance().close();
                 });


             });

             out.println(humanStarter.generateClientStartMessage(success));

             if (success) {
                 active.set(true);

                 success = humanResetter.reset(firstTime);
                 if (!success) MinecraftClient.getInstance().close();

                 System.out.println("start statesender 2");
                 stateSenderComplete.set(false);
                 CompletableFuture.runAsync(stateSender).thenRun(() -> stateSenderComplete.set(true));
//                 stateSender.start();
                 System.out.println("end statesender 2");

                 firstTime = false;
             } else {
                 MinecraftClient.getInstance().close();
             }

         } catch (Exception e) {
             e.printStackTrace();
             out.println(humanStarter.generateClientStartMessage(false));
         }
    }

    private int numStateSends = 0;
    private boolean sendState() {
        try {
            JsonObject message = new JsonObject();
            message.addProperty("context", "state");
            JsonObject body = new JsonObject();
            StateQueue.State state = StateQueue.poll();
            if (state != null) {
                Observation observation = state.observation();
                Action action = state.action();
                body.addProperty("stage", "game");
                body.add("observation", observation.toJson());
                body.add("action", action.toJson());
                message.add("body", body);
                out.println(message);
                out.println(observation.getFrameBase64());
                System.out.println("quamp 2 ey");
                return true;
            } else {
                if (numStateSends++ % 10000 == 0) System.out.println("quamp 2 nay");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
