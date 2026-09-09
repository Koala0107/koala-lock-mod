package kr.koala.korime_scene;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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

    public EvidenceEnvelopeScreen(Hand hand, ItemStack snapshot) {
        super(Text.literal("증거 봉투"));
        this.hand = hand;
        this.snapshot = snapshot;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int panelWidth = Math.min(250, width - 32);
        int panelHeight = Math.min(166, height - 32);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        context.fill(x - 2, y - 2, x + panelWidth + 2, y + panelHeight + 2, 0xFF08090A);
        context.fill(x, y, x + panelWidth, y + panelHeight, 0xFF202226);
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
                context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF30343A);

                int index = row * COLS + col;
                if (index < items.size()) context.drawItem(items.get(index), sx + 1, sy + 1);
            }
        }

        super.render(context, mouseX, mouseY, delta);

        for (int i = 0; i < items.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int sx = gridX + col * SLOT;
            int sy = gridY + row * SLOT;
            if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                context.drawItemTooltip(textRenderer, items.get(i), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelWidth = Math.min(250, width - 32);
            int panelHeight = Math.min(166, height - 32);
            int x = (width - panelWidth) / 2;
            int y = (height - panelHeight) / 2;
            int gridWidth = COLS * SLOT;
            int gridX = x + (panelWidth - gridWidth) / 2;
            int gridY = y + 46;

            int col = (int) ((mouseX - gridX) / SLOT);
            int row = (int) ((mouseY - gridY) / SLOT);
            if (mouseX >= gridX && mouseY >= gridY && col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                int index = row * COLS + col;
                List<ItemStack> items = EvidenceEnvelopeData.getStoredItems(snapshot);
                if (index >= 0 && index < items.size()) {
                    int sx = gridX + col * SLOT;
                    int sy = gridY + row * SLOT;
                    if (mouseX < sx + 18 && mouseY < sy + 18) {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeEnumConstant(hand);
                        buf.writeVarInt(index);
                        ClientPlayNetworking.send(EvidenceCollectionMod.EXTRACT_POUCH_ITEM_PACKET, buf);
                        close();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean shouldPause() { return false; }
}
