package kr.koala.crouchlock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class ItemEditorScreen extends Screen {
    private final BlockPos editorPos;
    private final ItemStack previewStack;
    private TextFieldWidget nameField;
    private TextFieldWidget descriptionField;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public ItemEditorScreen(BlockPos editorPos, ItemStack previewStack) {
        super(Text.translatable("screen.korime_scene.item_editor.title"));
        this.editorPos = editorPos.toImmutable();
        this.previewStack = previewStack;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(310, width - 20);
        panelHeight = Math.min(190, height - 20);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int innerX = panelX + 56;
        int innerWidth = panelWidth - 72;

        nameField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX, panelY + 55, innerWidth, 20,
                Text.translatable("screen.korime_scene.item_editor.name")));
        nameField.setMaxLength(128);
        nameField.setPlaceholder(Text.translatable("screen.korime_scene.item_editor.name_placeholder"));
        if (!previewStack.isEmpty() && previewStack.hasCustomName()) nameField.setText(previewStack.getName().getString());

        descriptionField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX, panelY + 96, innerWidth, 20,
                Text.translatable("screen.korime_scene.item_editor.description")));
        descriptionField.setMaxLength(512);
        descriptionField.setEditableColor(0xB45CFF);
        descriptionField.setUneditableColor(0xB45CFF);
        descriptionField.setPlaceholder(Text.translatable("screen.korime_scene.item_editor.description_placeholder")
                .copy().styled(style -> style.withColor(0xB45CFF)));
        descriptionField.setText(readFirstLore(previewStack));

        int gap = 8;
        int buttonWidth = (innerWidth - gap) / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(innerX, panelY + 145, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.korime_scene.item_editor.apply"), button -> apply())
                .dimensions(innerX + buttonWidth + gap, panelY + 145, buttonWidth, 20).build());

        nameField.setFocused(true);
    }

    private static String readFirstLore(ItemStack stack) {
        if (stack.isEmpty() || stack.getNbt() == null) return "";
        NbtCompound root = stack.getNbt();
        if (!root.contains("display", NbtElement.COMPOUND_TYPE)) return "";
        NbtCompound display = root.getCompound("display");
        if (!display.contains("Lore", NbtElement.LIST_TYPE)) return "";
        NbtList lore = display.getList("Lore", NbtElement.STRING_TYPE);
        if (lore.isEmpty()) return "";
        try {
            JsonObject obj = JsonParser.parseString(lore.getString(0)).getAsJsonObject();
            return obj.has("text") ? obj.get("text").getAsString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private void apply() {
        if (previewStack.isEmpty()) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(editorPos);
        buf.writeString(nameField.getText(), 128);
        buf.writeString(descriptionField.getText(), 512);
        ClientPlayNetworking.send(SceneToolsMod.EDIT_ITEM_PACKET, buf);
        close();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Keep the world visible, like the smartphone editor.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(panelX - 3, panelY - 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0xFF080A0D);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFECE8F1);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 30, 0xFF2B2430);

        context.drawCenteredTextWithShadow(textRenderer, title, panelX + panelWidth / 2, panelY + 11, 0xFFFFFFFF);
        context.fill(panelX + 14, panelY + 48, panelX + 48, panelY + 82, 0xFFB9B1C0);
        if (!previewStack.isEmpty()) context.drawItem(previewStack, panelX + 23, panelY + 57);

        context.drawText(textRenderer, Text.translatable("screen.korime_scene.item_editor.name"), panelX + 56, panelY + 43, 0xFF39333D, false);
        context.drawText(textRenderer, Text.translatable("screen.korime_scene.item_editor.description"), panelX + 56, panelY + 84, 0xFF9B59D0, false);
        context.drawText(textRenderer, Text.translatable("screen.korime_scene.item_editor.hint"), panelX + 18, panelY + 125, 0xFF655C69, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
