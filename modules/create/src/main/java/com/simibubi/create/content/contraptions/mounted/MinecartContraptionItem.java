package com.simibubi.create.content.contraptions.mounted;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionData;
import com.simibubi.create.content.contraptions.ContraptionMovementSetting;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import io.github.fabricators_of_create.porting_lib.util.MinecartAndRailUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.AbstractMinecart.Type;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.EntityHitResult;

public class MinecartContraptionItem extends Item {
	
	

	private final AbstractMinecart.Type minecartType;

	public static MinecartContraptionItem rideable(Properties builder) {
		return new MinecartContraptionItem(Type.RIDEABLE, builder);
	}

	public static MinecartContraptionItem furnace(Properties builder) {
		return new MinecartContraptionItem(Type.FURNACE, builder);
	}

	public static MinecartContraptionItem chest(Properties builder) {
		return new MinecartContraptionItem(Type.CHEST, builder);
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return AllConfigs.server().kinetics.minecartContraptionInContainers.get();
	}

	private MinecartContraptionItem(Type minecartTypeIn, Properties builder) {
		super(builder);
		this.minecartType = minecartTypeIn;
		DispenserBlock.registerBehavior(this, DISPENSER_BEHAVIOR);
	}

	// Taken and adjusted from MinecartItem
	private static final DispenseItemBehavior DISPENSER_BEHAVIOR = new DefaultDispenseItemBehavior() {
		private final DefaultDispenseItemBehavior behaviourDefaultDispenseItem = new DefaultDispenseItemBehavior();

		@Override
		public ItemStack execute(BlockSource source, ItemStack stack) {
			if (!canPlace())
				return behaviourDefaultDispenseItem.dispense(source, stack);

			Direction direction = source.state()
				.getValue(DispenserBlock.FACING);
			Level world = source.level();
			double d0 = source.pos().getX() + (double) direction.getStepX() * 1.125D;
			double d1 = Math.floor(source.pos().getY()) + (double) direction.getStepY();
			double d2 = source.pos().getZ() + (double) direction.getStepZ() * 1.125D;
			BlockPos blockpos = source.pos()
				.relative(direction);
			BlockState blockstate = world.getBlockState(blockpos);
			RailShape railshape = blockstate.getBlock() instanceof BaseRailBlock
				? MinecartAndRailUtil.getDirectionOfRail(blockstate, world, blockpos, null)
				: RailShape.NORTH_SOUTH;
			double d3;
			if (blockstate.is(BlockTags.RAILS)) {
				if (railshape.isAscending()) {
					d3 = 0.6D;
				} else {
					d3 = 0.1D;
				}
			} else {
				if (!blockstate.isAir() || !world.getBlockState(blockpos.below())
					.is(BlockTags.RAILS)) {
					return this.behaviourDefaultDispenseItem.dispense(source, stack);
				}

				BlockState blockstate1 = world.getBlockState(blockpos.below());
				RailShape railshape1 = blockstate1.getBlock() instanceof BaseRailBlock
					? MinecartAndRailUtil.getDirectionOfRail(blockstate1, world, blockpos.below(),
						null)
					: RailShape.NORTH_SOUTH;
				if (direction != Direction.DOWN && railshape1.isAscending()) {
					d3 = -0.4D;
				} else {
					d3 = -0.9D;
				}
			}

			AbstractMinecart abstractminecartentity = AbstractMinecart.createMinecart((ServerLevel) world, d0, d1 + d3, d2,
				((MinecartContraptionItem) stack.getItem()).minecartType, stack, null);
			if (stack.has(DataComponents.CUSTOM_NAME))
				abstractminecartentity.setCustomName(stack.getHoverName());
			world.addFreshEntity(abstractminecartentity);
			addContraptionToMinecart(world, stack, abstractminecartentity, direction);

			stack.shrink(1);
			return stack;
		}

		@Override
		protected void playSound(BlockSource source) {
			source.level()
				.levelEvent(1000, source.pos(), 0);
		}
	};

	// Taken and adjusted from MinecartItem
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		BlockPos blockpos = context.getClickedPos();
		BlockState blockstate = world.getBlockState(blockpos);
		if (!blockstate.is(BlockTags.RAILS)) {
			return InteractionResult.FAIL;
		} else if (!canPlace()) {
			Player player = context.getPlayer();
			if (player != null) {
				Component message = Lang.translateDirect("contraption.minecart_contraption_illegal_placement").withStyle(ChatFormatting.RED);
				player.displayClientMessage(message, true);
			}
			return InteractionResult.FAIL;
		} else {
			ItemStack itemstack = context.getItemInHand();
			if (!world.isClientSide) {
				RailShape railshape = blockstate.getBlock() instanceof BaseRailBlock
					? MinecartAndRailUtil.getDirectionOfRail(blockstate, world, blockpos, null)
					: RailShape.NORTH_SOUTH;
				double d0 = 0.0D;
				if (railshape.isAscending()) {
					d0 = 0.5D;
				}

				AbstractMinecart abstractminecartentity =
					AbstractMinecart.createMinecart((ServerLevel)world, (double) blockpos.getX() + 0.5D,
						(double) blockpos.getY() + 0.0625D + d0, (double) blockpos.getZ() + 0.5D, this.minecartType, itemstack, null);
				if (itemstack.has(DataComponents.CUSTOM_NAME))
					abstractminecartentity.setCustomName(itemstack.getHoverName());
				Player player = context.getPlayer();
				world.addFreshEntity(abstractminecartentity);
				addContraptionToMinecart(world, itemstack, abstractminecartentity,
					player == null ? null : player.getDirection());
			}

			itemstack.shrink(1);
			return InteractionResult.SUCCESS;
		}
	}

	// fabric: temp fix for command smuggling for Blanketcon
	private static boolean canPlace() {
		return AllConfigs.server().kinetics.contraptionPlacing.get();
	}

	public static void addContraptionToMinecart(Level world, ItemStack itemstack, AbstractMinecart cart,
		@Nullable Direction newFacing) {
		CompoundTag tag = ItemHelper.getOrCreateComponent(itemstack, AllDataComponents.MINECART_CONTRAPTION, new CompoundTag());
		if (tag.contains("Contraption")) {
			CompoundTag contraptionTag = tag.getCompound("Contraption");

			Direction intialOrientation = NBTHelper.readEnum(contraptionTag, "InitialOrientation", Direction.class);

			Contraption mountedContraption = Contraption.fromNBT(world, contraptionTag, false);
			OrientedContraptionEntity contraptionEntity =
				newFacing == null ? OrientedContraptionEntity.create(world, mountedContraption, intialOrientation)
					: OrientedContraptionEntity.createAtYaw(world, mountedContraption, intialOrientation,
						newFacing.toYRot());

			contraptionEntity.startRiding(cart);
			contraptionEntity.setPos(cart.getX(), cart.getY(), cart.getZ());
			world.addFreshEntity(contraptionEntity);
		}
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return "item.create.minecart_contraption";
	}

	public static InteractionResult wrenchCanBeUsedToPickUpMinecartContraptions(Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
		if (player == null || entity == null)
			return InteractionResult.PASS;
		if (!AllConfigs.server().kinetics.survivalContraptionPickup.get() && !player.isCreative())
			return InteractionResult.PASS;

		if (player.isSpectator()) // forge checks this, fabric does not
			return InteractionResult.PASS;
		if (AdventureUtil.isAdventure(player))
			return InteractionResult.PASS;

		ItemStack wrench = player.getItemInHand(hand);
		if (!AllItems.WRENCH.isIn(wrench))
			return InteractionResult.PASS;
		if (entity instanceof AbstractContraptionEntity)
			entity = entity.getVehicle();
		if (!(entity instanceof AbstractMinecart))
			return InteractionResult.PASS;
		if (!entity.isAlive())
			return InteractionResult.PASS;
		if (player instanceof DeployerFakePlayer dfp && dfp.onMinecartContraption)
			return InteractionResult.PASS;
		AbstractMinecart cart = (AbstractMinecart) entity;
		Type type = cart.getMinecartType();
		if (type != Type.RIDEABLE && type != Type.FURNACE && type != Type.CHEST)
			return InteractionResult.PASS;
		List<Entity> passengers = cart.getPassengers();
		if (passengers.isEmpty() || !(passengers.get(0) instanceof OrientedContraptionEntity))
			return InteractionResult.PASS;
		OrientedContraptionEntity oce = (OrientedContraptionEntity) passengers.get(0);
		Contraption contraption = oce.getContraption();

		if (ContraptionMovementSetting.isNoPickup(contraption.getBlocks()
			.values())) {
			player.displayClientMessage(Lang.translateDirect("contraption.minecart_contraption_illegal_pickup")
				.withStyle(ChatFormatting.RED), true);
			return InteractionResult.PASS;
		}

		if (world.isClientSide) {
			return InteractionResult.SUCCESS;
		}

		contraption.stop(world);

		for (MutablePair<StructureBlockInfo, MovementContext> pair : contraption.getActors())
			if (AllMovementBehaviours.getBehaviour(pair.left.state())instanceof PortableStorageInterfaceMovement psim)
				psim.reset(pair.right);

		ItemStack generatedStack = create(type, oce);
		generatedStack.set(DataComponents.CUSTOM_NAME, entity.getCustomName());

		if (ContraptionData.isTooLargeForPickup((CompoundTag)generatedStack.saveOptional(Create.getRegistryAccess()))) {
			MutableComponent message = Lang.translateDirect("contraption.minecart_contraption_too_big")
					.withStyle(ChatFormatting.RED);
			player.displayClientMessage(message, true);
			return InteractionResult.PASS;
		}

		if (contraption.getBlocks()
			.size() > 200)
			AllAdvancements.CART_PICKUP.awardTo(player);

		player.getInventory()
			.placeItemBackInInventory(generatedStack);
		oce.discard();
		entity.discard();
		return InteractionResult.SUCCESS;
	}

	public static ItemStack create(Type type, OrientedContraptionEntity entity) {
		ItemStack stack = ItemStack.EMPTY;

		switch (type) {
		case RIDEABLE:
			stack = AllItems.MINECART_CONTRAPTION.asStack();
			break;
		case FURNACE:
			stack = AllItems.FURNACE_MINECART_CONTRAPTION.asStack();
			break;
		case CHEST:
			stack = AllItems.CHEST_MINECART_CONTRAPTION.asStack();
			break;
		default:
			break;
		}

		if (stack.isEmpty())
			return stack;

		CompoundTag tag = entity.getContraption()
			.writeNBT(false);
		tag.remove("UUID");
		tag.remove("Pos");
		tag.remove("Motion");

		NBTHelper.writeEnum(tag, "InitialOrientation", entity.getInitialOrientation());

		ItemHelper.getOrCreateComponent(stack, AllDataComponents.MINECART_CONTRAPTION, new CompoundTag())
			.put("Contraption", tag);
		return stack;
	}
}
