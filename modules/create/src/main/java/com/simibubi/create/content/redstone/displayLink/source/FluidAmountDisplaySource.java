package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlockEntity;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.TankManipulationBehaviour;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.FluidFormatter;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.util.FluidUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FluidAmountDisplaySource extends SingleLineDisplaySource {

	@Override
	protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
		BlockEntity sourceBE = context.getSourceBlockEntity();
		if (!(sourceBE instanceof SmartObserverBlockEntity cobe))
			return EMPTY_LINE;

		TankManipulationBehaviour tankManipulationBehaviour = cobe.getBehaviour(TankManipulationBehaviour.OBSERVE);
		FilteringBehaviour filteringBehaviour = cobe.getBehaviour(FilteringBehaviour.TYPE);
		Storage<FluidVariant> handler = tankManipulationBehaviour.getInventory();

		if (handler == null)
			return EMPTY_LINE;

		long collected = 0;
		try (Transaction t = TransferUtil.getTransaction()) {
			for (StorageView<FluidVariant> view : handler.nonEmptyViews()) {
				FluidStack stack = new FluidStack(view);
				if (!filteringBehaviour.test(stack))
					continue;
				collected += stack.getAmount();
			}
		}

		return Components.literal(FluidFormatter.asString(collected, false, getUnit(context)));
	}

	@Override
	protected String getTranslationKey() {
		return "fluid_amount";
	}

	@Override
	protected boolean allowsLabeling(DisplayLinkContext context) {
		return true;
	}

	// fabric: droplets support

	protected FluidUnit getUnit(DisplayLinkContext context) {
		int format = context.sourceConfig().getInt("FluidUnit");
		return format == 0 ? FluidUnit.MILLIBUCKETS : FluidUnit.DROPLETS;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder,
			boolean isFirstLine) {
		if (isFirstLine) {
			builder.addSelectionScrollInput(0, 75,
					(si, l) -> si
							.forOptions(
									Lang.translatedOptions("display_source.fluid_amount", "millibuckets", "droplets"))
							.titled(Lang.translateDirect("display_source.fluid_amount.display")),
					"FluidUnit");
		}
	}
}
