package kr.koala.korime_scene;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class SafeDialScreen extends Screen {
    private static final double TWO_PI = Math.PI * 2.0;

    private final BlockPos pos;
    private final int[] entered = {-1, -1, -1};

    private int centerX;
    private int centerY;
    private int radius;
    private double dialAngle;
    private double lastPointerAngle;
    private boolean dragging;
    private int lastDirection;
    private int stage;
    private int previousNumber;

    public SafeDialScreen(BlockPos pos) {
        super(Text.literal("금고 다이얼"));
        this.pos = pos.toImmutable();
    }

    @Override
    protected void init() {
        centerX = width / 2;
        centerY = height / 2 + 2;
        radius = Math.min(92, Math.max(58, Math.min(width, height) / 4));

        int buttonWidth = 92;
        addDrawableChild(ButtonWidget.builder(Text.literal("열기"), b -> attemptOpen())
                .dimensions(centerX - buttonWidth / 2, centerY + radius + 18, buttonWidth, 20).build());
    }

    private static double pointerAngle(double mouseX, double mouseY, double cx, double cy) {
        return Math.atan2(mouseY - cy, mouseX - cx);
    }

    private static double normalizedDelta(double delta) {
        while (delta > Math.PI) delta -= TWO_PI;
        while (delta < -Math.PI) delta += TWO_PI;
        return delta;
    }

    private static double wrapAngle(double angle) {
        angle %= TWO_PI;
        if (angle < 0) angle += TWO_PI;
        return angle;
    }

    private int currentNumber() {
        double fromTop = wrapAngle(dialAngle + Math.PI / 2.0);
        return Math.floorMod((int)Math.round(fromTop / TWO_PI * 100.0), 100);
    }

    private int expectedDirection() {
        return stage == 1 ? -1 : 1;
    }

    private void handleTurn(double delta) {
        if (Math.abs(delta) < 0.001) return;
        int direction = delta > 0 ? 1 : -1;
        int before = currentNumber();
        dialAngle = wrapAngle(dialAngle + delta);
        int after = currentNumber();

        if (after != before && client != null && client.player != null) {
            client.player.playSound(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 0.10F, 1.7F);
        }

        if (lastDirection != 0 && direction != lastDirection && stage < 2) {
            if (lastDirection == expectedDirection()) {
                entered[stage] = previousNumber;
                stage++;
            } else {
                resetSequence();
            }
        }

        lastDirection = direction;
        previousNumber = after;
    }

    private void resetSequence() {
        entered[0] = entered[1] = entered[2] = -1;
        stage = 0;
        lastDirection = 0;
    }

    private void attemptOpen() {
        if (stage != 2 || lastDirection != 1) {
            resetSequence();
            return;
        }
        entered[2] = currentNumber();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeByte(entered[0]);
        buf.writeByte(entered[1]);
        buf.writeByte(entered[2]);
        ClientPlayNetworking.send(SceneToolsMod.SAFE_ATTEMPT_PACKET, buf);
        close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            double dx = mouseX - centerX;
            double dy = mouseY - centerY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= radius && distance >= radius * 0.22) {
                dragging = true;
                lastPointerAngle = pointerAngle(mouseX, mouseY, centerX, centerY);
                previousNumber = currentNumber();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            double next = pointerAngle(mouseX, mouseY, centerX, centerY);
            double delta = normalizedDelta(next - lastPointerAngle);
            lastPointerAngle = next;
            handleTurn(delta);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelWidth = radius * 2 + 72;
        int panelHeight = radius * 2 + 102;
        int x0 = centerX - panelWidth / 2;
        int y0 = centerY - radius - 42;

        context.fill(x0 - 3, y0 - 3, x0 + panelWidth + 3, y0 + panelHeight + 3, 0xEE050607);
        context.fill(x0, y0, x0 + panelWidth, y0 + panelHeight, 0xEE202429);
        context.drawCenteredTextWithShadow(textRenderer, title, centerX, y0 + 13, 0xFFF1F1F1);
        drawDial(context);

        String direction = stage == 0 ? "오른쪽 →" : stage == 1 ? "← 왼쪽" : "오른쪽 →";
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal((stage + 1) + "번째 · " + direction),
                centerX, centerY + radius + 3, 0xFFD4D7DA);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawDial(DrawContext context) {
        int outer = radius;
        int inner = (int)(radius * 0.79);
        int hub = (int)(radius * 0.20);

        fillCircle(context, centerX, centerY, outer, 0xFF0B0D10);
        fillCircle(context, centerX, centerY, outer - 5, 0xFF4A5057);
        fillCircle(context, centerX, centerY, inner, 0xFF171A1E);

        for (int n = 0; n < 100; n++) {
            double a = n / 100.0 * TWO_PI - Math.PI / 2.0;
            int len = n % 10 == 0 ? 12 : (n % 5 == 0 ? 9 : 5);
            int x1 = centerX + (int)Math.round(Math.cos(a) * (outer - 7));
            int y1 = centerY + (int)Math.round(Math.sin(a) * (outer - 7));
            int x2 = centerX + (int)Math.round(Math.cos(a) * (outer - 7 - len));
            int y2 = centerY + (int)Math.round(Math.sin(a) * (outer - 7 - len));
            drawLine(context, x1, y1, x2, y2, n % 10 == 0 ? 0xFFF0F0F0 : 0xFF9CA1A6);
        }

        for (int n = 0; n < 100; n += 10) {
            double a = n / 100.0 * TWO_PI - Math.PI / 2.0;
            int tx = centerX + (int)Math.round(Math.cos(a) * (inner - 12));
            int ty = centerY + (int)Math.round(Math.sin(a) * (inner - 12));
            String s = Integer.toString(n);
            context.drawText(textRenderer, Text.literal(s), tx - textRenderer.getWidth(s) / 2, ty - 4, 0xFFDFE2E5, false);
        }

        double pointer = dialAngle - Math.PI / 2.0;
        int px = centerX + (int)Math.round(Math.cos(pointer) * (inner - 9));
        int py = centerY + (int)Math.round(Math.sin(pointer) * (inner - 9));
        drawLine(context, centerX, centerY, px, py, 0xFFD9B44A);
        drawLine(context, centerX + 1, centerY, px + 1, py, 0xFFD9B44A);
        fillCircle(context, centerX, centerY, hub, 0xFF24282D);
        fillCircle(context, centerX, centerY, Math.max(4, hub - 5), 0xFF777E86);

        String number = String.format("%02d", currentNumber());
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(number), centerX, centerY - 5, 0xFFFFFFFF);
    }

    private static void fillCircle(DrawContext context, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            int half = (int)Math.sqrt(Math.max(0, r * r - y * y));
            context.fill(cx - half, cy + y, cx + half + 1, cy + y + 1, color);
        }
    }

    private static void drawLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            context.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    @Override public boolean shouldPause() { return false; }
}
