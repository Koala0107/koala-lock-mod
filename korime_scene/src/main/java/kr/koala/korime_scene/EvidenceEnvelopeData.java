package kr.koala.korime_scene;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** 30-slot evidence bag. Stored copies are view-only and cannot be extracted. */
public final class EvidenceEnvelopeData {
    public static final int MAX_ITEMS = 30;
    public static final int MAX_NOTE_LENGTH = 4096;
    private static final int MAX_STORED_ITEM_TEXT_LENGTH = 262_144;

    private static final String ROOT_KEY = "KorimeSceneEvidenceEnvelope";
    private static final String STORED_ITEMS_KEY = "StoredItems";
    private static final String LEGACY_STORED_ITEM_KEY = "StoredItem";
    private static final String NOTE_KEY = "Note";
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    private EvidenceEnvelopeData() { }

    public static int getItemCount(ItemStack envelope) {
        return getStoredItems(envelope).size();
    }

    public static boolean hasStoredItem(ItemStack envelope) {
        return getItemCount(envelope) > 0;
    }

    public static boolean isCaptured(ItemStack envelope) {
        return hasStoredItem(envelope);
    }

    public static boolean isFull(ItemStack envelope) {
        return getItemCount(envelope) >= MAX_ITEMS;
    }

    public static List<ItemStack> getStoredItems(ItemStack envelope) {
        List<ItemStack> result = new ArrayList<>();
        NbtCompound root = getRoot(envelope);
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

    public static boolean captureBlock(ItemStack envelope, World world, BlockPos pos) {
        if (isFull(envelope)) return false;

        BlockState state = world.getBlockState(pos);
        ItemStack captured = state.getBlock().getPickStack(world, pos, state);
        if (captured.isEmpty()) return false;
        captured = captured.copy();
        captured.setCount(1);

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            NbtCompound blockEntityNbt = blockEntity.createNbt();
            if (!blockEntityNbt.isEmpty()) {
                captured.getOrCreateNbt().put(BLOCK_ENTITY_TAG, blockEntityNbt.copy());
            }
        }

        return addStoredItem(envelope, captured);
    }

    public static boolean captureEntity(ItemStack envelope, Entity entity) {
        if (isFull(envelope)) return false;

        ItemStack captured = ItemStack.EMPTY;
        if (entity instanceof ItemFrameEntity frame) {
            captured = frame.getHeldItemStack().copy();
        } else if (entity instanceof ItemEntity dropped) {
            captured = dropped.getStack().copy();
        }

        if (captured.isEmpty()) return false;
        captured.setCount(1);
        return addStoredItem(envelope, captured);
    }

    private static boolean addStoredItem(ItemStack envelope, ItemStack captured) {
        NbtCompound itemNbt = captured.writeNbt(new NbtCompound());
        if (itemNbt.toString().length() > MAX_STORED_ITEM_TEXT_LENGTH) return false;

        List<ItemStack> current = getStoredItems(envelope);
        if (current.size() >= MAX_ITEMS) return false;
        current.add(captured.copy());

        NbtList list = new NbtList();
        for (ItemStack stack : current) {
            list.add(stack.writeNbt(new NbtCompound()));
        }
        NbtCompound root = getOrCreateRoot(envelope);
        root.put(STORED_ITEMS_KEY, list);
        root.remove(LEGACY_STORED_ITEM_KEY);
        return true;
    }

    public static String getNote(ItemStack envelope) {
        NbtCompound root = getRoot(envelope);
        return root == null ? "" : root.getString(NOTE_KEY);
    }

    public static void setNote(ItemStack envelope, String note) {
        getOrCreateRoot(envelope).putString(NOTE_KEY, trim(note, MAX_NOTE_LENGTH));
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

    private static String trim(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }
}
