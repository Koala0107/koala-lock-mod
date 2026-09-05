package kr.koala.crouchlock;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** A real button UI: no fake item stacks or chest slots are used. */
public final class KeypadScreen extends HandledScreen<KeypadScreenHandler> {
    private static final int PANEL_WIDTH = 180;
    private static final int PANEL_HEIGHT = 210;
    private static final int BUTTON_SIZE = 32;
    private static final int BUTTON_GAP = 4;

    private String input = "";
    private int seenErrorCounter;
    private int errorTicks;

    public KeypadScreen(KeypadScreenHandler handler, net.minecraft.entity.player.PlayerInventory inventory,
                        Text title) {
        super(handler, inventory, title);
        backgroundWidth = PANEL_WIDTH;
        backgroundHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int gridWidth = BUTTON_SIZE * 3 + BUTTON_GAP * 2;
        int left = (width - gridWidth) / 2;
        int top = (height - PANEL_HEIGHT) / 2 + 62;

        for (int digit = 1; digit <= 9; digit++) {
            int value = digit;
            int column = (digit - 1) % 3;
            int row = (digit - 1) / 3;
            addDrawableChild(ButtonWidget.builder(Text.literal(Integer.toString(digit)), button -> press(value))
                    .dimensions(left + column * (BUTTON_SIZE + BUTTON_GAP),
                            top + row * (BUTTON_SIZE + BUTTON_GAP), BUTTON_SIZE, BUTTON_SIZE)
                    .build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("0"), button -> press(0))
                .dimensions(left + BUTTON_SIZE + BUTTON_GAP,
                        top + 3 * (BUTTON_SIZE + BUTTON_GAP), BUTTON_SIZE, BUTTON_SIZE)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.crouchlock.clear"), button -> {
                    input = "";
                    playKeySound(0.65F);
                    click(KeypadScreenHandler.CLEAR_BUTTON);
                })
                .dimensions(left, top + 3 * (BUTTON_SIZE + BUTTON_GAP), BUTTON_SIZE, BUTTON_SIZE)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.crouchlock.confirm"), button -> {
                    if (input.length() >= KeypadScreenHandler.MIN_DIGITS
                            && input.length() <= KeypadScreenHandler.MAX_DIGITS) {
                        playKeySound(1.55F);
                        click(KeypadScreenHandler.CONFIRM_BUTTON);
                        input = "";
                    }
                })
                .dimensions(left + 2 * (BUTTON_SIZE + BUTTON_GAP),
                        top + 3 * (BUTTON_SIZE + BUTTON_GAP), BUTTON_SIZE, BUTTON_SIZE)
                .build());
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        int currentErrorCounter = handler.getErrorCounter();
        if (currentErrorCounter != seenErrorCounter) {
            seenErrorCounter = currentErrorCounter;
            errorTicks = 40;
        } else if (errorTicks > 0) {
            errorTicks--;
        }
    }

    private void press(int digit) {
        if (input.length() < KeypadScreenHandler.MAX_DIGITS) {
            input += digit;
            playKeySound(0.8F + digit * 0.06F);
            click(digit);
        }
    }

    private void playKeySound(float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 0.55F, pitch);
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
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int center = width / 2;
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF0181822);
        context.fill(left + 4, top + 4, left + PANEL_WIDTH - 4, top + PANEL_HEIGHT - 4, 0xFF303544);
        context.drawCenteredTextWithShadow(textRenderer, title, center, top + 11, 0xFFFFFF);
        context.fill(left + 25, top + 25, left + PANEL_WIDTH - 25, top + 43,
                errorTicks > 0 ? 0xFF9B2525 : 0xFF171B24);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("*".repeat(input.length())
                                + "_".repeat(KeypadScreenHandler.MAX_DIGITS - input.length()))
                        .formatted(Formatting.AQUA),
                center, top + 30, errorTicks > 0 ? 0xFFFFD0D0 : 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.crouchlock.length_hint").formatted(Formatting.GRAY),
                center, top + 48, 0xFFFFFF);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // The keypad has no inventory slots, so suppress HandledScreen's default
        // title and player-inventory ("보관함") labels. The centered title is
        // already drawn as part of our custom panel above.
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
