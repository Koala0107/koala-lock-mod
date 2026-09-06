package kr.koala.crouchlock;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Final save dialog: choose the evidence item name/subtitle and confirm the irreversible save. */
public final class SmartphoneFinalizeScreen extends Screen {
    private final SmartphoneScreenV2 parent;
    private final String initialTitle;
    private final String initialSubtitle;

    private TextFieldWidget titleField;
    private TextFieldWidget subtitleField;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public SmartphoneFinalizeScreen(SmartphoneScreenV2 parent, String initialTitle, String initialSubtitle) {
        super(Text.translatable("screen.crouchlock.smartphone.save.title"));
        this.parent = parent;
        this.initialTitle = initialTitle;
        this.initialSubtitle = initialSubtitle;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(270, width - 20);
        panelHeight = Math.min(150, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int innerX = panelX + 18;
        int innerWidth = panelWidth - 36;

        titleField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX, panelY + 43, innerWidth, 20,
                Text.translatable("screen.crouchlock.smartphone.save.name")));
        titleField.setMaxLength(SmartphoneData.MAX_TITLE_LENGTH);
        titleField.setPlaceholder(Text.translatable("screen.crouchlock.smartphone.save.name_placeholder"));
        titleField.setText(initialTitle);

        subtitleField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX, panelY + 78, innerWidth, 20,
                Text.translatable("screen.crouchlock.smartphone.save.subtitle")));
        subtitleField.setMaxLength(SmartphoneData.MAX_SUBTITLE_LENGTH);
        subtitleField.setPlaceholder(Text.translatable("screen.crouchlock.smartphone.save.subtitle_placeholder"));
        subtitleField.setText(initialSubtitle);

        int gap = 8;
        int buttonWidth = (innerWidth - gap) / 2;
        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.save.cancel"),
                        button -> returnToEditor())
                .dimensions(innerX, panelY + 118, buttonWidth, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.save.confirm"),
                        button -> confirmSave())
                .dimensions(innerX + buttonWidth + gap, panelY + 118, buttonWidth, 20)
                .build());

        titleField.setFocused(true);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(panelX - 3, panelY - 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0xFF080A0D);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFF1F1F1);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 28, 0xFF252D35);

        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.crouchlock.smartphone.save.title"),
                panelX + panelWidth / 2, panelY + 10, 0xFFFFFFFF);
        context.drawText(textRenderer, Text.translatable("screen.crouchlock.smartphone.save.name"),
                panelX + 18, panelY + 32, 0xFF33383D, false);
        context.drawText(textRenderer, Text.translatable("screen.crouchlock.smartphone.save.subtitle"),
                panelX + 18, panelY + 67, 0xFFB06C00, false);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.crouchlock.smartphone.save.warning"),
                panelX + panelWidth / 2, panelY + 103, 0xFFFFA000);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        returnToEditor();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void confirmSave() {
        parent.finalizeWithMetadata(titleField.getText(), subtitleField.getText());
    }

    private void returnToEditor() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
