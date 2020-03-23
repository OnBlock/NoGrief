package io.github.indicode.fabric.itsmine.mixin.projectile;

import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(ProjectileEntity.class)
public interface OwnedProjectile {
	@Accessor("ownerUuid")
	UUID getOwner();
}
