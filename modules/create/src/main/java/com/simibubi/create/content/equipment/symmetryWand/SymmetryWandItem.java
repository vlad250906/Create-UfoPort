package com.simibubi.create.content.equipment.symmetryWand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.equipment.symmetryWand.mirror.CrossPlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.EmptyMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.PlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class SymmetryWandItem extends Item {

	public static final String SYMMETRY = "symmetry";
	public static final String ENABLE = "enable";

	public SymmetryWandItem(Properties properties) {
		super(properties);
	}

	@Nonnull
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		BlockPos pos = context.getClickedPos();
		if (player == null)
			return InteractionResult.PASS;
		player.getCooldowns()
			.addCooldown(this, 5);
		ItemStack wand = player.getItemInHand(context.getHand());
		checkNBT(wand);

		// Shift -> open GUI
		if (player.isShiftKeyDown()) {
			if (player.level().isClientSide) {
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
					openWandGUI(wand, context.getHand());
				});
				player.getCooldowns()
					.addCooldown(this, 5);
			}
			return InteractionResult.SUCCESS;
		}

		if (context.getLevel().isClientSide || context.getHand() != InteractionHand.MAIN_HAND)
			return InteractionResult.SUCCESS;

		CompoundTag compound = wand.getOrDefault(AllDataComponents.SYM_WAND, new CompoundTag()).getCompound(SYMMETRY);
		pos = pos.relative(context.getClickedFace());
		SymmetryMirror previousElement = SymmetryMirror.fromNBT(compound);

		// No Shift -> Make / Move Mirror
		ItemHelper.getOrCreateComponent(wand, AllDataComponents.SYM_WAND, new CompoundTag())
			.putBoolean(ENABLE, true);
		Vec3 pos3d = new Vec3(pos.getX(), pos.getY(), pos.getZ());
		SymmetryMirror newElement = new PlaneMirror(pos3d);

		if (previousElement instanceof EmptyMirror) {
			newElement.setOrientation(
				(player.getDirection() == Direction.NORTH || player.getDirection() == Direction.SOUTH)
					? PlaneMirror.Align.XY.ordinal()
					: PlaneMirror.Align.YZ.ordinal());
			newElement.enable = true;
			ItemHelper.getOrCreateComponent(wand, AllDataComponents.SYM_WAND, new CompoundTag())
				.putBoolean(ENABLE, true);

		} else {
			previousElement.setPosition(pos3d);

			if (previousElement instanceof PlaneMirror) {
				previousElement.setOrientation(
					(player.getDirection() == Direction.NORTH || player.getDirection() == Direction.SOUTH)
						? PlaneMirror.Align.XY.ordinal()
						: PlaneMirror.Align.YZ.ordinal());
			}

			if (previousElement instanceof CrossPlaneMirror) {
				float rotation = player.getYHeadRot();
				float abs = Math.abs(rotation % 90);
				boolean diagonal = abs > 22 && abs < 45 + 22;
				previousElement
					.setOrientation(diagonal ? CrossPlaneMirror.Align.D.ordinal() : CrossPlaneMirror.Align.Y.ordinal());
			}

			newElement = previousElement;
		}

		compound = newElement.writeToNbt();
		ItemHelper.getOrCreateComponent(wand, AllDataComponents.SYM_WAND, new CompoundTag())
			.put(SYMMETRY, compound);
		
		player.setItemInHand(context.getHand(), wand);
		AllPackets.getChannel().sendToClient(
				new UpdateSymmetryWandPacket(InteractionHand.MAIN_HAND, newElement), (ServerPlayer)player);
		
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
		ItemStack wand = playerIn.getItemInHand(handIn);
		checkNBT(wand);

		// Shift -> Open GUI
		if (playerIn.isShiftKeyDown()) {
			if (worldIn.isClientSide) {
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
					openWandGUI(playerIn.getItemInHand(handIn), handIn);
				});
				playerIn.getCooldowns()
					.addCooldown(this, 5);
			}
			return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, wand);
		}

		// No Shift -> Clear Mirror
		ItemHelper.getOrCreateComponent(wand, AllDataComponents.SYM_WAND, new CompoundTag())
			.putBoolean(ENABLE, false);
		return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, wand);
	}

	@Environment(EnvType.CLIENT)
	private void openWandGUI(ItemStack wand, InteractionHand hand) {
		ScreenOpener.open(new SymmetryWandScreen(wand, hand));
	}

	private static void checkNBT(ItemStack wand) {
		if (!wand.has(AllDataComponents.SYM_WAND) || !wand.get(AllDataComponents.SYM_WAND)
			.contains(SYMMETRY)) {
			wand.set(AllDataComponents.SYM_WAND, new CompoundTag());
			wand.get(AllDataComponents.SYM_WAND)
				.put(SYMMETRY, new EmptyMirror(new Vec3(0, 0, 0)).writeToNbt());
			wand.get(AllDataComponents.SYM_WAND)
				.putBoolean(ENABLE, false);
		}
	}

	public static boolean isEnabled(ItemStack stack) {
		checkNBT(stack);
		CompoundTag tag = stack.getOrDefault(AllDataComponents.SYM_WAND, new CompoundTag());
		return tag.getBoolean(ENABLE) && !tag.getBoolean("Simulate");
	}

	public static SymmetryMirror getMirror(ItemStack stack) {
		checkNBT(stack);
		return SymmetryMirror.fromNBT(stack.getOrDefault(AllDataComponents.SYM_WAND, new CompoundTag())
			.getCompound(SYMMETRY));
	}

	public static void configureSettings(ItemStack stack, SymmetryMirror mirror) {
		checkNBT(stack);
		ItemHelper.getOrCreateComponent(stack, AllDataComponents.SYM_WAND, new CompoundTag())
			.put(SYMMETRY, mirror.writeToNbt());
	}

	public static void apply(Level world, ItemStack wand, Player player, BlockPos pos, BlockState block) {
		checkNBT(wand);
		if (!isEnabled(wand))
			return;
		if (!BlockItem.BY_BLOCK.containsKey(block.getBlock()))
			return;

		Map<BlockPos, BlockState> blockSet = new HashMap<>();
		blockSet.put(pos, block);
		SymmetryMirror symmetry = SymmetryMirror.fromNBT((CompoundTag) ItemHelper.getOrCreateComponent(wand, AllDataComponents.SYM_WAND, new CompoundTag())
			.getCompound(SYMMETRY));

		Vec3 mirrorPos = symmetry.getPosition();
		if (mirrorPos.distanceTo(Vec3.atLowerCornerOf(pos)) > AllConfigs.server().equipment.maxSymmetryWandRange.get())
			return;
		if (!player.isCreative() && isHoldingBlock(player, block)
			&& BlockHelper.simulateFindAndRemoveInInventory(block, player, 1) == 0) // fabric: simulate since the first block will already be removed
			return;

		symmetry.process(blockSet);
		BlockPos to = BlockPos.containing(mirrorPos);
		List<BlockPos> targets = new ArrayList<>();
		targets.add(pos);

		for (BlockPos position : blockSet.keySet()) {
			if (position.equals(pos))
				continue;

			if (world.isUnobstructed(block, position, CollisionContext.of(player))) {
				BlockState blockState = blockSet.get(position);
				for (Direction face : Iterate.directions)
					blockState = blockState.updateShape(face, world.getBlockState(position.relative(face)), world,
						position, position.relative(face));

				if (player.isCreative()) {
					world.setBlockAndUpdate(position, blockState);
					targets.add(position);
					continue;
				}

				BlockState toReplace = world.getBlockState(position);
				if (!toReplace.canBeReplaced())
					continue;
				if (toReplace.getDestroySpeed(world, position) == -1)
					continue;

				if (AllBlocks.CART_ASSEMBLER.has(blockState)) {
					BlockState railBlock = CartAssemblerBlock.getRailBlock(blockState);
					if (BlockHelper.findAndRemoveInInventory(railBlock, player, 1) == 0)
						continue;
					if (BlockHelper.findAndRemoveInInventory(blockState, player, 1) == 0)
						blockState = railBlock;
				} else {
					if (BlockHelper.findAndRemoveInInventory(blockState, player, 1) == 0)
						continue;
				}

//				BlockSnapshot blocksnapshot = BlockSnapshot.create(world.dimension(), world, position);
				BlockState cachedState = world.getBlockState(position);
				FluidState ifluidstate = world.getFluidState(position);
				world.setBlock(position, ifluidstate.createLegacyBlock(), Block.UPDATE_KNOWN_SHAPE);
				world.setBlockAndUpdate(position, blockState);

				CompoundTag wandNbt = ItemHelper.getOrCreateComponent(wand, AllDataComponents.SYM_WAND, new CompoundTag());
				wandNbt.putBoolean("Simulate", true);
				boolean placeInterrupted = !world.isUnobstructed(cachedState, position, CollisionContext.empty());//ForgeEventFactory.onBlockPlace(player, blocksnapshot, Direction.UP);
				wandNbt.putBoolean("Simulate", false);

				if (placeInterrupted) {
//					blocksnapshot.restore(true, false);
					world.setBlockAndUpdate(position, cachedState);
					continue;
				}
				targets.add(position);
			}
		}

		AllPackets.getChannel().sendToClientsTrackingAndSelf(new SymmetryEffectPacket(to, targets), player);
	}

	private static boolean isHoldingBlock(Player player, BlockState block) {
		ItemStack itemBlock = BlockHelper.getRequiredItem(block);
		return player.isHolding(itemBlock.getItem());
	}

	public static void remove(Level world, ItemStack wand, Player player, BlockPos pos, BlockState ogBlock) {
		BlockState air = Blocks.AIR.defaultBlockState();
		checkNBT(wand);
		if (!isEnabled(wand))
			return;

		Map<BlockPos, BlockState> blockSet = new HashMap<>();
		blockSet.put(pos, air);
		SymmetryMirror symmetry = SymmetryMirror.fromNBT((CompoundTag) ItemHelper.getOrCreateComponent(wand, AllDataComponents.SYM_WAND, new CompoundTag())
			.getCompound(SYMMETRY));

		Vec3 mirrorPos = symmetry.getPosition();
		if (mirrorPos.distanceTo(Vec3.atLowerCornerOf(pos)) > AllConfigs.server().equipment.maxSymmetryWandRange.get())
			return;

		symmetry.process(blockSet);

		BlockPos to = BlockPos.containing(mirrorPos);
		List<BlockPos> targets = new ArrayList<>();

		targets.add(pos);
		for (BlockPos position : blockSet.keySet()) {
			if (!player.isCreative() && ogBlock.getBlock() != world.getBlockState(position)
					.getBlock())
				continue;
			if (position.equals(pos))
				continue;

			BlockState blockstate = world.getBlockState(position);
			BlockEntity be = blockstate.hasBlockEntity() ? world.getBlockEntity(position) : null;
			if (!blockstate.isAir()) {
				if (handlePreEvent(world, player, position, blockstate, be))
					continue;
				targets.add(position);
				world.levelEvent(2001, position, Block.getId(blockstate));
				world.setBlock(position, air, 3);

				if (!player.isCreative()) {
					if (!player.getMainHandItem()
						.isEmpty())
						player.getMainHandItem()
							.mineBlock(world, blockstate, position, player);
					BlockEntity blockEntity = blockstate.hasBlockEntity() ? world.getBlockEntity(position) : null;
					Block.dropResources(blockstate, world, pos, blockEntity, player, player.getMainHandItem()); // Add fortune, silk touch and other loot modifiers
				}
				handlePostEvent(world, player, position, blockstate, be);
			}
		}

		AllPackets.getChannel().sendToClientsTrackingAndSelf(new SymmetryEffectPacket(to, targets), player);
	}

	/**
	 * Handling firing events before the wand changes blocks.
	 * @return true if canceled
	 */
	public static boolean handlePreEvent(Level world, Player player, BlockPos pos, BlockState state, BlockEntity be) {
		if (PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, state, be)) {
			return false;
		}
		PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(world, player, pos, state, be);
		return true;
	}

	public static void handlePostEvent(Level world, Player player, BlockPos pos, BlockState state, BlockEntity be) {
		PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(world, player, pos, state, be);
	}

//	@Override
//	@Environment(EnvType.CLIENT)
//	public void initializeClient(Consumer<IItemRenderProperties> consumer) {
//		consumer.accept(SimpleCustomRenderer.create(this, new SymmetryWandItemRenderer()));
//	}

}
