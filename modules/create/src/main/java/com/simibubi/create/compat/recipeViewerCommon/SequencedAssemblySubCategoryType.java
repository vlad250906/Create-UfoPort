package com.simibubi.create.compat.recipeViewerCommon;

import com.simibubi.create.compat.jei.category.sequencedAssembly.JeiSequencedAssemblySubCategory;

import java.util.function.Supplier;

public record SequencedAssemblySubCategoryType(Supplier<Supplier<JeiSequencedAssemblySubCategory>> jei) {

	public static final SequencedAssemblySubCategoryType PRESSING = new SequencedAssemblySubCategoryType(
			() -> JeiSequencedAssemblySubCategory.AssemblyPressing::new
	);
	public static final SequencedAssemblySubCategoryType SPOUTING = new SequencedAssemblySubCategoryType(
			() -> JeiSequencedAssemblySubCategory.AssemblySpouting::new
	);
	public static final SequencedAssemblySubCategoryType DEPLOYING = new SequencedAssemblySubCategoryType(
			() -> JeiSequencedAssemblySubCategory.AssemblyDeploying::new
	);
	public static final SequencedAssemblySubCategoryType CUTTING = new SequencedAssemblySubCategoryType(
			() -> JeiSequencedAssemblySubCategory.AssemblyCutting::new
	);
}
