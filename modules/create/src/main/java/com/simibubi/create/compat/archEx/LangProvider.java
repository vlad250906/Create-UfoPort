package com.simibubi.create.compat.archEx;

import java.util.List;
import java.util.function.BiConsumer;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public record LangProvider(String modId, List<ArchExGroup> groups, BiConsumer<String, String> langConsumer) {
	public static final String LANG_PREFIX = "architecture_extensions.grouped_block.";
	public void run() {
		groups.forEach(group -> {
			String key = LANG_PREFIX + modId + '.' + group.name();
			String translated = RegistrateLangProvider.toEnglishName(group.name());
			langConsumer.accept(key, translated);
		});
	}
}
