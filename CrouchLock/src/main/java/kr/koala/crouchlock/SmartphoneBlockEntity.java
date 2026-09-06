package kr.koala.crouchlock;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class SmartphoneBlockEntity extends BlockEntity {
    private static final String ITEM_NBT_KEY = "PhoneItemNbt";
    private SmartphoneData data = SmartphoneData.empty();

    public SmartphoneBlockEntity(BlockPos pos, BlockState state) {
        super(SmartphoneMod.SMARTPHONE_BLOCK_ENTITY, pos, state);
    }

    public SmartphoneData getData() {
        return data;
    }

    public void setData(SmartphoneData data) {
        this.data = new SmartphoneData(data.calls(), data.messages(), data.finalized(),
                data.title(), data.subtitle());
        markDirty();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getChunkManager().markForUpdate(pos);
        }
    }

    public ItemStack createPhoneStack() {
        ItemStack stack = new ItemStack(SmartphoneMod.SMARTPHONE);
        data.writeTo(stack);
        return stack;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        ItemStack temp = new ItemStack(SmartphoneMod.SMARTPHONE);
        data.writeTo(temp);
        if (temp.getNbt() != null) {
            nbt.put(ITEM_NBT_KEY, temp.getNbt().copy());
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains(ITEM_NBT_KEY, NbtElement.COMPOUND_TYPE)) {
            ItemStack temp = new ItemStack(SmartphoneMod.SMARTPHONE);
            temp.setNbt(nbt.getCompound(ITEM_NBT_KEY).copy());
            data = SmartphoneData.fromStack(temp);
        } else {
            data = SmartphoneData.empty();
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
