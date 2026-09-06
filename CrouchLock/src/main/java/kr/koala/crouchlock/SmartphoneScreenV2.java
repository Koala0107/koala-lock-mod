package kr.koala.crouchlock;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Compact phone UI. Finalized phones are view-only, like a signed written book. */
public final class SmartphoneScreenV2 extends Screen {
    private static final int PAGE_SIZE = 4;
    private static final int ROW_HEIGHT = 21;
    private static final int LIST_ICON_SIZE = 16;

    private final Hand hand;
    private final boolean editable;
    private final List<SmartphoneData.PhoneRecord> calls;
    private final List<SmartphoneData.PhoneRecord> messages;
    private final String savedTitle;
    private final String savedSubtitle;

    private boolean showingCalls = true;
    private int page;
    private int selected = -1;
    private boolean synced;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listX;
    private int listY;
    private int listWidth;

    private ButtonWidget callsTab;
    private ButtonWidget messagesTab;
    private ButtonWidget previousPage;
    private ButtonWidget nextPage;
    private ButtonWidget addButton;
    private ButtonWidget applyButton;
    private ButtonWidget deleteButton;
    private ButtonWidget finalizeButton;
    private TextFieldWidget nameField;
    private TextFieldWidget timeField;
    private TextFieldWidget detailField;

    public SmartphoneScreenV2(Hand hand, ItemStack stack) {
        this(hand, SmartphoneData.fromStack(stack), true);
    }

    /** Used by an installed phone block. Installed phones are always opened read-only. */
    public SmartphoneScreenV2(SmartphoneData data) {
        this(null, data, false);
    }

    private SmartphoneScreenV2(Hand hand, SmartphoneData data, boolean allowEditing) {
        super(Text.translatable("screen.crouchlock.smartphone"));
        this.hand = hand;
        this.editable = allowEditing && !data.finalized();
        this.calls = new ArrayList<>(data.calls());
        this.messages = new ArrayList<>(data.messages());
        this.savedTitle = data.title();
        this.savedSubtitle = data.subtitle();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(278, width - 12);
        panelHeight = editable ? Math.min(202, height - 8) : Math.min(160, height - 8);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int innerX = panelX + 13;
        int innerWidth = panelWidth - 26;
        listX = innerX;
        listY = panelY + 51;
        listWidth = innerWidth;

        callsTab = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.calls"),
                        button -> switchMode(true))
                .dimensions(innerX, panelY + 31, (innerWidth - 4) / 2, 17)
                .build());
        messagesTab = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.messages"),
                        button -> switchMode(false))
                .dimensions(innerX + (innerWidth + 4) / 2, panelY + 31,
                        (innerWidth - 4) / 2, 17)
                .build());

        int pageY = panelY + 137;
        previousPage = addDrawableChild(ButtonWidget.builder(Text.literal("‹"), button -> changePage(-1))
                .dimensions(innerX, pageY, 24, 16)
                .build());
        nextPage = addDrawableChild(ButtonWidget.builder(Text.literal("›"), button -> changePage(1))
                .dimensions(innerX + innerWidth - 24, pageY, 24, 16)
                .build());

        if (editable) {
            int fieldsY = panelY + 157;
            nameField = addDrawableChild(new TextFieldWidget(textRenderer,
                    innerX, fieldsY, 86, 18, Text.translatable("screen.crouchlock.smartphone.name")));
            nameField.setMaxLength(SmartphoneData.MAX_NAME_LENGTH);
            nameField.setPlaceholder(Text.translatable("screen.crouchlock.smartphone.name"));

            timeField = addDrawableChild(new TextFieldWidget(textRenderer,
                    innerX + 90, fieldsY, 52, 18, Text.translatable("screen.crouchlock.smartphone.time")));
            timeField.setMaxLength(SmartphoneData.MAX_TIME_LENGTH);
            timeField.setPlaceholder(Text.translatable("screen.crouchlock.smartphone.time"));

            detailField = addDrawableChild(new TextFieldWidget(textRenderer,
                    innerX + 146, fieldsY, innerWidth - 146, 18,
                    Text.translatable("screen.crouchlock.smartphone.detail")));
            detailField.setMaxLength(SmartphoneData.MAX_DETAIL_LENGTH);
            detailField.setPlaceholder(Text.translatable("screen.crouchlock.smartphone.detail"));

            int buttonY = panelY + 180;
            int gap = 3;
            int buttonWidth = (innerWidth - gap * 3) / 4;
            addButton = addDrawableChild(ButtonWidget.builder(
                            Text.translatable("screen.crouchlock.smartphone.add"), button -> addRecord())
                    .dimensions(innerX, buttonY, buttonWidth, 16).build());
            applyButton = addDrawableChild(ButtonWidget.builder(
                            Text.translatable("screen.crouchlock.smartphone.apply"), button -> applyRecord())
                    .dimensions(innerX + buttonWidth + gap, buttonY, buttonWidth, 16).build());
            deleteButton = addDrawableChild(ButtonWidget.builder(
                            Text.translatable("screen.crouchlock.smartphone.delete"), button -> deleteRecord())
                    .dimensions(innerX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 16).build());
            finalizeButton = addDrawableChild(ButtonWidget.builder(
                            Text.translatable("screen.crouchlock.smartphone.finalize"), button -> finalizePhone())
                    .dimensions(innerX + (buttonWidth + gap) * 3, buttonY,
                            innerWidth - (buttonWidth + gap) * 3, 16).build());
        }

        loadSelectedIntoFields();
        refreshWidgets();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        drawPhoneChrome(context);
        drawRecordList(context, mouseX, mouseY);

        List<SmartphoneData.PhoneRecord> records = activeRecords();
        int totalPages = Math.max(1, (records.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        Text status = Text.translatable("screen.crouchlock.smartphone.page",
                page + 1, totalPages, records.size());
        context.drawCenteredTextWithShadow(textRenderer, status,
                panelX + panelWidth / 2, panelY + 141, 0xFF58616A);

        if (!editable) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("screen.crouchlock.smartphone.read_only"),
                    panelX + panelWidth / 2, panelY + 151, 0xFF6C747B);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPhoneChrome(DrawContext context) {
        context.fill(panelX - 5, panelY - 4, panelX + panelWidth + 5, panelY + panelHeight + 5, 0x66000000);
        context.fill(panelX - 3, panelY - 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0xFF050709);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF171D22);
        context.fill(panelX + 6, panelY + 6, panelX + panelWidth - 6, panelY + panelHeight - 6, 0xFFF4F4F4);

        context.fill(panelX + 7, panelY + 7, panelX + panelWidth - 7, panelY + 28, 0xFF28333D);
        context.fill(panelX + panelWidth / 2 - 22, panelY + 4,
                panelX + panelWidth / 2 + 17, panelY + 7, 0xFF050709);

        int iconColor = showingCalls ? 0xFF2FB55D : 0xFF318BEA;
        drawModeIcon(context, panelX + 13, panelY + 11, 12, showingCalls, iconColor);
        context.drawTextWithShadow(textRenderer,
                showingCalls
                        ? Text.translatable("screen.crouchlock.smartphone.calls")
                        : Text.translatable("screen.crouchlock.smartphone.messages"),
                panelX + 31, panelY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("10:24"),
                panelX + panelWidth - 44, panelY + 12, 0xFFE7EDF3);

        context.fill(panelX + 9, panelY + 49, panelX + panelWidth - 9,
                panelY + 136, 0xFFFFFFFF);
        context.fill(panelX + 9, panelY + 49, panelX + panelWidth - 9, panelY + 50, 0xFFD0D4D8);
        context.fill(panelX + 9, panelY + 135, panelX + panelWidth - 9, panelY + 136, 0xFFD0D4D8);

        context.fill(panelX + panelWidth / 2 - 21, panelY + panelHeight - 4,
                panelX + panelWidth / 2 + 21, panelY + panelHeight - 2, 0xFF8B949C);
    }

    private void drawRecordList(DrawContext context, int mouseX, int mouseY) {
        List<SmartphoneData.PhoneRecord> records = activeRecords();
        for (int row = 0; row < PAGE_SIZE; row++) {
            int index = page * PAGE_SIZE + row;
            int y = listY + row * ROW_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX < listX + listWidth
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            boolean isSelected = editable && index == selected;

            if (hovered || isSelected) {
                context.fill(listX, y, listX + listWidth, y + ROW_HEIGHT,
                        isSelected ? 0xFFE3F0FB : 0xFFF1F4F6);
            }
            if (row > 0) {
                context.fill(listX + 25, y, listX + listWidth, y + 1, 0xFFE1E4E7);
            }
            if (index >= records.size()) {
                continue;
            }

            SmartphoneData.PhoneRecord record = records.get(index);
            String name = record.name().isBlank()
                    ? Text.translatable("screen.crouchlock.smartphone.unnamed").getString()
                    : record.name();
            String time = record.time().isBlank() ? "--:--" : record.time();
            String detail = record.detail().isBlank() ? "..." : record.detail();

            drawAvatar(context, listX + 4, y + 2, index);
            int textX = listX + 25;
            int timeWidth = textRenderer.getWidth(time);
            context.drawText(textRenderer,
                    Text.literal(textRenderer.trimToWidth(name, listWidth - 82)),
                    textX, y + 2, 0xFF1F252B, false);
            context.drawText(textRenderer, Text.literal(time),
                    listX + listWidth - timeWidth - 4, y + 2, 0xFF6C747B, false);

            String secondary = showingCalls ? callSecondaryText(detail)
                    : textRenderer.trimToWidth(detail, listWidth - 34);
            int color = showingCalls && isMissedCall(detail) ? 0xFFD83A3A : 0xFF707980;
            context.drawText(textRenderer, Text.literal(secondary), textX, y + 11, color, false);
        }
    }

    private void drawAvatar(DrawContext context, int x, int y, int index) {
        int[] palette = {0xFFBA704A, 0xFF6AA6D8, 0xFF63A875, 0xFF9A7AC2, 0xFFC09048, 0xFF70767C};
        int color = palette[Math.floorMod(index, palette.length)];
        drawModeIcon(context, x, y, LIST_ICON_SIZE, showingCalls, color);
    }

    private void drawModeIcon(DrawContext context, int x, int y, int size, boolean callMode, int background) {
        context.fill(x, y, x + size, y + size, background);
        int white = 0xFFFFFFFF;
        if (callMode) {
            int left = x + Math.max(2, size / 5);
            int top = y + Math.max(2, size / 5);
            int thick = Math.max(2, size / 6);
            context.fill(left, top, left + thick, top + thick * 2, white);
            context.fill(left + thick, top + thick, x + size - thick - 1, top + thick * 2, white);
            context.fill(x + size - thick * 2 - 1, top + thick, x + size - thick - 1, top + thick * 3, white);
        } else {
            int pad = Math.max(2, size / 5);
            context.fill(x + pad, y + pad, x + size - pad, y + size - pad - 1, white);
            context.fill(x + pad + 2, y + size - pad - 1,
                    x + pad + 5, y + size - pad + 2, white);
            int dotY = y + size / 2;
            context.fill(x + pad + 2, dotY, x + pad + 3, dotY + 1, background);
            context.fill(x + size / 2, dotY, x + size / 2 + 1, dotY + 1, background);
            context.fill(x + size - pad - 3, dotY, x + size - pad - 2, dotY + 1, background);
        }
    }

    private String callSecondaryText(String detail) {
        String prefix = isMissedCall(detail) ? "↙  " : "↗  ";
        return prefix + textRenderer.trimToWidth(detail.isBlank()
                ? Text.translatable("screen.crouchlock.smartphone.call_default").getString()
                : detail, listWidth - 42);
    }

    private boolean isMissedCall(String detail) {
        String normalized = detail.toLowerCase();
        return normalized.contains("miss") || normalized.contains("부재") || normalized.contains("실패")
                || normalized.contains("못") || normalized.contains("끊");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editable && button == 0 && mouseX >= listX && mouseX < listX + listWidth
                && mouseY >= listY && mouseY < listY + PAGE_SIZE * ROW_HEIGHT) {
            int row = MathHelper.floor((mouseY - listY) / ROW_HEIGHT);
            int index = page * PAGE_SIZE + row;
            if (index < activeRecords().size()) {
                selectIndex(index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        // Esc abandons the draft. Only the explicit save flow writes/finalizes the phone.
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private List<SmartphoneData.PhoneRecord> activeRecords() {
        return showingCalls ? calls : messages;
    }

    private void switchMode(boolean callsMode) {
        if (showingCalls == callsMode) {
            return;
        }
        commitFields();
        showingCalls = callsMode;
        page = 0;
        selected = -1;
        clearFields();
        refreshWidgets();
    }

    private void selectIndex(int index) {
        commitFields();
        selected = index;
        loadSelectedIntoFields();
        refreshWidgets();
    }

    private void changePage(int delta) {
        commitFields();
        List<SmartphoneData.PhoneRecord> records = activeRecords();
        int totalPages = Math.max(1, (records.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(totalPages - 1, page + delta));
        selected = -1;
        clearFields();
        refreshWidgets();
    }

    private void addRecord() {
        if (!editable) return;
        commitFields();
        List<SmartphoneData.PhoneRecord> records = activeRecords();
        if (records.size() >= SmartphoneData.MAX_RECORDS) return;
        records.add(new SmartphoneData.PhoneRecord("", "", ""));
        selected = records.size() - 1;
        page = selected / PAGE_SIZE;
        loadSelectedIntoFields();
        refreshWidgets();
        nameField.setFocused(true);
    }

    private void applyRecord() {
        commitFields();
        refreshWidgets();
    }

    private void deleteRecord() {
        if (!editable) return;
        List<SmartphoneData.PhoneRecord> records = activeRecords();
        if (selected < 0 || selected >= records.size()) return;
        records.remove(selected);
        selected = -1;
        int totalPages = Math.max(1, (records.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, totalPages - 1);
        clearFields();
        refreshWidgets();
    }

    private void finalizePhone() {
        if (!editable || synced || hand == null || client == null) return;
        commitFields();
        String title = savedTitle.isBlank()
                ? Text.translatable("screen.crouchlock.smartphone.save.default_name").getString()
                : savedTitle;
        String subtitle = savedSubtitle.isBlank()
                ? Text.translatable("screen.crouchlock.smartphone.save.default_subtitle").getString()
                : savedSubtitle;
        client.setScreen(new SmartphoneFinalizeScreen(this, title, subtitle));
    }

    void finalizeWithMetadata(String title, String subtitle) {
        if (!editable || synced || hand == null) return;
        commitFields();
        SmartphoneData data = new SmartphoneData(calls, messages, true, title, subtitle);

        if (client != null && client.player != null) {
            ItemStack held = client.player.getStackInHand(hand);
            if (held.isOf(SmartphoneMod.SMARTPHONE) && !SmartphoneData.fromStack(held).finalized()) {
                data.writeTo(held);
            }
        }

        if (ClientPlayNetworking.canSend(SmartphoneMod.SAVE_PACKET)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeEnumConstant(hand);
            data.writePacket(buf);
            ClientPlayNetworking.send(SmartphoneMod.SAVE_PACKET, buf);
        }
        synced = true;
        if (client != null) {
            client.setScreen(null);
        }
    }

    private void commitFields() {
        if (!editable || nameField == null) return;
        List<SmartphoneData.PhoneRecord> records = activeRecords();
        if (selected < 0 || selected >= records.size()) return;
        records.set(selected, new SmartphoneData.PhoneRecord(
                nameField.getText(), detailField.getText(), timeField.getText()));
    }

    private void loadSelectedIntoFields() {
        if (!editable || nameField == null) return;
        List<SmartphoneData.PhoneRecord> records = activeRecords();
        if (selected < 0 || selected >= records.size()) {
            clearFields();
            return;
        }
        SmartphoneData.PhoneRecord record = records.get(selected);
        nameField.setText(record.name());
        timeField.setText(record.time());
        detailField.setText(record.detail());
    }

    private void clearFields() {
        if (nameField != null) {
            nameField.setText("");
            timeField.setText("");
            detailField.setText("");
        }
    }

    private void refreshWidgets() {
        if (callsTab == null) return;
        List<SmartphoneData.PhoneRecord> records = activeRecords();
        int totalPages = Math.max(1, (records.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, totalPages - 1);

        callsTab.active = !showingCalls;
        messagesTab.active = showingCalls;
        previousPage.active = page > 0;
        nextPage.active = page + 1 < totalPages;

        if (editable) {
            addButton.active = records.size() < SmartphoneData.MAX_RECORDS;
            boolean hasSelection = selected >= 0 && selected < records.size();
            applyButton.active = hasSelection;
            deleteButton.active = hasSelection;
            nameField.setEditable(hasSelection);
            timeField.setEditable(hasSelection);
            detailField.setEditable(hasSelection);
            finalizeButton.active = true;
        }
    }
}
