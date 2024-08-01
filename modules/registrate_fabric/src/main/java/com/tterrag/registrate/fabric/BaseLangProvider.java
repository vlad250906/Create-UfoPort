package com.tterrag.registrate.fabric;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup.Provider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BaseLangProvider extends FabricLanguageProvider {
	
	private final Map<String, String> entries = new HashMap<>();
	
	protected BaseLangProvider(FabricDataOutput dataOutput, CompletableFuture<Provider> registryLookup) {
		super(dataOutput, registryLookup);
	}
	
	protected BaseLangProvider(FabricDataOutput output, String languageCode, CompletableFuture<Provider> registryLookup) {
		super(output, languageCode, registryLookup);
	}


	@Override
	public void generateTranslations(Provider registryLookup, TranslationBuilder translationBuilder) {
		entries.forEach(translationBuilder::add);
	}

	public void add(String key, String value) {
		entries.put(key, value);
	}
}
