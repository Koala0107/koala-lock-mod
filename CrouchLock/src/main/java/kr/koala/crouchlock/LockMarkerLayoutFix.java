package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Stable visual placement for lock displays. */
public final class LockMarkerLayoutFix implements ModInitializer {
    private static final String LAYOUT_TAG = CrouchLockMod.MOD_ID + ":marker_layout_v8";
    private static final String SECONDARY_TAG = CrouchLockMod.MOD_ID + ":secondary_marker_v7";
    private static final String SECONDARY_LOCK_PREFIX = CrouchLockMod.MOD_ID + ":secondary_lock:";
    private static final float DISPLAY_SCALE = 0.30F;
    private static final double SURFACE_GAP = 0.125;

    private static final Map<UUID, Boolean> OPEN_STATES = new HashMap<>();
    private static final Map<UUID, ChimeSequence> CHIMES = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            updateChimes(world);
            if (world.getTime() % 2 == 0) {
                syncMarkerLayout(world);
            }
        });
    }

    private static void syncMarkerLayout(ServerWorld world) {
        Map<UUID, List<BlockPos>> targets = collectTargets(world);
        Map<UUID, DisplayEntity.ItemDisplayEntity> primaryMarkers = new HashMap<>();
        Map<UUID, DisplayEntity.ItemDisplayEntity> secondaryMarkers = new HashMap<>();
        List<DisplayEntity.ItemDisplayEntity> duplicateSecondaries = new ArrayList<>();

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof DisplayEntity.ItemDisplayEntity marker)) {
                continue;
            }

            if (marker.getCommandTags().contains(CrouchLockMod.MARKER_TAG)) {
                markerLockId(marker).ifPresent(lockId -> primaryMarkers.put(lockId, marker));
                continue;
            }

            if (marker.getCommandTags().contains(SECONDARY_TAG)) {
                Optional<UUID> lockId = secondaryLockId(marker);
                if (lockId.isEmpty()) {
                    duplicateSecondaries.add(marker);
                    continue;
                }
                DisplayEntity.ItemDisplayEntity previous = secondaryMarkers.putIfAbsent(lockId.get(), marker);
                if (previous != null) {
                    duplicateSecondaries.add(marker);
                }
            }
        }

        for (DisplayEntity.ItemDisplayEntity duplicate : duplicateSecondaries) {
            duplicate.discard();
        }

        Set<UUID> activeOpenableLocks = new HashSet<>();

        for (Map.Entry<UUID, DisplayEntity.ItemDisplayEntity> entry : primaryMarkers.entrySet()) {
            UUID lockId = entry.getKey();
            List<BlockPos> positions = targets.get(lockId);
            if (positions == null || positions.isEmpty()) {
                continue;
            }

            PosePair poses = markerPoses(world, positions);
            applyPose(entry.getValue(), poses.primary());

            DisplayEntity.ItemDisplayEntity secondary = secondaryMarkers.remove(lockId);
            if (poses.secondary() != null) {
                if (secondary == null || secondary.isRemoved()) {
                    secondary = createSecondary(world, entry.getValue(), lockId);
                }
                applyPose(secondary, poses.secondary());
            } else if (secondary != null) {
                secondary.discard();
            }

            updateOpenState(world, lockId, positions, activeOpenableLocks);
        }

        for (Map.Entry<UUID, DisplayEntity.ItemDisplayEntity> entry : secondaryMarkers.entrySet()) {
            if (!targets.containsKey(entry.getKey()) || !needsTwoSides(world, targets.get(entry.getKey()))) {
                entry.getValue().discard();
            }
        }

        OPEN_STATES.keySet().removeIf(lockId -> !targets.containsKey(lockId) && !CHIMES.containsKey(lockId));
    }

    private static void updateOpenState(ServerWorld world, UUID lockId, List<BlockPos> positions,
                                        Set<UUID> activeOpenableLocks) {
        BlockPos first = lowerDoorHalf(world, positions.get(0));
        BlockState state = world.getBlockState(first);
        if (!(state.getBlock() instanceof DoorBlock) && !(state.getBlock() instanceof TrapdoorBlock)) {
            return;
        }
        if (!state.contains(Properties.OPEN)) {
            return;
        }

        activeOpenableLocks.add(lockId);
        boolean open = state.get(Properties.OPEN);
        Boolean previous = OPEN_STATES.put(lockId, open);
        if (previous != null && !previous && open) {
            startChime(world, lockId, first);
        }
    }

    private static void startChime(ServerWorld world, UUID lockId, BlockPos pos) {
        CHIMES.put(lockId, new ChimeSequence(world, pos.toImmutable(), 0, world.getTime()));
    }

    private static void updateChimes(ServerWorld world) {
        Iterator<Map.Entry<UUID, ChimeSequence>> iterator = CHIMES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ChimeSequence> entry = iterator.next();
            ChimeSequence sequence = entry.getValue();
            if (sequence.world() != world || world.getTime() < sequence.nextTick()) {
                continue;
            }

            float pitch = switch (sequence.step()) {
                case 0 -> 1.00F;
                case 1 -> 1.25F;
                default -> 1.50F;
            };
            world.playSound(null, sequence.pos(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    SoundCategory.BLOCKS, 0.75F, pitch);

            if (sequence.step() >= 2) {
                iterator.remove();
            } else {
                entry.setValue(new ChimeSequence(
                        world,
                        sequence.pos(),
                        sequence.step() + 1,
                        world.getTime() + 3
                ));
            }
        }
    }

    private static Map<UUID, List<BlockPos>> collectTargets(ServerWorld world) {
        Map<UUID, List<BlockPos>> targets = new HashMap<>();
        for (var entry : LockState.get(world).entries()) {
            BlockPos pos = BlockPos.fromLong(entry.getKey());
            if (!world.isChunkLoaded(pos)) {
                continue;
            }
            targets.computeIfAbsent(entry.getValue().lockId(), ignored -> new ArrayList<>())
                    .add(pos.toImmutable());
        }
        return targets;
    }

    private static boolean needsTwoSides(ServerWorld world, List<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) {
            return false;
        }
        BlockPos first = lowerDoorHalf(world, positions.get(0));
        BlockState state = world.getBlockState(first);
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapdoorBlock;
    }

    private static DisplayEntity.ItemDisplayEntity createSecondary(ServerWorld world,
                                                                    DisplayEntity.ItemDisplayEntity primary,
                                                                    UUID lockId) {
        NbtCompound copied = new NbtCompound();
        primary.writeNbt(copied);
        copied.remove("UUID");
        copied.remove("Pos");
        copied.remove("Motion");
        copied.remove("Rotation");
        copied.remove("Tags");

        DisplayEntity.ItemDisplayEntity secondary =
                new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
        secondary.readNbt(copied);
        secondary.addCommandTag(SECONDARY_TAG);
        secondary.addCommandTag(SECONDARY_LOCK_PREFIX + lockId);
        secondary.setInvulnerable(true);
        world.spawnEntity(secondary);
        return secondary;
    }

    private static void applyPose(DisplayEntity.ItemDisplayEntity marker, MarkerPose pose) {
        NbtCompound settings = new NbtCompound();
        marker.writeNbt(settings);
        settings.putString("billboard", "fixed");
        settings.putString("item_display", "fixed");
        settings.putFloat("width", 0.60F);
        settings.putFloat("height", 0.60F);
        settings.put("transformation", displayScale(DISPLAY_SCALE));
        marker.readNbt(settings);
        marker.refreshPositionAndAngles(
                pose.position().x,
                pose.position().y,
                pose.position().z,
                pose.yaw(),
                pose.pitch()
        );
        marker.setInvulnerable(true);
        marker.addCommandTag(LAYOUT_TAG);
    }

    private static PosePair markerPoses(ServerWorld world, List<BlockPos> positions) {
        BlockPos first = lowerDoorHalf(world, positions.get(0));
        BlockState state = world.getBlockState(first);

        if (state.getBlock() instanceof TrapdoorBlock) {
            return trapdoorPoses(world, first, state);
        }

        if (state.getBlock() instanceof DoorBlock) {
            return doorPoses(world, first, state);
        }

        if (state.getBlock() instanceof ChestBlock) {
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            Vec3d center = linkedBlockCenter(positions).add(
                    facing.getOffsetX() * (0.5 + SURFACE_GAP),
                    0.0,
                    facing.getOffsetZ() * (0.5 + SURFACE_GAP)
            );
            return new PosePair(new MarkerPose(center, yawFor(facing), 0.0F), null);
        }

        if (state.getBlock() instanceof BarrelBlock) {
            Direction facing = state.get(Properties.FACING);
            double outward = 0.5 + SURFACE_GAP;
            Vec3d center = Vec3d.ofCenter(first).add(
                    facing.getOffsetX() * outward,
                    facing.getOffsetY() * outward,
                    facing.getOffsetZ() * outward
            );
            return new PosePair(new MarkerPose(center, yawFor(facing), pitchFor(facing)), null);
        }

        Direction facing = blockFacing(state);
        double outward = 0.5 + SURFACE_GAP;
        Vec3d center = Vec3d.ofCenter(first).add(
                facing.getOffsetX() * outward,
                facing.getOffsetY() * outward,
                facing.getOffsetZ() * outward
        );
        return new PosePair(new MarkerPose(center, yawFor(facing), pitchFor(facing)), null);
    }

    private static PosePair trapdoorPoses(ServerWorld world, BlockPos pos, BlockState state) {
        Box bounds = state.getOutlineShape(world, pos).getBoundingBox();
        boolean open = state.contains(Properties.OPEN) && state.get(Properties.OPEN);

        if (!open) {
            Direction facing = state.contains(Properties.HORIZONTAL_FACING)
                    ? state.get(Properties.HORIZONTAL_FACING)
                    : Direction.SOUTH;
            double x = pos.getX() + (bounds.minX + bounds.maxX) * 0.5;
            double z = pos.getZ() + (bounds.minZ + bounds.maxZ) * 0.5;
            MarkerPose top = new MarkerPose(
                    new Vec3d(x, pos.getY() + bounds.maxY + SURFACE_GAP, z),
                    yawFor(facing),
                    -90.0F
            );
            MarkerPose bottom = new MarkerPose(
                    new Vec3d(x, pos.getY() + bounds.minY - SURFACE_GAP, z),
                    yawFor(facing.getOpposite()),
                    90.0F
            );
            return new PosePair(top, bottom);
        }

        Direction normal = horizontalNormalFromBounds(bounds);
        Vec3d panelCenter = new Vec3d(
                pos.getX() + (bounds.minX + bounds.maxX) * 0.5,
                pos.getY() + (bounds.minY + bounds.maxY) * 0.5,
                pos.getZ() + (bounds.minZ + bounds.maxZ) * 0.5
        );
        double halfThickness = halfThickness(bounds, normal);
        Vec3d offset = directionVector(normal).multiply(halfThickness + SURFACE_GAP);

        MarkerPose front = new MarkerPose(
                panelCenter.add(offset),
                yawFor(normal),
                0.0F
        );
        MarkerPose back = new MarkerPose(
                panelCenter.subtract(offset),
                yawFor(normal.getOpposite()),
                0.0F
        );
        return new PosePair(front, back);
    }

    private static PosePair doorPoses(ServerWorld world, BlockPos pos, BlockState state) {
        BlockState closedState = state.contains(Properties.OPEN)
                ? state.with(Properties.OPEN, false)
                : state;
        Box closedBounds = closedState.getOutlineShape(world, pos).getBoundingBox();
        Box actualBounds = state.getOutlineShape(world, pos).getBoundingBox();
        Direction facing = state.get(Properties.HORIZONTAL_FACING);

        boolean leftHinge = state.contains(Properties.DOOR_HINGE)
                && "left".equals(state.get(Properties.DOOR_HINGE).asString());
        Direction closedHandleSide = leftHinge
                ? facing.rotateYClockwise()
                : facing.rotateYCounterclockwise();

        Vec3d closedCenter = new Vec3d(
                pos.getX() + (closedBounds.minX + closedBounds.maxX) * 0.5,
                pos.getY() + 0.95,
                pos.getZ() + (closedBounds.minZ + closedBounds.maxZ) * 0.5
        );
        Vec3d hingePoint = closedCenter.subtract(directionVector(closedHandleSide).multiply(0.5));

        Vec3d actualCenter = new Vec3d(
                pos.getX() + (actualBounds.minX + actualBounds.maxX) * 0.5,
                pos.getY() + 0.95,
                pos.getZ() + (actualBounds.minZ + actualBounds.maxZ) * 0.5
        );

        Vec3d hingeToPanel = actualCenter.subtract(hingePoint);
        Vec3d handleDirection;
        if (hingeToPanel.lengthSquared() < 0.0001) {
            handleDirection = directionVector(closedHandleSide);
        } else {
            handleDirection = hingeToPanel.normalize();
        }
        Vec3d handle = hingePoint.add(handleDirection.multiply(0.80));

        Direction normal = horizontalNormalFromBounds(actualBounds);
        double halfThickness = halfThickness(actualBounds, normal);
        Vec3d offset = directionVector(normal).multiply(halfThickness + SURFACE_GAP);

        MarkerPose front = new MarkerPose(
                handle.add(offset),
                yawFor(normal),
                0.0F
        );
        MarkerPose back = new MarkerPose(
                handle.subtract(offset),
                yawFor(normal.getOpposite()),
                0.0F
        );
        return new PosePair(front, back);
    }

    private static Direction horizontalNormalFromBounds(Box bounds) {
        double centerX = (bounds.minX + bounds.maxX) * 0.5 - 0.5;
        double centerZ = (bounds.minZ + bounds.maxZ) * 0.5 - 0.5;
        if (Math.abs(centerX) >= Math.abs(centerZ) && Math.abs(centerX) > 0.001) {
            return centerX > 0.0 ? Direction.EAST : Direction.WEST;
        }
        if (Math.abs(centerZ) > 0.001) {
            return centerZ > 0.0 ? Direction.SOUTH : Direction.NORTH;
        }
        return Direction.SOUTH;
    }

    private static double halfThickness(Box bounds, Direction normal) {
        return switch (normal.getAxis()) {
            case X -> (bounds.maxX - bounds.minX) * 0.5;
            case Z -> (bounds.maxZ - bounds.minZ) * 0.5;
            default -> (bounds.maxY - bounds.minY) * 0.5;
        };
    }

    private static Vec3d directionVector(Direction direction) {
        return new Vec3d(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
    }

    private static Vec3d linkedBlockCenter(List<BlockPos> positions) {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (BlockPos pos : positions) {
            x += pos.getX() + 0.5;
            y += pos.getY() + 0.5;
            z += pos.getZ() + 0.5;
        }
        double count = positions.size();
        return new Vec3d(x / count, y / count, z / count);
    }

    private static Direction blockFacing(BlockState state) {
        if (state.contains(Properties.FACING)) {
            return state.get(Properties.FACING);
        }
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            return state.get(Properties.HORIZONTAL_FACING);
        }
        return Direction.SOUTH;
    }

    private static float yawFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static float pitchFor(Direction facing) {
        return switch (facing) {
            case UP -> -90.0F;
            case DOWN -> 90.0F;
            default -> 0.0F;
        };
    }

    private static NbtList displayScale(float scale) {
        NbtList matrix = new NbtList();
        float[] values = {
                scale, 0.0F, 0.0F, 0.0F,
                0.0F, scale, 0.0F, 0.0F,
                0.0F, 0.0F, scale, 0.0F,
                0.0F, 0.0F, 0.0F, 1.0F
        };
        for (float value : values) {
            matrix.add(NbtFloat.of(value));
        }
        return matrix;
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
        String prefix = CrouchLockMod.MOD_ID + ":";
        for (String tag : marker.getCommandTags()) {
            if (!tag.startsWith(prefix)
                    || CrouchLockMod.MARKER_TAG.equals(tag)
                    || tag.startsWith(CrouchLockMod.MOD_ID + ":marker_layout_")) {
                continue;
            }
            try {
                return Optional.of(UUID.fromString(tag.substring(prefix.length())));
            } catch (IllegalArgumentException ignored) {
                // Ignore unrelated command tags.
            }
        }
        return Optional.empty();
    }

    private static Optional<UUID> secondaryLockId(Entity marker) {
        for (String tag : marker.getCommandTags()) {
            if (!tag.startsWith(SECONDARY_LOCK_PREFIX)) {
                continue;
            }
            try {
                return Optional.of(UUID.fromString(tag.substring(SECONDARY_LOCK_PREFIX.length())));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private record MarkerPose(Vec3d position, float yaw, float pitch) {
    }

    private record PosePair(MarkerPose primary, MarkerPose secondary) {
    }

    private record ChimeSequence(ServerWorld world, BlockPos pos, int step, long nextTick) {
    }
}
