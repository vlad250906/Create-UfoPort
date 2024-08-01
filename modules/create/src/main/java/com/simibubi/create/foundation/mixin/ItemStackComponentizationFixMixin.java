package com.simibubi.create.foundation.mixin;

import java.util.concurrent.atomic.AtomicBoolean;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.serialization.Dynamic;
import com.simibubi.create.foundation.block.DyedBlockList;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix.ItemStackData;
import net.minecraft.world.item.DyeColor;;

@Mixin(ItemStackComponentizationFix.class)
public class ItemStackComponentizationFixMixin {
	
	@Inject(
		method = "fixItemStack",
		at = @At("TAIL")
	)
	private static void create$fixItemStack(ItemStackData itemStackData, Dynamic<?> tag, CallbackInfo callback) {
		itemStackData.moveTagToComponent("CollectingLight", "create:collecting_light");
		itemStackData.moveTagToComponent("InferredFromRecipe", "create:inferred_from_recipe");
		
		if(itemStackData.is("create:copper_backtank")
				|| itemStackData.is("create:copper_backtank_placeable")
				|| itemStackData.is("create:netherite_backtank") 
				|| itemStackData.is("create:netherite_backtank_placeable")) 
		{
			itemStackData.moveTagToComponent("Air", "create:air_tank");
		}
		
		if(itemStackData.is("create:belt_connector")) {
			itemStackData.moveTagToComponent("FirstPulley", "create:first_pulley");
		}
		
		if(itemStackData.is("create:display_link")) {
			itemStackData.moveTagToComponent("SelectedPos", "create:display_link_pos");
		}
		
		if(itemStackData.is("create:minecart_contraption") 
				|| itemStackData.is("create:furnace_minecart_contraption") 
				|| itemStackData.is("create:chest_minecart_contraption")) 
		{
			CompoundTag tag1 = new CompoundTag();
			itemStackData.removeTag("Contraption").result().ifPresent(arg -> tag1.put("Contraption", (Tag)arg.getValue()));
			if(!tag1.isEmpty()) itemStackData.setComponent("create:minecart_contraption", new Dynamic(NbtOps.INSTANCE, tag1));
		}
		
		//CompoundTag tag2 = new CompoundTag();
		//if(!tag2.isEmpty()) itemStackData.setComponent("create:blueprint_data", new Dynamic(NbtOps.INSTANCE, tag2));
		
		if(itemStackData.is("create:linked_controller") 
				|| itemStackData.is("create:filter") 
				|| itemStackData.is("create:attribute_filter")) 
		{
			CompoundTag tag5 = new CompoundTag();
			itemStackData.removeTag("Items").result().ifPresent(arg -> tag5.put("Items", (Tag)arg.getValue()));
			itemStackData.removeTag("RespectNBT").result().ifPresent(arg -> tag5.put("RespectNBT", (Tag)arg.getValue()));
			itemStackData.removeTag("Blacklist").result().ifPresent(arg -> tag5.put("Blacklist", (Tag)arg.getValue()));
			itemStackData.removeTag("WhitelistMode").result().ifPresent(arg -> tag5.put("WhitelistMode", (Tag)arg.getValue()));
			itemStackData.removeTag("MatchedAttributes").result().ifPresent(arg -> tag5.put("MatchedAttributes", (Tag)arg.getValue()));
			if(!tag5.isEmpty()) itemStackData.setComponent("create:filter_data", new Dynamic(NbtOps.INSTANCE, tag5));
		}
		
		if(itemStackData.is("create:clipboard")) {
			CompoundTag tag5 = new CompoundTag();
			itemStackData.removeTag("Readonly").result().ifPresent(arg -> tag5.put("Readonly", (Tag)arg.getValue()));
			itemStackData.removeTag("CopiedValues").result().ifPresent(arg -> tag5.put("CopiedValues", (Tag)arg.getValue()));
			itemStackData.removeTag("PreviouslyOpenedPage").result().ifPresent(arg -> tag5.put("PreviouslyOpenedPage", (Tag)arg.getValue()));
			itemStackData.removeTag("Type").result().ifPresent(arg -> tag5.put("Type", (Tag)arg.getValue()));
			itemStackData.removeTag("Pages").result().ifPresent(arg -> tag5.put("Pages", (Tag)arg.getValue()));
			if(!tag5.isEmpty()) itemStackData.setComponent("create:clipboard_editing", new Dynamic(NbtOps.INSTANCE, tag5));
		}
		
		if(itemStackData.is("create:sand_paper") || itemStackData.is("create:red_sand_paper")) {
			CompoundTag tag5 = new CompoundTag();
			itemStackData.removeTag("Polishing").result().ifPresent(arg -> tag5.put("Polishing", (Tag)arg.getValue()));
			itemStackData.removeTag("JEI").result().ifPresent(arg -> tag5.put("JEI", (Tag)arg.getValue()));
			if(!tag5.isEmpty()) itemStackData.setComponent("create:polishing", new Dynamic(NbtOps.INSTANCE, tag5));
		}
		
		if(itemStackData.is("create:wand_of_symmetry")) {
			CompoundTag tag5 = new CompoundTag();
			itemStackData.removeTag("symmetry").result().ifPresent(arg -> tag5.put("symmetry", (Tag)arg.getValue()));
			itemStackData.removeTag("enable").result().ifPresent(arg -> tag5.put("enable", (Tag)arg.getValue()));
			if(!tag5.isEmpty()) itemStackData.setComponent("create:symmetry_wand", new Dynamic(NbtOps.INSTANCE, tag5));
		}
		
		boolean isToolbox = false;
		for (DyeColor color : DyeColor.values()) {
			if(itemStackData.is("create:" + color.getSerializedName() + "_toolbox"))
				isToolbox = true;
		}
		if(isToolbox) {
			CompoundTag tag5 = new CompoundTag();
			itemStackData.removeTag("Inventory").result().ifPresent(arg -> tag5.put("Inventory", (Tag)arg.getValue()));
			itemStackData.removeTag("UniqueId").result().ifPresent(arg -> tag5.put("UniqueId", (Tag)arg.getValue()));
			if(!tag5.isEmpty()) itemStackData.setComponent("create:toolbox", new Dynamic(NbtOps.INSTANCE, tag5));
		}
		
		if(itemStackData.is("create:handheld_worldshaper")) {
			CompoundTag tag5 = new CompoundTag();
			itemStackData.removeTag("Brush").result().ifPresent(arg -> tag5.put("Brush", (Tag)arg.getValue()));
			itemStackData.removeTag("BrushParams").result().ifPresent(arg -> tag5.put("BrushParams", (Tag)arg.getValue()));
			itemStackData.removeTag("Tool").result().ifPresent(arg -> tag5.put("Tool", (Tag)arg.getValue()));
			itemStackData.removeTag("Placement").result().ifPresent(arg -> tag5.put("Placement", (Tag)arg.getValue()));
			itemStackData.removeTag("Pattern").result().ifPresent(arg -> tag5.put("Pattern", (Tag)arg.getValue()));
			itemStackData.removeTag("BlockUsed").result().ifPresent(arg -> tag5.put("BlockUsed", (Tag)arg.getValue()));
			itemStackData.removeTag("BlockData").result().ifPresent(arg -> tag5.put("BlockData", (Tag)arg.getValue()));
			itemStackData.removeTag("_Swap").result().ifPresent(arg -> tag5.put("_Swap", (Tag)arg.getValue()));
			if(!tag5.isEmpty()) itemStackData.setComponent("create:zapper", new Dynamic(NbtOps.INSTANCE, tag5));
		}
		
		CompoundTag tag4 = new CompoundTag();
		itemStackData.removeTag("SequencedAssembly").result().ifPresent(arg -> tag4.put("SequencedAssembly", (Tag)arg.getValue()));
		if(!tag4.isEmpty()) itemStackData.setComponent("create:sequenced_assembly", new Dynamic(NbtOps.INSTANCE, tag4));
		
		if(itemStackData.is("create:empty_schematic") ||
				itemStackData.is("create:schematic_and_quill") ||
				itemStackData.is("create:schematic") ||
				itemStackData.is("create:deployer")) 
		{
			CompoundTag tag5 = new CompoundTag();
			itemStackData.removeTag("Deployed").result().ifPresent(arg -> tag5.put("Deployed", (Tag)arg.getValue()));
			itemStackData.removeTag("Anchor").result().ifPresent(arg -> tag5.put("Anchor", (Tag)arg.getValue()));
			itemStackData.removeTag("Rotation").result().ifPresent(arg -> tag5.put("Rotation", (Tag)arg.getValue()));
			itemStackData.removeTag("Mirror").result().ifPresent(arg -> tag5.put("Mirror", (Tag)arg.getValue()));
			itemStackData.removeTag("File").result().ifPresent(arg -> tag5.put("File", (Tag)arg.getValue()));
			itemStackData.removeTag("Bounds").result().ifPresent(arg -> tag5.put("Bounds", (Tag)arg.getValue()));
			itemStackData.removeTag("Owner").result().ifPresent(arg -> tag5.put("Owner", (Tag)arg.getValue()));
			itemStackData.removeTag("SchematicHash").result().ifPresent(arg -> tag5.put("SchematicHash", (Tag)arg.getValue()));
			if(!tag5.isEmpty()) itemStackData.setComponent("create:schematic_data", new Dynamic(NbtOps.INSTANCE, tag5));
		}
		
		if(itemStackData.is("create:track") ||
				itemStackData.is("create:fake_track")) 
		{
			CompoundTag tag5 = new CompoundTag();
			itemStackData.removeTag("ExtendCurve").result().ifPresent(arg -> tag5.put("ExtendCurve", (Tag)arg.getValue()));
			itemStackData.removeTag("ConnectingFrom").result().ifPresent(arg -> tag5.put("ConnectingFrom", (Tag)arg.getValue()));
			if(!tag5.isEmpty()) itemStackData.setComponent("create:track_item", new Dynamic(NbtOps.INSTANCE, tag5));
		}
		
		if(itemStackData.is("create:track_station") ||
				itemStackData.is("create:track_signal") ||
				itemStackData.is("create:track_observer") ||
				itemStackData.is("create:track_signal")) 
		{
			CompoundTag tag5 = new CompoundTag();
			itemStackData.removeTag("SelectedPos").result().ifPresent(arg -> tag5.put("SelectedPos", (Tag)arg.getValue()));
			itemStackData.removeTag("SelectedDirection").result().ifPresent(arg -> tag5.put("SelectedDirection", (Tag)arg.getValue()));
			itemStackData.removeTag("Bezier").result().ifPresent(arg -> tag5.put("Bezier", (Tag)arg.getValue()));
			if(!tag5.isEmpty()) itemStackData.setComponent("create:track_targeting", new Dynamic(NbtOps.INSTANCE, tag5));
		}
	}
	
}
