package io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;

@Environment(EnvType.CLIENT)
@Mixin(Screen.class)
public interface TitleScreenAccessor {
	@Accessor("PANORAMA")
	public static PanoramaRenderer port_lib$getPanorama() {
		throw new AssertionError("port_lib$getPanorama mixin failed ;(");
	}
}
