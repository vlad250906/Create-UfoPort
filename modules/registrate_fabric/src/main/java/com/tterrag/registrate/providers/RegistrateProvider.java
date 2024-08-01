package com.tterrag.registrate.providers;

import net.minecraft.data.DataProvider;

import net.fabricmc.api.EnvType;

public interface RegistrateProvider extends DataProvider {
    
    EnvType getSide();
}
