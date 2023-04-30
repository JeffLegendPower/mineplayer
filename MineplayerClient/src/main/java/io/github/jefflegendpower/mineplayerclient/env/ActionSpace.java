package io.github.jefflegendpower.mineplayerclient.env;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;

import java.util.ArrayList;
import java.util.List;

public class ActionSpace {
    private List<Integer> keyPresses;
    private List<Integer> mousePresses;

    private double mouseMoveX;
    private double mouseMoveY;

    // RUN ON MAIN THREAD
    public void execute() {
        for (Integer key : keyPresses) {
            MineplayerClient.getVirtualKeyboard().toggleKeyState(key);
        }
        for (Integer button : mousePresses) {
            MineplayerClient.getVirtualMouse().toggleButtonState(button);
        }
        // TODO: mouse movement
    }

    public static ActionSpace fromJson(String actionSpaceString) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(actionSpaceString, JsonObject.class);

        // parse key mappings
        List<Integer> keyPresses = new ArrayList<>();
        JsonArray keyArray = jsonObject.getAsJsonArray("key_presses");
        for (JsonElement element : keyArray) {
            keyPresses.add(element.getAsInt());
        }

        // parse mouse mappings
        List<Integer> mousePresses = new ArrayList<>();
        JsonArray mouseArray = jsonObject.getAsJsonArray("mouse_presses");
        for (JsonElement element : mouseArray) {
            mousePresses.add(element.getAsInt());
        }

        JsonObject mouseMoveObject = jsonObject.getAsJsonObject("mouse_move");
        double mouseMoveX = mouseMoveObject.get("x").getAsDouble();
        double mouseMoveY = mouseMoveObject.get("y").getAsDouble();

        return new ActionSpace(keyPresses, mousePresses, mouseMoveX, mouseMoveY);
    }

    public ActionSpace(List<Integer> keyPresses, List<Integer> mousePresses, double mouseMoveX, double mouseMoveY) {
        this.keyPresses = keyPresses;
        this.mousePresses = mousePresses;
        this.mouseMoveX = mouseMoveX;
        this.mouseMoveY = mouseMoveY;
    }

    public List<Integer> getKeyPresses() {
        return keyPresses;
    }

    public List<Integer> getMousePresses() {
        return mousePresses;
    }
}
