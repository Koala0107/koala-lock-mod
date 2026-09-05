package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.Entity;
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

/**
 * Stable visual placement for lock displays.
 *
 * The main mod remains the owner of the display entity position. This class only changes
 * the render transform, so the two tick handlers no longer fight over the entity position.
 */
public final class LockMarkerLayoutFix implements ModInitializer {
    private static final String LAYOUT_TAG = CrouchLockMod.MOD_ID + ":marker_layout_v5";
    private static final float DISPLAY_SCALE = 0.30F;
    private static final double SURFACE_GAP = 0.075;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % 20 == 0) {
                syncMarkerLayout(world);
            }
        });
    }

    private static void syncMarkerLayout(ServerWorld world) {
        Map<UUID, List<BlockPos>> targets = new HashMap<>();
        for (var entry : LockState.get(world).entries()) {
            BlockPos pos = BlockPos.fromLong(entry.getKey());
            if (!world.isChunkLoaded(pos)) {
                continue;
            }
            targets.computeIfAbsent(entry.getValue().lockId(), ignored -> new ArrayList<>())
                    .add(pos.toImmutable());
        }

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof DisplayEntity.ItemDisplayEntity marker)
                    || !entity.getCommandTags().contains(CrouchLockMod.MARKER_TAG)) {
                continue;
            }

            Optional<UUID> lockId = markerLockId(entity);
            List<BlockPos> positions = lockId.map(targets::get).orElse(null);
            if (positions == null || positions.isEmpty()) {
                continue;
            }

            // Apply a layout version only once. Existing v2-v4 markers are migrated automatically.
            if (!marker.getCommandTags().contains(LAYOUT_TAG)) {
                applyLayout(world, marker, positions);
            }
        }
    }

    private static void applyLayout(ServerWorld world, DisplayEntity.ItemDisplayEntity marker,
                                    List<BlockPos> positions) {
        // This is the exact anchor used by CrouchLockMod. Keep the entity here so its own
        // synchronizer never tries to pull the marker back to another position.
        Vec3d anchor = baseMarkerPosition(world, positions.get(0));
        MarkerPose desired = markerPose(world, positions);
        Vec3d worldOffset = desired.position().subtract(anchor);
        Vec3d localOffset = worldToLocal(worldOffset, desired.yaw(), desired.pitch());

        // IMPORTANT: preserve the complete existing NBT, including the displayed ItemStack.
        // Reading a partial NBT compound into an existing ItemDisplay can clear display data.
        NbtCompound settings = new NbtCompound();
        marker.writeNbt(settings);
        settings.putString("billboard", "fixed");
        settings.putString("item_display", "fixed");

        // A larger render-culling box prevents translated double-chest markers from vanishing
        // when their visible model is away from the entity anchor.
        settings.putFloat("width", 1.50F);
        settings.putFloat("height", 1.50F);
        settings.put("transformation", translatedScale(localOffset, DISPLAY_SCALE));
        marker.readNbt(settings);

        marker.refreshPositionAndAngles(anchor.x, anchor.y, anchor.z,
                desired.yaw(), desired.pitch());
        marker.setInvulnerable(true);
        marker.addCommandTag(LAYOUT_TAG);
    }

    private static MarkerPose markerPose(ServerWorld world, List<BlockPos> positions) {
        BlockPos first = lowerDoorHalf(world, positions.get(0));
        BlockState state = world.getBlockState(first);

        if (state.getBlock() instanceof TrapdoorBlock) {
            boolean topHalf = state.contains(Properties.BLOCK_HALF)
                    && "top".equals(state.get(Properties.BLOCK_HALF).asString());

            // Closed trapdoor thickness is 3/16 block. Keep the marker horizontal and
            // slightly above the top surface, independent of whether the trapdoor is open.
            double surfaceY = topHalf ? 1.0 : 0.1875;
            return new MarkerPose(
                    new Vec3d(first.getX() + 0.5,
                            first.getY() + surfaceY + SURFACE_GAP,
                            first.getZ() + 0.5),
                    0.0F,
                    -90.0F
            );
        }

        if (state.getBlock() instanceof DoorBlock) {
            BlockState closedState = state.contains(Properties.OPEN)
                    ? state.with(Properties.OPEN, false)
                    : state;
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            Vec3d face = markerNearOutline(world, first, closedState, 0.95, SURFACE_GAP);

            // Hinge LEFT means the handle is on the viewer's right, and vice versa.
            boolean leftHinge = state.contains(Properties.DOOR_HINGE)
                    && "left".equals(state.get(Properties.DOOR_HINGE).asString());
            Direction handleSide = leftHinge
                    ? facing.rotateYClockwise()
                    : facing.rotateYCounterclockwise();

            Vec3d handle = face.add(
                    handleSide.getOffsetX() * 0.30,
                    0.0,
                    handleSide.getOffsetZ() * 0.30
            );
            return new MarkerPose(handle, yawFor(facing), 0.0F);
        }

        if (state.getBlock() instanceof ChestBlock) {
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            Vec3d center = linkedBlockCenter(positions).add(
                    facing.getOffsetX() * (0.5 + SURFACE_GAP),
                    0.0,
                    facing.getOffsetZ() * (0.5 + SURFACE_GAP)
            );
            return new MarkerPose(center, yawFor(facing), 0.0F);
        }

        if (state.getBlock() instanceof BarrelBlock) {
            Direction facing = state.get(Properties.FACING);
            // For a downward-facing barrel the base anchor is already outside the lower face;
            // avoid translating below its culling origin while keeping every other face clear.
            double outward = facing == Direction.DOWN ? 0.515 : 0.5 + SURFACE_GAP;
            Vec3d center = Vec3d.ofCenter(first).add(
                    facing.getOffsetX() * outward,
                    facing.getOffsetY() * outward,
                    facing.getOffsetZ() * outward
            );
            return new MarkerPose(center, yawFor(facing), pitchFor(facing));
        }

        Direction facing = blockFacing(state);
        double outward = facing == Direction.DOWN ? 0.515 : 0.5 + SURFACE_GAP;
        Vec3d center = Vec3d.ofCenter(first).add(
                facing.getOffsetX() * outward,
                facing.getOffsetY() * outward,
                facing.getOffsetZ() * outward
        );
        return new MarkerPose(center, yawFor(facing), pitchFor(facing));
    }

    /** Duplicates the main mod's current marker anchor so its synchronizer sees no position drift. */
    private static Vec3d baseMarkerPosition(ServerWorld world, BlockPos originalPos) {
        BlockPos pos = lowerDoorHalf(world, originalPos);
        BlockState state = world.getBlockState(pos);

        if (state.getBlock() instanceof TrapdoorBlock) {
            double panelHeight = state.contains(Properties.BLOCK_HALF)
                    && "top".equals(state.get(Properties.BLOCK_HALF).asString()) ? 0.875 : 0.125;
            return Vec3d.ofBottomCenter(pos).add(0.0, panelHeight, 0.0);
        }

        if (state.getBlock() instanceof DoorBlock) {
            BlockState fixedAnchorState = state.contains(Properties.OPEN)
                    ? state.with(Properties.OPEN, false)
                    : state;
            Vec3d doorFace = markerNearOutline(world, pos, fixedAnchorState, 0.76, 0.035);
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            boolean leftHinge = state.contains(Properties.DOOR_HINGE)
                    && "left".equals(state.get(Properties.DOOR_HINGE).asString());
            Direction handleSide = leftHinge
                    ? facing.rotateYCounterclockwise()
                    : facing.rotateYClockwise();
            return doorFace.add(handleSide.getOffsetX() * 0.29, 0.0,
                    handleSide.getOffsetZ() * 0.29);
        }

        Direction facing = blockFacing(state);
        return Vec3d.ofCenter(pos).add(
                facing.getOffsetX() * 0.515,
                facing.getOffsetY() * 0.515,
                facing.getOffsetZ() * 0.515
        );
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

    /** Converts a world-space visual offset to the display's local coordinates. */
    private static Vec3d worldToLocal(Vec3d offset, float yaw, float pitch) {
        Vec3d local = offset.rotateY((float) Math.toRadians(yaw));
        return local.rotateX((float) Math.toRadians(-pitch));
    }

    private static Vec3d markerNearOutline(ServerWorld world, BlockPos pos, BlockState state,
                                           double height, double outwardOffset) {
        Box bounds = state.getOutlineShape(world, pos).getBoundingBox();
        double localX = (bounds.minX + bounds.maxX) * 0.5;
        double localZ = (bounds.minZ + bounds.maxZ) * 0.5;
        double fromCenterX = localX - 0.5;
        double fromCenterZ = localZ - 0.5;
        double horizontalLength = Math.sqrt(fromCenterX * fromCenterX + fromCenterZ * fromCenterZ);
        if (horizontalLength > 0.001) {
            localX += fromCenterX / horizontalLength * outwardOffset;
            localZ += fromCenterZ / horizontalLength * outwardOffset;
        }
        return new Vec3d(pos.getX() + localX, pos.getY() + height, pos.getZ() + localZ);
    }

    private static NbtList translatedScale(Vec3d translation, float scale) {
        NbtList matrix = new NbtList();
        float[] values = {
                scale, 0.0F, 0.0F, (float) translation.x,
                0.0F, scale, 0.0F, (float) translation.y,
                0.0F, 0.0F, scale, (float) translation.z,
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

    private record MarkerPose(Vec3d position, float yaw, float pitch) {
    }
}
