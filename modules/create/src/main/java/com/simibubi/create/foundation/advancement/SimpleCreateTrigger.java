package com.simibubi.create.foundation.advancement;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SimpleCreateTrigger extends CriterionTriggerBase<SimpleCreateTrigger.Instance> {

	public SimpleCreateTrigger(String id) {
		super(id);
	}

	//@Override
	//public Instance createInstance(JsonObject json, DeserializationContext context) {
		//return new Instance(getId());
	//}

	public void trigger(ServerPlayer player) {
		super.trigger(player, null);
	}

	public Instance instance() {
		return new Instance(getId());
	}

	public static class Instance extends CriterionTriggerBase.Instance {

		public Instance(ResourceLocation idIn) {
			super(idIn, ContextAwarePredicate.create(new LootItemCondition[0]));
		}

		@Override
		protected boolean test(@Nullable List<Supplier<Object>> suppliers) {
			return true;
		}
	}

	@Override
	public Codec<Instance> codec() {
		Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				  Codec.STRING.optionalFieldOf("ddddd").forGetter((exa) -> {
					  return Optional.ofNullable("");
				  })
				).apply(instance, (stri) -> {
					return new SimpleCreateTrigger.Instance(getId());
				}));
		return CODEC;
	}
}
