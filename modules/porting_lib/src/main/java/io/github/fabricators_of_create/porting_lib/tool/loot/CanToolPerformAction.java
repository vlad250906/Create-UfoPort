package io.github.fabricators_of_create.porting_lib.tool.loot;

import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.fabricators_of_create.porting_lib.core.PortingLib;
import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/**
 * This LootItemCondition "porting_lib:can_tool_perform_action" can be used to check if a tool can perform a given ToolAction.
 */
public class CanToolPerformAction implements LootItemCondition {
	
	public static final MapCodec<CanToolPerformAction> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			Codec.STRING.fieldOf("action").forGetter(ctpa -> ctpa.action.name())
		).apply(instance, (str -> new CanToolPerformAction(ToolAction.get(str))))
	);
	public static final LootItemConditionType LOOT_CONDITION_TYPE = new LootItemConditionType(CODEC);

	final ToolAction action;

	public CanToolPerformAction(ToolAction action) {
		this.action = action;
	}

	@NotNull
	public LootItemConditionType getType() {
		return LOOT_CONDITION_TYPE;
	}

	@NotNull
	public Set<LootContextParam<?>> getReferencedContextParams() {
		return ImmutableSet.of(LootContextParams.TOOL);
	}

	public boolean test(LootContext lootContext) {
		ItemStack itemstack = lootContext.getParamOrNull(LootContextParams.TOOL);
		return itemstack != null && itemstack.canPerformAction(this.action);
	}

	public static LootItemCondition.Builder canToolPerformAction(ToolAction action) {
		return () -> new CanToolPerformAction(action);
	}

//	public static class Serializer extends MapCodec<CanToolPerformAction> {
//		public void serialize(JsonObject json, CanToolPerformAction itemCondition, @NotNull JsonSerializationContext context) {
//			json.addProperty("action", itemCondition.action.name());
//		}
//
//		@NotNull
//		public CanToolPerformAction deserialize(JsonObject json, @NotNull JsonDeserializationContext context) {
//			return new CanToolPerformAction(ToolAction.get(json.get("action").getAsString()));
//		}
//
//		@Override
//		public <T> RecordBuilder<T> encode(CanToolPerformAction input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
//			return null;
//		}
//
//		@Override
//		public <T> DataResult<CanToolPerformAction> decode(DynamicOps<T> ops, MapLike<T> input) {
//			return null;
//		}
//
//		@Override
//		public <T> Stream<T> keys(DynamicOps<T> ops) {
//			return null;
//		}
//	}

	public static void init() {
		Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, PortingLib.id("can_tool_perform_action"), CanToolPerformAction.LOOT_CONDITION_TYPE);
	}
}

