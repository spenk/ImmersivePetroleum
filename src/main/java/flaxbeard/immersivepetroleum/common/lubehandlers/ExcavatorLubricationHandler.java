package flaxbeard.immersivepetroleum.common.lubehandlers;

import blusunrize.immersiveengineering.common.blocks.metal.BucketWheelBlockEntity;
import blusunrize.immersiveengineering.common.blocks.metal.ExcavatorBlockEntity;
import blusunrize.immersiveengineering.common.config.IEServerConfig;
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

import java.util.function.Supplier;

public class ExcavatorLubricationHandler implements ILubricationHandler<ExcavatorBlockEntity>{
	private static Vec3i size = new Vec3i(3, 6, 3);

	@Override
	public Vec3i getStructureDimensions(){
		return size;
	}
	
	@Override
	public boolean isMachineEnabled(Level world, ExcavatorBlockEntity mbte){
		BlockPos wheelPos = mbte.getWheelCenterPos();
		BlockEntity center = world.getBlockEntity(wheelPos);
		
		if(center instanceof BucketWheelBlockEntity){
			BucketWheelBlockEntity wheel = (BucketWheelBlockEntity) center;
			if(!wheel.offsetToMaster.equals(BlockPos.ZERO)){
				// Just to make absolutely sure it's the master
				wheel = wheel.master();
			}
			
			return wheel.active;
		}
		return false;
	}
	
	@Override
	public BlockEntity isPlacedCorrectly(Level world, AutoLubricatorTileEntity lubricator, Direction facing){
		BlockPos target = lubricator.getBlockPos().offset(facing.getNormal());
		BlockEntity te = world.getBlockEntity(target);
		
		if(te instanceof ExcavatorBlockEntity){
			ExcavatorBlockEntity master = ((ExcavatorBlockEntity) te).master();
			
			if(master != null){
				Direction dir = master.getIsMirrored() ? master.getFacing().getClockWise() : master.getFacing().getCounterClockWise();
				if(dir == facing){
					return master;
				}
			}
		}
		
		return null;
	}
	
	@Override
	public void lubricate(Level world, int ticks, ExcavatorBlockEntity mbte){
		BlockPos wheelPos = mbte.getWheelCenterPos();
		BlockEntity center = world.getBlockEntity(wheelPos);
		
		if(center instanceof BucketWheelBlockEntity){
			BucketWheelBlockEntity wheel = (BucketWheelBlockEntity) center;
			if(!wheel.offsetToMaster.equals(BlockPos.ZERO)){
				// Just to make absolutely sure it's the master
				wheel = wheel.master();
			}
			
			if(world.isClientSide && ticks % 4 == 0){
				int consumed = IEServerConfig.MACHINES.excavator_consumption.get();
				int extracted = mbte.energyStorage.extractEnergy(consumed, true);
				if(extracted >= consumed){
					mbte.energyStorage.extractEnergy(extracted, false);
					wheel.rotation += IEServerConfig.MACHINES.excavator_speed.get() / 4F;
				}
			}else{
				wheel.rotation += IEServerConfig.MACHINES.excavator_speed.get() / 4F;
			}
		}
	}

	@Override
	public void spawnLubricantParticles(Level world, AutoLubricatorTileEntity lubricator, Direction facing, ExcavatorBlockEntity mbte){
		Direction f = mbte.getIsMirrored() ? facing : facing.getOpposite();
		
		float location = world.random.nextFloat();
		
		boolean flip = f.getAxis() == Direction.Axis.Z ^ facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ^ !mbte.getIsMirrored();
		float xO = 1.2F;
		float zO = -.5F;
		float yO = .5F;
		
		if(location > .5F){
			xO = 0.9F;
			yO = 0.8F;
			zO = 1.75F;
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
	public Tuple<BlockPos, Direction> getGhostBlockPosition(Level world, ExcavatorBlockEntity mbte){
		if(!mbte.isDummy()){
			BlockPos pos = mbte.getBlockPos()
					.relative(mbte.getFacing(), 4)
					.relative(mbte.getIsMirrored() ? mbte.getFacing().getCounterClockWise() : mbte.getFacing().getClockWise(), 2);
			Direction f = mbte.getIsMirrored() ? mbte.getFacing().getClockWise() : mbte.getFacing().getCounterClockWise();
			return new Tuple<BlockPos, Direction>(pos, f);
		}
		return null;
	}
	
	private static final ResourceLocation TEXTURE = new ResourceLocation(ImmersivePetroleum.MODID, "textures/models/lube_pipe.png");
	private static Supplier<IPModel> pipes_normal;
	private static Supplier<IPModel> pipes_mirrored;
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void renderPipes(AutoLubricatorTileEntity lubricator, ExcavatorBlockEntity mbte, PoseStack matrix, MultiBufferSource buffer, int combinedLight, int combinedOverlay){
		matrix.translate(0, -1, 0);
		Vec3i offset = mbte.getBlockPos().subtract(lubricator.getBlockPos());
		matrix.translate(offset.getX(), offset.getY(), offset.getZ());
		
		Direction rotation = mbte.getFacing();
		switch (rotation) {
			case NORTH -> {
				matrix.mulPose(new Quaternion(0, 90F, 0, true));
				matrix.translate(-1, 0, -1);
			}
			case SOUTH -> {
				matrix.mulPose(new Quaternion(0, 270F, 0, true));
				matrix.translate(0, 0, -2);
			}
			case EAST -> {
				matrix.translate(0, 0, -1);
			}
			case WEST -> {
				matrix.mulPose(new Quaternion(0, 180F, 0, true));
				matrix.translate(-1, 0, -2);
			}
			default -> {
			}
		}
		
		IPModel model = null;
		if(mbte.getIsMirrored()){
			if(pipes_mirrored == null)
				pipes_mirrored = IPModels.getSupplier(ModelLubricantPipes.Excavator.ID_MIRRORED);
			
			model = pipes_mirrored.get();
		}else{
			if(pipes_normal == null)
				pipes_normal = IPModels.getSupplier(ModelLubricantPipes.Excavator.ID_NORMAL);
			
			model = pipes_normal.get();
		}
		
		if(model != null){
			model.renderToBuffer(matrix, buffer.getBuffer(model.renderType(TEXTURE)), combinedLight, combinedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
		}
	}
}
