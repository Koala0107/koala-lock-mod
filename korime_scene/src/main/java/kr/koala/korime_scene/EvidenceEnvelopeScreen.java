package kr.koala.korime_scene;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class EvidenceEnvelopeScreen extends Screen {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Hand hand;
    private final ItemStack snapshot;
    private EditBoxWidget noteBox;

    public EvidenceEnvelopeScreen(Hand hand, ItemStack snapshot) {
        super(Text.literal("증거 봉투"));
        this.hand = hand;
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(390, width - 30);
        int x = (width - panelWidth) / 2;
        int top = Math.max(18, height / 2 - 135);

        noteBox = new EditBoxWidget(textRenderer, x + 12, top + 128, panelWidth - 24, 92,
                Text.literal("메모를 입력하세요"), Text.literal("증거 메모"));
        noteBox.setMaxLength(EvidenceEnvelopeData.MAX_NOTE_LENGTH);
        noteBox.setText(EvidenceEnvelopeData.getNote(snapshot));
        addDrawableChild(noteBox);

        int buttonY = top + 228;
        addDrawableChild(ButtonWidget.builder(Text.literal("닫기"), b -> close())
                .dimensions(x + 12, buttonY, 92, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("메모 저장"), b -> save())
                .dimensions(x + panelWidth - 116, buttonY, 104, 20).build());
    }

    private void save() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(hand);
        buf.writeString(noteBox.getText(), EvidenceEnvelopeData.MAX_NOTE_LENGTH);
        ClientPlayNetworking.send(EvidenceCollectionMod.ENVELOPE_NOTE_PACKET, buf);
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelWidth = Math.min(390, width - 30);
        int x = (width - panelWidth) / 2;
        int top = Math.max(18, height / 2 - 135);

        context.fill(x, top, x + panelWidth, top + 258, 0xEE1D2024);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("증거 봉투"), width / 2, top + 10, 0xFFFFFFFF);

        int tx = x + 14;
        int y = top + 31;
        context.drawTextWithShadow(textRenderer, Text.literal("대상: " + EvidenceEnvelopeData.getTargetId(snapshot)), tx, y, 0xFFFFD37A); y += 14;
        context.drawTextWithShadow(textRenderer, Text.literal("종류: " + EvidenceEnvelopeData.getTargetType(snapshot)), tx, y, 0xFFC8CDD2); y += 14;
        context.drawTextWithShadow(textRenderer, Text.literal(String.format("좌표: %.1f, %.1f, %.1f",
                EvidenceEnvelopeData.getX(snapshot), EvidenceEnvelopeData.getY(snapshot), EvidenceEnvelopeData.getZ(snapshot))), tx, y, 0xFFC8CDD2); y += 14;
        context.drawTextWithShadow(textRenderer, Text.literal("차원: " + EvidenceEnvelopeData.getDimension(snapshot)), tx, y, 0xFFC8CDD2); y += 14;
        context.drawTextWithShadow(textRenderer, Text.literal("채취자: " + EvidenceEnvelopeData.getCapturedBy(snapshot)), tx, y, 0xFFC8CDD2); y += 14;
        long capturedAt = EvidenceEnvelopeData.getCapturedAt(snapshot);
        String time = capturedAt <= 0 ? "-" : TIME_FORMAT.format(Instant.ofEpochMilli(capturedAt));
        context.drawTextWithShadow(textRenderer, Text.literal("채취 시각: " + time), tx, y, 0xFFC8CDD2); y += 14;
        context.drawTextWithShadow(textRenderer, Text.literal("NBT 스냅샷: " + (EvidenceEnvelopeData.hasTargetNbt(snapshot) ? "저장됨 · 읽기 전용" : "없음")), tx, y, 0xFF9BC69B);
        context.drawTextWithShadow(textRenderer, Text.literal("메모"), tx, top + 114, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
