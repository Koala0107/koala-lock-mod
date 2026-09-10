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

public final class ImageFrameScreen extends Screen {
    private final BlockPos pos;
    private final String currentUrl;
    private final int currentWidth;
    private final int currentHeight;
    private ImageFrameAlignment alignment;
    private boolean flipHorizontal;

    private TextFieldWidget urlField;
    private TextFieldWidget widthField;
    private TextFieldWidget heightField;
    private ButtonWidget alignmentButton;
    private ButtonWidget flipButton;

    public ImageFrameScreen(BlockPos pos, String url, int width, int height,
                            ImageFrameAlignment alignment, boolean flipHorizontal) {
        super(Text.literal("이미지 액자"));
        this.pos = pos.toImmutable();
        this.currentUrl = url == null ? "" : url;
        this.currentWidth = width;
        this.currentHeight = height;
        this.alignment = alignment == null ? ImageFrameAlignment.CENTER : alignment;
        this.flipHorizontal = flipHorizontal;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(390, width - 24);
        int x = (width - panelWidth) / 2;
        int y = Math.max(20, height / 2 - 106);
        int innerX = x + 18;
        int innerWidth = panelWidth - 36;

        urlField = addDrawableChild(new TextFieldWidget(textRenderer, innerX, y + 34, innerWidth, 20, Text.literal("URL")));
        urlField.setMaxLength(ImageFrameBlockEntity.MAX_URL_LENGTH);
        urlField.setText(currentUrl);
        urlField.setPlaceholder(Text.literal("https://.../image.png"));

        widthField = addDrawableChild(new TextFieldWidget(textRenderer, innerX, y + 76, 70, 20, Text.literal("가로")));
        widthField.setMaxLength(2);
        widthField.setText(Integer.toString(currentWidth));

        heightField = addDrawableChild(new TextFieldWidget(textRenderer, innerX + 92, y + 76, 70, 20, Text.literal("세로")));
        heightField.setMaxLength(2);
        heightField.setText(Integer.toString(currentHeight));

        alignmentButton = addDrawableChild(ButtonWidget.builder(alignmentText(), b -> {
                    alignment = alignment.next();
                    alignmentButton.setMessage(alignmentText());
                })
                .dimensions(innerX, y + 108, innerWidth, 20).build());

        flipButton = addDrawableChild(ButtonWidget.builder(flipText(), b -> {
                    flipHorizontal = !flipHorizontal;
                    flipButton.setMessage(flipText());
                })
                .dimensions(innerX, y + 136, innerWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("취소"), b -> close())
                .dimensions(innerX, y + 172, (innerWidth - 8) / 2, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("저장"), b -> save())
                .dimensions(innerX + (innerWidth - 8) / 2 + 8, y + 172, (innerWidth - 8) / 2, 20).build());

        urlField.setFocused(true);
    }

    private Text alignmentText() {
        return Text.literal("정렬: " + alignment.getDisplayName());
    }

    private Text flipText() {
        return Text.literal("좌우 반전: " + (flipHorizontal ? "켜짐" : "꺼짐"));
    }

    private void save() {
        int w = parseSize(widthField.getText(), currentWidth);
        int h = parseSize(heightField.getText(), currentHeight);

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(urlField.getText(), ImageFrameBlockEntity.MAX_URL_LENGTH);
        buf.writeByte(w);
        buf.writeByte(h);
        buf.writeByte(alignment.ordinal());
        buf.writeBoolean(flipHorizontal);
        ClientPlayNetworking.send(ImageFrameMod.SAVE_PACKET, buf);
        close();
    }

    private static int parseSize(String value, int fallback) {
        try {
            return Math.max(1, Math.min(ImageFrameBlockEntity.MAX_SIZE, Integer.parseInt(value.trim())));
        } catch (RuntimeException ignored) {
            return Math.max(1, Math.min(ImageFrameBlockEntity.MAX_SIZE, fallback));
        }
    }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelWidth = Math.min(390, width - 24);
        int x = (width - panelWidth) / 2;
        int y = Math.max(20, height / 2 - 106);

        context.fill(x - 3, y - 3, x + panelWidth + 3, y + 210, 0xEE050607);
        context.fill(x, y, x + panelWidth, y + 207, 0xEE202429);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, y + 11, 0xFFFFFFFF);
        context.drawText(textRenderer, Text.literal("이미지 URL"), x + 18, y + 24, 0xFFD5D8DB, false);
        context.drawText(textRenderer, Text.literal("가로"), x + 18, y + 65, 0xFFD5D8DB, false);
        context.drawText(textRenderer, Text.literal("세로"), x + 110, y + 65, 0xFFD5D8DB, false);
        context.drawText(textRenderer, Text.literal("1~20 블록"), x + 202, y + 81, 0xFF9CA1A6, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
