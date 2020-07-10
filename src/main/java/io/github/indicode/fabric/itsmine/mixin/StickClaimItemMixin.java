package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ItsMineConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class StickClaimItemMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
//    private void setStickPositionFirst(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
    private void setStickPositionFirst(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity player = context.getPlayer();
        if (context.getWorld().isClient) return;
        if (!player.isSneaking()) {
            if ((Object)this == Items.STICK) {
                Pair<BlockPos, BlockPos> posPair = ClaimManager.INSTANCE.stickPositions.get(context.getPlayer());
                if (posPair == null) return;
                else posPair = new Pair<>(context.getBlockPos(), posPair.getRight());
                ClaimManager.INSTANCE.stickPositions.put(context.getPlayer(), posPair);
                context.getPlayer().sendSystemMessage(new LiteralText("Position #1 set: " + context.getBlockPos().getX() + (ItsMineConfig.main().claims2d ? "" : " " + context.getBlockPos().getY()) + " " + context.getBlockPos().getZ()).formatted(Formatting.GREEN), player.getUuid());
                if (posPair.getRight() != null) {
                    player.sendSystemMessage(new LiteralText("Area Selected. Type /claim create <name> to create your claim!").formatted(Formatting.GOLD), player.getUuid());
                    if (!ItsMineConfig.main().claims2d) player.sendSystemMessage(new LiteralText("Remember that claims are three dimensional. Don't forget to expand up/down or select a big enough area...").formatted(Formatting.LIGHT_PURPLE).formatted(Formatting.ITALIC), player.getUuid());
                }
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }
}
