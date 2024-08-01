
package io.github.fabricators_of_create.porting_lib.util;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.mojang.serialization.MapCodec;

import io.github.fabricators_of_create.porting_lib.core.PortingLib;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.impl.resource.conditions.conditions.TrueResourceCondition;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.resources.ResourceLocation;

public class TrueCondition implements ResourceCondition {
	public static final ResourceLocation ID = PortingLib.id("true");
	public static final TrueCondition INSTANCE = new TrueCondition();
	public static final MapCodec<TrueCondition> CODEC = MapCodec.unit(TrueCondition::new);
	public static final ResourceConditionType<TrueCondition> TYPE = ResourceConditionType.create(ID, TrueCondition.CODEC);
	
	public static void init() {
	}

	@Override
	public ResourceConditionType<?> getType() {
		return TYPE;
	}

	@Override
	public boolean test(@Nullable Provider registryLookup) {
		return true;
	}
}
