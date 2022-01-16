package flaxbeard.immersivepetroleum.common.lubehandlers;

import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.common.blocks.metal.CrusherBlockEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcess;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler.ILubricationHandler;
import flaxbeard.immersivepetroleum.client.model.IPModel;
import flaxbeard.immersivepetroleum.client.model.IPModels;
import flaxbeard.immersivepetroleum.client.model.ModelLubricantPipes;
import flaxbeard.immersivepetroleum.common.IPContent.Fluids;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.AutoLubricatorTileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;
import java.util.function.Supplier;

public class CrusherLubricationHandler implements ILubricationHandler<CrusherBlockEntity>{
	private static Vec3i size = new Vec3i(3, 3, 5);
	
	@Override
	public Vec3i getStructureDimensions(){
		return size;
	}
	
	@Override
	public boolean isMachineEnabled(Level world, CrusherBlockEntity mbte){
		return mbte.shouldRenderAsActive();
	}
	
	@Override
	public BlockEntity isPlacedCorrectly(Level world, AutoLubricatorTileEntity lubricator, Direction facing){
		BlockPos target = lubricator.getBlockPos().offset(facing.getNormal());
		BlockEntity te = world.getBlockEntity(target);
		
		if(te instanceof CrusherBlockEntity){
			CrusherBlockEntity master = ((CrusherBlockEntity) te).master();
			
			if(master != null && master.getFacing().getOpposite() == facing){
				return master;
			}
		}
		
		return null;
	}
	
	@Override
	public void lubricate(Level world, int ticks, CrusherBlockEntity mbte){
		if(world.isClientSide){
			Iterator<MultiblockProcess<CrusherRecipe>> processIterator = mbte.processQueue.iterator();
			MultiblockProcess<CrusherRecipe> process = processIterator.next();
			
			if(ticks % 4 == 0){
				int consume = mbte.energyStorage.extractEnergy(process.energyPerTick, true);
				if(consume >= process.energyPerTick){
					mbte.energyStorage.extractEnergy(process.energyPerTick, false);
					
					if(process.processTick < process.maxTicks)
						process.processTick++;
					
					if(process.processTick >= process.maxTicks && mbte.processQueue.size() > 1){
						process = processIterator.next();
						
						if(process.processTick < process.maxTicks)
							process.processTick++;
					}
				}
			}
		}else{
			mbte.animation_barrelRotation += 18f / 4f;
			mbte.animation_barrelRotation %= 360f;
		}
	}
	
	@Override
	public void spawnLubricantParticles(Level world, AutoLubricatorTileEntity lubricator, Direction facing, CrusherBlockEntity mbte){
		Direction f = mbte.getIsMirrored() ? facing : facing.getOpposite();
		
		float location = world.random.nextFloat();
		
		boolean flip = f.getAxis() == Direction.Axis.Z ^ facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ^ !mbte.getIsMirrored();
		float xO = 2.5F;
		float zO = -0.1F;
		float yO = 1.3F;
		
		if(location > .5F){
			xO = 1.0F;
			yO = 3.0F;
			zO = 1.65F;
		}
		
		if(facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
			xO = -xO + 1;
		if(!flip)
			zO = -zO + 1;
		
		float x = lubricator.getBlockPos().getX() + (f.getAxis() == Direction.Axis.X ? xO : zO);
		float y = lubricator.getBlockPos().getY() + yO;
		float z = lubricator.getBlockPos().getZ() + (f.getAxis() == Direction.Axis.X ? zO : xO);
		
		for(int i = 0;i < 3;i++){
			float r1 = (world.random.nextFloat() - .5F) * 2F;
			float r2 = (world.random.nextFloat() - .5F) * 2F;
			float r3 = world.random.nextFloat();
			BlockState n = Fluids.lubricant.block.defaultBlockState();
			world.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, n), x, y, z, r1 * 0.04F, r3 * 0.0125F, r2 * 0.025F);
		}
	}
	
	@Override
	public Tuple<BlockPos, Direction> getGhostBlockPosition(Level world, CrusherBlockEntity mbte){
		if(!mbte.isDummy()){
			BlockPos pos = mbte.getBlockPos().relative(mbte.getFacing(), 2);
			Direction f = mbte.getFacing().getOpposite();
			return new Tuple<BlockPos, Direction>(pos, f);
		}
		return null;
	}
	
	private static final ResourceLocation TEXTURE = new ResourceLocation(ImmersivePetroleum.MODID, "textures/models/lube_pipe.png");
	private static Supplier<IPModel> pipes;
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void renderPipes(AutoLubricatorTileEntity lubricator, CrusherBlockEntity mbte, PoseStack matrix, MultiBufferSource buffer, int combinedLight, int combinedOverlay){
		matrix.translate(0, -1, 0);
		Vec3i offset = mbte.getBlockPos().subtract(lubricator.getBlockPos());
		matrix.translate(offset.getX(), offset.getY(), offset.getZ());
		
		Direction rotation = mbte.getFacing();
		switch(rotation){
			case NORTH:{
				matrix.mulPose(new Quaternion(0, 90F, 0, true));
				matrix.translate(-1, 0, 0);
				break;
			}
			case SOUTH:{
				matrix.mulPose(new Quaternion(0, 270F, 0, true));
				matrix.translate(0, 0, -1);
				break;
			}
			case EAST:{
				matrix.translate(0, 0, 0);
				break;
			}
			case WEST:{
				matrix.mulPose(new Quaternion(0, 180F, 0, true));
				matrix.translate(-1, 0, -1);
				break;
			}
			default:
				break;
		}
		
		if(pipes == null)
			pipes = IPModels.getSupplier(ModelLubricantPipes.Crusher.ID);
		
		IPModel model;
		if((model = pipes.get()) != null){
			model.renderToBuffer(matrix, buffer.getBuffer(model.renderType(TEXTURE)), combinedLight, combinedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
		}
	}
}
