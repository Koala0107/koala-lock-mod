package kr.koala.korime_scene;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;

/** Multiline edit box styled like white paper instead of the vanilla black box. */
public final class WhiteEditBoxWidget extends EditBoxWidget {
    public WhiteEditBoxWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text placeholder, Text message) {
        super(textRenderer, x, y, width, height, placeholder, message);
    }

    @Override
    protected void drawBox(DrawContext context) {
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFFFFFFFF);
        context.fill(getX(), getY(), getX() + getWidth(), getY() + 1, 0xFFE6E6E6);
        context.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), 0xFFE6E6E6);
        context.fill(getX(), getY(), getX() + 1, getY() + getHeight(), 0xFFE6E6E6);
        context.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), 0xFFE6E6E6);
    }

    @Override
    protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
        // Vanilla EditBoxWidget supplies light text colors for its dark box. Multiplying
        // the text pass by black keeps the same editor/cursor behavior on white paper.
        RenderSystem.setShaderColor(0.08F, 0.08F, 0.08F, 1.0F);
        super.renderContents(context, mouseX, mouseY, delta);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
