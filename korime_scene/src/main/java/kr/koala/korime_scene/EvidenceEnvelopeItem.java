package kr.koala.korime_scene;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
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
        ItemStack stack = context.getStack();
        if (EvidenceEnvelopeData.isFull(stack)) {
            if (!context.getWorld().isClient() && context.getPlayer() != null) {
                context.getPlayer().sendMessage(Text.literal("증거 봉투가 가득 찼습니다. (30/30)"), true);
            }
            return ActionResult.FAIL;
        }
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;

        if (EvidenceEnvelopeData.captureBlock(stack, context.getWorld(), context.getBlockPos())) {
            if (context.getPlayer() != null) {
                context.getPlayer().sendMessage(Text.literal("증거물을 봉투에 넣었습니다. (" + EvidenceEnvelopeData.getItemCount(stack) + "/30)"), true);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, net.minecraft.util.Hand hand) {
        if (EvidenceEnvelopeData.isFull(stack)) return ActionResult.FAIL;
        if (user.getWorld().isClient()) return ActionResult.SUCCESS;
        if (EvidenceEnvelopeData.captureEntity(stack, entity)) {
            user.sendMessage(Text.literal("증거물을 봉투에 넣었습니다. (" + EvidenceEnvelopeData.getItemCount(stack) + "/30)"), true);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
