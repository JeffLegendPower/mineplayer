package io.github.jefflegendpower.mineplayerclient.human.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HumanStartScreen extends Screen {

    private static final String TEXT_BODY_RAW = """
            Welcome to Mineplayer!
            Press "Start" to start the environment.
            Press "Exit" to close the environment.
            At any time press esc to close the environment.
            """;

    private static final List<Text> TEXT_BODY = Arrays.stream(TEXT_BODY_RAW.split("\n"))
            .map(Text::literal)
            .collect(Collectors.toList());

    private static final Text TEXT_START = Text.literal("Start");
    private static final Text TEXT_EXIT = Text.literal("Exit");

    private final Runnable start;
    private final Runnable exit;

    public HumanStartScreen(Runnable start, Runnable exit) {
        super(Text.literal("Start Human Play"));
        this.start = start;
        this.exit = exit;
    }

    @Override
    public void init() {
        super.init();

        this.addDrawableChild(ButtonWidget.builder(TEXT_START, (btn) -> start.run())
                .dimensions(32, this.height - 40, 174, 20).build());

        this.addDrawableChild(ButtonWidget.builder(TEXT_EXIT, (btn) -> exit.run())
                .dimensions(this.width - 174 - 32, this.height - 40, 174, 20).build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        super.render(matrices, mouseX, mouseY, delta);

        drawTextWithShadow(matrices, this.textRenderer, Text.literal("Start Recording"), centerX("Start Recording"), 32, 0xff0000);

        for (int i = 0; i < TEXT_BODY.size(); i++) {
            if (TEXT_BODY.get(i).getString().isEmpty()) continue;
            drawTextWithShadow(matrices, this.textRenderer, TEXT_BODY.get(i), centerX(TEXT_BODY.get(i).getString()), 80 + i * 12, 0xffffff);
        }
    }

    private int centerX(String text) {
        return this.width / 2 - this.textRenderer.getWidth(text) / 2;
    }
}
