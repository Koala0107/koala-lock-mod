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

import java.util.ArrayList;
import java.util.List;

public final class SmartphoneScreen extends Screen {
    private static final int PAGE_SIZE = 4;

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

    private ButtonWidget callsTab;
    private ButtonWidget messagesTab;
    private final ButtonWidget[] rowButtons = new ButtonWidget[PAGE_SIZE];
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
        panelWidth = Math.min(308, width - 12);
        panelHeight = Math.min(232, height - 8);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int innerX = panelX + 14;
        int innerWidth = panelWidth - 28;

        callsTab = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.calls"),
                        button -> switchMode(true))
                .dimensions(innerX, panelY + 29, (innerWidth - 4) / 2, 20)
                .build());
        messagesTab = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.messages"),
                        button -> switchMode(false))
                .dimensions(innerX + (innerWidth + 4) / 2, panelY + 29,
                        (innerWidth - 4) / 2, 20)
                .build());

        for (int i = 0; i < PAGE_SIZE; i++) {
            final int row = i;
            rowButtons[i] = addDrawableChild(ButtonWidget.builder(Text.empty(),
                            button -> selectRow(row))
                    .dimensions(innerX, panelY + 55 + i * 22, innerWidth, 20)
                    .build());
        }

        previousPage = addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> changePage(-1))
                .dimensions(innerX, panelY + 143, 28, 18)
                .build());
        nextPage = addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> changePage(1))
                .dimensions(innerX + innerWidth - 28, panelY + 143, 28, 18)
                .build());

        nameField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX, panelY + 169, 96, 18, Text.translatable("screen.crouchlock.smartphone.name")));
        nameField.setMaxLength(SmartphoneData.MAX_NAME_LENGTH);
        nameField.setPlaceholder(Text.translatable("screen.crouchlock.smartphone.name"));

        timeField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX + 100, panelY + 169, 58, 18, Text.translatable("screen.crouchlock.smartphone.time")));
        timeField.setMaxLength(SmartphoneData.MAX_TIME_LENGTH);
        timeField.setPlaceholder(Text.translatable("screen.crouchlock.smartphone.time"));

        detailField = addDrawableChild(new TextFieldWidget(textRenderer,
                innerX + 162, panelY + 169, innerWidth - 162, 18,
                Text.translatable("screen.crouchlock.smartphone.detail")));
        detailField.setMaxLength(SmartphoneData.MAX_DETAIL_LENGTH);
        detailField.setPlaceholder(Text.translatable("screen.crouchlock.smartphone.detail"));

        int actionGap = 4;
        int actionWidth = (innerWidth - actionGap * 2) / 3;
        addButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.add"), button -> addRecord())
                .dimensions(innerX, panelY + 191, actionWidth, 18)
                .build());
        applyButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.apply"), button -> applyRecord())
                .dimensions(innerX + actionWidth + actionGap, panelY + 191, actionWidth, 18)
                .build());
        deleteButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.delete"), button -> deleteRecord())
                .dimensions(innerX + (actionWidth + actionGap) * 2, panelY + 191, actionWidth, 18)
                .build());

        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.crouchlock.smartphone.save_close"), button -> close())
                .dimensions(innerX, panelY + 213, innerWidth, 16)
                .build());

        loadSelectedIntoFields();
        refreshWidgets();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        context.fill(panelX - 3, panelY - 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0xFF080B0E);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF20262C);
        context.fill(panelX + 8, panelY + 7, panelX + panelWidth - 8, panelY + panelHeight - 7, 0xFF111820);
        context.fill(panelX + panelWidth / 2 - 22, panelY + 4,
                panelX + panelWidth / 2 + 22, panelY + 7, 0xFF06090B);

        context.drawCenteredTextWithShadow(textRenderer, title,
                panelX + panelWidth / 2, panelY + 11, 0xFFFFFFFF);

        List<SmartphoneData.PhoneRecord> records = activeRecords();
        int totalPages = Math.max(1, (records.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        Text status = Text.translatable("screen.crouchlock.smartphone.page",
                page + 1, totalPages, records.size());
        context.drawCenteredTextWithShadow(textRenderer, status,
                panelX + panelWidth / 2, panelY + 148, 0xFF9FB6C9);

        super.render(context, mouseX, mouseY, delta);
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

    private void selectRow(int row) {
        commitFields();
        int index = page * PAGE_SIZE + row;
        if (index >= activeRecords().size()) {
            return;
        }
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

        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            ButtonWidget button = rowButtons[i];
            if (index < records.size()) {
                SmartphoneData.PhoneRecord record = records.get(index);
                String name = record.name().isBlank()
                        ? Text.translatable("screen.crouchlock.smartphone.unnamed").getString()
                        : record.name();
                String time = record.time().isBlank() ? "--:--" : record.time();
                String detail = record.detail().isBlank() ? "..." : record.detail();
                String marker = index == selected ? "> " : "  ";
                String line = marker + (index + 1) + ". " + name + "  " + time + "  " + detail;
                button.setMessage(Text.literal(textRenderer.trimToWidth(line, button.getWidth() - 12)));
                button.active = true;
            } else {
                button.setMessage(Text.literal("—"));
                button.active = false;
            }
        }
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
