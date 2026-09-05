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
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class CrouchLockMod implements ModInitializer {
    public static final String MOD_ID = "crouchlock";
    public static final String KEY_LOCK = "key";
    public static final String KEYPAD_LOCK = "keypad";
    private static final ConcurrentLinkedQueue<Runnable> END_OF_TICK_ACTIONS = new ConcurrentLinkedQueue<>();

    public static final Item LOCK_KEY = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "lock_key"),
            new LockKeyItem(new Item.Settings().maxCount(1))
    );

    public static final Item KEYPAD = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "keypad"),
            new Item(new Item.Settings().maxCount(16))
    );

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(LOCK_KEY);
            entries.add(KEYPAD);
        });
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
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Runnable action;
            while ((action = END_OF_TICK_ACTIONS.poll()) != null) {
                action.run();
            }
        });
    }

    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)
                || !(player instanceof ServerPlayerEntity serverPlayer)) {
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

        if (existing.isPresent() && KEYPAD_LOCK.equals(existing.get().type())) {
            openKeypadUnlock(serverPlayer, serverWorld, hand, hit, existing.get(), player.isSneaking());
            return ActionResult.SUCCESS;
        }

        if (player.isSneaking() && existing.isEmpty() && heldStack.isOf(KEYPAD)) {
            openKeypadSetup(serverPlayer, serverWorld, hand, linkedPositions);
            return ActionResult.SUCCESS;
        }

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
                putLinkedLock(serverWorld, linkedPositions,
                        new LockState.LockEntry(KEY_LOCK, heldKeyId.toString(), lockId,
                                player.getUuid(), ""));
                send(player, "message.crouchlock.locked");
                return ActionResult.SUCCESS;
            }

            if (keyMatches(heldStack, existing.get())) {
                lockState.removeLock(existing.get().lockId());
                send(player, "message.crouchlock.unlocked");
                return ActionResult.SUCCESS;
            }

            send(player, "message.crouchlock.wrong_key");
            return ActionResult.FAIL;
        }

        if (existing.isEmpty() || keyMatches(heldStack, existing.get())) {
            return ActionResult.PASS;
        }

        send(player, "message.crouchlock.locked_need_key");
        return ActionResult.FAIL;
    }

    private static void openKeypadSetup(ServerPlayerEntity player, ServerWorld world, Hand hand,
                                        List<BlockPos> linkedPositions) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new KeypadScreenHandler(syncId, inventory, code -> {
                    LockState lockState = LockState.get(world);
                    if (findExistingLock(lockState, linkedPositions).isPresent()) {
                        send(player, "message.crouchlock.already_locked");
                        return false;
                    }

                    for (BlockPos pos : linkedPositions) {
                        BlockState state = world.getBlockState(pos);
                        if (!isLockable(world, pos, state)) {
                            send(player, "message.crouchlock.target_changed");
                            return false;
                        }
                    }

                    ItemStack keypadStack = player.getStackInHand(hand);
                    if (!keypadStack.isOf(KEYPAD) && !player.getAbilities().creativeMode) {
                        send(player, "message.crouchlock.need_keypad");
                        return false;
                    }

                    UUID lockId = UUID.randomUUID();
                    putLinkedLock(world, linkedPositions,
                            new LockState.LockEntry(KEYPAD_LOCK, hashCode(lockId, code), lockId,
                                    player.getUuid(), ""));
                    if (!player.getAbilities().creativeMode) {
                        keypadStack.decrement(1);
                    }
                    send(player, "message.crouchlock.keypad_locked");
                    return true;
                }),
                Text.translatable("screen.crouchlock.set_code")
        ));
    }

    private static void openKeypadUnlock(ServerPlayerEntity player, ServerWorld world, Hand hand,
                                         BlockHitResult hit, LockState.LockEntry originalLock,
                                         boolean removeLock) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new KeypadScreenHandler(syncId, inventory, code -> {
                    LockState lockState = LockState.get(world);
                    Optional<LockState.LockEntry> current = lockState.get(hit.getBlockPos());
                    if (current.isEmpty() || !current.get().lockId().equals(originalLock.lockId())) {
                        send(player, "message.crouchlock.target_changed");
                        return true;
                    }

                    if (!current.get().credential().equals(hashCode(current.get().lockId(), code))) {
                        send(player, "message.crouchlock.wrong_code");
                        return false;
                    }

                    if (removeLock) {
                        lockState.removeLock(current.get().lockId());
                        send(player, "message.crouchlock.unlocked");
                    } else {
                        END_OF_TICK_ACTIONS.add(() -> {
                            if (player.isRemoved()) {
                                return;
                            }
                            BlockState targetState = world.getBlockState(hit.getBlockPos());
                            if (isLockable(world, hit.getBlockPos(), targetState)) {
                                targetState.onUse(world, player, hand, hit);
                            }
                        });
                    }
                    return true;
                }),
                Text.translatable(removeLock
                        ? "screen.crouchlock.remove_code"
                        : "screen.crouchlock.enter_code")
        ));
    }

    private static void putLinkedLock(ServerWorld world, List<BlockPos> positions, LockState.LockEntry prototype) {
        LockState state = LockState.get(world);
        for (BlockPos pos : positions) {
            Block block = world.getBlockState(pos).getBlock();
            String blockId = Registries.BLOCK.getId(block).toString();
            state.put(pos, new LockState.LockEntry(prototype.type(), prototype.credential(),
                    prototype.lockId(), prototype.ownerId(), blockId));
        }
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

        if (keyMatches(player.getMainHandStack(), existing.get())
                || keyMatches(player.getOffHandStack(), existing.get())) {
            return true;
        }

        send(player, KEYPAD_LOCK.equals(existing.get().type())
                ? "message.crouchlock.cannot_break_keypad"
                : "message.crouchlock.cannot_break");
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
        if (!KEY_LOCK.equals(lock.type())) {
            return false;
        }
        return LockKeyItem.getKeyId(stack)
                .map(id -> id.toString().equals(lock.credential()))
                .orElse(false);
    }

    private static String hashCode(UUID lockId, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((lockId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void send(PlayerEntity player, String translationKey) {
        player.sendMessage(Text.translatable(translationKey), true);
    }
}
