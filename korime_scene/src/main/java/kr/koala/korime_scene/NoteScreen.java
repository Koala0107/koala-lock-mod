package kr.koala.korime_scene;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public final class NoteScreen extends Screen {
    private final Hand hand;
    private final ItemStack snapshot;
    private TextFieldWidget titleField;
    private EditBoxWidget bodyBox;

    public NoteScreen(Hand hand, ItemStack snapshot) {
        super(Text.literal("노트"));
        this.hand = hand;
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(420, width - 30);
        int x = (width - panelWidth) / 2;
        int top = Math.max(18, height / 2 - 145);

        titleField = new TextFieldWidget(textRenderer, x + 12, top + 35, panelWidth - 24, 20, Text.literal("제목"));
        titleField.setMaxLength(NoteData.MAX_TITLE_LENGTH);
        titleField.setText(NoteData.getTitle(snapshot));
        addDrawableChild(titleField);

        bodyBox = new EditBoxWidget(textRenderer, x + 12, top + 73, panelWidth - 24, 145,
                Text.literal("자유롭게 입력하세요"), Text.literal("노트 내용"));
        bodyBox.setMaxLength(NoteData.MAX_BODY_LENGTH);
        bodyBox.setText(NoteData.getBody(snapshot));
        addDrawableChild(bodyBox);

        int buttonY = top + 228;
        addDrawableChild(ButtonWidget.builder(Text.literal("닫기"), b -> close())
                .dimensions(x + 12, buttonY, 92, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("저장"), b -> save())
                .dimensions(x + panelWidth - 104, buttonY, 92, 20).build());
    }

    private void save() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(hand);
        buf.writeString(titleField.getText(), NoteData.MAX_TITLE_LENGTH);
        buf.writeString(bodyBox.getText(), NoteData.MAX_BODY_LENGTH);
        ClientPlayNetworking.send(EvidenceCollectionMod.NOTE_SAVE_PACKET, buf);
        close();
    }

    @Override
    public void tick() {
        if (titleField != null) titleField.tick();
        if (bodyBox != null) bodyBox.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelWidth = Math.min(420, width - 30);
        int x = (width - panelWidth) / 2;
        int top = Math.max(18, height / 2 - 145);

        context.fill(x, top, x + panelWidth, top + 258, 0xEEF1E8C7);
        context.fill(x + 6, top + 6, x + panelWidth - 6, top + 252, 0xFFF8F0D5);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("노트"), width / 2, top + 12, 0xFF3B332A);
        context.drawTextWithShadow(textRenderer, Text.literal("제목"), x + 12, top + 25, 0xFF54483B);
        context.drawTextWithShadow(textRenderer, Text.literal("내용"), x + 12, top + 62, 0xFF54483B);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
