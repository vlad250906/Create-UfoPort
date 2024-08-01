package com.simibubi.create.content.trains.station;

import java.util.Objects;
import java.util.Optional;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.map.CustomRenderedMapDecoration;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.NbtFixer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class StationMarker {
	// Not MANSION or MONUMENT to allow map extending
	public static final Holder<MapDecorationType> TYPE = MapDecorationTypes.RED_MARKER;

	private final BlockPos source;
	private final BlockPos target;
	private final Component name;
	private final String id;

	public StationMarker(BlockPos source, BlockPos target, Component name) {
		this.source = source;
		this.target = target;
		this.name = name;
		id = "create:station-" + target.getX() + "," + target.getY() + "," + target.getZ();
	}

	public static StationMarker load(CompoundTag tag) {
		BlockPos source = NbtFixer.readBlockPos(tag, "source");
		BlockPos target = NbtFixer.readBlockPos(tag, "target");
		Component name = Component.Serializer.fromJson(tag.getString("name"), Create.getRegistryAccess());
		if (name == null) name = Components.immutableEmpty();

		return new StationMarker(source, target, name);
	}

	public static StationMarker fromWorld(BlockGetter level, BlockPos pos) {
		Optional<StationBlockEntity> stationOption = AllBlockEntityTypes.TRACK_STATION.get(level, pos);

		if (stationOption.isEmpty() || stationOption.get().getStation() == null)
			return null;

		String name = stationOption.get()
			.getStation().name;
		return new StationMarker(pos, BlockEntityBehaviour.get(stationOption.get(), TrackTargetingBehaviour.TYPE)
			.getPositionForMapMarker(), Components.literal(name));
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.put("source", NbtUtils.writeBlockPos(source));
		tag.put("target", NbtUtils.writeBlockPos(target));
		tag.putString("name", Component.Serializer.toJson(name, Create.getRegistryAccess()));

		return tag;
	}

	public BlockPos getSource() {
		return source;
	}

	public BlockPos getTarget() {
		return target;
	}

	public Component getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StationMarker that = (StationMarker) o;

		if (!target.equals(that.target)) return false;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(target, name);
	}

	public static class Decoration extends MapDecoration implements CustomRenderedMapDecoration {
		private static final ResourceLocation TEXTURE = Create.asResource("textures/gui/station_map_icon.png");

		public Decoration(byte x, byte y, Component name) {
			super(TYPE, x, y, (byte) 0, Optional.ofNullable(name));
		}

		public static Decoration from(MapDecoration decoration) {
			return new StationMarker.Decoration(decoration.x(), decoration.y(), decoration.name().get());
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource bufferSource, boolean active, int packedLight, MapItemSavedData mapData, int index) {
			poseStack.pushPose();

			poseStack.translate(x() / 2D + 64.0, y() / 2D + 64.0, -0.02D);

			poseStack.pushPose();

			poseStack.translate(0.5f, 0f, 0);
			poseStack.scale(4.5F, 4.5F, 3.0F);

			VertexConsumer buffer = bufferSource.getBuffer(RenderType.text(TEXTURE));
			Matrix4f mat = poseStack.last().pose();
			float zOffset = -0.001f;
			buffer.addVertex(mat, -1, -1, zOffset * index).setColor(255, 255, 255, 255).setUv(0.0f		, 0.0f		 ).setLight(packedLight);
			buffer.addVertex(mat, -1,  1, zOffset * index).setColor(255, 255, 255, 255).setUv(0.0f		, 0.0f + 1.0f).setLight(packedLight);
			buffer.addVertex(mat,  1,  1, zOffset * index).setColor(255, 255, 255, 255).setUv(0.0f + 1.0f, 0.0f + 1.0f).setLight(packedLight);
			buffer.addVertex(mat,  1, -1, zOffset * index).setColor(255, 255, 255, 255).setUv(0.0f + 1.0f, 0.0f		 ).setLight(packedLight);

			poseStack.popPose();

			if (name() != null) {
				Font font = Minecraft.getInstance().font;
				Component component = name().get();
				float f6 = (float)font.width(component);
//				float f7 = Mth.clamp(25.0F / f6, 0.0F, 6.0F / 9.0F);
				poseStack.pushPose();
//				poseStack.translate((double)(0.0F + (float)getX() / 2.0F + 64.0F / 2.0F), (double)(0.0F + (float)getY() / 2.0F + 64.0F + 4.0F), (double)-0.025F);
				poseStack.translate(0, 6.0D, -0.005F);

				poseStack.scale(0.8f, 0.8f, 1.0F);
				poseStack.translate(-f6 / 2f + .5f, 0, 0);
//				poseStack.scale(f7, f7, 1.0F);
				font.drawInBatch(component, 0.0F, 0.0F, -1, false, poseStack.last()
					.pose(), bufferSource, Font.DisplayMode.NORMAL, Integer.MIN_VALUE, packedLight);
				poseStack.popPose();
			}

			poseStack.popPose();
		}
	}
}
