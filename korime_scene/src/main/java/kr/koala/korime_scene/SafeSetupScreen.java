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

public final class SafeSetupScreen extends Screen {
    private final BlockPos pos;
    private final TextFieldWidget[] fields = new TextFieldWidget[3];
    private int panelX, panelY, panelWidth, panelHeight;

    public SafeSetupScreen(BlockPos pos) {
        super(Text.literal("금고 조합 설정"));
        this.pos = pos.toImmutable();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(330, width - 20);
        panelHeight = Math.min(180, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int innerX = panelX + 22;
        int gap = 8;
        int fieldWidth = (panelWidth - 44 - gap * 2) / 3;
        for (int i = 0; i < 3; i++) {
            fields[i] = addDrawableChild(new TextFieldWidget(textRenderer,
                    innerX + i * (fieldWidth + gap), panelY + 68, fieldWidth, 22,
                    Text.literal(Integer.toString(i + 1))));
            fields[i].setMaxLength(2);
            fields[i].setTextPredicate(s -> {
                if (s.isEmpty()) return true;
                if (!s.chars().allMatch(Character::isDigit)) return false;
                try { return Integer.parseInt(s) <= 99; }
                catch (NumberFormatException e) { return false; }
            });
            fields[i].setPlaceholder(Text.literal("00"));
        }

        int buttonWidth = 100;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> close())
                .dimensions(panelX + 30, panelY + 132, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("저장"), b -> save())
                .dimensions(panelX + panelWidth - 30 - buttonWidth, panelY + 132, buttonWidth, 20).build());
        fields[0].setFocused(true);
    }

    private void save() {
        int[] combo = new int[3];
        for (int i = 0; i < 3; i++) {
            String s = fields[i].getText().trim();
            if (s.isEmpty()) return;
            try { combo[i] = Integer.parseInt(s); }
            catch (NumberFormatException e) { return; }
            if (combo[i] < 0 || combo[i] > 99) return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeByte(combo[0]);
        buf.writeByte(combo[1]);
        buf.writeByte(combo[2]);
        ClientPlayNetworking.send(SceneToolsMod.SAFE_SETUP_PACKET, buf);
        close();
    }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(panelX - 3, panelY - 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0xFF070809);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF25292E);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + panelWidth / 2, panelY + 16, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("0~99 세 숫자 · 오른쪽 → 왼쪽 → 오른쪽"),
                panelX + panelWidth / 2, panelY + 40, 0xFFC9CDD1);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("→        ←        →"),
                panelX + panelWidth / 2, panelY + 56, 0xFFD9B44A);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
