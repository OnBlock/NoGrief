package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class StickClaimItemMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void setStickPositionFirst(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient) return;
        if (!context.getPlayer().isSneaking()) {
            if ((Object)this == Items.STICK) {
                Pair<BlockPos, BlockPos> posPair = ClaimManager.INSTANCE.stickPositions.get(context.getPlayer());
                if (posPair == null) return;
                else posPair = new Pair<>(context.getBlockPos(), posPair.getRight());
                ClaimManager.INSTANCE.stickPositions.put(context.getPlayer(), posPair);
                context.getPlayer().sendMessage(new LiteralText("Position #1 set: " + context.getBlockPos().getX() + " " + context.getBlockPos().getY() + " " + context.getBlockPos().getZ()).formatted(Formatting.GREEN));
                if (posPair.getRight() != null) {
                    context.getPlayer().sendMessage(new LiteralText("Area Selected. Type /claim create <name> to create your claim!").formatted(Formatting.GOLD));
                    context.getPlayer().sendMessage(new LiteralText("Remember that claims are three dimensional. Don't forget to expand up/down or select a big enough area...").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
                }
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }
    @Inject(method = "canMine", at = @At("HEAD"), cancellable = true)
    private void setStickPositionSecond(BlockState blockState_1, World world, BlockPos pos, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient) return;
        if (!player.isSneaking()) {
            if ((Object)this == Items.STICK) {
                Pair<BlockPos, BlockPos> posPair = ClaimManager.INSTANCE.stickPositions.get(player);
                if (posPair == null) return;
                else posPair = new Pair<>(posPair.getLeft(), pos);
                ClaimManager.INSTANCE.stickPositions.put(player, posPair);
                player.sendMessage(new LiteralText("Position #2 set: " + pos.getX() + " " + pos.getY() + " " + pos.getZ()).formatted(Formatting.GREEN));
                if (posPair.getLeft() != null) {
                    player.sendMessage(new LiteralText("Area Selected. Type /claim create <name> to create your claim!").formatted(Formatting.GOLD));
                    player.sendMessage(new LiteralText("Remember that claims are three dimensional. Don't forget to expand up/down or select a big enough area...").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
                }
                cir.setReturnValue(false);
            }
        }
    }
}
