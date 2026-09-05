package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.resource.featuretoggle.FeatureFlags;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class CrouchLockMod implements ModInitializer {
    public static final String MOD_ID = "crouchlock";
    public static final String KEY_LOCK = "key";
    public static final String KEYPAD_LOCK = "keypad";
    public static final String MARKER_TAG = MOD_ID + ":marker";

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

    public static final ScreenHandlerType<KeypadScreenHandler> KEYPAD_SCREEN_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(MOD_ID, "keypad"),
            new ScreenHandlerType<>(KeypadScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(LOCK_KEY);
            entries.add(KEYPAD);
        });
        UseBlockCallback.EVENT.register(CrouchLockMod::onUseBlock);
        AttackEntityCallback.EVENT.register(CrouchLockMod::onAttackEntity);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld serverWorld) {
                LockState lockState = LockState.get(serverWorld);
                findExistingLock(lockState, linkedPositions(serverWorld, pos, state)).ifPresent(lock -> {
                    lockState.removeLock(lock.lockId());
                    removeLockMarkers(serverWorld, pos, lock.lockId());
                });
            }
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % 20 == 0) {
                LockState lockState = LockState.get(world);
                for (LockState.RemovedLock removed : lockState.pruneInvalid(world)) {
                    removeLockMarkers(world, removed.pos(), removed.lockId());
                }
                syncLockMarkers(world);
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

            if (existing.isEmpty()) {
                if (LockKeyItem.getKeyId(heldStack).isPresent()) {
                    send(player, "message.crouchlock.key_already_assigned");
                    return ActionResult.FAIL;
                }
                UUID heldKeyId = LockKeyItem.getOrCreateKeyId(heldStack);
                UUID lockId = UUID.randomUUID();
                putLinkedLock(serverWorld, linkedPositions,
                        new LockState.LockEntry(KEY_LOCK, heldKeyId.toString(), lockId,
                                player.getUuid(), ""));
                send(player, "message.crouchlock.locked");
                return ActionResult.SUCCESS;
            }

            if (keyMatches(heldStack, existing.get())) {
                lockState.removeLock(existing.get().lockId());
                removeLockMarkers(serverWorld, clickedPos, existing.get().lockId());
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
                        removeLockMarkers(world, hit.getBlockPos(), current.get().lockId());
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
        if (!positions.isEmpty()) {
            spawnLockMarker(world, positions.get(0), prototype);
        }
    }

    private static ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand,
                                               Entity entity, EntityHitResult hitResult) {
        if (!entity.getCommandTags().contains(MARKER_TAG)) {
            return ActionResult.PASS;
        }

        if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }

        Optional<UUID> lockId = markerLockId(entity);
        if (lockId.isPresent()) {
            LockState.get(serverWorld).removeLock(lockId.get());
            entity.discard();
            send(player, "message.crouchlock.lock_destroyed");
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    static boolean isLockable(World world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        return block instanceof ChestBlock
                || block instanceof BarrelBlock
                || block instanceof DoorBlock
                || block instanceof TrapdoorBlock;
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

    private static void spawnLockMarker(ServerWorld world, BlockPos pos, LockState.LockEntry lock) {
        removeLockMarkers(world, pos, lock.lockId());
        Vec3d markerPos = markerPosition(world, pos);
        ItemStack stack = new ItemStack(KEYPAD_LOCK.equals(lock.type()) ? KEYPAD : LOCK_KEY);
        stack.setCustomName(Text.translatable(KEYPAD_LOCK.equals(lock.type())
                ? "item.crouchlock.keypad_lock.marker"
                : "item.crouchlock.lock_key.marker"));
        stack.getOrCreateNbt().putString("CrouchLockLockId", lock.lockId().toString());
        DisplayEntity.ItemDisplayEntity marker = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
        NbtCompound displaySettings = new NbtCompound();
        displaySettings.putString("billboard", "center");
        displaySettings.putString("item_display", "fixed");
        displaySettings.putFloat("width", 0.45F);
        displaySettings.putFloat("height", 0.65F);
        marker.readNbt(displaySettings);
        marker.getStackReference(0).set(stack);
        marker.refreshPositionAndAngles(markerPos.x, markerPos.y, markerPos.z, 0.0F, 0.0F);
        marker.addCommandTag(MARKER_TAG);
        marker.addCommandTag(markerTag(lock.lockId()));
        marker.setInvulnerable(true);
        marker.setCustomName(stack.getName());
        world.spawnEntity(marker);
    }

    private static void syncLockMarkers(ServerWorld world) {
        LockState state = LockState.get(world);
        Map<UUID, MarkerTarget> targets = new HashMap<>();
        for (var entry : state.entries()) {
            BlockPos pos = BlockPos.fromLong(entry.getKey());
            if (!world.isChunkLoaded(pos)) {
                continue;
            }
            LockState.LockEntry lock = entry.getValue();
            targets.putIfAbsent(lock.lockId(), new MarkerTarget(pos, lock));
        }

        List<Entity> existingMarkers = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity.getCommandTags().contains(MARKER_TAG)) {
                existingMarkers.add(entity);
            }
        }

        Set<UUID> positioned = new HashSet<>();
        for (Entity marker : existingMarkers) {
            Optional<UUID> lockId = markerLockId(marker);
            MarkerTarget target = lockId.map(targets::get).orElse(null);
            if (!(marker instanceof DisplayEntity.ItemDisplayEntity)
                    || lockId.isEmpty() || target == null || !positioned.add(lockId.get())) {
                marker.discard();
                continue;
            }

            Vec3d expectedPosition = markerPosition(world, target.pos());
            if (marker.getPos().squaredDistanceTo(expectedPosition) > 0.0025) {
                marker.refreshPositionAndAngles(expectedPosition.x, expectedPosition.y,
                        expectedPosition.z, 0.0F, 0.0F);
            }
        }

        for (Map.Entry<UUID, MarkerTarget> entry : targets.entrySet()) {
            if (!positioned.contains(entry.getKey())) {
                MarkerTarget target = entry.getValue();
                spawnLockMarker(world, target.pos(), target.lock());
            }
        }
    }

    private static Vec3d markerPosition(ServerWorld world, BlockPos originalPos) {
        BlockPos pos = lowerDoorHalf(world, originalPos);
        BlockState state = world.getBlockState(pos);

        if (state.getBlock() instanceof TrapdoorBlock) {
            BlockState fixedAnchorState = state.contains(Properties.OPEN)
                    ? state.with(Properties.OPEN, true)
                    : state;
            return markerNearOutline(world, pos, fixedAnchorState, 0.50);
        }

        if (state.getBlock() instanceof DoorBlock) {
            BlockState fixedAnchorState = state.contains(Properties.OPEN)
                    ? state.with(Properties.OPEN, false)
                    : state;
            return markerNearOutline(world, pos, fixedAnchorState, 0.58);
        }

        Direction facing;
        if (state.contains(Properties.FACING)) {
            facing = state.get(Properties.FACING);
        } else if (state.contains(Properties.HORIZONTAL_FACING)) {
            facing = state.get(Properties.HORIZONTAL_FACING);
        } else {
            facing = Direction.SOUTH;
        }
        return Vec3d.ofCenter(pos).add(
                facing.getOffsetX() * 0.66,
                facing.getOffsetY() * 0.66,
                facing.getOffsetZ() * 0.66
        );
    }

    private static Vec3d markerNearOutline(ServerWorld world, BlockPos pos, BlockState state,
                                           double height) {
        Box bounds = state.getOutlineShape(world, pos).getBoundingBox();
        double localX = (bounds.minX + bounds.maxX) * 0.5;
        double localZ = (bounds.minZ + bounds.maxZ) * 0.5;
        double fromCenterX = localX - 0.5;
        double fromCenterZ = localZ - 0.5;
        double horizontalLength = Math.sqrt(fromCenterX * fromCenterX + fromCenterZ * fromCenterZ);
        if (horizontalLength > 0.001) {
            localX += fromCenterX / horizontalLength * 0.12;
            localZ += fromCenterZ / horizontalLength * 0.12;
        }
        return new Vec3d(pos.getX() + localX, pos.getY() + height, pos.getZ() + localZ);
    }

    private static BlockPos lowerDoorHalf(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock
                && state.contains(Properties.DOUBLE_BLOCK_HALF)
                && "upper".equals(state.get(Properties.DOUBLE_BLOCK_HALF).asString())) {
            BlockPos lower = pos.down();
            if (world.getBlockState(lower).isOf(state.getBlock())) {
                return lower;
            }
        }
        return pos;
    }

    private static Optional<UUID> markerLockId(Entity marker) {
        String prefix = MOD_ID + ":";
        for (String tag : marker.getCommandTags()) {
            if (!tag.startsWith(prefix) || MARKER_TAG.equals(tag)) {
                continue;
            }
            try {
                return Optional.of(UUID.fromString(tag.substring(prefix.length())));
            } catch (IllegalArgumentException ignored) {
                // Ignore unrelated command tags and continue looking for the lock UUID.
            }
        }
        return Optional.empty();
    }

    private static void removeLockMarkers(ServerWorld world, BlockPos pos, UUID lockId) {
        List<Entity> matchingMarkers = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity.getCommandTags().contains(MARKER_TAG)
                    && entity.getCommandTags().contains(markerTag(lockId))) {
                matchingMarkers.add(entity);
            }
        }
        for (Entity entity : matchingMarkers) {
            entity.discard();
        }
    }

    private static String markerTag(UUID lockId) {
        return MOD_ID + ":" + lockId;
    }

    private record MarkerTarget(BlockPos pos, LockState.LockEntry lock) {
    }

    private static void send(PlayerEntity player, String translationKey) {
        player.sendMessage(Text.translatable(translationKey), true);
    }
}
