package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ClaimPlayerEntity;
import io.github.indicode.fabric.itsmine.Functions;
import io.github.indicode.fabric.itsmine.MonitorableWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * @author Indigo Amann
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements MonitorableWorld {
    @Shadow
    @Final
    private Map<UUID, Entity> entitiesByUuid;

    protected ServerWorldMixin(MutableWorldProperties mutableWorldProperties, DimensionType dimensionType, Supplier<Profiler> supplier, boolean bl, boolean bl2, long l) {
        super(mutableWorldProperties, dimensionType, supplier, bl, bl2, l);
    }


    @Shadow public abstract List<ServerPlayerEntity> getPlayers();

    @Redirect(method = "processSyncedBlockEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendToAround(Lnet/minecraft/entity/player/PlayerEntity;DDDDLnet/minecraft/util/registry/RegistryKey;Lnet/minecraft/network/Packet;)V"))
    private void sendPistonUpdate(PlayerManager manager, PlayerEntity playerEntity_1, double double_1, double double_2, double double_3, double double_4, RegistryKey<DimensionType> dimensionType_1, Packet<?> packet_1) {
        manager.sendToAround(playerEntity_1, double_1, double_2, double_3, double_4, this.method_27983(), packet_1);
        Functions.doPistonUpdate((ServerWorld) (Object)this, packet_1);
    }



    @Inject(method = "tick", at = @At(value = "RETURN"))
    public void tickActions(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        ClaimManager.INSTANCE.claimsByName.forEach((s, claim) ->{
            if(claim.rent.getRentedUntil() != 0){
                if(claim.rent.getRentTimeLeft() < 0){
                    System.out.println("Rent expired -> method call");
                    this.getPlayers().forEach(serverPlayerEntity -> {
                        if(serverPlayerEntity.getUuid() == claim.rent.getTenant()){
                            serverPlayerEntity.sendSystemMessage(new LiteralText("Rent expired").formatted(Formatting.RED), serverPlayerEntity.getUuid());
                            claim.endRent();
                        }
                    });
                }
            }
        });
        this.getPlayers().forEach(playerEntity -> {
            ((ClaimPlayerEntity) playerEntity).tickMessageCooldown();
        });

    }


    @Override
    public int loadedEntities() {
        if (this.entitiesByUuid == null)
            return -1;
        return this.entitiesByUuid.size();
    }

    @Override
    public Map<UUID, Entity> EntityList() {
        return entitiesByUuid;
    }
}
