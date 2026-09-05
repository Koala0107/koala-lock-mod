package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CrouchLockMod implements ModInitializer {
    public static final String MOD_ID = "crouchlock";
    public static final Item LOCK_KEY = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "lock_key"),
            new LockKeyItem(new Item.Settings().maxCount(1))
    );

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(LOCK_KEY));
        UseBlockCallback.EVENT.register(CrouchLockMod::onUseBlock);
        PlayerBlockBreakEvents.BEFORE.register(CrouchLockMod::beforeBlockBreak);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld serverWorld) {
                LockState lockState = LockState.get(serverWorld);
                findExistingLock(lockState, linkedPositions(serverWorld, pos, state))
                        .ifPresent(lock -> lockState.removeLock(lock.lockId()));
            }
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % 200 == 0) {
                LockState.get(world).pruneInvalid(world);
            }
        });
    }

    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }

        BlockPos clickedPos = hit.getBlockPos();
        BlockState clickedState = serverWorld.getBlockState(clickedPos);
        if (!isLockable(serverWorld, clickedPos, clickedState)) {
            return ActionResult.PASS;
        }

        List<BlockPos> linkedPositions = linkedPositions(serverWorld, clickedPos, clickedState);
        LockState lockState = LockState.get(serverWorld);
        Optional<LockState.LockEntry> existing = findExistingLock(lockState, linkedPositions);
        ItemStack heldStack = player.getStackInHand(hand);

        if (player.isSneaking()) {
            if (!heldStack.isOf(LOCK_KEY)) {
                if (existing.isPresent()) {
                    send(player, "message.crouchlock.wrong_key");
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
            }

            UUID heldKeyId = LockKeyItem.getOrCreateKeyId(heldStack);
            if (existing.isEmpty()) {
                UUID lockId = UUID.randomUUID();
                for (BlockPos pos : linkedPositions) {
                    Block block = serverWorld.getBlockState(pos).getBlock();
                    String blockId = Registries.BLOCK.getId(block).toString();
                    lockState.put(pos, new LockState.LockEntry(heldKeyId, lockId, player.getUuid(), blockId));
                }
                send(player, "message.crouchlock.locked");
                return ActionResult.SUCCESS;
            }

            if (existing.get().keyId().equals(heldKeyId) || canBypass(player)) {
                lockState.removeLock(existing.get().lockId());
                send(player, "message.crouchlock.unlocked");
                return ActionResult.SUCCESS;
            }

            send(player, "message.crouchlock.wrong_key");
            return ActionResult.FAIL;
        }

        if (existing.isEmpty() || canBypass(player) || keyMatches(heldStack, existing.get())) {
            return ActionResult.PASS;
        }

        send(player, "message.crouchlock.locked_need_key");
        return ActionResult.FAIL;
    }

    private static boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos,
                                            BlockState blockState, net.minecraft.block.entity.BlockEntity blockEntity) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
            return true;
        }

        LockState state = LockState.get(serverWorld);
        Optional<LockState.LockEntry> existing = findExistingLock(
                state,
                linkedPositions(serverWorld, pos, blockState)
        );
        if (existing.isEmpty()) {
            return true;
        }

        if (canBypass(player)
                || keyMatches(player.getMainHandStack(), existing.get())
                || keyMatches(player.getOffHandStack(), existing.get())) {
            return true;
        }

        send(player, "message.crouchlock.cannot_break");
        return false;
    }

    static boolean isLockable(World world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        return block instanceof DoorBlock
                || block instanceof TrapdoorBlock
                || block instanceof FenceGateBlock
                || state.createScreenHandlerFactory(world, pos) != null;
    }

    private static List<BlockPos> linkedPositions(ServerWorld world, BlockPos pos, BlockState state) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(pos.toImmutable());

        if (state.getBlock() instanceof DoorBlock && state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            BlockPos otherHalf = state.get(Properties.DOUBLE_BLOCK_HALF).asString().equals("upper")
                    ? pos.down()
                    : pos.up();
            if (world.getBlockState(otherHalf).isOf(state.getBlock())) {
                positions.add(otherHalf.toImmutable());
            }
        }

        if (state.contains(Properties.CHEST_TYPE) && state.get(Properties.CHEST_TYPE) != ChestType.SINGLE) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos neighbor = pos.offset(direction);
                BlockState neighborState = world.getBlockState(neighbor);
                if (neighborState.isOf(state.getBlock())
                        && neighborState.contains(Properties.CHEST_TYPE)
                        && neighborState.get(Properties.CHEST_TYPE) != ChestType.SINGLE) {
                    positions.add(neighbor.toImmutable());
                }
            }
        }

        return new ArrayList<>(positions);
    }

    private static Optional<LockState.LockEntry> findExistingLock(LockState state, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            Optional<LockState.LockEntry> lock = state.get(pos);
            if (lock.isPresent()) {
                return lock;
            }
        }
        return Optional.empty();
    }

    private static boolean keyMatches(ItemStack stack, LockState.LockEntry lock) {
        return LockKeyItem.getKeyId(stack).map(lock.keyId()::equals).orElse(false);
    }

    private static boolean canBypass(PlayerEntity player) {
        return player instanceof ServerPlayerEntity serverPlayer && serverPlayer.hasPermissionLevel(2);
    }

    private static void send(PlayerEntity player, String translationKey) {
        player.sendMessage(Text.translatable(translationKey), true);
    }
}
