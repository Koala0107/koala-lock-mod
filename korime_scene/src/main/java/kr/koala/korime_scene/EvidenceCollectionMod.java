package kr.koala.korime_scene;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.state.property.Properties;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class EvidenceCollectionMod implements ModInitializer {
    public static final Identifier NOTE_SAVE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "note_save");
    public static final Identifier NOTE_REMOVE_PAGE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "note_remove_page");
    public static final Identifier OPEN_CONTAINER_EVIDENCE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "evidence_container_open");
    public static final Identifier TAKE_CONTAINER_EVIDENCE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "evidence_container_take");
    public static final Identifier EXTRACT_POUCH_ITEM_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "evidence_pouch_extract");

    public static final Item EVIDENCE_ENVELOPE = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "evidence_pouch"),
            new EvidenceEnvelopeItem(new Item.Settings().maxCount(1))
    );

    public static final Item NOTE = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "note"),
            new NoteItem(new Item.Settings().maxCount(1))
    );

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(NOTE_SAVE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final Hand hand;
                    final int pageIndex;
                    final String body;
                    try {
                        hand = buf.readEnumConstant(Hand.class);
                        pageIndex = buf.readVarInt();
                        body = buf.readString(NoteData.MAX_BODY_LENGTH);
                    } catch (RuntimeException ignored) {
                        return;
                    }
                    server.execute(() -> {
                        ItemStack stack = player.getStackInHand(hand);
                        if (!stack.isOf(NOTE)) return;
                        if (pageIndex < 0 || pageIndex >= NoteData.MAX_PAGES) return;
                        NoteData.setPage(stack, pageIndex, body);
                        player.currentScreenHandler.sendContentUpdates();
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(NOTE_REMOVE_PAGE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final Hand hand;
                    final int pageIndex;
                    try {
                        hand = buf.readEnumConstant(Hand.class);
                        pageIndex = buf.readVarInt();
                    } catch (RuntimeException ignored) {
                        return;
                    }
                    server.execute(() -> {
                        ItemStack stack = player.getStackInHand(hand);
                        if (!stack.isOf(NOTE)) return;
                        NoteData.removePage(stack, pageIndex);
                        player.currentScreenHandler.sendContentUpdates();
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(TAKE_CONTAINER_EVIDENCE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final Hand hand;
                    final BlockPos pos;
                    final int slot;
                    try {
                        hand = buf.readEnumConstant(Hand.class);
                        pos = buf.readBlockPos();
                        slot = buf.readVarInt();
                    } catch (RuntimeException ignored) {
                        return;
                    }

                    server.execute(() -> {
                        ItemStack pouch = player.getStackInHand(hand);
                        if (!pouch.isOf(EVIDENCE_ENVELOPE) || EvidenceEnvelopeData.isFull(pouch)) return;
                        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;

                        Inventory inventory = getEvidenceInventory(player.getWorld(), pos);
                        if (inventory == null || slot < 0 || slot >= inventory.size()) return;

                        ItemStack source = inventory.getStack(slot);
                        if (source.isEmpty()) return;
                        if (!EvidenceEnvelopeData.addItemCopy(pouch, source)) return;

                        player.currentScreenHandler.sendContentUpdates();
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(EXTRACT_POUCH_ITEM_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final Hand hand;
                    final int index;
                    try {
                        hand = buf.readEnumConstant(Hand.class);
                        index = buf.readVarInt();
                    } catch (RuntimeException ignored) {
                        return;
                    }
                    server.execute(() -> {
                        ItemStack pouch = player.getStackInHand(hand);
                        if (!pouch.isOf(EVIDENCE_ENVELOPE)) return;
                        ItemStack extracted = EvidenceEnvelopeData.removeStoredItem(pouch, index);
                        if (extracted.isEmpty()) return;
                        if (!player.getInventory().insertStack(extracted)) {
                            player.dropItem(extracted, false);
                        }
                        player.currentScreenHandler.sendContentUpdates();
                    });
                });
    }

    public static Inventory getEvidenceInventory(World world, BlockPos pos) {
        BlockEntity firstEntity = world.getBlockEntity(pos);
        if (!(firstEntity instanceof Inventory first)) return null;

        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)
                || !state.contains(Properties.CHEST_TYPE)
                || state.get(Properties.CHEST_TYPE) == ChestType.SINGLE) {
            return first;
        }

        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos otherPos = pos.offset(direction);
            BlockState otherState = world.getBlockState(otherPos);
            if (!otherState.isOf(state.getBlock())
                    || !otherState.contains(Properties.CHEST_TYPE)
                    || otherState.get(Properties.CHEST_TYPE) == ChestType.SINGLE) {
                continue;
            }
            BlockEntity otherEntity = world.getBlockEntity(otherPos);
            if (otherEntity instanceof Inventory second) {
                return new DoubleInventory(first, second);
            }
        }
        return first;
    }
}
