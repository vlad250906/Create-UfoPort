package com.simibubi.create.content.schematics.packet;

import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SchematicPlacePacket extends SimplePacketBase {

	public ItemStack stack;

	public SchematicPlacePacket(ItemStack stack) {
		this.stack = stack;
	}

	public SchematicPlacePacket(RegistryFriendlyByteBuf buffer) {
		stack = ItemStack.STREAM_CODEC.decode(buffer);
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		ItemStack.STREAM_CODEC.encode(buffer, stack);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null)
				return;
			if (!player.isCreative())
				return;

			Level world = player.level();
			SchematicPrinter printer = new SchematicPrinter();
			printer.loadSchematic(stack, world, !player.canUseGameMasterBlocks());
			if (!printer.isLoaded() || printer.isErrored())
				return;

			boolean includeAir = AllConfigs.server().schematics.creativePrintIncludesAir.get();

			while (printer.advanceCurrentPos()) {
				if (!printer.shouldPlaceCurrent(world))
					continue;

				printer.handleCurrentTarget((pos, state, blockEntity) -> {
					boolean placingAir = state.isAir();
					if (placingAir && !includeAir)
						return;

					CompoundTag data = BlockHelper.prepareBlockEntityData(state, blockEntity);
					BlockHelper.placeSchematicBlock(world, state, pos, null, data);
				}, (pos, entity) -> {
					world.addFreshEntity(entity);
				});
			}
		});
		return true;
	}

}
