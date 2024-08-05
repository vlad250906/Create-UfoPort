package io.github.fabricators_of_create.porting_lib;

import io.github.fabricators_of_create.porting_lib.command.ConfigCommand;
import io.github.fabricators_of_create.porting_lib.command.EnumArgument;
import io.github.fabricators_of_create.porting_lib.util.UsernameCache;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.serialization.Codec;

import io.github.fabricators_of_create.porting_lib.command.ModIdArgument;
import io.github.fabricators_of_create.porting_lib.core.PortingLib;
import io.github.fabricators_of_create.porting_lib.data.ConditionalRecipe;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.item.FluidHandlerItemStack;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemItemStorages;
import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;
import io.github.fabricators_of_create.porting_lib.util.PortingHooks;
import io.github.fabricators_of_create.porting_lib.util.TierSortingRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

public class PortingLibBase implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Porting Lib Base");
	
	public static DataComponentType<Integer> TOOLTIP_HIDE = null;
	public static Level serverLevel = null;
	
	private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, name, (builder.apply(DataComponentType.builder())).build());
    }
	
	public static enum TooltipPart {
        ENCHANTMENTS,
        MODIFIERS,
        UNBREAKABLE,
        DYE,
        UPGRADES;

        private final int mask = 1 << this.ordinal();

        public int getMask() {
            return this.mask;
        }
    }
	
	@Override
	public void onInitialize() {
		TOOLTIP_HIDE = register("portlib_tooltip_hide", builder -> builder.persistent(Codec.INT));
		PayloadTypeRegistry.playS2C().register(TierSortingRegistry.SyncPacket.PACKET_ID, TierSortingRegistry.SyncPacket.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(NetworkHooks.OpenScreenPacket.PACKET_ID, NetworkHooks.OpenScreenPacket.STREAM_CODEC);
		
		TierSortingRegistry.init();
		ConditionalRecipe.init();
		ItemItemStorages.init();
		UsernameCache.load();
		PortingHooks.init();
		DataComponentType<FluidStack> FLUID_DATA = FluidHandlerItemStack.FLUID_DATA;
		// can be used to force all mixins to apply
		// MixinEnvironment.getCurrentEnvironment().audit();

		ArgumentTypeRegistry.registerArgumentType(PortingLib.id("modid"), ModIdArgument.class,
				SingletonArgumentInfo.contextFree(ModIdArgument::modIdArgument));
		ArgumentTypeRegistry.registerArgumentType(PortingLib.id("enum"), EnumArgument.class,
				new EnumArgument.Info());

		CommandRegistrationCallback.EVENT.register(ConfigCommand::register);
		
		ServerWorldEvents.LOAD.register((server, level) -> {
			serverLevel = level;
		});
	}
	
	public static RegistryAccess getRegistryAccess() {
		if(serverLevel == null) {
			return Minecraft.getInstance().level.registryAccess();
		}else {
			return serverLevel.registryAccess();
		}
	}
}
