package flaxbeard.immersivepetroleum.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.DistillationTowerTileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = ImmersivePetroleum.MODID, bus = Bus.MOD)
public class MultiblockDistillationTowerRenderer implements BlockEntityRenderer<DistillationTowerTileEntity> {
	
	public boolean shouldRenderOffScreen(DistillationTowerTileEntity te){
		return true;
	}
	
	public void render(DistillationTowerTileEntity te, float partialTicks, PoseStack transform, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn){
		if(te != null && te.formed && !te.isDummy()){
			if(te.shouldRenderAsActive()){
				combinedOverlayIn = OverlayTexture.NO_OVERLAY;
				
				transform.pushPose();
				{
					Direction rotation = te.getFacing();
					switch(rotation){
						case NORTH:{
							// transform.rotate(new Quaternion(0, 0, 0, true));
							transform.translate(3, 0, 4);
							break;
						}
						case SOUTH:{
							transform.mulPose(new Quaternion(0F, 180F, 0F, true));
							transform.translate(2, 0, 3);
							break;
						}
						case EAST:{
							transform.mulPose(new Quaternion(0, 270F, 0, true));
							transform.translate(3, 0, 3);
							break;
						}
						case WEST:{
							transform.mulPose(new Quaternion(0, 90F, 0, true));
							transform.translate(2, 0, 4);
							break;
						}
						default:
							break;
					}
					
					float br = 0.75F; // "Brightness"
					
					// Is it the most efficient way of doing this? Probably not.
					// Does it make me look smart af? hell yeah..
					VertexConsumer buf = bufferIn.getBuffer(IPRenderTypes.DISTILLATION_TOWER_ACTIVE);
					if(te.getIsMirrored()){
						transform.pushPose();
						{
							transform.translate(-4.0, 0.0, -4.0);
							Matrix4f mat = transform.last().pose();
							
							// Active Boiler Front
							int ux=96, vy=134;
							int w=32, h=24;
							float uw=w/256F, vh=h/256F, u0=ux/256F, v0=vy/256F, u1=u0+uw, v1=v0+vh;
							
							buf.vertex(mat, -0.0015F, 0.5F, w/16F)			.color(br,br,br, 1.0F).uv(u1, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, -0.0015F, 0.5F+h/16F, w/16F)	.color(br,br,br, 1.0F).uv(u1, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, -0.0015F, 0.5F+h/16F, 0.0F)	.color(br,br,br, 1.0F).uv(u0, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, -0.0015F, 0.5F, 0.0F)			.color(br,br,br, 1.0F).uv(u0, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							
							// Active Boiler Back
							ux=96; vy=158;
							w=32; h=24;
							uw=w/256F; vh=h/256F; u0=ux/256F; v0=vy/256F; u1=u0+uw; v1=v0+vh;
							
							buf.vertex(mat, 1.0015F, 0.5F+h/16F, 0.0F)		.color(br,br,br, 1.0F).uv(u1, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, 1.0015F, 0.5F+h/16F, w/16F)	.color(br,br,br, 1.0F).uv(u0, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, 1.0015F, 0.5F, w/16F)			.color(br,br,br, 1.0F).uv(u0, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, 1.0015F, 0.5F, 0.0F)			.color(br,br,br, 1.0F).uv(u1, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							
							// Active Boiler Side
							ux=80; vy=134;
							w=16; h=24;
							uw=w/256F; vh=h/256F; u0=ux/256F; v0=vy/256F; u1=u0+uw; v1=v0+vh;
							
							buf.vertex(mat, w/16F, 0.5F, 2.0015F)			.color(br,br,br, 1.0F).uv(u1, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, w/16F, 0.5F+h/16F, 2.0015F)	.color(br,br,br, 1.0F).uv(u1, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, 0.0F, 0.5F+h/16F, 2.0015F)		.color(br,br,br, 1.0F).uv(u0, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, 0.0F, 0.5F, 2.0015F)			.color(br,br,br, 1.0F).uv(u0, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
						}
						transform.popPose();
						
					}else{
						transform.pushPose();
						{
							transform.translate(-2.0, 0.0, -4.0);
							Matrix4f mat = transform.last().pose();
							
							// Active Boiler Back
							int ux=96, vy=158;
							int w=32, h=24;
							float uw=w/256F, vh=h/256F, u0=ux/256F, v0=vy/256F, u1=u0+uw, v1=v0+vh;
							
							buf.vertex(mat, -0.0015F, 0.5F, w/16F)			.color(br,br,br, 1.0F).uv(u0, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, -0.0015F, 0.5F+h/16F, w/16F)	.color(br,br,br, 1.0F).uv(u0, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, -0.0015F, 0.5F+h/16F, 0.0F)	.color(br,br,br, 1.0F).uv(u1, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, -0.0015F, 0.5F, 0.0F)			.color(br,br,br, 1.0F).uv(u1, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							
							// Active Boiler Front
							ux=96; vy=134;
							w=32; h=24;
							uw=w/256F; vh=h/256F; u0=ux/256F; v0=vy/256F; u1=u0+uw; v1=v0+vh;
							
							buf.vertex(mat, 1.0015F, 0.5F+h/16F, 0.0F)		.color(br,br,br, 1.0F).uv(u0, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, 1.0015F, 0.5F+h/16F, w/16F)	.color(br,br,br, 1.0F).uv(u1, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, 1.0015F, 0.5F, w/16F)			.color(br,br,br, 1.0F).uv(u1, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, 1.0015F, 0.5F, 0.0F)			.color(br,br,br, 1.0F).uv(u0, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							
							// Active Boiler Side
							ux=80; vy=134;
							w=16; h=24;
							uw=w/256F; vh=h/256F; u0=ux/256F; v0=vy/256F; u1=u0+uw; v1=v0+vh;
							
							buf.vertex(mat, w/16F, 0.5F, 2.0015F)			.color(br,br,br, 1.0F).uv(u0, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, w/16F, 0.5F+h/16F, 2.0015F)	.color(br,br,br, 1.0F).uv(u0, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, 0.0F, 0.5F+h/16F, 2.0015F)		.color(br,br,br, 1.0F).uv(u1, v0).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
							buf.vertex(mat, 0.0F, 0.5F, 2.0015F)			.color(br,br,br, 1.0F).uv(u1, v1).overlayCoords(combinedOverlayIn).uv2(combinedLightIn).normal(1, 1, 1).endVertex();
						}
						transform.popPose();
					}
				}
				transform.popPose();
			}
		}
	}
}
