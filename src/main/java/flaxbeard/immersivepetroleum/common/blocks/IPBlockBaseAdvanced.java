package flaxbeard.immersivepetroleum.common.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class IPBlockBaseAdvanced extends IPBlockBase{
	protected static IProperty<?>[] tempProperties;
	public final String name;
	public final IProperty<?>[] additionalProperties;
	boolean isHidden;
	boolean hasFlavour;
	protected List<BlockRenderLayer> renderLayers;
	protected int lightOpacity;
	protected PushReaction mobilityFlag;
	protected boolean canHammerHarvest;
	protected boolean notNormalBlock;
	
	public IPBlockBaseAdvanced(String name, Properties blockProps, @Nullable Class<? extends BlockItem> itemBlock, IProperty<?>... additionalProperties){
		super(name, setTempProperties(blockProps, additionalProperties));
		this.renderLayers = Collections.singletonList(BlockRenderLayer.SOLID);
		this.mobilityFlag = PushReaction.NORMAL;
		this.name = name;
		this.additionalProperties = (IProperty[]) Arrays.copyOf(tempProperties, tempProperties.length);
		this.setDefaultState(getInitDefaultState());
		if(itemBlock != null){
			try{
				Item item = (Item) itemBlock.getConstructor(Block.class, net.minecraft.item.Item.Properties.class).newInstance(this, (new net.minecraft.item.Item.Properties()).group(ImmersivePetroleum.creativeTab));
				item.setRegistryName(getRegistryName());
				IPContent.registeredIPItems.add(item);
			}catch(Exception var7){
				throw new RuntimeException(var7);
			}
		}
		
		this.lightOpacity = 15;
	}
	
	protected static Properties setTempProperties(Properties blockProps, Object[] additionalProperties){
		List<IProperty<?>> propList = new ArrayList<>();
		Object[] var3 = additionalProperties;
		int var4 = additionalProperties.length;
		
		for(int var5 = 0;var5 < var4;++var5){
			Object o = var3[var5];
			if(o instanceof IProperty){
				propList.add((IProperty<?>) o);
			}
			
			if(o instanceof IProperty[]){
				propList.addAll(Arrays.asList((IProperty<?>[]) ((IProperty<?>[]) o)));
			}
		}
		
		tempProperties = (IProperty[]) propList.toArray(new IProperty[0]);
		return blockProps.variableOpacity();
	}
	
	public IPBlockBaseAdvanced setHidden(boolean shouldHide){
		this.isHidden = shouldHide;
		return this;
	}
	
	public IPBlockBaseAdvanced setNotNormalBlock(){
		this.notNormalBlock = true;
		return this;
	}

	public IPBlockBaseAdvanced setHasFlavour(boolean shouldHave){
		this.hasFlavour = shouldHave;
		return this;
	}

	public IPBlockBaseAdvanced setBlockLayer(BlockRenderLayer... layer){
		Preconditions.checkArgument(layer.length > 0);
		this.renderLayers = Arrays.asList(layer);
		return this;
	}

	public IPBlockBaseAdvanced setMobility(PushReaction flag){
		this.mobilityFlag = flag;
		return this;
	}

	public IPBlockBaseAdvanced setLightOpacity(int opacity){
		this.lightOpacity = opacity;
		return this;
	}

	public IPBlockBaseAdvanced setHammerHarvest(){
		this.canHammerHarvest = true;
		return this;
	}
	
	protected BlockState getInitDefaultState(){
		return (BlockState) this.stateContainer.getBaseState();
	}
	
	@Override
	public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer){
		return this.renderLayers.contains(layer);
	}
	
	@Override
	public BlockRenderLayer getRenderLayer(){
		return this.notNormalBlock ? BlockRenderLayer.CUTOUT : BlockRenderLayer.SOLID;
	}
	
	@Override
	public int getOpacity(BlockState state, IBlockReader worldIn, BlockPos pos){
		if(this.notNormalBlock){
			return 0;
		}else if(state.isOpaqueCube(worldIn, pos)){
			return this.lightOpacity;
		}else{
			return state.propagatesSkylightDown(worldIn, pos) ? 0 : 1;
		}
	}
	
	@Override
	public PushReaction getPushReaction(BlockState state){
		return this.mobilityFlag;
	}
	
	@Override
	public boolean propagatesSkylightDown(BlockState p_200123_1_, IBlockReader p_200123_2_, BlockPos p_200123_3_){
		return this.notNormalBlock || super.propagatesSkylightDown(p_200123_1_, p_200123_2_, p_200123_3_);
	}
	
	@Override
	public boolean causesSuffocation(BlockState state, IBlockReader worldIn, BlockPos pos){
		return !this.notNormalBlock;
	}
	
	@Override
	public boolean isNormalCube(BlockState state, IBlockReader world, BlockPos pos){
		return !this.notNormalBlock;
	}
	
	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder){
		super.fillStateContainer(builder);
		builder.add(tempProperties);
	}
	
	@SuppressWarnings("unchecked")
	protected <V extends Comparable<V>> BlockState applyProperty(BlockState in, IProperty<V> prop, Object val){
		return (BlockState) in.with(prop, (V) val);
	}
	
	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack){
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items){
		items.add(new ItemStack(this, 1));
	}
	
	@Override
	public boolean eventReceived(BlockState state, World worldIn, BlockPos pos, int eventID, int eventParam){
		if(worldIn.isRemote && eventID == 255){
			worldIn.notifyBlockUpdate(pos, state, state, 3);
			return true;
		}else{
			return false;
		}
	}
}