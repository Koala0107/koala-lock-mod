package kr.koala.korime_scene;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class ContainerEvidencePickerScreen extends Screen {
    private static final int SLOT = 20;

    private final Hand hand;
    private final BlockPos pos;
    private final List<ItemStack> items;

    public ContainerEvidencePickerScreen(Hand hand, BlockPos pos, List<ItemStack> items) {
        super(Text.literal("증거 채취"));
        this.hand = hand;
        this.pos = pos.toImmutable();
        this.items = List.copyOf(items);
    }

    private int columns() {
        return Math.max(1, Math.min(9, items.size()));
    }

    private int rows() {
        return Math.max(1, (items.size() + columns() - 1) / columns());
    }

    private int panelWidth() {
        return columns() * SLOT + 28;
    }

    private int panelHeight() {
        return rows() * SLOT + 56;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int cols = columns();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        context.fill(x - 2, y - 2, x + panelWidth + 2, y + panelHeight + 2, 0xFF08090A);
        context.fill(x, y, x + panelWidth, y + panelHeight, 0xFF202226);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("증거로 복사할 아이템 선택"), width / 2, y + 12, 0xFFFFFFFF);

        int gridX = x + 14;
        int gridY = y + 34;
        for (int i = 0; i < items.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int sx = gridX + col * SLOT;
            int sy = gridY + row * SLOT;

            context.fill(sx, sy, sx + 18, sy + 18, 0xFF050607);
            context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF30343A);

            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) context.drawItem(stack, sx + 1, sy + 1);
        }

        super.render(context, mouseX, mouseY, delta);

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            int col = i % cols;
            int row = i / cols;
            int sx = gridX + col * SLOT;
            int sy = gridY + row * SLOT;
            if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                context.drawItemTooltip(textRenderer, stack, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int cols = columns();
            int panelWidth = panelWidth();
            int panelHeight = panelHeight();
            int x = (width - panelWidth) / 2;
            int y = (height - panelHeight) / 2;
            int gridX = x + 14;
            int gridY = y + 34;

            if (mouseX >= gridX && mouseY >= gridY) {
                int col = (int) ((mouseX - gridX) / SLOT);
                int row = (int) ((mouseY - gridY) / SLOT);
                if (col >= 0 && col < cols && row >= 0 && row < rows()) {
                    int index = row * cols + col;
                    if (index >= 0 && index < items.size()) {
                        int sx = gridX + col * SLOT;
                        int sy = gridY + row * SLOT;
                        if (mouseX < sx + 18 && mouseY < sy + 18 && !items.get(index).isEmpty()) {
                            PacketByteBuf buf = PacketByteBufs.create();
                            buf.writeEnumConstant(hand);
                            buf.writeBlockPos(pos);
                            buf.writeVarInt(index);
                            ClientPlayNetworking.send(EvidenceCollectionMod.TAKE_CONTAINER_EVIDENCE_PACKET, buf);
                            close();
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() { return false; }
}
