package flaxbeard.immersivepetroleum.client.render;

import blusunrize.immersiveengineering.client.utils.GuiHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler.ILubricationHandler;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.AutoLubricatorTileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;

public class AutoLubricatorRenderer implements BlockEntityRenderer<AutoLubricatorTileEntity> {
	
	@Override
	public boolean shouldRenderOffScreen(AutoLubricatorTileEntity te){
		return true;
	}

	@Override
	public void render(AutoLubricatorTileEntity te, float partialTicks, PoseStack transform, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
		if(te.isSlave) {
			return;
		}

		FluidStack fs = te.tank.getFluid();
		float level = 0;
		if(!fs.isEmpty()){
			level = fs.getAmount() / (float) te.tank.getCapacity();
		}

		if(level > 0){
			float height = 16;

			transform.pushPose();
			{
				float scale = 0.0625f;
				transform.translate(0.25, 0.875, 0.25);
				transform.scale(scale, scale, scale);

				VertexConsumer builder = bufferIn.getBuffer(RenderType.translucent());

				float h = height * level;
				GuiHelper.drawRepeatedFluidSprite(builder, transform, fs, 0, 0, 8, h);
				transform.mulPose(new Quaternion(0, 90, 0, true));
				transform.translate(-7.98, 0, 0);
				GuiHelper.drawRepeatedFluidSprite(builder, transform, fs, 0, 0, 8, h);
				transform.mulPose(new Quaternion(0, 90, 0, true));
				transform.translate(-7.98, 0, 0);
				GuiHelper.drawRepeatedFluidSprite(builder, transform, fs, 0, 0, 8, h);
				transform.mulPose(new Quaternion(0, 90, 0, true));
				transform.translate(-7.98, 0, 0);
				GuiHelper.drawRepeatedFluidSprite(builder, transform, fs, 0, 0, 8, h);
				if(h < height){
					transform.mulPose(new Quaternion(90, 0, 0, true));
					transform.translate(0, 0, -h);
					GuiHelper.drawRepeatedFluidSprite(builder, transform, fs, 0, 0, 8, 8);
				}
			}
			transform.popPose();
		}

		transform.pushPose();
		{
			BlockPos target = te.getBlockPos().offset(te.getFacing().getNormal());
			BlockEntity test = te.getLevel().getBlockEntity(target);

			ILubricationHandler<BlockEntity> handler = LubricatedHandler.getHandlerForTile(test);
			if(handler != null){
				BlockEntity master = handler.isPlacedCorrectly(te.getLevel(), te, te.getFacing());
				if(master != null){
					handler.renderPipes(te, master, transform, bufferIn, combinedLightIn, combinedOverlayIn);
				}
			}
		}
		transform.popPose();
	}
}
