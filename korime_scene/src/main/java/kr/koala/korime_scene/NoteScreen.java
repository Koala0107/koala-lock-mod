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
        int panelWidth = Math.min(320, width - 36);
        int panelHeight = Math.min(196, height - 36);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        bodyBox = new WhiteEditBoxWidget(textRenderer, x + 12, y + 31, panelWidth - 24, panelHeight - 43,
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
        // Intentionally blank: the note draws its own clean white paper panel.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelWidth = Math.min(320, width - 36);
        int panelHeight = Math.min(196, height - 36);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        context.fill(0, 0, width, height, 0xFFFDFDFD);
        context.fill(x - 1, y - 1, x + panelWidth + 1, y + panelHeight + 1, 0xFFEDEDED);
        context.fill(x, y, x + panelWidth, y + panelHeight, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("노트"), width / 2, y + 11, 0xFF222222);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
