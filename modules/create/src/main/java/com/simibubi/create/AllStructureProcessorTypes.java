package com.simibubi.create;

import com.simibubi.create.content.schematics.SchematicProcessor;

import io.github.fabricators_of_create.porting_lib.util.LazyRegistrar;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

public class AllStructureProcessorTypes {
	private static final LazyRegistrar<StructureProcessorType<?>> REGISTER = LazyRegistrar.create(Registries.STRUCTURE_PROCESSOR, Create.ID);

	public static final RegistryObject<StructureProcessorType<SchematicProcessor>> SCHEMATIC = REGISTER.register("schematic", () -> () -> SchematicProcessor.CODEC);

	public static void register() {
		REGISTER.register();
	}
}
