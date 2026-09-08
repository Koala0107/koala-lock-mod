package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Keeps lock/keypad displays fixed to their world-facing surfaces instead of the camera. */
public final class LockMarkerLayoutFix implements ModInitializer {
    private static final String LAYOUT_TAG = CrouchLockMod.MOD_ID + ":marker_layout_v8";
    private static final String SECONDARY_TAG = CrouchLockMod.MOD_ID + ":secondary_marker_v7";
    private static final String SECONDARY_LOCK_PREFIX = CrouchLockMod.MOD_ID + ":secondary_lock:";
    private static final float DISPLAY_SCALE = 0.30F;
    private static final double SURFACE_GAP = 0.125D;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if ((world.getTime() & 1L) == 0L) syncMarkerLayout(world);
        });
    }

    private static void syncMarkerLayout(ServerWorld world) {
        Map<UUID, List<BlockPos>> targets = collectTargets(world);
        Map<UUID, DisplayEntity.ItemDisplayEntity> primary = new HashMap<>();
        Map<UUID, DisplayEntity.ItemDisplayEntity> secondary = new HashMap<>();
        List<DisplayEntity.ItemDisplayEntity> badSecondary = new ArrayList<>();

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof DisplayEntity.ItemDisplayEntity display)) continue;
            if (display.getCommandTags().contains(CrouchLockMod.MARKER_TAG)) {
                markerLockId(display).ifPresent(id -> primary.putIfAbsent(id, display));
            } else if (display.getCommandTags().contains(SECONDARY_TAG)) {
                Optional<UUID> id = secondaryLockId(display);
                if (id.isEmpty()) {
                    badSecondary.add(display);
                } else {
                    DisplayEntity.ItemDisplayEntity duplicate = secondary.putIfAbsent(id.get(), display);
                    if (duplicate != null) badSecondary.add(display);
                }
            }
        }
        badSecondary.forEach(Entity::discard);

        for (Map.Entry<UUID, DisplayEntity.ItemDisplayEntity> entry : primary.entrySet()) {
            UUID lockId = entry.getKey();
            List<BlockPos> positions = targets.get(lockId);
            if (positions == null || positions.isEmpty()) continue;

            PosePair poses = markerPoses(world, positions);
            applyPose(entry.getValue(), poses.primary());

            DisplayEntity.ItemDisplayEntity second = secondary.remove(lockId);
            if (poses.secondary() != null) {
                if (second == null || second.isRemoved()) {
                    second = createSecondary(world, entry.getValue(), lockId);
                }
                applyPose(second, poses.secondary());
            } else if (second != null) {
                second.discard();
            }
        }

        for (Map.Entry<UUID, DisplayEntity.ItemDisplayEntity> entry : secondary.entrySet()) {
            List<BlockPos> positions = targets.get(entry.getKey());
            if (positions == null || !needsTwoSides(world, positions)) entry.getValue().discard();
        }
    }

    private static Map<UUID, List<BlockPos>> collectTargets(ServerWorld world) {
        Map<UUID, List<BlockPos>> targets = new HashMap<>();
        for (Map.Entry<Long, LockState.LockEntry> entry : LockState.get(world).entries()) {
            BlockPos pos = BlockPos.fromLong(entry.getKey());
            if (!world.isChunkLoaded(pos)) continue;
            targets.computeIfAbsent(entry.getValue().lockId(), ignored -> new ArrayList<>()).add(pos.toImmutable());
        }
        return targets;
    }

    private static boolean needsTwoSides(ServerWorld world, List<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) return false;
        BlockPos pos = lowerDoorHalf(world, positions.get(0));
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapdoorBlock;
    }

    private static DisplayEntity.ItemDisplayEntity createSecondary(ServerWorld world,
                                                                    DisplayEntity.ItemDisplayEntity primary,
                                                                    UUID lockId) {
        NbtCompound nbt = new NbtCompound();
        primary.writeNbt(nbt);
        nbt.remove("UUID");
        nbt.remove("Pos");
        nbt.remove("Motion");
        nbt.remove("Rotation");
        nbt.remove("Tags");

        DisplayEntity.ItemDisplayEntity secondary =
                new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
        secondary.readNbt(nbt);
        secondary.addCommandTag(SECONDARY_TAG);
        secondary.addCommandTag(SECONDARY_LOCK_PREFIX + lockId);
        secondary.setInvulnerable(true);
        world.spawnEntity(secondary);
        return secondary;
    }

    private static void applyPose(DisplayEntity.ItemDisplayEntity marker, MarkerPose pose) {
        NbtCompound nbt = new NbtCompound();
        marker.writeNbt(nbt);
        nbt.putString("billboard", "fixed");
        nbt.putString("item_display", "fixed");
        nbt.putFloat("width", 0.60F);
        nbt.putFloat("height", 0.60F);
        nbt.put("transformation", displayScale(DISPLAY_SCALE));
        marker.readNbt(nbt);
        marker.refreshPositionAndAngles(pose.position().x, pose.position().y, pose.position().z,
                pose.yaw(), pose.pitch());
        marker.setInvulnerable(true);
        marker.addCommandTag(LAYOUT_TAG);
    }

    private static PosePair markerPoses(ServerWorld world, List<BlockPos> positions) {
        BlockPos pos = lowerDoorHalf(world, positions.get(0));
        BlockState state = world.getBlockState(pos);

        if (state.getBlock() instanceof TrapdoorBlock) return trapdoorPoses(world, pos, state);
        if (state.getBlock() instanceof DoorBlock) return doorPoses(world, pos, state);

        if (state.getBlock() instanceof ChestBlock) {
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            Vec3d position = linkedBlockCenter(positions).add(
                    facing.getOffsetX() * 0.625D, 0.0D, facing.getOffsetZ() * 0.625D);
            return new PosePair(new MarkerPose(position, yawFor(facing), 0.0F), null);
        }

        if (state.getBlock() instanceof BarrelBlock) {
            Direction facing = state.get(Properties.FACING);
            Vec3d position = Vec3d.ofCenter(pos).add(
                    facing.getOffsetX() * 0.625D,
                    facing.getOffsetY() * 0.625D,
                    facing.getOffsetZ() * 0.625D);
            return new PosePair(new MarkerPose(position, yawFor(facing), pitchFor(facing)), null);
        }

        Direction facing = blockFacing(state);
        Vec3d position = Vec3d.ofCenter(pos).add(
                facing.getOffsetX() * 0.625D,
                facing.getOffsetY() * 0.625D,
                facing.getOffsetZ() * 0.625D);
        return new PosePair(new MarkerPose(position, yawFor(facing), pitchFor(facing)), null);
    }

    private static PosePair trapdoorPoses(ServerWorld world, BlockPos pos, BlockState state) {
        Box bounds = state.getOutlineShape(world, pos).getBoundingBox();
        boolean open = state.contains(Properties.OPEN) && state.get(Properties.OPEN);

        if (!open) {
            Direction facing = state.contains(Properties.HORIZONTAL_FACING)
                    ? state.get(Properties.HORIZONTAL_FACING) : Direction.SOUTH;
            double x = pos.getX() + (bounds.minX + bounds.maxX) * 0.5D;
            double z = pos.getZ() + (bounds.minZ + bounds.maxZ) * 0.5D;
            MarkerPose top = new MarkerPose(
                    new Vec3d(x, pos.getY() + bounds.maxY + SURFACE_GAP, z),
                    yawFor(facing), -90.0F);
            MarkerPose bottom = new MarkerPose(
                    new Vec3d(x, pos.getY() + bounds.minY - SURFACE_GAP, z),
                    yawFor(facing.getOpposite()), 90.0F);
            return new PosePair(top, bottom);
        }

        Direction normal = horizontalNormalFromBounds(bounds);
        double x = pos.getX() + (bounds.minX + bounds.maxX) * 0.5D;
        double y = pos.getY() + (bounds.minY + bounds.maxY) * 0.5D;
        double z = pos.getZ() + (bounds.minZ + bounds.maxZ) * 0.5D;
        Vec3d center = new Vec3d(x, y, z);
        Vec3d offset = directionVector(normal).multiply(halfThickness(bounds, normal) + SURFACE_GAP);
        return new PosePair(
                new MarkerPose(center.add(offset), yawFor(normal), 0.0F),
                new MarkerPose(center.subtract(offset), yawFor(normal.getOpposite()), 0.0F));
    }

    private static PosePair doorPoses(ServerWorld world, BlockPos pos, BlockState state) {
        BlockState closedState = state.contains(Properties.OPEN) ? state.with(Properties.OPEN, false) : state;
        Box closedBounds = closedState.getOutlineShape(world, pos).getBoundingBox();
        Box currentBounds = state.getOutlineShape(world, pos).getBoundingBox();
        Direction facing = state.get(Properties.HORIZONTAL_FACING);
        boolean leftHinge = state.contains(Properties.DOOR_HINGE)
                && state.get(Properties.DOOR_HINGE) == DoorHinge.LEFT;
        Direction hingeSide = leftHinge ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();

        Vec3d closedCenter = new Vec3d(
                pos.getX() + (closedBounds.minX + closedBounds.maxX) * 0.5D,
                pos.getY() + 0.95D,
                pos.getZ() + (closedBounds.minZ + closedBounds.maxZ) * 0.5D);
        Vec3d pivot = closedCenter.subtract(directionVector(hingeSide).multiply(0.5D));
        Vec3d currentCenter = new Vec3d(
                pos.getX() + (currentBounds.minX + currentBounds.maxX) * 0.5D,
                pos.getY() + 0.95D,
                pos.getZ() + (currentBounds.minZ + currentBounds.maxZ) * 0.5D);
        Vec3d delta = currentCenter.subtract(pivot);
        Vec3d radial = delta.lengthSquared() < 1.0E-4D ? directionVector(hingeSide) : delta.normalize();
        Vec3d panelCenter = pivot.add(radial.multiply(0.8D));

        Direction normal = horizontalNormalFromBounds(currentBounds);
        Vec3d offset = directionVector(normal).multiply(halfThickness(currentBounds, normal) + SURFACE_GAP);
        return new PosePair(
                new MarkerPose(panelCenter.add(offset), yawFor(normal), 0.0F),
                new MarkerPose(panelCenter.subtract(offset), yawFor(normal.getOpposite()), 0.0F));
    }

    private static Direction horizontalNormalFromBounds(Box bounds) {
        double x = (bounds.minX + bounds.maxX) * 0.5D - 0.5D;
        double z = (bounds.minZ + bounds.maxZ) * 0.5D - 0.5D;
        if (Math.abs(x) >= Math.abs(z) && Math.abs(x) > 0.001D) return x > 0 ? Direction.EAST : Direction.WEST;
        if (Math.abs(z) > 0.001D) return z > 0 ? Direction.SOUTH : Direction.NORTH;
        return Direction.SOUTH;
    }

    private static double halfThickness(Box bounds, Direction normal) {
        return switch (normal.getAxis()) {
            case X -> (bounds.maxX - bounds.minX) * 0.5D;
            case Y -> (bounds.maxY - bounds.minY) * 0.5D;
            case Z -> (bounds.maxZ - bounds.minZ) * 0.5D;
        };
    }

    private static Vec3d directionVector(Direction direction) {
        return new Vec3d(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
    }

    private static Vec3d linkedBlockCenter(List<BlockPos> positions) {
        double x = 0, y = 0, z = 0;
        for (BlockPos pos : positions) {
            Vec3d center = Vec3d.ofCenter(pos);
            x += center.x;
            y += center.y;
            z += center.z;
        }
        double count = Math.max(1, positions.size());
        return new Vec3d(x / count, y / count, z / count);
    }

    private static Direction blockFacing(BlockState state) {
        if (state.contains(Properties.FACING)) return state.get(Properties.FACING);
        if (state.contains(Properties.HORIZONTAL_FACING)) return state.get(Properties.HORIZONTAL_FACING);
        return Direction.SOUTH;
    }

    private static float yawFor(Direction direction) {
        return switch (direction) {
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static float pitchFor(Direction direction) {
        return switch (direction) {
            case UP -> -90.0F;
            case DOWN -> 90.0F;
            default -> 0.0F;
        };
    }

    private static NbtList displayScale(float scale) {
        NbtList matrix = new NbtList();
        float[] values = {
                scale, 0, 0, 0,
                0, scale, 0, 0,
                0, 0, scale, 0,
                0, 0, 0, 1
        };
        for (float value : values) matrix.add(NbtFloat.of(value));
        return matrix;
    }

    private static BlockPos lowerDoorHalf(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock
                && state.contains(Properties.DOUBLE_BLOCK_HALF)
                && "upper".equals(state.get(Properties.DOUBLE_BLOCK_HALF).asString())) {
            BlockPos lower = pos.down();
            if (world.getBlockState(lower).isOf(state.getBlock())) return lower;
        }
        return pos;
    }

    private static Optional<UUID> markerLockId(Entity marker) {
        String prefix = CrouchLockMod.MOD_ID + ":";
        for (String tag : marker.getCommandTags()) {
            if (!tag.startsWith(prefix) || CrouchLockMod.MARKER_TAG.equals(tag)
                    || tag.startsWith(CrouchLockMod.MOD_ID + ":marker_layout_")) continue;
            try {
                return Optional.of(UUID.fromString(tag.substring(prefix.length())));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Optional.empty();
    }

    private static Optional<UUID> secondaryLockId(Entity marker) {
        for (String tag : marker.getCommandTags()) {
            if (!tag.startsWith(SECONDARY_LOCK_PREFIX)) continue;
            try {
                return Optional.of(UUID.fromString(tag.substring(SECONDARY_LOCK_PREFIX.length())));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Optional.empty();
    }

    private record MarkerPose(Vec3d position, float yaw, float pitch) {
    }

    private record PosePair(MarkerPose primary, MarkerPose secondary) {
    }
}
