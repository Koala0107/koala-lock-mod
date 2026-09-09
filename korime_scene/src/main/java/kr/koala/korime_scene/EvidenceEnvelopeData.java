package kr.koala.korime_scene;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

/** 30-slot evidence pouch. Stored item copies can be taken back out. */
public final class EvidenceEnvelopeData {
    public static final int MAX_ITEMS = 30;
    private static final int MAX_STORED_ITEM_TEXT_LENGTH = 262_144;

    private static final String ROOT_KEY = "KorimeSceneEvidenceEnvelope";
    private static final String STORED_ITEMS_KEY = "StoredItems";
    private static final String LEGACY_STORED_ITEM_KEY = "StoredItem";

    private EvidenceEnvelopeData() { }

    public static int getItemCount(ItemStack pouch) {
        return getStoredItems(pouch).size();
    }

    public static boolean hasStoredItem(ItemStack pouch) {
        return getItemCount(pouch) > 0;
    }

    public static boolean isCaptured(ItemStack pouch) {
        return hasStoredItem(pouch);
    }

    public static boolean isFull(ItemStack pouch) {
        return getItemCount(pouch) >= MAX_ITEMS;
    }

    public static List<ItemStack> getStoredItems(ItemStack pouch) {
        List<ItemStack> result = new ArrayList<>();
        NbtCompound root = getRoot(pouch);
        if (root == null) return result;

        if (root.contains(STORED_ITEMS_KEY, NbtElement.LIST_TYPE)) {
            NbtList list = root.getList(STORED_ITEMS_KEY, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size() && result.size() < MAX_ITEMS; i++) {
                try {
                    ItemStack stack = ItemStack.fromNbt(list.getCompound(i));
                    if (!stack.isEmpty()) result.add(stack);
                } catch (RuntimeException ignored) { }
            }
        } else if (root.contains(LEGACY_STORED_ITEM_KEY, NbtElement.COMPOUND_TYPE)) {
            try {
                ItemStack legacy = ItemStack.fromNbt(root.getCompound(LEGACY_STORED_ITEM_KEY));
                if (!legacy.isEmpty()) result.add(legacy);
            } catch (RuntimeException ignored) { }
        }
        return result;
    }

    /** Adds one exact NBT-preserving copy of an existing item stack. */
    public static boolean addItemCopy(ItemStack pouch, ItemStack source) {
        if (source == null || source.isEmpty() || isFull(pouch)) return false;

        ItemStack captured = source.copy();
        captured.setCount(1);
        NbtCompound itemNbt = captured.writeNbt(new NbtCompound());
        if (itemNbt.toString().length() > MAX_STORED_ITEM_TEXT_LENGTH) return false;

        List<ItemStack> current = getStoredItems(pouch);
        if (current.size() >= MAX_ITEMS) return false;
        current.add(captured);
        writeStoredItems(pouch, current);
        return true;
    }

    /** Removes and returns one stored item. */
    public static ItemStack removeStoredItem(ItemStack pouch, int index) {
        List<ItemStack> current = getStoredItems(pouch);
        if (index < 0 || index >= current.size()) return ItemStack.EMPTY;
        ItemStack removed = current.remove(index);
        writeStoredItems(pouch, current);
        return removed;
    }

    private static void writeStoredItems(ItemStack pouch, List<ItemStack> items) {
        NbtList list = new NbtList();
        for (int i = 0; i < items.size() && i < MAX_ITEMS; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) list.add(stack.writeNbt(new NbtCompound()));
        }
        NbtCompound root = getOrCreateRoot(pouch);
        root.put(STORED_ITEMS_KEY, list);
        root.remove(LEGACY_STORED_ITEM_KEY);
        // Remove the retired memo field from old pouches when they are next modified.
        root.remove("Note");
    }

    private static NbtCompound getRoot(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)) return null;
        return nbt.getCompound(ROOT_KEY);
    }

    private static NbtCompound getOrCreateRoot(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)) nbt.put(ROOT_KEY, new NbtCompound());
        return nbt.getCompound(ROOT_KEY);
    }
}
