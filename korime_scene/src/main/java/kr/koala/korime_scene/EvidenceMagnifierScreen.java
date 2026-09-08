package kr.koala.korime_scene;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class EvidenceMagnifierScreen extends Screen {
    private final BlockPos pos;
    private TextFieldWidget textField;
    private int panelX, panelY, panelWidth, panelHeight;

    public EvidenceMagnifierScreen(BlockPos pos) {
        super(Text.literal("증거 돋보기"));
        this.pos = pos.toImmutable();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(330, width - 20);
        panelHeight = Math.min(150, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int innerX = panelX + 18;
        int innerWidth = panelWidth - 36;

        textField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX, panelY + 55, innerWidth, 20, Text.literal("증거 문장")));
        textField.setMaxLength(EvidenceMagnifierBlockEntity.MAX_TEXT_LENGTH);
        textField.setPlaceholder(Text.literal("문장 또는 JSON 텍스트를 입력해줘"));

        int gap = 8;
        int buttonWidth = (innerWidth - gap) / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> close())
                .dimensions(innerX, panelY + 105, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("저장"), b -> save())
                .dimensions(innerX + buttonWidth + gap, panelY + 105, buttonWidth, 20).build());

        textField.setFocused(true);
    }

    private void save() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(textField.getText(), EvidenceMagnifierBlockEntity.MAX_TEXT_LENGTH);
        ClientPlayNetworking.send(SceneToolsMod.EVIDENCE_MAGNIFIER_SAVE_PACKET, buf);
        close();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Keep world visible.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(panelX - 3, panelY - 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0xFF080A0D);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFF0F6FA);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 30, 0xFF287FAE);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + panelWidth / 2, panelY + 11, 0xFFFFFFFF);
        context.drawText(textRenderer, Text.literal("증거 문장"), panelX + 18, panelY + 41, 0xFF34424B, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
