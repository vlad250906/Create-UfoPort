package io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;

@Mixin(PotionBrewing.Mix.class)
public interface PotionBrewing$MixAccessor<T> {
	@Accessor("from")
	Holder<T> port_lib$from();

	@Accessor("ingredient")
	Ingredient port_lib$ingredient();

	@Accessor("to")
	Holder<T> port_lib$to();
}
