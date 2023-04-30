package io.github.jefflegendpower.mineplayerclient.client;

import io.github.jefflegendpower.mineplayerclient.Mineplayer;
import io.github.jefflegendpower.mineplayerclient.env.EnvTCPServer;
import io.github.jefflegendpower.mineplayerclient.inputs.VirtualKeyboard;
import io.github.jefflegendpower.mineplayerclient.inputs.VirtualMouse;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.IOException;

@Environment(EnvType.CLIENT)
public class MineplayerClient implements ClientModInitializer {

    private static VirtualKeyboard virtualKeyboard = new VirtualKeyboard();
    private static VirtualMouse virtualMouse = new VirtualMouse();

    private static io.github.jefflegendpower.mineplayerclient.env.Environment env;

    @Override
    public void onInitializeClient() {
        EnvTCPServer envTCPServer = new EnvTCPServer();

        Thread thread = new Thread("EnvServerSocketHandler") {
            @Override
            public void run() {
                try {
                    envTCPServer.start(444, false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public static VirtualKeyboard getVirtualKeyboard() {
        return virtualKeyboard;
    }

    public static VirtualMouse getVirtualMouse() {
        return virtualMouse;
    }

    public static Identifier mineplayerIdentifier(String path) {
        return new Identifier("mineplayer", path);
    }

    public static void runOnMainThread(Runnable runnable) {
        MinecraftClient.getInstance().executeSync(runnable);
    }

    public static io.github.jefflegendpower.mineplayerclient.env.Environment getEnv() {
        return env;
    }

    public static void setEnv(io.github.jefflegendpower.mineplayerclient.env.Environment env) {
        MineplayerClient.env = env;
    }
}
