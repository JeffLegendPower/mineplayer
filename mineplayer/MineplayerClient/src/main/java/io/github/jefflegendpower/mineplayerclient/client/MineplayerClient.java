package io.github.jefflegendpower.mineplayerclient.client;

import io.github.jefflegendpower.mineplayerclient.human.screen.HumanStartScreen;
import io.github.jefflegendpower.mineplayerclient.tcp.TCPClient;
import io.github.jefflegendpower.mineplayerclient.inputs.VirtualKeyboard;
import io.github.jefflegendpower.mineplayerclient.inputs.VirtualMouse;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class MineplayerClient implements ClientModInitializer {

    private static VirtualKeyboard virtualKeyboard = new VirtualKeyboard();
    private static VirtualMouse virtualMouse = new VirtualMouse();

    private static io.github.jefflegendpower.mineplayerclient.env.Environment env;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(new ClientTickEvents.StartTick() {

            boolean ran = false;

            @Override
            public void onStartTick(MinecraftClient client) {
                if (ran || client.getOverlay() instanceof SplashOverlay) return;
                MinecraftClient.getInstance().options.pauseOnLostFocus = false;

                TCPClient TCPClient = new TCPClient();

                Thread thread = new Thread("EnvSocketHandler") {
                    @Override
                    public void run() {
                        TCPClient.start("127.0.0.1", 2880, false);
                    }
                };
                thread.start();
                ran = true;
            }
        });
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
