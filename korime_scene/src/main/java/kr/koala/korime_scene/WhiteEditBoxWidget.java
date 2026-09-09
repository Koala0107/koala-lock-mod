package kr.koala.korime_scene;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;

/** Multiline edit box styled to match the CCTV log panel. */
public final class WhiteEditBoxWidget extends EditBoxWidget {
    public WhiteEditBoxWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text placeholder, Text message) {
        super(textRenderer, x, y, width, height, placeholder, message);
    }

    @Override
    protected void drawBox(DrawContext context) {
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFFF7F7F7);
        context.fill(getX(), getY(), getX() + getWidth(), getY() + 1, 0xFFD0D2D4);
        context.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), 0xFFD0D2D4);
        context.fill(getX(), getY(), getX() + 1, getY() + getHeight(), 0xFFD0D2D4);
        context.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), 0xFFD0D2D4);
    }
}
