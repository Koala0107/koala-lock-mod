package kr.koala.korime_scene;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public final class NoteData {
    public static final int MAX_BODY_LENGTH = 8192;
    private static final String ROOT_KEY = "KorimeSceneNote";
    private static final String BODY_KEY = "Body";

    private NoteData() { }

    public static String getBody(ItemStack stack) {
        NbtCompound root = getRoot(stack);
        return root == null ? "" : root.getString(BODY_KEY);
    }

    public static void setBody(ItemStack stack, String body) {
        getOrCreateRoot(stack).putString(BODY_KEY, trim(body, MAX_BODY_LENGTH));
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
