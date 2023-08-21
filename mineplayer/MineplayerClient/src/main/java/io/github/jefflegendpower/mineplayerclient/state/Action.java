package io.github.jefflegendpower.mineplayerclient.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;

public class Action {

    private static final Set<Integer> currentKeyToggles = new HashSet<>();
    private static final Set<Integer> currentMouseToggles = new HashSet<>();
    private static double currentMouseMoveX;
    private static double currentMouseMoveY;

    private final Set<Integer> keyToggles;
    private final Set<Integer> mouseToggles;

    private final double mouseMoveX;
    private final double mouseMoveY;

    public Action(Set<Integer> keyToggles, Set<Integer> mouseToggles, double mouseMoveX, double mouseMoveY) {
        this.keyToggles = keyToggles;
        this.mouseToggles = mouseToggles;
        this.mouseMoveX = mouseMoveX;
        this.mouseMoveY = mouseMoveY;
    }

    public JsonObject toJson() {
        JsonObject body = new JsonObject();

        JsonArray keyTogglesJson = new JsonArray();
        for (int keyCode : keyToggles) keyTogglesJson.add(keyCode);

        JsonArray mouseTogglesJson = new JsonArray();
        for (int mouseCode : mouseToggles) mouseTogglesJson.add(mouseCode);

        JsonArray mouseMove = new JsonArray();
        mouseMove.add(mouseMoveX);
        mouseMove.add(mouseMoveY);

        body.add("key_toggles", keyTogglesJson);
        body.add("mouse_toggles", mouseTogglesJson);
        body.add("mouse_move", mouseMove);
        return body;
    }

    public Set<Integer> getKeyToggles() {
        return keyToggles;
    }

    public Set<Integer> getMouseToggles() {
        return mouseToggles;
    }

    public double getMouseMoveX() {
        return mouseMoveX;
    }

    public double getMouseMoveY() {
        return mouseMoveY;
    }

    public static Action getAction() {
        return new Action(currentKeyToggles, currentMouseToggles, currentMouseMoveX, currentMouseMoveY);
    }

    public static void keyToggle(int keyCode) {
        if (currentKeyToggles.contains(keyCode)) currentKeyToggles.remove(keyCode);
        else currentKeyToggles.add(keyCode);
    }

    public static void mouseToggle(int mouseCode) {
        if (currentMouseToggles.contains(mouseCode)) currentMouseToggles.remove(mouseCode);
        else currentMouseToggles.add(mouseCode);
    }

    public static void setMouseCoords(double x, double y) {
        currentMouseMoveX = x;
        currentMouseMoveY = y;
    }
}
