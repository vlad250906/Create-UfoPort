package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin {
	
	@ModifyVariable(method = "getU(F)F", at = @At("HEAD"), ordinal = 0)
	private float create$getU(float y) {
		if(y > 1f) {
			//System.out.println("getU() with y > 1f!!!!!!");
			//try {
				//int a = 1 / 0;
				//System.out.println(a);
			//}catch(Exception e) {
				//e.printStackTrace();
			//}
		}
		return y;
	}
	
}
