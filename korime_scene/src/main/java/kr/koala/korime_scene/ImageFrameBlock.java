package kr.koala.korime_scene;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ImageFrameBlock extends Block implements BlockEntityProvider {
    public static final MapCodec<ImageFrameBlock> CODEC = createCodec(ImageFrameBlock::new);
    public static final DirectionProperty FACING = Properties.FACING;

    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 1);
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0, 0, 15, 16, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0, 0, 0, 1, 16, 16);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(15, 0, 0, 16, 16, 16);
    private static final VoxelShape UP_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 1, 16);
    private static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(0, 15, 0, 16, 16, 16);

    public ImageFrameBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        // The clicked face becomes the visible side of the image. This makes the frame
        // sit naturally on walls, floors, and ceilings without needing a support block.
        return getDefaultState().with(FACING, context.getSide());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            case UP -> UP_SHAPE;
            case DOWN -> DOWN_SHAPE;
        };
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              net.minecraft.entity.player.PlayerEntity player, Hand hand, BlockHitResult hit) {
        return ActionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ImageFrameBlockEntity(pos, state);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
