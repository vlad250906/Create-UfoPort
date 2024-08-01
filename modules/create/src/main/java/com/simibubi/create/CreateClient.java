package com.simibubi.create;

import com.jozufozu.flywheel.fabric.event.FlywheelEvents;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.sodium.SodiumCompat;
import com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHandler;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.contraptions.render.SBBContraptionManager;
import com.simibubi.create.content.decoration.encasing.CasingConnectivity;
import com.simibubi.create.content.equipment.armor.AllArmorMaterials;
import com.simibubi.create.content.equipment.armor.RemainingAirOverlay;
import com.simibubi.create.content.equipment.bell.SoulPulseEffectHandler;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonRenderHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import com.simibubi.create.content.equipment.zapper.ZapperRenderHandler;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelRenderer;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler;
import com.simibubi.create.content.schematics.client.ClientSchematicLoader;
import com.simibubi.create.content.schematics.client.SchematicAndQuillHandler;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.TrainHUD;
import com.simibubi.create.content.trains.track.TrackPlacementOverlay;
import com.simibubi.create.foundation.ClientResourceReloadListener;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsClient;
import com.simibubi.create.foundation.events.ClientEvents;
import com.simibubi.create.foundation.events.InputEvents;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.outliner.Outliner;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.ponder.element.WorldSectionElement;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.CreateContexts;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.render.SuperByteBufferCache;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.ModelSwapper;
import com.simibubi.create.foundation.utility.ghost.GhostBlocks;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.ponder.AllPonderTags;
import com.simibubi.create.infrastructure.ponder.PonderIndex;

import io.github.fabricators_of_create.porting_lib.util.ArmorTextureRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.ChatFormatting;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public class CreateClient implements ClientModInitializer {

	public static final SuperByteBufferCache BUFFER_CACHE = new SuperByteBufferCache();
	public static final Outliner OUTLINER = new Outliner();
	public static final GhostBlocks GHOST_BLOCKS = new GhostBlocks();
	public static final ModelSwapper MODEL_SWAPPER = new ModelSwapper();
	public static final CasingConnectivity CASING_CONNECTIVITY = new CasingConnectivity();

	public static final ClientSchematicLoader SCHEMATIC_SENDER = new ClientSchematicLoader();
	public static final SchematicHandler SCHEMATIC_HANDLER = new SchematicHandler();
	public static final SchematicAndQuillHandler SCHEMATIC_AND_QUILL_HANDLER = new SchematicAndQuillHandler();
	public static final SuperGlueSelectionHandler GLUE_HANDLER = new SuperGlueSelectionHandler();

	public static final ZapperRenderHandler ZAPPER_RENDER_HANDLER = new ZapperRenderHandler();
	public static final PotatoCannonRenderHandler POTATO_CANNON_RENDER_HANDLER = new PotatoCannonRenderHandler();
	public static final SoulPulseEffectHandler SOUL_PULSE_EFFECT_HANDLER = new SoulPulseEffectHandler();
	public static final GlobalRailwayManager RAILWAYS = new GlobalRailwayManager();
	public static final ValueSettingsClient VALUE_SETTINGS_HANDLER = new ValueSettingsClient();

	public static final ClientResourceReloadListener RESOURCE_RELOAD_LISTENER = new ClientResourceReloadListener();

	@Override
	public void onInitializeClient() { // onCtorClient and clientInit merged
//		modEventBus.addListener(CreateClient::clientInit); // merged together
//		modEventBus.addListener(AllParticleTypes::registerFactories); // ParticleManagerRegistrationCallback in ClientEvents
		
		FlywheelEvents.GATHER_CONTEXT.register(CreateContexts::flwInit);
		FlywheelEvents.GATHER_CONTEXT.register(ContraptionRenderDispatcher::gatherContext);

		MODEL_SWAPPER.registerListeners();

		ZAPPER_RENDER_HANDLER.registerListeners();
		POTATO_CANNON_RENDER_HANDLER.registerListeners();

		// clientInit start

		BUFFER_CACHE.registerCompartment(CachedBufferer.GENERIC_BLOCK);
		BUFFER_CACHE.registerCompartment(CachedBufferer.PARTIAL);
		BUFFER_CACHE.registerCompartment(CachedBufferer.DIRECTIONAL_PARTIAL);
		BUFFER_CACHE.registerCompartment(KineticBlockEntityRenderer.KINETIC_BLOCK);
		BUFFER_CACHE.registerCompartment(WaterWheelRenderer.WATER_WHEEL);
		BUFFER_CACHE.registerCompartment(SBBContraptionManager.CONTRAPTION, 20);
		BUFFER_CACHE.registerCompartment(WorldSectionElement.DOC_WORLD_SECTION, 20);

		AllKeys.register();
		AllPartialModels.init();

		AllPonderTags.register();
		PonderIndex.register();

		registerOverlays();
		UIRenderHelper.init();

		// fabric exclusive
		ClientEvents.register();
		InputEvents.register();
		//AllPackets.getChannel().initClientListener();
		RenderTypes.init();
//		ArmorTextureRegistry.register(AllArmorMaterials.COPPER, CopperArmorItem.TEXTURE);
		AllFluids.initRendering();
		initCompat();
	}

	@SuppressWarnings("Convert2MethodRef") // may cause class loading issues if changed
	private static void initCompat() {
		Mods.SODIUM.executeIfInstalled(() -> () -> SodiumCompat.init());
	}

	private static void registerOverlays() {
		HudRenderCallback.EVENT.register((graphics, partialTicks) -> {
			Window window = Minecraft.getInstance().getWindow();

			RemainingAirOverlay.render(graphics, window.getGuiScaledWidth(), window.getGuiScaledHeight()); // Create's Remaining Air
			TrainHUD.renderOverlay(graphics, partialTicks.getGameTimeDeltaPartialTick(true), window); // Create's Train Driver HUD
			GoggleOverlayRenderer.renderOverlay(graphics, partialTicks.getGameTimeDeltaPartialTick(true), window); // Create's Goggle Information
			BlueprintOverlayRenderer.renderOverlay(graphics, partialTicks.getGameTimeDeltaPartialTick(true), window); // Create's Blueprints
			LinkedControllerClientHandler.renderOverlay(graphics, partialTicks.getGameTimeDeltaPartialTick(true), window); // Create's Linked Controller
			SCHEMATIC_HANDLER.renderOverlay(graphics, partialTicks.getGameTimeDeltaPartialTick(true), window); // Create's Schematics
			ToolboxHandlerClient.renderOverlay(graphics, partialTicks.getGameTimeDeltaPartialTick(true), window); // Create's Toolboxes
			VALUE_SETTINGS_HANDLER.render(graphics, window.getGuiScaledWidth(), window.getGuiScaledHeight()); // Create's Value Settings
			TrackPlacementOverlay.renderOverlay(Minecraft.getInstance().gui, graphics); // Create's Track Placement

			// fabric: normally a separate event listener
			PlacementHelpers.afterRenderOverlayLayer(graphics, partialTicks.getGameTimeDeltaPartialTick(true), window);
		});
	}

	public static void invalidateRenderers() {
		BUFFER_CACHE.invalidate();

		SCHEMATIC_HANDLER.updateRenderers();
		ContraptionRenderDispatcher.reset();
	}

	public static void checkGraphicsFanciness() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;

		if (mc.options.graphicsMode().get() != GraphicsStatus.FABULOUS)
			return;

		if (AllConfigs.client().ignoreFabulousWarning.get())
			return;

		MutableComponent text = ComponentUtils.wrapInSquareBrackets(Components.literal("WARN"))
			.withStyle(ChatFormatting.GOLD)
			.append(Components.literal(
				" Some of Create's visual features will not be available while Fabulous graphics are enabled!"))
			.withStyle(style -> style
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/create dismissFabulousWarning"))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					Components.literal("Click here to disable this warning"))));

		mc.player.displayClientMessage(text, false);
	}

}
