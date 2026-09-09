package kr.koala.korime_scene;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class SafeBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    public static final int SLOT_COUNT = 5;

    private final SimpleInventory inventory = new SimpleInventory(SLOT_COUNT) {
        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return Inventory.canPlayerUse(SafeBlockEntity.this, player);
        }
    };

    public SafeBlockEntity(BlockPos pos, BlockState state) {
        super(SceneToolsMod.SAFE_BLOCK_ENTITY, pos, state);
        inventory.addListener(sender -> markDirty());
    }

    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.korime_scene.safe");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new HopperScreenHandler(syncId, playerInventory, inventory);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList items = new NbtList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) continue;
            NbtCompound item = new NbtCompound();
            item.putByte("Slot", (byte) slot);
            stack.writeNbt(item);
            items.add(item);
        }
        nbt.put("Items", items);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        inventory.clear();
        NbtList items = nbt.getList("Items", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < items.size(); i++) {
            NbtCompound item = items.getCompound(i);
            int slot = item.getByte("Slot") & 255;
            if (slot >= 0 && slot < SLOT_COUNT) {
                inventory.setStack(slot, ItemStack.fromNbt(item));
            }
        }
    }
}
