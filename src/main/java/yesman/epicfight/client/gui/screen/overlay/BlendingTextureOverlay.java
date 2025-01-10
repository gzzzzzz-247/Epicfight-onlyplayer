package yesman.epicfight.client.gui.screen.overlay;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlendingTextureOverlay extends OverlayManager.Overlay {
	public ResourceLocation texture;
	private boolean isAlive = true;
	
	public BlendingTextureOverlay(ResourceLocation texture) {
		this.texture = texture;
	}
	
	public void remove() {
		this.isAlive = false;
	}
	
	@Override
	public boolean render(int xResolution, int yResolution) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, this.texture);
		GlStateManager._enableBlend();
		GlStateManager._disableDepthTest();
		GlStateManager._blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		Tesselator tessellator = Tesselator.getInstance();
	    BufferBuilder bufferbuilder = tessellator.getBuilder();
	    bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
	    bufferbuilder.vertex(0, 0, 1).uv(0, 0).endVertex();
	    bufferbuilder.vertex(0, yResolution, 1).uv(0, 1).endVertex();
	    bufferbuilder.vertex(xResolution, yResolution, 1).uv(1, 1).endVertex();
	    bufferbuilder.vertex(xResolution, 0, 1).uv(1, 0).endVertex();
	    tessellator.end();
	    
	    return !this.isAlive;
	}
}