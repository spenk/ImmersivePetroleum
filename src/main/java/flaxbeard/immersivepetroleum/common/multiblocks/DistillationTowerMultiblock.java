package flaxbeard.immersivepetroleum.common.multiblocks;

import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPContent.Multiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class DistillationTowerMultiblock extends IETemplateMultiblock{
	public static final DistillationTowerMultiblock INSTANCE = new DistillationTowerMultiblock();
	
	private DistillationTowerMultiblock(){
		super(new ResourceLocation(ImmersivePetroleum.MODID, "multiblocks/distillationtower"),
				new BlockPos(2, 0, 2), new BlockPos(0, 1, 3), new BlockPos(4, 16, 4), Multiblock.distillationtower);
	}
	
	@Override
	public float getManualScale(){
		return 6;
	}
	
/*	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean canRenderFormedStructure(){
		return true;
	}
	
	@OnlyIn(Dist.CLIENT)
	private static ItemStack renderStack;
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void renderFormedStructure(PoseStack transform, MultiBufferSource buffer){
		if(renderStack == null)
			renderStack = new ItemStack(Multiblock.distillationtower);
		
		// "Undo" the GUI Perspective Transform
		transform.translate(2.5, 0.5, 2.5);
		
		ClientUtils.mc().getItemRenderer().renderStatic(
				renderStack,
				ItemTransforms.TransformType.NONE,
				0xf000f0,
				OverlayTexture.NO_OVERLAY,
				transform,
				buffer,
				0);
	}*/
}
