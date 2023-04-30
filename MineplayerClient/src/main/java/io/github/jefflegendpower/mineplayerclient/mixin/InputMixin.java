package io.github.jefflegendpower.mineplayerclient.mixin;

import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;
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
        GLFW.glfwSetKeyCallback(handle, keyCallback);
        GLFW.glfwSetCharModsCallback(handle, charModsCallback);

        MineplayerClient.getVirtualKeyboard().setKeyCallback(keyCallback);
        MineplayerClient.getVirtualKeyboard().setCharModsCallback(charModsCallback);
        MineplayerClient.getVirtualKeyboard().setWindow(handle);
    }

    /**
     * @author JeffLegendPower
     * @reason Allows to inject virtual mouse to be connected to the bot
     */
    @Overwrite()
    public static void setMouseCallbacks(long handle, GLFWCursorPosCallbackI cursorPosCallback, GLFWMouseButtonCallbackI mouseButtonCallback, GLFWScrollCallbackI scrollCallback, GLFWDropCallbackI dropCallback) {
        GLFW.glfwSetCursorPosCallback(handle, cursorPosCallback);
        GLFW.glfwSetMouseButtonCallback(handle, mouseButtonCallback);
        GLFW.glfwSetScrollCallback(handle, scrollCallback);
        GLFW.glfwSetDropCallback(handle, dropCallback);

        MineplayerClient.getVirtualMouse().setCursorPosCallback(cursorPosCallback);
        MineplayerClient.getVirtualMouse().setMouseButtonCallback(mouseButtonCallback);
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
