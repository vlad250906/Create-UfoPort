package com.simibubi.create.content.redstone.displayLink;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTarget;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.item.BlockUseBypassingItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class DisplayLinkBlockItem extends BlockItem implements BlockUseBypassingItem {

	public DisplayLinkBlockItem(Block pBlock, Properties pProperties) {
		super(pBlock, pProperties);
	}

	// fabric: handled by BlockUseBypassingItem
//	public static InteractionResult gathererItemAlwaysPlacesWhenUsed(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
//		ItemStack usedItem = player.getItemInHand(hand);
//		if (usedItem.getItem() instanceof DisplayLinkBlockItem) {
//			if (AllBlocks.DISPLAY_LINK.has(level
//				.getBlockState(hitResult.getBlockPos())))
//				return InteractionResult.PASS;
//			return InteractionResult.FAIL;
//		}
//		return InteractionResult.PASS;
//	}

	@Override
	public boolean shouldBypass(BlockState state, BlockPos pos, Level level, Player player, InteractionHand hand) {
		ItemStack usedItem = player.getItemInHand(hand);
		if (usedItem.getItem() instanceof DisplayLinkBlockItem) {
			if (!AllBlocks.DISPLAY_LINK.has(state))
				return true;
		}
		return false;
	}

	@Override
	public InteractionResult useOn(UseOnContext pContext) {
		ItemStack stack = pContext.getItemInHand();
		BlockPos pos = pContext.getClickedPos();
		Level level = pContext.getLevel();
		BlockState state = level.getBlockState(pos);
		Player player = pContext.getPlayer();

		if (player == null)
			return InteractionResult.FAIL;

		if (player.isShiftKeyDown() && stack.has(AllDataComponents.DISPLAY_LINK_POS)) {
			if (level.isClientSide)
				return InteractionResult.SUCCESS;
			player.displayClientMessage(Lang.translateDirect("display_link.clear"), true);
			stack.remove(AllDataComponents.DISPLAY_LINK_POS);
			return InteractionResult.SUCCESS;
		}

		if (!stack.has(AllDataComponents.DISPLAY_LINK_POS)) {
			if (level.isClientSide)
				return InteractionResult.SUCCESS;
			stack.set(AllDataComponents.DISPLAY_LINK_POS, pos);
			player.displayClientMessage(Lang.translateDirect("display_link.set"), true);
			return InteractionResult.SUCCESS;
		}

		//CompoundTag tag = stack.getTag();
		CompoundTag teTag = new CompoundTag();

		BlockPos selectedPos = stack.get(AllDataComponents.DISPLAY_LINK_POS);
		BlockPos placedPos = pos.relative(pContext.getClickedFace(), state.canBeReplaced() ? 0 : 1);

		if (!selectedPos.closerThan(placedPos, AllConfigs.server().logistics.displayLinkRange.get())) {
			player.displayClientMessage(Lang.translateDirect("display_link.too_far")
				.withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}

		teTag.put("TargetOffset", NbtUtils.writeBlockPos(selectedPos.subtract(placedPos)));
		teTag.putString("id", "display_link");
		stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(teTag));

		InteractionResult useOn = super.useOn(pContext);
		if (level.isClientSide || useOn == InteractionResult.FAIL)
			return useOn;

		ItemStack itemInHand = player.getItemInHand(pContext.getHand());
		if (!itemInHand.isEmpty())
			itemInHand.applyComponents(DataComponentMap.EMPTY);
			//itemInHand.setTag(null);
		player.displayClientMessage(Lang.translateDirect("display_link.success")
			.withStyle(ChatFormatting.GREEN), true);
		return useOn;
	}

	private static BlockPos lastShownPos = null;
	private static AABB lastShownAABB = null;

	@Environment(EnvType.CLIENT)
	public static void clientTick() {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		ItemStack heldItemMainhand = player.getMainHandItem();
		if (!(heldItemMainhand.getItem() instanceof DisplayLinkBlockItem))
			return;
		if (!heldItemMainhand.has(AllDataComponents.DISPLAY_LINK_POS))
			return;
		BlockPos selectedPos = heldItemMainhand.get(AllDataComponents.DISPLAY_LINK_POS);

		if (!selectedPos.equals(lastShownPos)) {
			lastShownAABB = getBounds(selectedPos);
			lastShownPos = selectedPos;
		}

		CreateClient.OUTLINER.showAABB("target", lastShownAABB)
			.colored(0xffcb74)
			.lineWidth(1 / 16f);
	}

	@Environment(EnvType.CLIENT)
	private static AABB getBounds(BlockPos pos) {
		Level world = Minecraft.getInstance().level;
		DisplayTarget target = AllDisplayBehaviours.targetOf(world, pos);

		if (target != null)
			return target.getMultiblockBounds(world, pos);

		BlockState state = world.getBlockState(pos);
		VoxelShape shape = state.getShape(world, pos);
		return shape.isEmpty() ? new AABB(BlockPos.ZERO)
			: shape.bounds()
				.move(pos);
	}

}
