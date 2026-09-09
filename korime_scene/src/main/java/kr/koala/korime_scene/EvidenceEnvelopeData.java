package kr.koala.korime_scene;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Read-only snapshot data stored inside an evidence envelope item. */
public final class EvidenceEnvelopeData {
    public static final int MAX_NOTE_LENGTH = 4096;
    private static final int MAX_SNAPSHOT_TEXT_LENGTH = 262_144;

    private static final String ROOT_KEY = "KorimeSceneEvidenceEnvelope";
    private static final String CAPTURED_KEY = "Captured";
    private static final String TARGET_TYPE_KEY = "TargetType";
    private static final String TARGET_ID_KEY = "TargetId";
    private static final String TARGET_STATE_KEY = "TargetState";
    private static final String TARGET_NBT_KEY = "TargetNbt";
    private static final String X_KEY = "X";
    private static final String Y_KEY = "Y";
    private static final String Z_KEY = "Z";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String CAPTURED_BY_KEY = "CapturedBy";
    private static final String CAPTURED_AT_KEY = "CapturedAt";
    private static final String NOTE_KEY = "Note";

    private EvidenceEnvelopeData() { }

    public static boolean isCaptured(ItemStack stack) {
        NbtCompound root = getRoot(stack);
        return root != null && root.getBoolean(CAPTURED_KEY);
    }

    public static boolean captureBlock(ItemStack stack, World world, BlockPos pos, PlayerEntity player) {
        if (isCaptured(stack)) return false;

        BlockState state = world.getBlockState(pos);
        NbtCompound targetNbt = null;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            targetNbt = blockEntity.createNbtWithIdentifyingData();
            if (targetNbt.toString().length() > MAX_SNAPSHOT_TEXT_LENGTH) return false;
        }

        NbtCompound root = getOrCreateRoot(stack);
        String previousNote = sanitize(root.getString(NOTE_KEY), MAX_NOTE_LENGTH);
        root.putBoolean(CAPTURED_KEY, true);
        root.putString(TARGET_TYPE_KEY, "block");
        root.putString(TARGET_ID_KEY, Registries.BLOCK.getId(state.getBlock()).toString());
        root.putString(TARGET_STATE_KEY, state.toString());
        if (targetNbt != null) root.put(TARGET_NBT_KEY, targetNbt);
        else root.remove(TARGET_NBT_KEY);
        root.putDouble(X_KEY, pos.getX());
        root.putDouble(Y_KEY, pos.getY());
        root.putDouble(Z_KEY, pos.getZ());
        root.putString(DIMENSION_KEY, world.getRegistryKey().getValue().toString());
        root.putString(CAPTURED_BY_KEY, player == null ? "" : player.getName().getString());
        root.putLong(CAPTURED_AT_KEY, System.currentTimeMillis());
        root.putString(NOTE_KEY, previousNote);
        return true;
    }

    public static boolean captureEntity(ItemStack stack, World world, Entity entity, PlayerEntity player) {
        if (isCaptured(stack)) return false;

        NbtCompound targetNbt = entity.writeNbt(new NbtCompound());
        if (targetNbt.toString().length() > MAX_SNAPSHOT_TEXT_LENGTH) return false;

        NbtCompound root = getOrCreateRoot(stack);
        String previousNote = sanitize(root.getString(NOTE_KEY), MAX_NOTE_LENGTH);
        root.putBoolean(CAPTURED_KEY, true);
        root.putString(TARGET_TYPE_KEY, "entity");
        root.putString(TARGET_ID_KEY, Registries.ENTITY_TYPE.getId(entity.getType()).toString());
        root.putString(TARGET_STATE_KEY, "");
        root.put(TARGET_NBT_KEY, targetNbt);
        root.putDouble(X_KEY, entity.getX());
        root.putDouble(Y_KEY, entity.getY());
        root.putDouble(Z_KEY, entity.getZ());
        root.putString(DIMENSION_KEY, world.getRegistryKey().getValue().toString());
        root.putString(CAPTURED_BY_KEY, player == null ? "" : player.getName().getString());
        root.putLong(CAPTURED_AT_KEY, System.currentTimeMillis());
        root.putString(NOTE_KEY, previousNote);
        return true;
    }

    public static String getTargetType(ItemStack stack) { return string(stack, TARGET_TYPE_KEY); }
    public static String getTargetId(ItemStack stack) { return string(stack, TARGET_ID_KEY); }
    public static String getTargetState(ItemStack stack) { return string(stack, TARGET_STATE_KEY); }
    public static String getDimension(ItemStack stack) { return string(stack, DIMENSION_KEY); }
    public static String getCapturedBy(ItemStack stack) { return string(stack, CAPTURED_BY_KEY); }
    public static long getCapturedAt(ItemStack stack) {
        NbtCompound root = getRoot(stack);
        return root == null ? 0L : root.getLong(CAPTURED_AT_KEY);
    }
    public static double getX(ItemStack stack) { return number(stack, X_KEY); }
    public static double getY(ItemStack stack) { return number(stack, Y_KEY); }
    public static double getZ(ItemStack stack) { return number(stack, Z_KEY); }
    public static String getNote(ItemStack stack) { return string(stack, NOTE_KEY); }

    public static boolean hasTargetNbt(ItemStack stack) {
        NbtCompound root = getRoot(stack);
        return root != null && root.contains(TARGET_NBT_KEY, NbtElement.COMPOUND_TYPE);
    }

    public static NbtCompound getTargetNbtCopy(ItemStack stack) {
        NbtCompound root = getRoot(stack);
        if (root == null || !root.contains(TARGET_NBT_KEY, NbtElement.COMPOUND_TYPE)) return new NbtCompound();
        return root.getCompound(TARGET_NBT_KEY).copy();
    }

    public static void setNote(ItemStack stack, String note) {
        getOrCreateRoot(stack).putString(NOTE_KEY, sanitize(note, MAX_NOTE_LENGTH));
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

    private static String string(ItemStack stack, String key) {
        NbtCompound root = getRoot(stack);
        return root == null ? "" : root.getString(key);
    }

    private static double number(ItemStack stack, String key) {
        NbtCompound root = getRoot(stack);
        return root == null ? 0.0 : root.getDouble(key);
    }

    private static String sanitize(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
