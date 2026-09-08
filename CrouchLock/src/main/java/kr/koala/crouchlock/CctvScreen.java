package kr.koala.crouchlock;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class CctvScreen extends Screen {
    private final List<String> records;
    private final List<OrderedText> wrapped = new ArrayList<>();
    private int scroll;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public CctvScreen(List<String> records) {
        super(Text.translatable("screen.korime_scene.cctv.title"));
        this.records = List.copyOf(records);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(330, width - 24);
        panelHeight = Math.min(220, height - 24);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        rebuildWrapped();
    }

    private void rebuildWrapped() {
        wrapped.clear();
        int lineWidth = Math.max(80, panelWidth - 40);
        if (records.isEmpty()) {
            wrapped.add(Text.translatable("screen.korime_scene.cctv.empty").asOrderedText());
        } else {
            for (int i = records.size() - 1; i >= 0; i--) {
                wrapped.addAll(textRenderer.wrapLines(Text.literal(records.get(i)), lineWidth));
                wrapped.add(Text.literal(" ").asOrderedText());
            }
        }
        scroll = Math.min(scroll, maxScroll());
    }

    private int visibleLines() {
        return Math.max(1, (panelHeight - 64) / 11);
    }

    private int maxScroll() {
        return Math.max(0, wrapped.size() - visibleLines());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) scroll = Math.max(0, scroll - 2);
        if (verticalAmount < 0) scroll = Math.min(maxScroll(), scroll + 2);
        return true;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Keep the world visible behind the CCTV log window.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(panelX - 3, panelY - 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0xFF080A0D);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFE7E8EA);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 30, 0xFF22272D);
        context.fill(panelX + 12, panelY + 42, panelX + panelWidth - 12, panelY + panelHeight - 12, 0xFFF7F7F7);

        context.drawCenteredTextWithShadow(textRenderer, title, panelX + panelWidth / 2, panelY + 11, 0xFFFFFFFF);
        context.drawText(textRenderer, Text.translatable("screen.korime_scene.cctv.subtitle"), panelX + 16, panelY + 34, 0xFF59616A, false);

        int y = panelY + 49;
        int end = Math.min(wrapped.size(), scroll + visibleLines());
        for (int i = scroll; i < end; i++) {
            context.drawText(textRenderer, wrapped.get(i), panelX + 20, y, 0xFF24282C, false);
            y += 11;
        }

        if (maxScroll() > 0) {
            int trackTop = panelY + 48;
            int trackBottom = panelY + panelHeight - 18;
            int thumbHeight = Math.max(16, (trackBottom - trackTop) * visibleLines() / wrapped.size());
            int thumbY = trackTop + (trackBottom - trackTop - thumbHeight) * scroll / maxScroll();
            context.fill(panelX + panelWidth - 18, trackTop, panelX + panelWidth - 15, trackBottom, 0xFFD0D2D4);
            context.fill(panelX + panelWidth - 18, thumbY, panelX + panelWidth - 15, thumbY + thumbHeight, 0xFF5C6570);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
