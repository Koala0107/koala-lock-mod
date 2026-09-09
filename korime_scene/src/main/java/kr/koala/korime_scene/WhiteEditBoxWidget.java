package kr.koala.korime_scene;

import com.mojang.blaze3d.systems.RenderSystem;
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

    @Override
    protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
        // EditBoxWidget already renders dark text. A light multiplier brings its core
        // glyph color close to the CCTV body's #24282C instead of crushing it to black.
        RenderSystem.setShaderColor(0.65F, 0.65F, 0.65F, 1.0F);
        super.renderContents(context, mouseX, mouseY, delta);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
