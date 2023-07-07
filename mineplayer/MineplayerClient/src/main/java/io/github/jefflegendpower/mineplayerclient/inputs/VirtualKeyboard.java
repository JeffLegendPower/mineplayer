package io.github.jefflegendpower.mineplayerclient.inputs;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class VirtualKeyboard {

    private long window;

    private final ConcurrentHashMap<Integer, Boolean> keyStates = new ConcurrentHashMap<>();

    // CALL ALL THESE ON MAIN THREAD
    // https://www.glfw.org/docs/3.3/group__input.html#ga1caf18159767e761185e49a3be019f8d
    // https://www.glfw.org/docs/3.3/group__input.html#ga2485743d0b59df3791c45951c4195265
    private GLFWKeyCallbackI keyCallback;
    // https://www.glfw.org/docs/3.3/group__input.html#ga0b7f4ad13c2b17435ff13b6dcfb4e43c
    private GLFWCharModsCallbackI charModsCallback;

    public void setKeyState(int key, boolean state) {
        keyStates.put(key, state);

        if (keyCallback != null) {
            keyCallback.invoke(window, key, GLFW.glfwGetKeyScancode(key), state ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE, getMods());
        }
        if (charModsCallback != null) {
            // For now do nothing
        }
    }

    public void toggleKeyState(int key) {
        if (keyStates.containsKey(key)) {
            setKeyState(key, !keyStates.get(key));
        } else {
            setKeyState(key, true);
        }
    }

    public void clearKeys() {
        for (Map.Entry<Integer, Boolean> entry : keyStates.entrySet()) {
            if (entry.getValue()) {
                setKeyState(entry.getKey(), false);
            }
        }
    }

    public boolean getKeyState(int key) {
        return keyStates.getOrDefault(key, false);
    }

    public Map<Integer, Boolean> getKeyStates() {
        return keyStates;
    }

    public void setKeyCallback(GLFWKeyCallbackI keyCallback) {
        this.keyCallback = keyCallback;
    }

    public void setCharModsCallback(GLFWCharModsCallbackI charModsCallback) {
        this.charModsCallback = charModsCallback;
    }

    public void setWindow(long window) {
        this.window = window;
    }

    public long getWindow() {
        return window;
    }

    public int getMods() {
        int mods = 0;
        if (getKeyState(GLFW.GLFW_KEY_LEFT_SHIFT) || getKeyState(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            mods |= GLFW.GLFW_MOD_SHIFT;
        }
        if (getKeyState(GLFW.GLFW_KEY_LEFT_CONTROL) || getKeyState(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            mods |= GLFW.GLFW_MOD_CONTROL;
        }
        if (getKeyState(GLFW.GLFW_KEY_LEFT_ALT) || getKeyState(GLFW.GLFW_KEY_RIGHT_ALT)) {
            mods |= GLFW.GLFW_MOD_ALT;
        }
        if (getKeyState(GLFW.GLFW_KEY_LEFT_SUPER) || getKeyState(GLFW.GLFW_KEY_RIGHT_SUPER)) {
            mods |= GLFW.GLFW_MOD_SUPER;
        }
        if (getKeyState(GLFW.GLFW_KEY_CAPS_LOCK)) {
            mods |= GLFW.GLFW_MOD_CAPS_LOCK;
        }
        if (getKeyState(GLFW.GLFW_KEY_NUM_LOCK)) {
            mods |= GLFW.GLFW_MOD_NUM_LOCK;
        }
        return mods;
    }
}
