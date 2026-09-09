package kr.koala.korime_scene;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.List;

public final class EvidenceEnvelopeScreen extends Screen {
    private static final int COLS = 6;
    private static final int ROWS = 5;
    private static final int SLOT = 20;

    private final Hand hand;
    private final ItemStack snapshot;
    private EditBoxWidget noteBox;
    private boolean saved;

    public EvidenceEnvelopeScreen(Hand hand, ItemStack snapshot) {
        super(Text.literal("증거 봉투"));
        this.hand = hand;
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(250, width - 32);
        int panelHeight = Math.min(286, height - 32);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        noteBox = new EditBoxWidget(textRenderer, x + 14, y + 154, panelWidth - 28, panelHeight - 172,
                Text.literal("메모"), Text.literal("증거 메모"));
        noteBox.setMaxLength(EvidenceEnvelopeData.MAX_NOTE_LENGTH);
        noteBox.setText(EvidenceEnvelopeData.getNote(snapshot));
        addDrawableChild(noteBox);
    }

    private void saveIfNeeded() {
        if (saved || noteBox == null) return;
        saved = true;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(hand);
        buf.writeString(noteBox.getText(), EvidenceEnvelopeData.MAX_NOTE_LENGTH);
        ClientPlayNetworking.send(EvidenceCollectionMod.ENVELOPE_NOTE_PACKET, buf);
    }

    @Override
    public void close() {
        saveIfNeeded();
        super.close();
    }

    @Override
    public void removed() {
        saveIfNeeded();
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int panelWidth = Math.min(250, width - 32);
        int panelHeight = Math.min(286, height - 32);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        context.fill(x - 2, y - 2, x + panelWidth + 2, y + panelHeight + 2, 0xFF090A0B);
        context.fill(x, y, x + panelWidth, y + panelHeight, 0xFF17191C);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("증거 봉투"), width / 2, y + 12, 0xFFFFFFFF);

        List<ItemStack> items = EvidenceEnvelopeData.getStoredItems(snapshot);
        context.drawTextWithShadow(textRenderer, Text.literal(items.size() + " / " + EvidenceEnvelopeData.MAX_ITEMS), x + 14, y + 31, 0xFFBFC5CB);

        int gridWidth = COLS * SLOT;
        int gridX = x + (panelWidth - gridWidth) / 2;
        int gridY = y + 46;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int sx = gridX + col * SLOT;
                int sy = gridY + row * SLOT;
                context.fill(sx, sy, sx + 18, sy + 18, 0xFF050607);
                context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF25292D);

                int index = row * COLS + col;
                if (index < items.size()) {
                    ItemStack item = items.get(index);
                    context.drawItem(item, sx + 1, sy + 1);
                }
            }
        }

        context.drawTextWithShadow(textRenderer, Text.literal("메모"), x + 14, y + 140, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = row * COLS + col;
                if (index >= items.size()) continue;
                int sx = gridX + col * SLOT;
                int sy = gridY + row * SLOT;
                if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                    context.drawItemTooltip(textRenderer, items.get(index), mouseX, mouseY);
                    return;
                }
            }
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
