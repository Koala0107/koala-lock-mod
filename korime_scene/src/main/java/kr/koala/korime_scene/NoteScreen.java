package kr.koala.korime_scene;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public final class NoteScreen extends Screen {
    private final Hand hand;
    private final ItemStack snapshot;
    private WhiteEditBoxWidget bodyBox;
    private boolean saved;

    public NoteScreen(Hand hand, ItemStack snapshot) {
        super(Text.literal("노트"));
        this.hand = hand;
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(330, width - 24);
        int panelHeight = Math.min(220, height - 24);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        bodyBox = new WhiteEditBoxWidget(textRenderer, x + 12, y + 38, panelWidth - 24, panelHeight - 50,
                Text.literal("자유롭게 입력하세요"), Text.literal("노트"));
        bodyBox.setMaxLength(NoteData.MAX_BODY_LENGTH);
        bodyBox.setText(NoteData.getBody(snapshot));
        addDrawableChild(bodyBox);
        setInitialFocus(bodyBox);
    }

    private void saveIfNeeded() {
        if (saved || bodyBox == null) return;
        saved = true;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(hand);
        buf.writeString(bodyBox.getText(), NoteData.MAX_BODY_LENGTH);
        ClientPlayNetworking.send(EvidenceCollectionMod.NOTE_SAVE_PACKET, buf);
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
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // CCTV-style window: keep the world visible behind the panel.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelWidth = Math.min(330, width - 24);
        int panelHeight = Math.min(220, height - 24);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        context.fill(x - 3, y - 3, x + panelWidth + 3, y + panelHeight + 3, 0xFF080A0D);
        context.fill(x, y, x + panelWidth, y + panelHeight, 0xFFE7E8EA);
        context.fill(x, y, x + panelWidth, y + 30, 0xFF22272D);
        context.fill(x + 10, y + 36, x + panelWidth - 10, y + panelHeight - 10, 0xFFF1F1F1);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("노트"), width / 2, y + 11, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
