package kr.koala.korime_scene;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** One-item evidence bag. The copied item is stored inside the envelope and is never extractable. */
public final class EvidenceEnvelopeData {
    public static final int MAX_NOTE_LENGTH = 4096;
    private static final int MAX_STORED_ITEM_TEXT_LENGTH = 262_144;

    private static final String ROOT_KEY = "KorimeSceneEvidenceEnvelope";
    private static final String STORED_ITEM_KEY = "StoredItem";
    private static final String NOTE_KEY = "Note";
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    private EvidenceEnvelopeData() { }

    public static boolean hasStoredItem(ItemStack envelope) {
        return !getStoredItem(envelope).isEmpty();
    }

    /** Compatibility alias used by the existing client hooks. */
    public static boolean isCaptured(ItemStack envelope) {
        return hasStoredItem(envelope);
    }

    public static ItemStack getStoredItem(ItemStack envelope) {
        NbtCompound root = getRoot(envelope);
        if (root == null || !root.contains(STORED_ITEM_KEY, NbtElement.COMPOUND_TYPE)) return ItemStack.EMPTY;
        try {
            return ItemStack.fromNbt(root.getCompound(STORED_ITEM_KEY));
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean captureBlock(ItemStack envelope, World world, BlockPos pos) {
        if (hasStoredItem(envelope)) return false;

        BlockState state = world.getBlockState(pos);
        ItemStack captured = state.getBlock().getPickStack(world, pos, state);
        if (captured.isEmpty()) return false;
        captured = captured.copy();
        captured.setCount(1);

        // Keep the placed block entity data on the copied item. This preserves chest
        // contents and custom mod data such as CCTV/safe/image-frame state without
        // modifying or removing the original block in the world.
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            NbtCompound blockEntityNbt = blockEntity.createNbt();
            if (!blockEntityNbt.isEmpty()) {
                captured.getOrCreateNbt().put(BLOCK_ENTITY_TAG, blockEntityNbt.copy());
            }
        }

        return store(envelope, captured);
    }

    public static boolean captureEntity(ItemStack envelope, Entity entity) {
        if (hasStoredItem(envelope)) return false;

        ItemStack captured = ItemStack.EMPTY;
        if (entity instanceof ItemFrameEntity frame) {
            captured = frame.getHeldItemStack().copy();
        } else if (entity instanceof ItemEntity dropped) {
            captured = dropped.getStack().copy();
        }

        if (captured.isEmpty()) return false;
        captured.setCount(1);
        return store(envelope, captured);
    }

    private static boolean store(ItemStack envelope, ItemStack captured) {
        NbtCompound itemNbt = captured.writeNbt(new NbtCompound());
        if (itemNbt.toString().length() > MAX_STORED_ITEM_TEXT_LENGTH) return false;
        NbtCompound root = getOrCreateRoot(envelope);
        root.put(STORED_ITEM_KEY, itemNbt);
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
