package flaxbeard.immersivepetroleum.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.OptionalDouble;

public class IPRenderTypes{
	static final ResourceLocation activeTexture = new ResourceLocation(ImmersivePetroleum.MODID, "textures/multiblock/distillation_tower_active.png");
	
	/**
	 * Intended to only be used by {@link MultiblockDistillationTowerRenderer}
	 */
	public static final RenderType DISTILLATION_TOWER_ACTIVE;
	public static final RenderType TRANSLUCENT_LINES;
	
	static final RenderStateShard.TextureStateShard TEXTURE_ACTIVE_TOWER = new RenderStateShard.TextureStateShard(activeTexture, false, false);
	//static final RenderStateShard.ShaderStateShard SHADE_ENABLED = new RenderStateShard.ShaderStateShard(true);
	static final RenderStateShard.LightmapStateShard LIGHTMAP_ENABLED = new RenderStateShard.LightmapStateShard(true);
	static final RenderStateShard.OverlayStateShard OVERLAY_ENABLED = new RenderStateShard.OverlayStateShard(true);
	static final RenderStateShard.OverlayStateShard OVERLAY_DISABLED = new RenderStateShard.OverlayStateShard(false);
	static final RenderStateShard.DepthTestStateShard DEPTH_ALWAYS = new RenderStateShard.DepthTestStateShard("always", GL11.GL_ALWAYS);
	static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("translucent_transparency", () -> {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
	}, RenderSystem::disableBlend);
	static final RenderStateShard.TransparencyStateShard NO_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("no_transparency", () -> {
		RenderSystem.disableBlend();
	}, () -> {
	});
	static final RenderStateShard.DiffuseLightingState DIFFUSE_LIGHTING_ENABLED = new RenderStateShard.DiffuseLightingState(true);
	
	static{
		TRANSLUCENT_LINES = RenderType.create(
				ImmersivePetroleum.MODID+":translucent_lines",
				DefaultVertexFormat.POSITION_COLOR,
				VertexFormat.Mode.LINES,
				256,
				RenderType.State.getBuilder().transparency(TRANSLUCENT_TRANSPARENCY)
					.line(new LineState(OptionalDouble.of(3.5)))
					.texture(new TextureState())
					.depthTest(DEPTH_ALWAYS)
					.build(false)
		);
		
		DISTILLATION_TOWER_ACTIVE = RenderType.makeType(
				ImmersivePetroleum.MODID+":distillation_tower_active",
				DefaultVertexFormats.BLOCK,
				GL11.GL_QUADS,
				256,
				true,
				false,
				RenderType.State.getBuilder()
					.texture(TEXTURE_ACTIVE_TOWER)
					.shadeModel(SHADE_ENABLED)
					.lightmap(LIGHTMAP_ENABLED)
					.overlay(OVERLAY_DISABLED)
					.build(false)
		);
	}
	
	/** Same as vanilla, just without an overlay */
	public static RenderType getEntitySolid(ResourceLocation locationIn){
		RenderType.State RenderStateShard = RenderType.State.getBuilder()
				.texture(new RenderStateShard.TextureState(locationIn, false, false))
				.transparency(NO_TRANSPARENCY)
				.diffuseLighting(DIFFUSE_LIGHTING_ENABLED)
				.lightmap(LIGHTMAP_ENABLED)
				.overlay(OVERLAY_DISABLED)
				.build(true);
		return RenderType.makeType("entity_solid", DefaultVertexFormat.NEW_ENTITY, 7, 256, true, false, RenderStateShard);
	}
	
	public static MultiBufferSource disableLighting(MultiBufferSource in){
		return type -> {
			@SuppressWarnings("deprecation")
			RenderType rt = new RenderType(
					ImmersivePetroleum.MODID + ":" + type + "_no_lighting",
					type.getVertexFormat(),
					type.getDrawMode(),
					type.getBufferSize(),
					type.isUseDelegate(),
					false,
					() -> {
						type.setupRenderState();
						
						RenderSystem.disableLighting();
					}, () -> {
						type.clearRenderState();
					}){};
			return in.getBuffer(rt);
		};
	}
}
