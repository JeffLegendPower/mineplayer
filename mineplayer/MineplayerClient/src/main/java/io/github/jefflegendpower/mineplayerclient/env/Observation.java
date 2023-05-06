package io.github.jefflegendpower.mineplayerclient.env;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.jefflegendpower.mineplayerclient.utils.StringByteUtils;
import org.apache.commons.codec.binary.Base64;

import java.util.Map;

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
        for (boolean entry : keyStates.values()) {
            keyStatesJson.add(entry ? 1 : 0);
        }

        JsonArray mouseButtonStatesJson = new JsonArray();
        for (boolean entry : mouseButtonStates.values())
            mouseButtonStatesJson.add(entry ? 1 : 0);

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
}
