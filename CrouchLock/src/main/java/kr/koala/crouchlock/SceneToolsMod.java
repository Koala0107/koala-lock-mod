package kr.koala.crouchlock;

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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class SceneToolsMod implements ModInitializer {
    public static final Identifier EDIT_ITEM_PACKET = new Identifier(CrouchLockMod.MOD_ID, "edit_item");

    public static final Block CCTV = Registry.register(
            Registries.BLOCK,
            new Identifier(CrouchLockMod.MOD_ID, "cctv"),
            new CctvBlock(AbstractBlock.Settings.create()
                    .strength(0.8F)
                    .sounds(BlockSoundGroup.METAL)
                    .nonOpaque())
    );

    public static final BlockEntityType<CctvBlockEntity> CCTV_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(CrouchLockMod.MOD_ID, "cctv"),
            FabricBlockEntityTypeBuilder.create(CctvBlockEntity::new, CCTV).build()
    );

    public static final Item CCTV_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(CrouchLockMod.MOD_ID, "cctv"),
            new BlockItem(CCTV, new Item.Settings())
    );

    public static final Block ITEM_EDITOR = Registry.register(
            Registries.BLOCK,
            new Identifier(CrouchLockMod.MOD_ID, "item_editor"),
            new ItemEditorBlock(AbstractBlock.Settings.create()
                    .strength(2.5F)
                    .sounds(BlockSoundGroup.METAL))
    );

    public static final Item ITEM_EDITOR_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(CrouchLockMod.MOD_ID, "item_editor"),
            new BlockItem(ITEM_EDITOR, new Item.Settings())
    );

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(EDIT_ITEM_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final BlockPos editorPos;
                    final String name;
                    final String description;
                    try {
                        editorPos = buf.readBlockPos();
                        name = buf.readString(128);
                        description = buf.readString(512);
                    } catch (RuntimeException ignored) {
                        return;
                    }

                    server.execute(() -> {
                        if (!player.getWorld().getBlockState(editorPos).isOf(ITEM_EDITOR)) return;
                        if (player.squaredDistanceTo(editorPos.getX() + 0.5, editorPos.getY() + 0.5, editorPos.getZ() + 0.5) > 64.0) return;

                        ItemStack stack = player.getMainHandStack();
                        if (stack.isEmpty()) return;

                        String cleanName = name.trim();
                        String cleanDescription = description.trim();

                        if (cleanName.isEmpty()) stack.removeCustomName();
                        else stack.setCustomName(net.minecraft.text.Text.literal(cleanName));

                        NbtCompound root = stack.getOrCreateNbt();
                        NbtCompound display = root.contains("display", 10) ? root.getCompound("display") : new NbtCompound();
                        if (cleanDescription.isEmpty()) {
                            display.remove("Lore");
                        } else {
                            NbtList lore = new NbtList();
                            String json = "{\"text\":\"" + escapeJson(cleanDescription) + "\",\"color\":\"light_purple\",\"italic\":false}";
                            lore.add(NbtString.of(json));
                            display.put("Lore", lore);
                        }
                        if (display.isEmpty()) root.remove("display");
                        else root.put("display", display);

                        player.currentScreenHandler.sendContentUpdates();
                    });
                });
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
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int)c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }
}
