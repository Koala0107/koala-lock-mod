package kr.koala.korime_scene;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class EvidenceMagnifierBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<EvidenceMagnifierBlock> CODEC = createCodec(EvidenceMagnifierBlock::new);

    // The rendered magnifier is a vertical card centered in the block. Keep the
    // interaction outline on that card instead of the old floor-level shape.
    private static final VoxelShape NORTH_SOUTH_SHAPE = Block.createCuboidShape(4, 4, 7.25, 12, 12, 8.75);
    private static final VoxelShape EAST_WEST_SHAPE = Block.createCuboidShape(7.25, 4, 4, 8.75, 12, 12);

    public EvidenceMagnifierBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends HorizontalFacingBlock> getCodec() { return CODEC; }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = state.get(FACING);
        return facing == Direction.EAST || facing == Direction.WEST ? EAST_WEST_SHAPE : NORTH_SOUTH_SHAPE;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, net.minecraft.entity.player.PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof EvidenceMagnifierBlockEntity evidence && evidence.isSaved()) {
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 0.75F, 0.85F);
            if (world instanceof ServerWorld serverWorld) serverWorld.scheduleBlockTick(pos, this, 2);
            player.sendMessage(EvidenceTextUtil.parse(evidence.getEvidenceText()), false);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.getBlockEntity(pos) instanceof EvidenceMagnifierBlockEntity evidence && evidence.isSaved()) {
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 0.85F, 1.25F);
        }
    }

    @Override public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new EvidenceMagnifierBlockEntity(pos, state); }
    @Override public BlockState rotate(BlockState state, BlockRotation rotation) { return state.with(FACING, rotation.rotate(state.get(FACING))); }
    @Override public BlockState mirror(BlockState state, BlockMirror mirror) { return state.rotate(mirror.getRotation(state.get(FACING))); }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(FACING); }
}
