package kr.koala.crouchlock;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class CctvEditorScreen extends Screen {
    private final BlockPos cctvPos;
    private final List<String> records;
    private final List<OrderedText> previewLines = new ArrayList<>();
    private TextFieldWidget recordField;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public CctvEditorScreen(BlockPos cctvPos, List<String> currentRecords) {
        super(Text.literal("CCTV 기록 편집"));
        this.cctvPos = cctvPos.toImmutable();
        this.records = new ArrayList<>(currentRecords);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(360, width - 20);
        panelHeight = Math.min(240, height - 20);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int innerX = panelX + 18;
        int innerWidth = panelWidth - 36;

        recordField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX, panelY + 48, innerWidth, 20,
                Text.literal("CCTV 기록 문장")));
        recordField.setMaxLength(CctvBlockEntity.MAX_RECORD_LENGTH);
        recordField.setPlaceholder(Text.literal("증거로 보여줄 문장을 입력해줘"));

        int gap = 6;
        int smallWidth = (innerWidth - gap) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("문장 추가"), button -> addRecord())
                .dimensions(innerX, panelY + 74, smallWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("마지막 문장 삭제"), button -> deleteLast())
                .dimensions(innerX + smallWidth + gap, panelY + 74, smallWidth, 20).build());

        int buttonWidth = (innerWidth - gap) / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(innerX, panelY + panelHeight - 34, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("최종 저장"), button -> saveFinal())
                .dimensions(innerX + buttonWidth + gap, panelY + panelHeight - 34, buttonWidth, 20).build());

        rebuildPreview();
        recordField.setFocused(true);
    }

    private void addRecord() {
        String line = recordField.getText().trim();
        if (line.isEmpty() || records.size() >= CctvBlockEntity.MAX_RECORDS) return;
        records.add(line);
        recordField.setText("");
        rebuildPreview();
    }

    private void deleteLast() {
        if (records.isEmpty()) return;
        records.remove(records.size() - 1);
        rebuildPreview();
    }

    private void rebuildPreview() {
        previewLines.clear();
        int lineWidth = Math.max(80, panelWidth - 50);
        int start = Math.max(0, records.size() - 6);
        for (int i = start; i < records.size(); i++) {
            previewLines.addAll(textRenderer.wrapLines(Text.literal((i + 1) + ". " + records.get(i)), lineWidth));
        }
    }

    private void saveFinal() {
        String pending = recordField.getText().trim();
        if (!pending.isEmpty() && records.size() < CctvBlockEntity.MAX_RECORDS) {
            records.add(pending);
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(cctvPos);
        buf.writeVarInt(records.size());
        for (String line : records) {
            buf.writeString(line, CctvBlockEntity.MAX_RECORD_LENGTH);
        }
        ClientPlayNetworking.send(SceneToolsMod.CCTV_SAVE_PACKET, buf);
        close();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Keep the world visible, matching the smartphone editor.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(panelX - 3, panelY - 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0xFF080A0D);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFE9ECEF);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 30, 0xFF22272D);

        context.drawCenteredTextWithShadow(textRenderer, title, panelX + panelWidth / 2, panelY + 11, 0xFFFFFFFF);
        context.drawText(textRenderer, Text.literal("문장을 추가한 뒤 최종 저장하면 읽기 전용 증거가 돼."), panelX + 18, panelY + 35, 0xFF59616A, false);

        int previewTop = panelY + 104;
        int previewBottom = panelY + panelHeight - 44;
        context.fill(panelX + 14, previewTop - 5, panelX + panelWidth - 14, previewBottom, 0xFFF7F7F7);

        int y = previewTop;
        int maxY = previewBottom - 10;
        for (OrderedText line : previewLines) {
            if (y > maxY) break;
            context.drawText(textRenderer, line, panelX + 20, y, 0xFF24282C, false);
            y += 11;
        }

        String count = records.size() + " / " + CctvBlockEntity.MAX_RECORDS;
        context.drawText(textRenderer, Text.literal(count), panelX + panelWidth - 18 - textRenderer.getWidth(count), panelY + 35, 0xFF6B727A, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
