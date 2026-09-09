package kr.koala.korime_scene;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public final class NoteScreen extends Screen {
    private final Hand hand;
    private final ItemStack snapshot;
    private final List<String> pages;
    private WhiteEditBoxWidget bodyBox;
    private ButtonWidget previousButton;
    private ButtonWidget nextButton;
    private ButtonWidget addPageButton;
    private int pageIndex;

    public NoteScreen(Hand hand, ItemStack snapshot) {
        super(Text.literal("노트"));
        this.hand = hand;
        this.snapshot = snapshot;
        this.pages = new ArrayList<>(NoteData.getPages(snapshot));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(330, width - 24);
        int panelHeight = Math.min(220, height - 24);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        bodyBox = new WhiteEditBoxWidget(textRenderer, x + 12, y + 38, panelWidth - 24, panelHeight - 72,
                Text.literal("자유롭게 입력하세요"), Text.literal("노트"));
        bodyBox.setMaxLength(NoteData.MAX_BODY_LENGTH);
        bodyBox.setText(pages.get(pageIndex));
        addDrawableChild(bodyBox);

        previousButton = addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> changePage(-1))
                .dimensions(x + 12, y + panelHeight - 28, 34, 18).build());
        nextButton = addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> changePage(1))
                .dimensions(x + 50, y + panelHeight - 28, 34, 18).build());
        addPageButton = addDrawableChild(ButtonWidget.builder(Text.literal("+ 페이지"), b -> addPage())
                .dimensions(x + panelWidth - 84, y + panelHeight - 28, 72, 18).build());

        updateButtons();
        setInitialFocus(bodyBox);
    }

    private void saveCurrentPage() {
        if (bodyBox == null) return;
        String text = bodyBox.getText();
        pages.set(pageIndex, text);

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(hand);
        buf.writeVarInt(pageIndex);
        buf.writeString(text, NoteData.MAX_BODY_LENGTH);
        ClientPlayNetworking.send(EvidenceCollectionMod.NOTE_SAVE_PACKET, buf);
    }

    private void changePage(int delta) {
        int target = pageIndex + delta;
        if (target < 0 || target >= pages.size()) return;
        saveCurrentPage();
        pageIndex = target;
        bodyBox.setText(pages.get(pageIndex));
        updateButtons();
        setFocused(bodyBox);
    }

    private void addPage() {
        if (pages.size() >= NoteData.MAX_PAGES) return;
        saveCurrentPage();
        pages.add("");
        pageIndex = pages.size() - 1;
        bodyBox.setText("");
        updateButtons();
        setFocused(bodyBox);
    }

    private void updateButtons() {
        if (previousButton != null) previousButton.active = pageIndex > 0;
        if (nextButton != null) nextButton.active = pageIndex < pages.size() - 1;
        if (addPageButton != null) addPageButton.active = pages.size() < NoteData.MAX_PAGES;
    }

    @Override
    public void close() {
        saveCurrentPage();
        super.close();
    }

    @Override
    public void removed() {
        saveCurrentPage();
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
        context.fill(x + 10, y + 36, x + panelWidth - 10, y + panelHeight - 36, 0xFFF1F1F1);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("노트"), width / 2, y + 11, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal((pageIndex + 1) + " / " + pages.size()), width / 2, y + panelHeight - 24, 0xFF5C6570);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
