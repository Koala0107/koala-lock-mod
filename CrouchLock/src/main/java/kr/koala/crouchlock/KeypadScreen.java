package kr.koala.crouchlock;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** A real button UI: no fake item stacks or chest slots are used. */
public final class KeypadScreen extends HandledScreen<KeypadScreenHandler> {
    private String input = "";

    public KeypadScreen(KeypadScreenHandler handler, net.minecraft.entity.player.PlayerInventory inventory,
                        Text title) {
        super(handler, inventory, title);
        backgroundWidth = 220;
        backgroundHeight = 230;
    }

    @Override
    protected void init() {
        super.init();
        int left = x + 35;
        int top = y + 62;
        int size = 45;
        int gap = 5;

        for (int digit = 1; digit <= 9; digit++) {
            int value = digit;
            int column = (digit - 1) % 3;
            int row = (digit - 1) / 3;
            addDrawableChild(ButtonWidget.builder(Text.literal(Integer.toString(digit)), button -> press(value))
                    .dimensions(left + column * (size + gap), top + row * (size + gap), size, size)
                    .build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("0"), button -> press(0))
                .dimensions(left + size + gap, top + 3 * (size + gap), size, size)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.crouchlock.clear"), button -> {
                    input = "";
                    click(KeypadScreenHandler.CLEAR_BUTTON);
                })
                .dimensions(left, top + 3 * (size + gap), size, size)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.crouchlock.confirm"), button -> {
                    if (input.length() >= KeypadScreenHandler.MIN_DIGITS
                            && input.length() <= KeypadScreenHandler.MAX_DIGITS) {
                        click(KeypadScreenHandler.CONFIRM_BUTTON);
                        input = "";
                    }
                })
                .dimensions(left + 2 * (size + gap), top + 3 * (size + gap), size, size)
                .build());
    }

    private void press(int digit) {
        if (input.length() < KeypadScreenHandler.MAX_DIGITS) {
            input += digit;
            click(digit);
        }
    }

    private void click(int buttonId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, buttonId);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int left = x;
        int top = y;
        context.fill(left, top, left + backgroundWidth, top + backgroundHeight, 0xF0181822);
        context.fill(left + 4, top + 4, left + backgroundWidth - 4, top + backgroundHeight - 4, 0xFF303544);
        context.drawCenteredTextWithShadow(textRenderer, title, left + backgroundWidth / 2, top + 12, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("*".repeat(input.length())
                                + "_".repeat(KeypadScreenHandler.MAX_DIGITS - input.length()))
                        .formatted(Formatting.AQUA),
                left + backgroundWidth / 2, top + 35, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.crouchlock.length_hint").formatted(Formatting.GRAY),
                left + backgroundWidth / 2, top + 48, 0xFFFFFF);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
