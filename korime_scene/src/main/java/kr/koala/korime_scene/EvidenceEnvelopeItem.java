package kr.koala.korime_scene;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public final class EvidenceEnvelopeItem extends Item {
    public EvidenceEnvelopeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        ItemStack stack = context.getStack();
        if (EvidenceEnvelopeData.isCaptured(stack)) return ActionResult.PASS;
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;

        boolean captured = EvidenceEnvelopeData.captureBlock(stack, context.getWorld(), context.getBlockPos(), context.getPlayer());
        if (captured && context.getPlayer() != null) {
            context.getPlayer().sendMessage(Text.literal("증거를 봉투에 채취했습니다."), true);
            return ActionResult.SUCCESS;
        }
        if (context.getPlayer() != null) {
            context.getPlayer().sendMessage(Text.literal("이 대상은 증거 봉투에 저장하기에는 데이터가 너무 큽니다."), true);
        }
        return ActionResult.FAIL;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, net.minecraft.util.Hand hand) {
        if (EvidenceEnvelopeData.isCaptured(stack)) return ActionResult.PASS;
        if (user.getWorld().isClient()) return ActionResult.SUCCESS;
        if (EvidenceEnvelopeData.captureEntity(stack, user.getWorld(), entity, user)) {
            user.sendMessage(Text.literal("증거를 봉투에 채취했습니다."), true);
            return ActionResult.SUCCESS;
        }
        user.sendMessage(Text.literal("이 대상은 증거 봉투에 저장하기에는 데이터가 너무 큽니다."), true);
        return ActionResult.FAIL;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        if (!EvidenceEnvelopeData.isCaptured(stack)) {
            tooltip.add(Text.literal("설치된 블록이나 엔티티에 우클릭해 증거를 채취").formatted(Formatting.GRAY));
            return;
        }
        tooltip.add(Text.literal("채취됨: " + EvidenceEnvelopeData.getTargetId(stack)).formatted(Formatting.GOLD));
        tooltip.add(Text.literal("우클릭하여 기록 확인 / 메모 작성").formatted(Formatting.GRAY));
    }
}
