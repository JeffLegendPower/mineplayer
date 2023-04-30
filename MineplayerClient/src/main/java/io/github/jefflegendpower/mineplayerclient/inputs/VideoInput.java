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
//        BufferedImage exampleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                int i = (x + (width * y)) * 4;
//                int r = buffer.get(i) & 0xFF;
//                int g = buffer.get(i + 1) & 0xFF;
//                int b = buffer.get(i + 2) & 0xFF;
//                exampleImage.setRGB(x, height - y, (0xFF << 24) | (r << 16) | (g << 8) | b);
//            }
//        }
//
//        File outputfile = new File("test_save.png");
//        try {
//            ImageIO.write(exampleImage, "png", outputfile);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
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
