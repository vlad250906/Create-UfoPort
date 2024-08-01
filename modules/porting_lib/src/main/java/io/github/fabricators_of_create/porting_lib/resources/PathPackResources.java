package io.github.fabricators_of_create.porting_lib.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.FileUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;

/**
 * Defines a resource pack from an arbitrary Path.
 * <p>
 * This is primarily intended to support including optional resource packs inside a mod,
 * such as to have alternative textures to use along with Programmer Art, or optional
 * alternative recipes for compatibility ot to replace vanilla recipes.
 */
public class PathPackResources extends AbstractPackResources {
	private static final Logger LOGGER = LogUtils.getLogger();
	private final Path source;

	/**
	 * Constructs a java.nio.Path-based resource pack.
	 *
	 * @param packId the identifier of the pack.
	 * This identifier should be unique within the pack finder, preferably the name of the file or folder containing the resources.
	 * @param isBuiltin whether this pack resources should be considered builtin
	 * @param source the root path of the pack. This needs to point to the folder that contains "assets" and/or "data", not the asset folder itself!
	 */
	public PathPackResources(String packId, boolean isBuiltin, final Path source) {
		super(new PackLocationInfo(packId, Component.literal(source.getFileName().toString()), PackSource.DEFAULT, 
				Optional.empty()));
		this.source = source;
	}

	/**
	 * Returns the source path containing the resource pack.
	 * This is used for error display.
	 *
	 * @return the root path of the resources.
	 */
	public Path getSource() {
		return this.source;
	}

	/**
	 * Implement to return a file or folder path for the given set of path components.
	 *
	 * @param paths One or more path strings to resolve. Can include slash-separated paths.
	 * @return the resulting path, which may not exist.
	 */
	protected Path resolve(String... paths) {
		Path path = getSource();
		for (String name : paths)
			path = path.resolve(name);
		return path;
	}

	@Nullable
	@Override
	public IoSupplier<InputStream> getRootResource(String... paths) {
		final Path path = resolve(paths);
		if (!Files.exists(path))
			return null;

		return IoSupplier.create(path);
	}

	@Override
	public void listResources(PackType type, String namespace, String path, ResourceOutput resourceOutput) {
		List<String> parts = FileUtil.decomposePath(path).getOrThrow();
		net.minecraft.server.packs.PathPackResources.listPath(namespace, resolve(type.getDirectory(), namespace).toAbsolutePath(), parts, resourceOutput);
				//.ifRight(dataResult -> LOGGER.error("Invalid path {}: {}", path, dataResult.message()));
	}

	@Override
	public Set<String> getNamespaces(PackType type) {
		return getNamespacesFromDisk(type);
	}

	@NotNull
	private Set<String> getNamespacesFromDisk(final PackType type) {
		try {
			Path root = resolve(type.getDirectory());
			try (Stream<Path> walker = Files.walk(root, 1)) {
				return walker
						.filter(Files::isDirectory)
						.map(root::relativize)
						.filter(p -> p.getNameCount() > 0) // Skip the root entry
						.map(p -> p.toString().replaceAll("/$", "")) // Remove the trailing slash, if present
						.filter(s -> !s.isEmpty()) // Filter empty strings, otherwise empty strings default to minecraft namespace in ResourceLocations
						.collect(Collectors.toSet());
			}
		} catch (IOException e) {
			if (type == PackType.SERVER_DATA) { // We still have to add the resource namespace if client resources exist, as we load langs (which are in assets) on server
				return this.getNamespaces(PackType.CLIENT_RESOURCES);
			} else {
				return Collections.emptySet();
			}
		}
	}

	@Override
	public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
		return this.getRootResource(getPathFromLocation(location.getPath().startsWith("lang/") ? PackType.CLIENT_RESOURCES : type, location));
	}

	private static String[] getPathFromLocation(PackType type, ResourceLocation location) {
		String[] parts = location.getPath().split("/");
		String[] result = new String[parts.length + 2];
		result[0] = type.getDirectory();
		result[1] = location.getNamespace();
		System.arraycopy(parts, 0, result, 2, parts.length);
		return result;
	}

	@Override
	public void close() {}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s: %s (%s)", getClass().getName(), this.packId(), getSource());
	}

	@SuppressWarnings("deprecation")
	@NotNull
	public static PathPackResources createPackForMod(ModContainer mf) {
		return new PathPackResources(mf.getMetadata().getName(), false, mf.getRootPath()){
			@Nonnull
			@Override
			protected Path resolve(@Nonnull String... paths)
			{
				if (paths.length < 1) {
					throw new IllegalArgumentException("Missing path");
				}
				String path = String.join("/", paths);
				for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
					if (modContainer.findPath(path).isPresent())
						return modContainer.findPath(path).get();
				}

				return mf.findPath(path).orElseThrow();
			}
		};
	}
}
