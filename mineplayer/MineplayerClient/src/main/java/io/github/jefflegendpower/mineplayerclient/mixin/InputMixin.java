package io.github.jefflegendpower.mineplayerclient.mixin;

import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
import io.github.jefflegendpower.mineplayerclient.state.Action;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(InputUtil.class)
public class InputMixin {

/**
 * @author JeffLegendPower
 * @reason Allows to inject virtual keyboard to be connected to the bot
 */
    @Overwrite()
    public static boolean isKeyPressed(long handle, int code) {
        if (hasEnv())
            return MineplayerClient.getVirtualKeyboard().getKeyState(code);
        else
            return GLFW.glfwGetKey(handle, code) == 1;
    }

    /**
     * @author JeffLegendPower
     * @reason Allows to inject virtual keyboard to be connected to the bot
     */
    @Overwrite()
    public static void setKeyboardCallbacks(long handle, GLFWKeyCallbackI keyCallback, GLFWCharModsCallbackI charModsCallback) {
        GLFWKeyCallbackI newKeyCallback = (window, key, scancode, action, mods) -> {
            Action.keyToggle(key);
            // get all the mod keys used and add them too
            if (mods != 0) {
                if ((mods & GLFW.GLFW_MOD_SHIFT) != 0) {
                    Action.keyToggle(GLFW.GLFW_KEY_LEFT_SHIFT);
                }
                if ((mods & GLFW.GLFW_MOD_CONTROL) != 0) {
                    Action.keyToggle(GLFW.GLFW_KEY_LEFT_CONTROL);
                }
                if ((mods & GLFW.GLFW_MOD_ALT) != 0) {
                    Action.keyToggle(GLFW.GLFW_KEY_LEFT_ALT);
                }
                if ((mods & GLFW.GLFW_MOD_SUPER) != 0) {
                    Action.keyToggle(GLFW.GLFW_KEY_LEFT_SUPER);
                }
            }

            if (keyCallback != null) {
                keyCallback.invoke(window, key, scancode, action, mods);
            }
        };

        GLFW.glfwSetKeyCallback(handle, newKeyCallback);
        GLFW.glfwSetCharModsCallback(handle, charModsCallback);

        MineplayerClient.getVirtualKeyboard().setKeyCallback(newKeyCallback);
        MineplayerClient.getVirtualKeyboard().setCharModsCallback(charModsCallback);
        MineplayerClient.getVirtualKeyboard().setWindow(handle);
    }

    /**
     * @author JeffLegendPower
     * @reason Allows to inject virtual mouse to be connected to the bot
     */
    @Overwrite()
    public static void setMouseCallbacks(long handle, GLFWCursorPosCallbackI cursorPosCallback, GLFWMouseButtonCallbackI mouseButtonCallback, GLFWScrollCallbackI scrollCallback, GLFWDropCallbackI dropCallback) {
        GLFWCursorPosCallbackI newCursorPosCallback = (window, xpos, ypos) -> {
            Action.setMouseCoords(xpos, ypos);
            if (cursorPosCallback != null) {
                cursorPosCallback.invoke(window, xpos, ypos);
            }
        };

        GLFWMouseButtonCallbackI newMouseButtonCallback = (window, button, action, mods) -> {
            Action.mouseToggle(button);
            if (mouseButtonCallback != null) {
                mouseButtonCallback.invoke(window, button, action, mods);
            }
        };

        GLFW.glfwSetCursorPosCallback(handle, newCursorPosCallback);
        GLFW.glfwSetMouseButtonCallback(handle, newMouseButtonCallback);
        GLFW.glfwSetScrollCallback(handle, scrollCallback);
        GLFW.glfwSetDropCallback(handle, dropCallback);

        MineplayerClient.getVirtualMouse().setCursorPosCallback(newCursorPosCallback);
        MineplayerClient.getVirtualMouse().setMouseButtonCallback(newMouseButtonCallback);
        MineplayerClient.getVirtualMouse().setScrollCallback(scrollCallback);
        MineplayerClient.getVirtualMouse().setDropCallback(dropCallback);
        MineplayerClient.getVirtualMouse().setWindow(handle);
    }

    /**
     * @author JeffLegendPower
     * @reason Allows to inject virtual mouse to be connected to the bot
     */
    @Overwrite()
    public static void setCursorParameters(long handler, int inputModeValue, double x, double y) {
        MineplayerClient.getVirtualMouse().setMouseX(x);
        MineplayerClient.getVirtualMouse().setMouseY(y);
        MineplayerClient.getVirtualMouse().setInputMode(GLFW.GLFW_CURSOR, inputModeValue);
        GLFW.glfwSetInputMode(handler, GLFW.GLFW_CURSOR, inputModeValue);
        GLFW.glfwSetCursorPos(handler, x, y);
    }

    private static boolean hasEnv() {
        return MineplayerClient.getEnv() != null;
    }
}
