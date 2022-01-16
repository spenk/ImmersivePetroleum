package flaxbeard.immersivepetroleum.common.multiblocks;

import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPContent.Multiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class CokerUnitMultiblock extends IETemplateMultiblock {
	public static final CokerUnitMultiblock INSTANCE = new CokerUnitMultiblock();
	
	public CokerUnitMultiblock(){
		super(new ResourceLocation(ImmersivePetroleum.MODID, "multiblocks/cokerunit"),
				new BlockPos(4, 0, 2), new BlockPos(4, 1, 4), new BlockPos(9, 23, 5),
				Multiblock.cokerunit);
	}
	
	@Override
	public float getManualScale(){
		return 4.0F;
	}
	
/*	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean canRenderFormedStructure(){
		return true;
	}
	
	@OnlyIn(Dist.CLIENT)
	private static ItemStack renderStack;
	
	@Override
	public void renderFormedStructure(PoseStack transform, MultiBufferSource buffer){
		if(renderStack == null)
			renderStack = new ItemStack(Multiblock.cokerunit);
		
		// "Undo" the GUI Perspective Transform
		transform.translate(4.5, 0.5, 2.5);
		
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
