package kr.koala.korime_scene;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class EvidenceBreakConfirmScreen extends Screen {
    private final BlockPos pos;

    public EvidenceBreakConfirmScreen(BlockPos pos) {
        super(Text.literal("증거 파괴"));
        this.pos = pos.toImmutable();
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(280, width - 24);
        int x = (width - panelWidth) / 2;
        int y = height / 2 + 20;
        int gap = 8;
        int buttonWidth = (panelWidth - gap) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("취소"), b -> close())
                .dimensions(x, y, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("확인"), b -> confirmBreak())
                .dimensions(x + buttonWidth + gap, y, buttonWidth, 20).build());
    }

    private void confirmBreak() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ClientPlayNetworking.send(SceneToolsMod.EVIDENCE_BREAK_PACKET, buf);
        close();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelWidth = Math.min(304, width - 24);
        int panelHeight = 92;
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        context.fill(x - 3, y - 3, x + panelWidth + 3, y + panelHeight + 3, 0xEE050607);
        context.fill(x, y, x + panelWidth, y + panelHeight, 0xEE202429);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("진짜로 부수겠습니까?"), width / 2, y + 23, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("저장된 증거가 사라집니다."), width / 2, y + 40, 0xFFB8BCC0);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
