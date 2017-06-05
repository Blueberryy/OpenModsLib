package openmods.model;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

public class ModelTextureMap {

	private final Map<String, ResourceLocation> textures;

	public ModelTextureMap() {
		this(ImmutableMap.<String, ResourceLocation> of());
	}

	private ModelTextureMap(Map<String, ResourceLocation> textures) {
		this.textures = ImmutableMap.copyOf(textures);
	}

	public Collection<ResourceLocation> getTextures() {
		return textures.values();
	}

	public Optional<ModelTextureMap> update(Map<String, String> updates) {
		if (updates.isEmpty()) return Optional.absent();

		final Map<String, ResourceLocation> newTextures = Maps.newHashMap(this.textures);

		for (Map.Entry<String, String> e : updates.entrySet()) {
			final String location = e.getValue();
			if (Strings.isNullOrEmpty(location)) {
				newTextures.remove(e.getKey());
			} else {
				newTextures.put(e.getKey(), new ResourceLocation(location));
			}
		}

		return Optional.of(new ModelTextureMap(newTextures));
	}

	public Iterable<TextureAtlasSprite> bake(Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		final Set<ResourceLocation> texturesToGet = Sets.newHashSet(textures.values());
		return Iterables.transform(texturesToGet, bakedTextureGetter);
	}

	public Map<String, TextureAtlasSprite> bakeWithKeys(Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		return Maps.transformValues(textures, bakedTextureGetter);
	}
}
