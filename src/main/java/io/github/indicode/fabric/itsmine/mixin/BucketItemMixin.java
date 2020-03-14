package io.github.indicode.fabric.itsmine.mixin;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.BucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Indigo Amann
 */
@Mixin(BucketItem.class)
public interface BucketItemMixin {
    @Accessor("fluid")
    Fluid getFluid();
}
