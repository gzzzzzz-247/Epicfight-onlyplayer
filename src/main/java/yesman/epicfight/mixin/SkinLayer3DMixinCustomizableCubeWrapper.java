package yesman.epicfight.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.tr7zw.skinlayers.versionless.render.CustomizableCube;
import dev.tr7zw.skinlayers.versionless.util.Direction;

public abstract class SkinLayer3DMixinCustomizableCubeWrapper extends CustomizableCube {
	private SkinLayer3DMixinCustomizableCubeWrapper(int u, int v, float x, float y, float z, float sizeX, float sizeY,
			float sizeZ, float extraX, float extraY, float extraZ, boolean mirror, float textureWidth,
			float textureHeight, Direction[] hide, Direction[][] hideCorners) {
		super(u, v, x, y, z, sizeX, sizeY, sizeZ, extraX, extraY, extraZ, mirror, textureWidth, textureHeight, hide, hideCorners);
	}
	
	@Mixin(value = CustomizableCube.class)
	public interface SkinLayer3DMixinCustomModelCube {
		@Accessor
		public abstract CustomizableCube.Polygon[] getPolygons();
	}
}
