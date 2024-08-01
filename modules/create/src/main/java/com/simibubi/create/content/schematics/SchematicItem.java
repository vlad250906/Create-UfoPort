package com.simibubi.create.content.schematics;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.client.SchematicEditScreen;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class SchematicItem extends Item {

	private static final Logger LOGGER = LogUtils.getLogger();

	public SchematicItem(Properties properties) {
		super(properties);
	}

	public static ItemStack create(HolderGetter<Block> lookup, String schematic, String owner) {
		ItemStack blueprint = AllItems.SCHEMATIC.asStack();

		CompoundTag tag = new CompoundTag();
		tag.putBoolean("Deployed", false);
		tag.putString("Owner", owner);
		tag.putString("File", schematic);
		tag.put("Anchor", NbtUtils.writeBlockPos(BlockPos.ZERO));
		tag.putString("Rotation", Rotation.NONE.name());
		tag.putString("Mirror", Mirror.NONE.name());
		blueprint.set(AllDataComponents.SCHEMATIC_DATA, tag);

		writeSize(lookup, blueprint);
		return blueprint;
	}

	@Override
	@Environment(value = EnvType.CLIENT)
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		if (stack.has(AllDataComponents.SCHEMATIC_DATA)) {
			if (stack.get(AllDataComponents.SCHEMATIC_DATA)
				.contains("File"))
				tooltip.add(Components.literal(ChatFormatting.GOLD + stack.get(AllDataComponents.SCHEMATIC_DATA)
					.getString("File")));
		} else {
			tooltip.add(Lang.translateDirect("schematic.invalid").withStyle(ChatFormatting.RED));
		}
		super.appendHoverText(stack, context, tooltip, flagIn);
	}

	public static void writeSize(HolderGetter<Block> lookup, ItemStack blueprint) {
		CompoundTag tag = ItemHelper.getOrCreateComponent(blueprint, AllDataComponents.SCHEMATIC_DATA, new CompoundTag());
		StructureTemplate t = loadSchematic(lookup, blueprint);
		tag.put("Bounds", NBTHelper.writeVec3i(t.getSize()));
		SchematicInstances.clearHash(blueprint);
	}

	public static StructurePlaceSettings getSettings(ItemStack blueprint) {
		return getSettings(blueprint, true);
	}

	public static StructurePlaceSettings getSettings(ItemStack blueprint, boolean processNBT) {
		CompoundTag tag = blueprint.getOrDefault(AllDataComponents.SCHEMATIC_DATA, new CompoundTag());
		StructurePlaceSettings settings = new StructurePlaceSettings();
		settings.setRotation(Rotation.valueOf(tag.getString("Rotation")));
		settings.setMirror(Mirror.valueOf(tag.getString("Mirror")));
		if (processNBT)
			settings.addProcessor(SchematicProcessor.INSTANCE);
		return settings;
	}

	public static StructureTemplate loadSchematic(HolderGetter<Block> lookup, ItemStack blueprint) {
		StructureTemplate t = new StructureTemplate();
		String owner = blueprint.getOrDefault(AllDataComponents.SCHEMATIC_DATA, new CompoundTag())
			.getString("Owner");
		String schematic = blueprint.getOrDefault(AllDataComponents.SCHEMATIC_DATA, new CompoundTag())
			.getString("File");

		if (!schematic.endsWith(".nbt"))
			return t;

		Path dir;
		Path file;

//		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
//			dir = Paths.get("schematics", "uploaded").toAbsolutePath();
//			file = Paths.get(owner, schematic);
//		} else {
//			dir = Paths.get("schematics").toAbsolutePath();
//			file = Paths.get(schematic);
//		}
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			dir = Paths.get("schematics", "uploaded").toAbsolutePath();
			file = Paths.get(owner, schematic);
		} else {
			dir = Paths.get("schematics").toAbsolutePath();
			file = Paths.get(schematic);
		}

		Path path = dir.resolve(file).normalize();
		if (!path.startsWith(dir))
			return t;

		try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
				new GZIPInputStream(Files.newInputStream(path, StandardOpenOption.READ))))) {
			CompoundTag nbt = NbtIo.read(stream, NbtAccounter.create(0x20000000L));
			t.load(lookup, nbt);
		} catch (IOException e) {
			LOGGER.warn("Failed to read schematic", e);
		}

		return t;
	}

	@Nonnull
	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getPlayer() != null && !onItemUse(context.getPlayer(), context.getHand()))
			return super.useOn(context);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (!onItemUse(playerIn, handIn))
			return super.use(worldIn, playerIn, handIn);
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
	}

	private boolean onItemUse(Player player, InteractionHand hand) {
		if (!player.isShiftKeyDown() || hand != InteractionHand.MAIN_HAND)
			return false;
		if (!player.getItemInHand(hand)
			.has(AllDataComponents.SCHEMATIC_DATA))
			return false;
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::displayBlueprintScreen);
		return true;
	}

	@Environment(value = EnvType.CLIENT)
	protected void displayBlueprintScreen() {
		ScreenOpener.open(new SchematicEditScreen());
	}

}
