package io.github.jefflegendpower.mineplayerclient.inputs;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
import org.lwjgl.glfw.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualMouse {

    private long window;

    private AtomicDouble mouseX = new AtomicDouble();
    private AtomicDouble mouseY = new AtomicDouble();

    private AtomicDouble scrollX = new AtomicDouble();
    private AtomicDouble scrollY = new AtomicDouble();

    private int inputMode = GLFW.GLFW_CURSOR;
    private int inputModeValue;

    private ConcurrentHashMap<Integer, Boolean> mouseButtonStates = new ConcurrentHashMap<>();

    // CALL ALL THESE ON MAIN THREAD
    private GLFWCursorPosCallbackI cursorPosCallback;
    private GLFWMouseButtonCallbackI mouseButtonCallback;
    private GLFWScrollCallbackI scrollCallback;
    private GLFWDropCallbackI dropCallback; // This is to be called when a file is dropped on the window (not to be implemented yet)

    public void setButtonState(int button, boolean state) {
        mouseButtonStates.put(button, state);

        if (mouseButtonCallback != null)
            mouseButtonCallback.invoke(window, button, state ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE, MineplayerClient.getVirtualKeyboard().getMods());
    }

    public void getButtonState(int button) {
        mouseButtonStates.getOrDefault(button, false);
    }

    public void toggleButtonState(int button) {
        if (mouseButtonStates.containsKey(button)) {
            setButtonState(button, !mouseButtonStates.get(button));
        } else {
            setButtonState(button, true);
        }
    }

    public Map<Integer, Boolean> getButtonStates() {
        return mouseButtonStates;
    }

    public void scroll(double x, double y) {
        this.scrollX.addAndGet(x);
        this.scrollY.addAndGet(y);

        if (scrollCallback != null)
            scrollCallback.invoke(window, x, y);
    }

    public void moveMouse(double x, double y) {
        double mouseX = this.mouseX.addAndGet(x);
        double mouseY = this.mouseY.addAndGet(y);

        cursorPosCallback.invoke(window, mouseX, mouseY);
    }

    public void resetMouse() {
        this.mouseX.set(0);
        this.mouseY.set(0);

        cursorPosCallback.invoke(window, 0, 0);
    }

    public void setMouseX(double mouseX) {
        moveMouse(-this.mouseX.get() + mouseX, 0);
    }

    public void setMouseY(double mouseY) {
        moveMouse(0, -this.mouseY.get() + mouseY);
    }

    public double getMouseX() {
        return mouseX.get();
    }

    public double getMouseY() {
        return mouseY.get();
    }

    // https://www.glfw.org/docs/3.3/glfw3_8h.html#ae3bbe2315b7691ab088159eb6c9110fc
    // https://www.glfw.org/docs/3.3/group__input.html#gaa92336e173da9c8834558b54ee80563b
    public void setInputMode(int mode, int value) {
        this.inputMode = mode;
        this.inputModeValue = value;
    }

    public void setCursorPosCallback(GLFWCursorPosCallbackI cursorPosCallback) {
        this.cursorPosCallback = cursorPosCallback;
    }

    public void setMouseButtonCallback(GLFWMouseButtonCallbackI mouseButtonCallback) {
        this.mouseButtonCallback = mouseButtonCallback;
    }

    public void setScrollCallback(GLFWScrollCallbackI scrollCallback) {
        this.scrollCallback = scrollCallback;
    }

    public void setDropCallback(GLFWDropCallbackI dropCallback) {
        this.dropCallback = dropCallback;
    }

    public void setWindow(long window) {
        this.window = window;
    }

    public long getWindow() {
        return window;
    }


}
