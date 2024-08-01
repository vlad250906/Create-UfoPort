package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class CurvedTrackInteraction {

	static final int breakerId = new Object().hashCode();

	static int breakTicks;
	static int breakTimeout;
	static float breakProgress;
	static BlockPos breakPos;

	public static void clientTick() {
		BezierPointSelection result = TrackBlockOutline.result;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;

		if (!player.getAbilities().mayBuild)
			return;

		if (mc.options.keyAttack.isDown() && result != null) {
			breakPos = result.blockEntity()
				.getBlockPos();
			BlockState blockState = level.getBlockState(breakPos);
			if (blockState.isAir()) {
				resetBreakProgress();
				return;
			}

			if (breakTicks % 4.0F == 0.0F) {
				SoundType soundtype = blockState.getSoundType();
				mc.getSoundManager()
					.play(new SimpleSoundInstance(soundtype.getHitSound(), SoundSource.BLOCKS,
						(soundtype.getVolume() + 1.0F) / 8.0F, soundtype.getPitch() * 0.5F,
						level.random, BlockPos.containing(result.vec())));
			}

			boolean creative = player.getAbilities().instabuild;

			breakTicks++;
			breakTimeout = 2;
			breakProgress += creative ? 0.125f : blockState.getDestroyProgress(player, level, breakPos) / 8f;

			Vec3 vec = VecHelper.offsetRandomly(result.vec(), level.random, 0.25f);
			level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState), vec.x, vec.y, vec.z, 0, 0, 0);

			int progress = (int) (breakProgress * 10.0F) - 1;
			level.destroyBlockProgress(player.getId(), breakPos, progress);
			player.swing(InteractionHand.MAIN_HAND);

			if (breakProgress >= 1) {
				AllPackets.getChannel().sendToServer(new CurvedTrackDestroyPacket(breakPos, result.loc()
					.curveTarget(), BlockPos.containing(result.vec()), false));
				resetBreakProgress();
			}

			return;
		}

		if (breakTimeout == 0)
			return;
		if (--breakTimeout > 0)
			return;

		resetBreakProgress();
	}

	private static void resetBreakProgress() {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;

		if (breakPos != null && level != null)
			level.destroyBlockProgress(mc.player.getId(), breakPos, -1);

		breakProgress = 0;
		breakTicks = 0;
		breakPos = null;
	}

	public static boolean onClickInput(boolean isUse, boolean isAttack) {
		BezierPointSelection result = TrackBlockOutline.result;
		if (result == null)
			return false;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;

		if (player == null || level == null)
			return false;

		if (isUse) {
			ItemStack heldItem = player.getMainHandItem();
			Item item = heldItem.getItem();
			if (AllTags.AllBlockTags.TRACKS.matches(heldItem)) {
				player.displayClientMessage(Lang.translateDirect("track.turn_start")
					.withStyle(ChatFormatting.RED), true);
				player.swing(InteractionHand.MAIN_HAND);
				return true;
			}
			if (item instanceof TrackTargetingBlockItem ttbi && ttbi.useOnCurve(result, heldItem)) {
				player.swing(InteractionHand.MAIN_HAND);
				return true;
			}
			if (AllItems.WRENCH.isIn(heldItem) && player.isShiftKeyDown()) {
				AllPackets.getChannel()
					.sendToServer(new CurvedTrackDestroyPacket(result.blockEntity()
						.getBlockPos(),
						result.loc()
							.curveTarget(),
						BlockPos.containing(result.vec()), true));
				resetBreakProgress();
				player.swing(InteractionHand.MAIN_HAND);
				return true;
			}
		}

		if (isAttack)
			return true;

		return false;
	}

}
