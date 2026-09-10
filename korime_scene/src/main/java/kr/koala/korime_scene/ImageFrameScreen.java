package kr.koala.korime_scene;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

public final class ImageFrameScreen extends Screen {
    private final BlockPos pos;
    private final String currentUrl;
    private final float currentWidth;
    private final float currentHeight;
    private final float currentRotation;
    private ImageFrameAlignment alignment;
    private boolean flipHorizontal;

    private TextFieldWidget urlField;
    private TextFieldWidget widthField;
    private TextFieldWidget heightField;
    private TextFieldWidget rotationField;
    private ButtonWidget alignmentButton;
    private ButtonWidget flipButton;

    public ImageFrameScreen(BlockPos pos, String url, float width, float height,
                            ImageFrameAlignment alignment, boolean flipHorizontal, float rotationDegrees) {
        super(Text.literal("이미지 액자"));
        this.pos = pos.toImmutable();
        this.currentUrl = url == null ? "" : url;
        this.currentWidth = width;
        this.currentHeight = height;
        this.alignment = alignment == null ? ImageFrameAlignment.CENTER : alignment;
        this.flipHorizontal = flipHorizontal;
        this.currentRotation = rotationDegrees;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(410, width - 24);
        int x = (width - panelWidth) / 2;
        int y = Math.max(14, height / 2 - 126);
        int innerX = x + 18;
        int innerWidth = panelWidth - 36;

        urlField = addDrawableChild(new TextFieldWidget(textRenderer, innerX, y + 34, innerWidth, 20, Text.literal("URL")));
        urlField.setMaxLength(ImageFrameBlockEntity.MAX_URL_LENGTH);
        urlField.setText(currentUrl);
        urlField.setPlaceholder(Text.literal("https://.../image.png"));

        widthField = addDrawableChild(new TextFieldWidget(textRenderer, innerX, y + 76, 80, 20, Text.literal("가로")));
        widthField.setMaxLength(4);
        widthField.setText(formatNumber(currentWidth));

        heightField = addDrawableChild(new TextFieldWidget(textRenderer, innerX + 102, y + 76, 80, 20, Text.literal("세로")));
        heightField.setMaxLength(4);
        heightField.setText(formatNumber(currentHeight));

        rotationField = addDrawableChild(new TextFieldWidget(textRenderer, innerX + 224, y + 76, 100, 20, Text.literal("회전")));
        rotationField.setMaxLength(5);
        rotationField.setText(formatNumber(currentRotation));

        alignmentButton = addDrawableChild(ButtonWidget.builder(alignmentText(), b -> {
                    alignment = alignment.next();
                    alignmentButton.setMessage(alignmentText());
                })
                .dimensions(innerX, y + 112, innerWidth, 20).build());

        flipButton = addDrawableChild(ButtonWidget.builder(flipText(), b -> {
                    flipHorizontal = !flipHorizontal;
                    flipButton.setMessage(flipText());
                })
                .dimensions(innerX, y + 140, innerWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("취소"), b -> close())
                .dimensions(innerX, y + 180, (innerWidth - 8) / 2, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("저장"), b -> save())
                .dimensions(innerX + (innerWidth - 8) / 2 + 8, y + 180, (innerWidth - 8) / 2, 20).build());

        urlField.setFocused(true);
    }

    private Text alignmentText() {
        return Text.literal("정렬: " + alignment.getDisplayName());
    }

    private Text flipText() {
        return Text.literal("좌우 반전: " + (flipHorizontal ? "켜짐" : "꺼짐"));
    }

    private void save() {
        float w = parseSize(widthField.getText(), currentWidth);
        float h = parseSize(heightField.getText(), currentHeight);
        float rotation = parseRotation(rotationField.getText(), currentRotation);

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(urlField.getText(), ImageFrameBlockEntity.MAX_URL_LENGTH);
        buf.writeFloat(w);
        buf.writeFloat(h);
        buf.writeByte(alignment.ordinal());
        buf.writeBoolean(flipHorizontal);
        buf.writeFloat(rotation);
        ClientPlayNetworking.send(ImageFrameMod.SAVE_PACKET, buf);
        close();
    }

    private static float parseSize(String value, float fallback) {
        try {
            float parsed = Float.parseFloat(value.trim());
            if (!Float.isFinite(parsed)) throw new NumberFormatException();
            parsed = Math.max(ImageFrameBlockEntity.MIN_SIZE, Math.min(ImageFrameBlockEntity.MAX_SIZE, parsed));
            return Math.round(parsed * 10.0F) / 10.0F;
        } catch (RuntimeException ignored) {
            float parsed = Math.max(ImageFrameBlockEntity.MIN_SIZE, Math.min(ImageFrameBlockEntity.MAX_SIZE, fallback));
            return Math.round(parsed * 10.0F) / 10.0F;
        }
    }

    private static float parseRotation(String value, float fallback) {
        try {
            float parsed = Float.parseFloat(value.trim());
            if (!Float.isFinite(parsed)) throw new NumberFormatException();
            parsed %= 360.0F;
            if (parsed < 0.0F) parsed += 360.0F;
            return Math.round(parsed * 10.0F) / 10.0F;
        } catch (RuntimeException ignored) {
            float parsed = fallback % 360.0F;
            if (parsed < 0.0F) parsed += 360.0F;
            return Math.round(parsed * 10.0F) / 10.0F;
        }
    }

    private static String formatNumber(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001F) return Integer.toString(Math.round(value));
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelWidth = Math.min(410, width - 24);
        int x = (width - panelWidth) / 2;
        int y = Math.max(14, height / 2 - 126);

        context.fill(x - 3, y - 3, x + panelWidth + 3, y + 218, 0xEE050607);
        context.fill(x, y, x + panelWidth, y + 215, 0xEE202429);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, y + 11, 0xFFFFFFFF);
        context.drawText(textRenderer, Text.literal("이미지 URL"), x + 18, y + 24, 0xFFD5D8DB, false);
        context.drawText(textRenderer, Text.literal("가로"), x + 18, y + 65, 0xFFD5D8DB, false);
        context.drawText(textRenderer, Text.literal("세로"), x + 120, y + 65, 0xFFD5D8DB, false);
        context.drawText(textRenderer, Text.literal("회전(°)"), x + 242, y + 65, 0xFFD5D8DB, false);
        context.drawText(textRenderer, Text.literal("0.1~50.0 블록 / 0.1° 단위"), x + 18, y + 101, 0xFF9CA1A6, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
