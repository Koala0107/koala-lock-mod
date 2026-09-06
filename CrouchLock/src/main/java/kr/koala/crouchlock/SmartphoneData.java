package kr.koala.crouchlock;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Per-item smartphone evidence data stored directly in the ItemStack NBT. */
public final class SmartphoneData {
    public static final int MAX_RECORDS = 32;
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_DETAIL_LENGTH = 160;
    public static final int MAX_TIME_LENGTH = 16;

    private static final String ROOT_KEY = "CrouchLockSmartphone";
    private static final String CALLS_KEY = "Calls";
    private static final String MESSAGES_KEY = "Messages";
    private static final String FINALIZED_KEY = "Finalized";
    private static final String NAME_KEY = "Name";
    private static final String DETAIL_KEY = "Detail";
    private static final String TIME_KEY = "Time";

    private final List<PhoneRecord> calls;
    private final List<PhoneRecord> messages;
    private final boolean finalized;

    /** Creating data from the editor means the phone is being permanently saved. */
    public SmartphoneData(List<PhoneRecord> calls, List<PhoneRecord> messages) {
        this(calls, messages, true);
    }

    public SmartphoneData(List<PhoneRecord> calls, List<PhoneRecord> messages, boolean finalized) {
        this.calls = copyAndLimit(calls);
        this.messages = copyAndLimit(messages);
        this.finalized = finalized;
    }

    public static SmartphoneData empty() {
        return new SmartphoneData(List.of(), List.of(), false);
    }

    public static SmartphoneData fromStack(ItemStack stack) {
        NbtCompound stackNbt = stack.getNbt();
        if (stackNbt == null || !stackNbt.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)) {
            return empty();
        }

        NbtCompound phone = stackNbt.getCompound(ROOT_KEY);
        return new SmartphoneData(
                readNbtList(phone.getList(CALLS_KEY, NbtElement.COMPOUND_TYPE)),
                readNbtList(phone.getList(MESSAGES_KEY, NbtElement.COMPOUND_TYPE)),
                phone.getBoolean(FINALIZED_KEY)
        );
    }

    public List<PhoneRecord> calls() {
        return List.copyOf(calls);
    }

    public List<PhoneRecord> messages() {
        return List.copyOf(messages);
    }

    public boolean finalized() {
        return finalized;
    }

    public void writeTo(ItemStack stack) {
        NbtCompound phone = new NbtCompound();
        phone.put(CALLS_KEY, writeNbtList(calls));
        phone.put(MESSAGES_KEY, writeNbtList(messages));
        phone.putBoolean(FINALIZED_KEY, finalized);
        stack.getOrCreateNbt().put(ROOT_KEY, phone);
    }

    public void writePacket(PacketByteBuf buf) {
        buf.writeBoolean(finalized);
        writePacketList(buf, calls);
        writePacketList(buf, messages);
    }

    public static SmartphoneData readPacket(PacketByteBuf buf) {
        boolean finalized = buf.readBoolean();
        return new SmartphoneData(readPacketList(buf), readPacketList(buf), finalized);
    }

    private static List<PhoneRecord> readNbtList(NbtList list) {
        List<PhoneRecord> records = new ArrayList<>();
        int count = Math.min(list.size(), MAX_RECORDS);
        for (int i = 0; i < count; i++) {
            NbtCompound record = list.getCompound(i);
            records.add(new PhoneRecord(
                    record.getString(NAME_KEY),
                    record.getString(DETAIL_KEY),
                    record.getString(TIME_KEY)
            ));
        }
        return records;
    }

    private static NbtList writeNbtList(List<PhoneRecord> records) {
        NbtList list = new NbtList();
        int count = Math.min(records.size(), MAX_RECORDS);
        for (int i = 0; i < count; i++) {
            PhoneRecord record = records.get(i);
            NbtCompound tag = new NbtCompound();
            tag.putString(NAME_KEY, record.name());
            tag.putString(DETAIL_KEY, record.detail());
            tag.putString(TIME_KEY, record.time());
            list.add(tag);
        }
        return list;
    }

    private static void writePacketList(PacketByteBuf buf, List<PhoneRecord> records) {
        int count = Math.min(records.size(), MAX_RECORDS);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            PhoneRecord record = records.get(i);
            buf.writeString(record.name(), MAX_NAME_LENGTH);
            buf.writeString(record.detail(), MAX_DETAIL_LENGTH);
            buf.writeString(record.time(), MAX_TIME_LENGTH);
        }
    }

    private static List<PhoneRecord> readPacketList(PacketByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_RECORDS) {
            throw new IllegalArgumentException("Invalid smartphone record count: " + count);
        }

        List<PhoneRecord> records = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            records.add(new PhoneRecord(
                    buf.readString(MAX_NAME_LENGTH),
                    buf.readString(MAX_DETAIL_LENGTH),
                    buf.readString(MAX_TIME_LENGTH)
            ));
        }
        return records;
    }

    private static List<PhoneRecord> copyAndLimit(List<PhoneRecord> source) {
        List<PhoneRecord> result = new ArrayList<>();
        int count = Math.min(source.size(), MAX_RECORDS);
        for (int i = 0; i < count; i++) {
            PhoneRecord record = source.get(i);
            result.add(new PhoneRecord(record.name(), record.detail(), record.time()));
        }
        return result;
    }

    public record PhoneRecord(String name, String detail, String time) {
        public PhoneRecord {
            name = sanitize(name, MAX_NAME_LENGTH);
            detail = sanitize(detail, MAX_DETAIL_LENGTH);
            time = sanitize(time, MAX_TIME_LENGTH);
        }

        private static String sanitize(String value, int maxLength) {
            if (value == null) {
                return "";
            }
            String cleaned = value.replace('\n', ' ').replace('\r', ' ');
            return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
        }
    }
}
