package io.github.tropheusj.milk.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import io.github.tropheusj.milk.Milk;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

@Mixin(value = MilkBucketItem.class, priority = 921) // apply sooner, minimize conflicts
public abstract class MilkBucketItemMixin extends Item implements DispensibleContainerItem {
	public MilkBucketItemMixin(Properties settings) {
		super(settings);
	}

	/**
	 * @author Tropheus Jay
	 * @reason Add bucket functionality to milk bucket. Overwrite to fail-fast in conflicts.
	 */
	@Overwrite
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack itemStack = user.getItemInHand(hand);
		BlockHitResult blockHitResult = getPlayerPOVHitResult(world, user, ClipContext.Fluid.NONE);
		if (Milk.STILL_MILK == null || !Milk.MILK_PLACING_ENABLED || (blockHitResult.getType() == HitResult.Type.MISS || user.isShiftKeyDown())) {
			return ItemUtils.startUsingInstantly(world, user, hand);
		} else if (blockHitResult.getType() != HitResult.Type.BLOCK) {
			return InteractionResultHolder.pass(itemStack);
		}

		BlockPos hit = blockHitResult.getBlockPos();
		Direction direction = blockHitResult.getDirection();
		BlockPos offset = hit.offset(direction.getNormal());
		if (world.mayInteract(user, hit) && user.mayUseItemAt(offset, direction, itemStack)) {
			if (this.emptyContents(user, world, offset, blockHitResult)) {
				this.checkExtraContent(user, world, itemStack, offset);
				if (user instanceof ServerPlayer server) {
					CriteriaTriggers.PLACED_BLOCK.trigger(server, offset, itemStack);
				}

				user.awardStat(Stats.ITEM_USED.get(this));
				return InteractionResultHolder.sidedSuccess(finishUsingItem(itemStack, world, user), world.isClientSide());
			} else {
				return InteractionResultHolder.fail(itemStack);
			}

		}
		return InteractionResultHolder.fail(itemStack);
	}
	
	

	@Override
	public boolean emptyContents(@Nullable Player player, Level world, BlockPos pos, @Nullable BlockHitResult hitResult) {
		if (Milk.STILL_MILK == null || !Milk.MILK_PLACING_ENABLED)
			return false;
		BlockState blockState = world.getBlockState(pos);
		Block block = blockState.getBlock();
		boolean canPlace = blockState.canBeReplaced(Milk.STILL_MILK);
		
		boolean bl2 = blockState.isAir() ||
				canPlace || 
				block instanceof LiquidBlockContainer && ((LiquidBlockContainer)block).canPlaceLiquid(player, world, pos, blockState, Milk.STILL_MILK);
		if (!bl2) {
			return hitResult != null && this.emptyContents(player, world, hitResult.getBlockPos().offset(hitResult.getDirection().getNormal()), null);
		} else if (world.dimensionType().ultraWarm()) {
			int i = pos.getX();
			int j = pos.getY();
			int k = pos.getZ();
			world.playSound(
					player,
					pos,
					SoundEvents.FIRE_EXTINGUISH,
					SoundSource.BLOCKS,
					0.5F,
					2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F
			);

			for(int l = 0; l < 8; ++l) {
				world.addParticle(
						ParticleTypes.LARGE_SMOKE, (double)i + Math.random(), (double)j + Math.random(), (double)k + Math.random(), 0.0, 0.0, 0.0
				);
			}

			return true;
		} else if (block instanceof LiquidBlockContainer fillable) {
			fillable.placeLiquid(world, pos, blockState, Milk.STILL_MILK.getSource(false));
			this.playEmptyingSound(player, world, pos);
			return true;
		} else {
			if (!world.isClientSide() && canPlace && !blockState.liquid()) {
				world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
			if (!world.setBlock(pos, Milk.STILL_MILK.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL | Block.UPDATE_IMMEDIATE)
					&& !blockState.getFluidState().isSource()) {
				return false;
			} else {
				this.playEmptyingSound(player, world, pos);
				return true;
			}
		}
	}

	protected void playEmptyingSound(@Nullable Player player, LevelAccessor world, BlockPos pos) {
		world.playSound(player, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
		world.gameEvent(player, GameEvent.FLUID_PLACE, pos);
	}
}
