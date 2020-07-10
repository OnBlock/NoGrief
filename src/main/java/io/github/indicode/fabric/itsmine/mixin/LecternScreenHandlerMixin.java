//package io.github.indicode.fabric.itsmine.mixin;
//
//import io.github.indicode.fabric.itsmine.claim.Claim;
//import io.github.indicode.fabric.itsmine.ClaimManager;
//import io.github.indicode.fabric.itsmine.util.ClaimUtil;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.screen.LecternScreenHandler;
//import net.minecraft.text.LiteralText;
//import net.minecraft.util.Formatting;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(LecternScreenHandler.class)
//public abstract class LecternScreenHandlerMixin {
//
//
//    @Inject(method = "onButtonClick", at = @At(value = "HEAD"))
//    private void canTake(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir){
//        player.sendSystemMessage(new LiteralText("onButtonClick"));
//        cir.setReturnValue(true);
//        cir.setReturnValue(false);
//        Claim claim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.dimension);
//        if(claim != null){
//            if(claim.isChild) claim = ClaimUtil.getParentClaim(claim);
//            if(!claim.hasPermission(player.getUuid(), Claim.Permission.INTERACT_LECTERN)){
//                player.sendSystemMessage(new LiteralText("return true").formatted(Formatting.GREEN));
//                cir.setReturnValue(true);
//            } else {
//                player.sendSystemMessage(new LiteralText("return false").formatted(Formatting.GREEN));
//                cir.setReturnValue(false);
//            }
//        } else {
//            player.sendSystemMessage(new LiteralText("return true").formatted(Formatting.GREEN));
//            cir.setReturnValue(true);
//        }
//    }
//}