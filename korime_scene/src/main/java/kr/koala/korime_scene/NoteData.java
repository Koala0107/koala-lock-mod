package kr.koala.korime_scene;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.List;

public final class NoteData {
    public static final int MAX_BODY_LENGTH = 8192;
    public static final int MAX_PAGES = 256;

    private static final String ROOT_KEY = "KorimeSceneNote";
    private static final String BODY_KEY = "Body";
    private static final String PAGES_KEY = "Pages";

    private NoteData() { }

    /** Returns at least one page and transparently reads old one-page notes. */
    public static List<String> getPages(ItemStack stack) {
        List<String> pages = new ArrayList<>();
        NbtCompound root = getRoot(stack);
        if (root != null && root.contains(PAGES_KEY, NbtElement.LIST_TYPE)) {
            NbtList list = root.getList(PAGES_KEY, NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size() && pages.size() < MAX_PAGES; i++) {
                pages.add(trim(list.getString(i), MAX_BODY_LENGTH));
            }
        }

        if (pages.isEmpty()) {
            String legacy = root == null ? "" : trim(root.getString(BODY_KEY), MAX_BODY_LENGTH);
            pages.add(legacy);
        }
        return pages;
    }

    /** Compatibility helper for older callers. */
    public static String getBody(ItemStack stack) {
        return getPages(stack).get(0);
    }

    /** Compatibility helper: writes the first page. */
    public static void setBody(ItemStack stack, String body) {
        setPage(stack, 0, body);
    }

    public static void setPage(ItemStack stack, int pageIndex, String body) {
        if (pageIndex < 0 || pageIndex >= MAX_PAGES) return;

        List<String> pages = getPages(stack);
        while (pages.size() <= pageIndex && pages.size() < MAX_PAGES) pages.add("");
        if (pageIndex >= pages.size()) return;
        pages.set(pageIndex, trim(body, MAX_BODY_LENGTH));
        writePages(stack, pages);
    }

    public static void removePage(ItemStack stack, int pageIndex) {
        List<String> pages = getPages(stack);
        if (pages.size() <= 1 || pageIndex < 0 || pageIndex >= pages.size()) return;
        pages.remove(pageIndex);
        if (pages.isEmpty()) pages.add("");
        writePages(stack, pages);
    }

    private static void writePages(ItemStack stack, List<String> pages) {
        NbtList list = new NbtList();
        int count = Math.min(pages.size(), MAX_PAGES);
        for (int i = 0; i < count; i++) {
            list.add(NbtString.of(trim(pages.get(i), MAX_BODY_LENGTH)));
        }
        if (list.isEmpty()) list.add(NbtString.of(""));

        NbtCompound root = getOrCreateRoot(stack);
        root.put(PAGES_KEY, list);
        root.remove(BODY_KEY);
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
