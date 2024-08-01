package io.github.fabricators_of_create.porting_lib.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import javax.annotation.Nullable;

public interface CustomMapItem {
	@Nullable
	default MapItemSavedData getCustomMapData(ItemStack stack, Level level) {
		MapId id = stack.get(DataComponents.MAP_ID);
		return MapItem.getSavedData(id, level);
	}
}
