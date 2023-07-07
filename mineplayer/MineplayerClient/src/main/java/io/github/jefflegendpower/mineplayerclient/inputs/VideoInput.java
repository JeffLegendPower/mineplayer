package io.github.jefflegendpower.mineplayerclient.inputs;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.option.GameOptions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

public class VideoInput {

    private Framebuffer fbo;
    private final int width;
    private final int height;

    public VideoInput(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void initialize() {
        this.fbo = new WindowFramebuffer(width, height);
    }

    public void getRGBFrame(ByteBuffer buffer) {
        this.fbo.beginWrite(true);
        MinecraftClient.getInstance().getFramebuffer().draw(width, height, true);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        this.fbo.endWrite();
        GlStateManager._enableDepthTest();
        MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
    }

    public int bufferSize() {
        return width * height * 4;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void cleanup() {
        this.fbo.delete();
    }
}
