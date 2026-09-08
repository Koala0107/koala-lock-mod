package kr.koala.crouchlock;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class EvidenceMagnifierBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<EvidenceMagnifierBlock> CODEC = createCodec(EvidenceMagnifierBlock::new);
    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 2, 14);

    public EvidenceMagnifierBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends HorizontalFacingBlock> getCodec() { return CODEC; }
    @Override public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }
    @Override public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) { return SHAPE; }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, net.minecraft.entity.player.PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof EvidenceMagnifierBlockEntity evidence && evidence.isSaved()) {
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL, SoundCategory.BLOCKS, 0.75F, 0.85F);
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.BLOCKS, 0.85F, 1.25F);
            player.sendMessage(EvidenceTextUtil.parse(evidence.getEvidenceText()), false);
        }
        return ActionResult.SUCCESS;
    }

    @Override public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new EvidenceMagnifierBlockEntity(pos, state); }
    @Override public BlockState rotate(BlockState state, BlockRotation rotation) { return state.with(FACING, rotation.rotate(state.get(FACING))); }
    @Override public BlockState mirror(BlockState state, BlockMirror mirror) { return state.rotate(mirror.getRotation(state.get(FACING))); }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(FACING); }
}
