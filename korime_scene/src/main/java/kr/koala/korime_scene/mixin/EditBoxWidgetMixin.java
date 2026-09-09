package kr.koala.korime_scene.mixin;

import kr.koala.korime_scene.WhiteEditBoxWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EditBoxWidget.class)
public abstract class EditBoxWidgetMixin {
    private static final int NOTE_TEXT_COLOR = 0xFF24282C;

    @Redirect(
            method = "renderContents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"),
            require = 0
    )
    private int korimeScene$orderedTextWithoutShadow(DrawContext context, TextRenderer renderer, OrderedText text,
                                                     int x, int y, int color) {
        if ((Object) this instanceof WhiteEditBoxWidget) {
            return context.drawText(renderer, text, x, y, NOTE_TEXT_COLOR, false);
        }
        return context.drawTextWithShadow(renderer, text, x, y, color);
    }

    @Redirect(
            method = "renderContents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I"),
            require = 0
    )
    private int korimeScene$stringWithoutShadow(DrawContext context, TextRenderer renderer, String text,
                                                int x, int y, int color) {
        if ((Object) this instanceof WhiteEditBoxWidget) {
            return context.drawText(renderer, text, x, y, NOTE_TEXT_COLOR, false);
        }
        return context.drawTextWithShadow(renderer, text, x, y, color);
    }

    @Redirect(
            method = "renderContents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"),
            require = 0
    )
    private int korimeScene$textWithoutShadow(DrawContext context, TextRenderer renderer, Text text,
                                              int x, int y, int color) {
        if ((Object) this instanceof WhiteEditBoxWidget) {
            return context.drawText(renderer, text, x, y, NOTE_TEXT_COLOR, false);
        }
        return context.drawTextWithShadow(renderer, text, x, y, color);
    }

    // EditBoxWidget uses its overlay text for the character counter. Suppress only that
    // text for the note widget; the scrollbar itself still renders normally.
    @Redirect(
            method = "renderOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"),
            require = 0
    )
    private int korimeScene$hideOrderedOverlayText(DrawContext context, TextRenderer renderer, OrderedText text,
                                                   int x, int y, int color) {
        if ((Object) this instanceof WhiteEditBoxWidget) return 0;
        return context.drawTextWithShadow(renderer, text, x, y, color);
    }

    @Redirect(
            method = "renderOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I"),
            require = 0
    )
    private int korimeScene$hideStringOverlayText(DrawContext context, TextRenderer renderer, String text,
                                                  int x, int y, int color) {
        if ((Object) this instanceof WhiteEditBoxWidget) return 0;
        return context.drawTextWithShadow(renderer, text, x, y, color);
    }

    @Redirect(
            method = "renderOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"),
            require = 0
    )
    private int korimeScene$hideTextOverlayText(DrawContext context, TextRenderer renderer, Text text,
                                                int x, int y, int color) {
        if ((Object) this instanceof WhiteEditBoxWidget) return 0;
        return context.drawTextWithShadow(renderer, text, x, y, color);
    }
}
