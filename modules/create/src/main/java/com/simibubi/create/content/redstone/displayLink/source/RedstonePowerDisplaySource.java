package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RedstonePowerDisplaySource extends PercentOrProgressBarDisplaySource {

	@Override
	protected String getTranslationKey() {
		return "redstone_power";
	}

	@Override
	protected MutableComponent formatNumeric(DisplayLinkContext context, Float currentLevel) {
		return Components.literal(String.valueOf((int) (currentLevel * 15)));
	}

	@Override
	protected boolean allowsLabeling(DisplayLinkContext context) {
		return true;
	}

	@Override
	protected Float getProgress(DisplayLinkContext context) {
		BlockState blockState = context.level()
			.getBlockState(context.getSourcePos());
		return Math.max(context.level()
			.getDirectSignalTo(context.getSourcePos()),
			blockState.getOptionalValue(BlockStateProperties.POWER)
				.orElse(0))
			/ 15f;
	}

	@Override
	protected boolean progressBarActive(DisplayLinkContext context) {
		return context.sourceConfig()
			.getInt("Mode") != 0;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
		super.initConfigurationWidgets(context, builder, isFirstLine);
		if (isFirstLine)
			return;
		builder.addSelectionScrollInput(0, 120,
			(si, l) -> si.forOptions(Lang.translatedOptions("display_source.redstone_power", "number", "progress_bar"))
				.titled(Lang.translateDirect("display_source.redstone_power.display")),
			"Mode");
	}

}
