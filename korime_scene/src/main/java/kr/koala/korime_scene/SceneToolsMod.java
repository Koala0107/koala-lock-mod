package kr.koala.korime_scene;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class SceneToolsMod implements ModInitializer {
    public static final Identifier EDIT_ITEM_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "edit_item");
    public static final Identifier CCTV_SAVE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "cctv_save");
    public static final Identifier CCTV_BREAK_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "cctv_break");
    public static final Identifier EVIDENCE_MAGNIFIER_SAVE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "evidence_magnifier_save");
    public static final Identifier EVIDENCE_BREAK_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "evidence_break");
    public static final Identifier SAFE_SETUP_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "safe_setup");
    public static final Identifier SAFE_ATTEMPT_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "safe_attempt");

    public static final Block CCTV = Registry.register(
            Registries.BLOCK,
            new Identifier(KorimeSceneMod.MOD_ID, "cctv"),
            new CctvBlock(AbstractBlock.Settings.create().strength(0.8F).sounds(BlockSoundGroup.METAL).nonOpaque())
    );

    public static final BlockEntityType<CctvBlockEntity> CCTV_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(KorimeSceneMod.MOD_ID, "cctv"),
            FabricBlockEntityTypeBuilder.create(CctvBlockEntity::new, CCTV).build()
    );

    public static final Item CCTV_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "cctv"),
            new BlockItem(CCTV, new Item.Settings())
    );

    public static final Block ITEM_EDITOR = Registry.register(
            Registries.BLOCK,
            new Identifier(KorimeSceneMod.MOD_ID, "item_editor"),
            new ItemEditorBlock(AbstractBlock.Settings.create().strength(2.5F).sounds(BlockSoundGroup.METAL))
    );

    public static final Item ITEM_EDITOR_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "item_editor"),
            new BlockItem(ITEM_EDITOR, new Item.Settings())
    );

    public static final Block EVIDENCE_MAGNIFIER = Registry.register(
            Registries.BLOCK,
            new Identifier(KorimeSceneMod.MOD_ID, "evidence"),
            new EvidenceMagnifierBlock(AbstractBlock.Settings.create().strength(0.35F).sounds(BlockSoundGroup.GLASS).noCollision())
    );

    public static final BlockEntityType<EvidenceMagnifierBlockEntity> EVIDENCE_MAGNIFIER_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(KorimeSceneMod.MOD_ID, "evidence"),
            FabricBlockEntityTypeBuilder.create(EvidenceMagnifierBlockEntity::new, EVIDENCE_MAGNIFIER).build()
    );

    public static final Item EVIDENCE_MAGNIFIER_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "evidence"),
            new BlockItem(EVIDENCE_MAGNIFIER, new Item.Settings())
    );

    public static final Block SAFE = Registry.register(
            Registries.BLOCK,
            new Identifier(KorimeSceneMod.MOD_ID, "safe"),
            new SafeBlock(AbstractBlock.Settings.create().strength(5.0F, 6.0F).sounds(BlockSoundGroup.METAL).nonOpaque())
    );

    public static final BlockEntityType<SafeBlockEntity> SAFE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(KorimeSceneMod.MOD_ID, "safe"),
            FabricBlockEntityTypeBuilder.create(SafeBlockEntity::new, SAFE).build()
    );

    public static final Item SAFE_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "safe"),
            new BlockItem(SAFE, new Item.Settings())
    );

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(SAFE_SETUP_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final BlockPos pos;
                    final int first, second, third;
                    try {
                        pos = buf.readBlockPos();
                        first = buf.readUnsignedByte();
                        second = buf.readUnsignedByte();
                        third = buf.readUnsignedByte();
                    } catch (RuntimeException ignored) { return; }
                    server.execute(() -> {
                        if (!isValidSafeTarget(player, pos)) return;
                        if (first > 99 || second > 99 || third > 99) return;
                        if (!(player.getWorld().getBlockEntity(pos) instanceof SafeBlockEntity safe)) return;
                        if (safe.isCombinationSet()) return;
                        safe.setCombination(first, second, third);
                        player.getWorld().playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.6F, 1.25F);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(SAFE_ATTEMPT_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final BlockPos pos;
                    final int first, second, third;
                    try {
                        pos = buf.readBlockPos();
                        first = buf.readUnsignedByte();
                        second = buf.readUnsignedByte();
                        third = buf.readUnsignedByte();
                    } catch (RuntimeException ignored) { return; }
                    server.execute(() -> {
                        if (!isValidSafeTarget(player, pos)) return;
                        if (!(player.getWorld().getBlockEntity(pos) instanceof SafeBlockEntity safe)) return;
                        if (safe.matches(first, second, third)) {
                            player.getWorld().playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 0.8F, 0.85F);
                            player.openHandledScreen(safe);
                        } else {
                            player.getWorld().playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 0.45F, 0.75F);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(EVIDENCE_BREAK_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final BlockPos pos;
                    try { pos = buf.readBlockPos(); } catch (RuntimeException ignored) { return; }
                    server.execute(() -> breakValidated(player, pos, EVIDENCE_MAGNIFIER));
                });

        ServerPlayNetworking.registerGlobalReceiver(CCTV_BREAK_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final BlockPos pos;
                    try { pos = buf.readBlockPos(); } catch (RuntimeException ignored) { return; }
                    server.execute(() -> breakValidated(player, pos, CCTV));
                });

        ServerPlayNetworking.registerGlobalReceiver(EVIDENCE_MAGNIFIER_SAVE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final BlockPos pos;
                    final String text;
                    try {
                        pos = buf.readBlockPos();
                        text = buf.readString(EvidenceMagnifierBlockEntity.MAX_TEXT_LENGTH);
                    } catch (RuntimeException ignored) { return; }
                    server.execute(() -> {
                        if (!player.getWorld().getBlockState(pos).isOf(EVIDENCE_MAGNIFIER)) return;
                        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;
                        if (!(player.getWorld().getBlockEntity(pos) instanceof EvidenceMagnifierBlockEntity evidence)) return;
                        evidence.saveEvidence(text);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(CCTV_SAVE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final BlockPos cctvPos;
                    final List<String> records = new ArrayList<>();
                    try {
                        cctvPos = buf.readBlockPos();
                        int count = Math.min(buf.readVarInt(), CctvBlockEntity.MAX_RECORDS);
                        if (count < 0) return;
                        for (int i = 0; i < count; i++) records.add(buf.readString(CctvBlockEntity.MAX_RECORD_LENGTH));
                    } catch (RuntimeException ignored) { return; }
                    server.execute(() -> {
                        if (!player.getWorld().getBlockState(cctvPos).isOf(CCTV)) return;
                        if (player.squaredDistanceTo(cctvPos.getX() + 0.5, cctvPos.getY() + 0.5, cctvPos.getZ() + 0.5) > 64.0) return;
                        if (!(player.getWorld().getBlockEntity(cctvPos) instanceof CctvBlockEntity cctv)) return;
                        cctv.setEvidence(records, true);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(EDIT_ITEM_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final BlockPos editorPos;
                    final String name;
                    final String firstLine;
                    final String secondLine;
                    final String thirdLine;
                    try {
                        editorPos = buf.readBlockPos();
                        name = buf.readString(128);
                        firstLine = buf.readString(512);
                        secondLine = buf.readString(512);
                        thirdLine = buf.readString(512);
                    } catch (RuntimeException ignored) { return; }
                    server.execute(() -> {
                        if (!player.getWorld().getBlockState(editorPos).isOf(ITEM_EDITOR)) return;
                        if (player.squaredDistanceTo(editorPos.getX() + 0.5, editorPos.getY() + 0.5, editorPos.getZ() + 0.5) > 64.0) return;
                        ItemStack stack = player.getMainHandStack();
                        if (stack.isEmpty()) return;
                        String cleanName = name.trim();
                        if (cleanName.isEmpty()) stack.removeCustomName();
                        else stack.setCustomName(net.minecraft.text.Text.literal(cleanName));

                        NbtCompound root = stack.getOrCreateNbt();
                        NbtCompound display = root.contains("display", 10) ? root.getCompound("display") : new NbtCompound();
                        NbtList lore = new NbtList();
                        addLoreLine(lore, firstLine);
                        addLoreLine(lore, secondLine);
                        addLoreLine(lore, thirdLine);
                        if (lore.isEmpty()) display.remove("Lore");
                        else display.put("Lore", lore);

                        if (display.isEmpty()) root.remove("display");
                        else root.put("display", display);
                        player.currentScreenHandler.sendContentUpdates();
                    });
                });
    }

    private static void addLoreLine(NbtList lore, String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) return;
        String json = "{\"text\":\"" + escapeJson(clean) + "\",\"color\":\"light_purple\",\"italic\":false}";
        lore.add(NbtString.of(json));
    }

    private static void breakValidated(net.minecraft.server.network.ServerPlayerEntity player, BlockPos pos, Block block) {
        if (!player.getWorld().getBlockState(pos).isOf(block)) return;
        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;
        player.getWorld().breakBlock(pos, true, player);
    }

    private static boolean isValidSafeTarget(net.minecraft.server.network.ServerPlayerEntity player, BlockPos pos) {
        return player.getWorld().getBlockState(pos).isOf(SAFE)
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    private static String escapeJson(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (c < 0x20) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
            }
        }
        return out.toString();
    }
}
