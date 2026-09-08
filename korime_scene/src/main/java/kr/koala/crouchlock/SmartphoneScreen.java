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

public final class SmartphoneScreen extends Screen {
    private static final int PAGE_SIZE = 5;
    private static final int ROW_HEIGHT = 25;

    private final Hand hand;
    private final List<SmartphoneData.PhoneRecord> calls;
    private final List<SmartphoneData.PhoneRecord> messages;

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

    private TextFieldWidget nameField;
    private TextFieldWidget timeField;
    private TextFieldWidget detailField;

    public SmartphoneScreen(Hand hand, ItemStack stack) {
        super(Text.translatable("screen.crouchlock.smartphone"));
        this.hand = hand;
        SmartphoneData data = SmartphoneData.fromStack(stack);
        this.calls = new ArrayList<>(data.calls());
        this.messages = new ArrayList<>(data.messages());
    }

    @Override
    protected void init() {
        panelWidth = Math.min(300, width - 12);
        panelHeight = Math.min(252, height - 8);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int innerX = panelX + 15;
        int innerWidth = panelWidth - 30;
        listX = innerX;
        listY = panelY + 58;
        listWidth = innerWidth;

        callsTab = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.calls"),
                        button -> switchMode(true))
                .dimensions(innerX, panelY + 35, (innerWidth - 4) / 2, 18)
                .build());
        messagesTab = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.messages"),
                        button -> switchMode(false))
                .dimensions(innerX + (innerWidth + 4) / 2, panelY + 35,
                        (innerWidth - 4) / 2, 18)
                .build());

        previousPage = addDrawableChild(ButtonWidget.builder(Text.literal("‹"), button -> changePage(-1))
                .dimensions(innerX, panelY + 187, 28, 18)
                .build());
        nextPage = addDrawableChild(ButtonWidget.builder(Text.literal("›"), button -> changePage(1))
                .dimensions(innerX + innerWidth - 28, panelY + 187, 28, 18)
                .build());

        nameField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX, panelY + 211, 94, 18, Text.translatable("screen.crouchlock.smartphone.name")));
        nameField.setMaxLength(SmartphoneData.MAX_NAME_LENGTH);
        nameField.setPlaceholder(Text.translatable("screen.crouchlock.smartphone.name"));

        timeField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX + 98, panelY + 211, 58, 18, Text.translatable("screen.crouchlock.smartphone.time")));
        timeField.setMaxLength(SmartphoneData.MAX_TIME_LENGTH);
        timeField.setPlaceholder(Text.translatable("screen.crouchlock.smartphone.time"));

        detailField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX + 160, panelY + 211, innerWidth - 160, 18,
                Text.translatable("screen.crouchlock.smartphone.detail")));
        detailField.setMaxLength(SmartphoneData.MAX_DETAIL_LENGTH);
        detailField.setPlaceholder(Text.translatable("screen.crouchlock.smartphone.detail"));

        int actionGap = 4;
        int actionWidth = (innerWidth - actionGap * 2) / 3;
        addButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.add"), button -> addRecord())
                .dimensions(innerX, panelY + 232, actionWidth, 16)
                .build());
        applyButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.apply"), button -> applyRecord())
                .dimensions(innerX + actionWidth + actionGap, panelY + 232, actionWidth, 16)
                .build());
        deleteButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.delete"), button -> deleteRecord())
                .dimensions(innerX + (actionWidth + actionGap) * 2, panelY + 232, actionWidth, 16)
                .build());

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
                panelX + panelWidth / 2, panelY + 192, 0xFFB9C8D6);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPhoneChrome(DrawContext context) {
        context.fill(panelX - 5, panelY - 4, panelX + panelWidth + 5, panelY + panelHeight + 5, 0x66000000);
        context.fill(panelX - 3, panelY - 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0xFF050709);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF171D22);
        context.fill(panelX + 7, panelY + 7, panelX + panelWidth - 7, panelY + panelHeight - 7, 0xFFF3F3F3);

        context.fill(panelX + 8, panelY + 8, panelX + panelWidth - 8, panelY + 31, 0xFF28333D);
        context.fill(panelX + panelWidth / 2 - 24, panelY + 5,
                panelX + panelWidth / 2 + 18, panelY + 7, 0xFF050709);
        context.fill(panelX + panelWidth / 2 + 22, panelY + 5,
                panelX + panelWidth / 2 + 25, panelY + 8, 0xFF0A0D10);

        int iconColor = showingCalls ? 0xFF2FB55D : 0xFF318BEA;
        context.fill(panelX + 14, panelY + 13, panelX + 24, panelY + 23, iconColor);
        context.drawTextWithShadow(textRenderer, showingCalls ? Text.literal("☎") : Text.literal("✉"),
                panelX + 15, panelY + 13, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer,
                showingCalls
                        ? Text.translatable("screen.crouchlock.smartphone.calls")
                        : Text.translatable("screen.crouchlock.smartphone.messages"),
                panelX + 30, panelY + 14, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("10:24"),
                panelX + panelWidth - 45, panelY + 14, 0xFFE7EDF3);

        context.fill(panelX + 10, panelY + 55, panelX + panelWidth - 10, panelY + 184, 0xFFFFFFFF);
        context.fill(panelX + 10, panelY + 55, panelX + panelWidth - 10, panelY + 56, 0xFFD0D4D8);
        context.fill(panelX + 10, panelY + 184, panelX + panelWidth - 10, panelY + 185, 0xFFD0D4D8);

        context.fill(panelX + 11, panelY + 207, panelX + panelWidth - 11, panelY + 231, 0xFFE8EBEE);
        context.fill(panelX + panelWidth / 2 - 23, panelY + panelHeight - 4,
                panelX + panelWidth / 2 + 23, panelY + panelHeight - 2, 0xFF8B949C);
    }

    private void drawRecordList(DrawContext context, int mouseX, int mouseY) {
        List<SmartphoneData.PhoneRecord> records = activeRecords();

        for (int row = 0; row < PAGE_SIZE; row++) {
            int index = page * PAGE_SIZE + row;
            int y = listY + row * ROW_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX < listX + listWidth
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            boolean isSelected = index == selected;

            if (hovered || isSelected) {
                context.fill(listX, y, listX + listWidth, y + ROW_HEIGHT, isSelected ? 0xFFE6F1FB : 0xFFF1F4F6);
            }
            if (row > 0) {
                context.fill(listX + 34, y, listX + listWidth, y + 1, 0xFFE1E4E7);
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

            drawAvatar(context, listX + 4, y + 4, index, showingCalls);

            context.drawText(textRenderer, Text.literal(textRenderer.trimToWidth(name, listWidth - 96)),
                    listX + 38, y + 4, 0xFF1F252B, false);
            int timeWidth = textRenderer.getWidth(time);
            context.drawText(textRenderer, Text.literal(time), listX + listWidth - timeWidth - 5,
                    y + 4, 0xFF6C747B, false);

            String secondary = showingCalls
                    ? callSecondaryText(detail)
                    : textRenderer.trimToWidth(detail, listWidth - 52);
            int secondaryColor = showingCalls && isMissedCall(detail) ? 0xFFD83A3A : 0xFF707980;
            context.drawText(textRenderer, Text.literal(secondary), listX + 38, y + 14, secondaryColor, false);
        }
    }

    private void drawAvatar(DrawContext context, int x, int y, int index, boolean callMode) {
        int[] palette = {0xFFBA704A, 0xFF6AA6D8, 0xFF63A875, 0xFF9A7AC2, 0xFFC09048, 0xFF70767C};
        int color = palette[Math.floorMod(index, palette.length)];
        context.fill(x, y, x + 26, y + 17, color);
        context.fill(x + 3, y + 3, x + 23, y + 14, 0x33000000);
        context.drawCenteredTextWithShadow(textRenderer,
                callMode ? Text.literal("☎") : Text.literal("✉"), x + 13, y + 4, 0xFFFFFFFF);
    }

    private String callSecondaryText(String detail) {
        if (detail.isBlank()) {
            return "☎  " + Text.translatable("screen.crouchlock.smartphone.calls").getString();
        }
        String prefix = isMissedCall(detail) ? "↙  " : "↗  ";
        return prefix + textRenderer.trimToWidth(detail, listWidth - 58);
    }

    private boolean isMissedCall(String detail) {
        String normalized = detail.toLowerCase();
        return normalized.contains("miss") || normalized.contains("부재") || normalized.contains("실패")
                || normalized.contains("못") || normalized.contains("끊");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= listX && mouseX < listX + listWidth
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
        syncToServer();
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
        commitFields();
        List<SmartphoneData.PhoneRecord> records = activeRecords();
        if (records.size() >= SmartphoneData.MAX_RECORDS) {
            return;
        }
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
        List<SmartphoneData.PhoneRecord> records = activeRecords();
        if (selected < 0 || selected >= records.size()) {
            return;
        }
        records.remove(selected);
        selected = -1;
        int totalPages = Math.max(1, (records.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, totalPages - 1);
        clearFields();
        refreshWidgets();
    }

    private void commitFields() {
        List<SmartphoneData.PhoneRecord> records = activeRecords();
        if (selected < 0 || selected >= records.size() || nameField == null) {
            return;
        }
        records.set(selected, new SmartphoneData.PhoneRecord(
                nameField.getText(), detailField.getText(), timeField.getText()));
    }

    private void loadSelectedIntoFields() {
        if (nameField == null) {
            return;
        }
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
        if (callsTab == null) {
            return;
        }

        List<SmartphoneData.PhoneRecord> records = activeRecords();
        int totalPages = Math.max(1, (records.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, totalPages - 1);

        callsTab.active = !showingCalls;
        messagesTab.active = showingCalls;
        previousPage.active = page > 0;
        nextPage.active = page + 1 < totalPages;
        addButton.active = records.size() < SmartphoneData.MAX_RECORDS;
        boolean hasSelection = selected >= 0 && selected < records.size();
        applyButton.active = hasSelection;
        deleteButton.active = hasSelection;
        nameField.setEditable(hasSelection);
        timeField.setEditable(hasSelection);
        detailField.setEditable(hasSelection);
    }

    private void syncToServer() {
        if (synced) {
            return;
        }
        commitFields();
        SmartphoneData data = new SmartphoneData(calls, messages);

        if (client != null && client.player != null) {
            ItemStack held = client.player.getStackInHand(hand);
            if (held.isOf(SmartphoneMod.SMARTPHONE)) {
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
    }
}
