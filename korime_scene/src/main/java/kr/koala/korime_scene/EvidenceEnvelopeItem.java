package kr.koala.korime_scene;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public final class EvidenceEnvelopeItem extends Item {
    public EvidenceEnvelopeItem(Settings settings) {
        super(settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("증거 봉투");
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.isSneaking()) return ActionResult.PASS;

        ItemStack envelope = context.getStack();
        if (EvidenceEnvelopeData.isFull(envelope)) {
            if (!context.getWorld().isClient()) {
                player.sendMessage(Text.literal("증거 봉투가 가득 찼습니다. (30/30)"), true);
            }
            return ActionResult.FAIL;
        }

        BlockEntity blockEntity = context.getWorld().getBlockEntity(context.getBlockPos());
        if (!(blockEntity instanceof Inventory inventory)) {
            // Normal blocks and non-inventory devices cannot be copied into the bag.
            return ActionResult.PASS;
        }

        if (context.getWorld().isClient()) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(context.getHand());
        buf.writeBlockPos(context.getBlockPos());
        buf.writeVarInt(inventory.size());
        for (int i = 0; i < inventory.size(); i++) {
            buf.writeItemStack(inventory.getStack(i).copy());
        }
        ServerPlayNetworking.send(serverPlayer, EvidenceCollectionMod.OPEN_CONTAINER_EVIDENCE_PACKET, buf);
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, net.minecraft.util.Hand hand) {
        // Evidence envelopes no longer copy entities or placed objects directly.
        return ActionResult.PASS;
    }
}
