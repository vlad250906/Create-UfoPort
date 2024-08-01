package io.github.fabricators_of_create.porting_lib.util;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * Helper class to define a custom tier
 */
@SuppressWarnings("ClassCanBeRecord") // can't make it a record because the method names will be obfuscated
public final class LazyTier implements Tier {
	private final TagKey<Block> incorrectBlockForDrops;
	private final int uses;
	private final float speed;
	private final float attackDamageBonus;
	private final int enchantmentValue;
	@Nonnull
	private final TagKey<Block> tag;
	@Nonnull
	private final Supplier<Ingredient> repairIngredient;

	public LazyTier(TagKey<Block> incorrectBlockForDrops, int uses, float speed, float attackDamageBonus, int enchantmentValue,
					@Nonnull TagKey<Block> tag, @Nonnull Supplier<Ingredient> repairIngredient) {
		this.incorrectBlockForDrops = incorrectBlockForDrops;
		this.uses = uses;
		this.speed = speed;
		this.attackDamageBonus = attackDamageBonus;
		this.enchantmentValue = enchantmentValue;
		this.tag = tag;
		this.repairIngredient = repairIngredient;
	}

	@Override
	public int getUses() {
		return this.uses;
	}

	@Override
	public float getSpeed() {
		return this.speed;
	}

	@Override
	public float getAttackDamageBonus() {
		return this.attackDamageBonus;
	}

	@Override
	public int getEnchantmentValue() {
		return this.enchantmentValue;
	}

	@Nonnull
	public TagKey<Block> getTag() {
		return this.tag;
	}

	@Nonnull
	@Override
	public Ingredient getRepairIngredient() {
		return this.repairIngredient.get();
	}
	
	@Override
	public TagKey<Block> getIncorrectBlocksForDrops() {
		return incorrectBlockForDrops;
	}

	@Override
	public String toString() {
		return "DefaultTier[" +
				"uses=" + uses + ", " +
				"speed=" + speed + ", " +
				"attackDamageBonus=" + attackDamageBonus + ", " +
				"enchantmentValue=" + enchantmentValue + ", " +
				"tag=" + tag + ", " +
				"repairIngredient=" + repairIngredient + ']';
	}
	
}
