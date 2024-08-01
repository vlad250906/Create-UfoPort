package io.github.fabricators_of_create.porting_lib.tags.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Generate tag translations given a class.
 * The class is expected to contain static fields holding TagKeys of Items.
 */
public class ItemTagLangProvider extends FabricLanguageProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(ItemTagLangProvider.class);
	private final Class<?> tagClass;
	private final Map<TagKey<Item>, String> specialCases;

	public ItemTagLangProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup, Class<?> tagClass) {
		this(output, registryLookup, tagClass, Map.of());
	}

	public ItemTagLangProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup, Class<?> tagClass, Map<TagKey<Item>, String> specialCases) {
		super(output, registryLookup);
		this.tagClass = tagClass;
		this.specialCases = specialCases;
	}

	@Override
	public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder translationBuilder) {
		for (Field field : tagClass.getDeclaredFields()) {
			field.setAccessible(true);
			try {
				Object o = field.get(null);
				if (o instanceof TagKey<?> tag) {
					ResourceLocation id = tag.location();
					String path = id.getPath().replaceAll("/", ".");
					String key = "tag.item.%s.%s".formatted(id.getNamespace(), path);

					String english = specialCases.containsKey(tag)
							? specialCases.get(tag)
							: toEnglish(id.getPath());

					translationBuilder.add(key, english);
					LOGGER.info("Translated tag [{}] as [{}] with key [{}]", id, english, key);
				}
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static String toEnglish(String path) {
		// crops/nether_wart
		String[] sections = path.split("/");
		// "crops", "nether_wart"
		StringBuilder result = new StringBuilder();
		for (int i = sections.length - 1; i >= 0; i--) {
			String section = sections[i];
			result.append(toSentence(section)).append(" ");
		}
		return result.toString().trim();
	}

	private static String toSentence(String path) {
		return Arrays.stream(path.split("_"))
				.map(ItemTagLangProvider::uppercase)
				.collect(Collectors.joining(" "));
	}

	private static String uppercase(String word) {
		char first = word.charAt(0);
		char upper = Character.toUpperCase(first);
		return upper + word.substring(1);
	}
}