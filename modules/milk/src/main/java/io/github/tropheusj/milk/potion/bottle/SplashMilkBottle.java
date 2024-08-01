package io.github.tropheusj.milk.potion.bottle;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class SplashMilkBottle extends SplashPotionItem {
	public SplashMilkBottle(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		world.playSound(null, user.getX(), user.getY(), user.getZ(),
				SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
				0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));

		ItemStack itemStack = user.getItemInHand(hand);
		if (!world.isClientSide()) {
			ThrownPotion potionEntity = new ThrownPotion(world, user);
			potionEntity.setItem(itemStack);
			potionEntity.shootFromRotation(user, user.getXRot(), user.getYRot(), -20.0F, 0.5F, 1.0F);
			((PotionItemEntityExtensions) potionEntity).setMilk(true);
			world.addFreshEntity(potionEntity);
		}

		user.awardStat(Stats.ITEM_USED.get(this));
		if (!user.isCreative()) {
			itemStack.shrink(1);
		}

		return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
	}
	
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
			TooltipFlag tooltipFlag) {
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return getDescriptionId();
	}
	
	@Override
	public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
		return Util.make(new ThrownPotion(level, pos.x(), pos.y(), pos.z()), entity -> {
			entity.setItem(stack);
			((PotionItemEntityExtensions) entity).setMilk(true);
		});
	}
}
