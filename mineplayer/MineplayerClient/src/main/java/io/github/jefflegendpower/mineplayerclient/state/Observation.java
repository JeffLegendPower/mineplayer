package io.github.jefflegendpower.mineplayerclient.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
import io.github.jefflegendpower.mineplayerclient.inputs.VideoInput;
import io.github.jefflegendpower.mineplayerclient.utils.StringByteUtils;
import org.apache.commons.codec.binary.Base64;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Observation {

    private Map<Integer, Boolean> keyStates;
    private Map<Integer, Boolean> mouseButtonStates;
    private double currentMouseX;
    private double currentMouseY;
    private byte[] frame;
    private int viewportWidth;
    private int viewportHeight;

    public Observation(Map<Integer, Boolean> keyStates, Map<Integer, Boolean> mouseButtonStates,
                       double currentMouseX, double currentMouseY,
                       int viewportWidth, int viewportHeight, byte[] frame) {
        this.keyStates = keyStates;
        this.mouseButtonStates = mouseButtonStates;
        this.currentMouseX = currentMouseX;
        this.currentMouseY = currentMouseY;
        this.frame = frame;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();

        JsonArray keyStatesJson = new JsonArray();
        for (Map.Entry<Integer, Boolean> entry : keyStates.entrySet())
            if (entry.getValue()) keyStatesJson.add(entry.getKey());

        JsonArray mouseButtonStatesJson = new JsonArray();
        for (Map.Entry<Integer, Boolean> entry : mouseButtonStates.entrySet())
            if (entry.getValue()) mouseButtonStatesJson.add(entry.getKey());

        JsonArray currentMousePos = new JsonArray();
        currentMousePos.add(currentMouseX);
        currentMousePos.add(currentMouseY);

        jsonObject.add("key_states", keyStatesJson);
        jsonObject.add("mouse_states", mouseButtonStatesJson);
        jsonObject.add("mouse_pos", currentMousePos);

        JsonObject viewportInfo = new JsonObject();
        viewportInfo.addProperty("width", viewportWidth);
        viewportInfo.addProperty("height", viewportHeight);
        viewportInfo.addProperty("encoded_length", getFrameBase64().length());
        jsonObject.add("viewport_info", viewportInfo);

        return jsonObject;
    }

    public byte[] getFrame() {
        return frame;
    }

    public String getFrameBase64() {
        return Base64.encodeBase64String(frame);
    }

    public Map<Integer, Boolean> getKeyStates() {
        return keyStates;
    }

    public Map<Integer, Boolean> getMouseButtonStates() {
        return mouseButtonStates;
    }

    public double getMouseX() {
        return currentMouseX;
    }

    public double getMouseY() {
        return currentMouseY;
    }

    public static Observation getObservation(VideoInput videoInput, boolean active) {
        if (!active) return null;
        ByteBuffer buffer = ByteBuffer.allocateDirect(videoInput.bufferSize());
        AtomicInteger success = new AtomicInteger(0);
        RenderSystem.recordRenderCall(() -> {
            try {
                videoInput.getRGBFrame(buffer);
                success.set(1);
            } catch (Exception e) {
                e.printStackTrace();
                success.set(2);
            }
        });

        while (success.get() == 0 && success.get() != 2) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        buffer.position(0);
        byte[] rgb = new byte[buffer.capacity()];
        buffer.get(rgb);
        return new Observation(
                MineplayerClient.getVirtualKeyboard().getKeyStates(),
                MineplayerClient.getVirtualMouse().getButtonStates(),
                MineplayerClient.getVirtualMouse().getMouseX(),
                MineplayerClient.getVirtualMouse().getMouseY(),
                videoInput.getWidth(),
                videoInput.getHeight(),
                rgb
        );
    }
}
