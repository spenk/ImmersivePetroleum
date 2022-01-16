package flaxbeard.immersivepetroleum.client.model;

import blusunrize.immersiveengineering.api.excavator.MineralMix;
import blusunrize.immersiveengineering.client.models.ModelCoresample;
import blusunrize.immersiveengineering.common.items.CoresampleItem;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import com.google.common.cache.Cache;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

// TODO Get the Coresample to display shit
@SuppressWarnings("unused")
public class ModelCoresampleExtended extends ModelCoresample{
	private Fluid fluid;
	
	public ModelCoresampleExtended(@Nullable MineralMix[] mineral, @Nullable Fluid fluid){
		super(mineral);
		this.fluid = fluid;
	}
	
	@Override
	public List<BakedQuad> getQuads(BlockState coreState, Direction side, Random rand, IModelData extraData){
		List<BakedQuad> bakedQuads = super.getQuads(coreState, side, rand, extraData);
		return bakedQuads;
	}

	@Override
	public ItemOverrides getOverrides(){
		return overrideList2;
	}

	ItemOverrides overrideList2 = new ItemOverrides(){
		
		@Override
		public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel worldIn, @Nullable LivingEntity entityIn, int unused) {
			String resName = ItemNBTHelper.hasKey(stack, "resType") ? ItemNBTHelper.getString(stack, "resType") : null;
			if(ItemNBTHelper.hasKey(stack, "resAmount") && resName == null && ItemNBTHelper.getInt(stack, "resAmount") > 0){
				resName = "resAmount";
			}
			
			MineralMix[] minerals = CoresampleItem.getMineralMixes(stack);
			if(minerals.length > 0){
				try{
					String cacheKey = "";
					for(int i = 0;i < minerals.length;i++){
						cacheKey += (i > 0 ? "_" : "") + minerals[i].getId().toString();
					}
					
					return getSampleCache().get(cacheKey, () -> new ModelCoresampleExtended(minerals, null));
				}catch(ExecutionException e){
					throw new RuntimeException(e);
				}
			}
			
			return originalModel;
		}
	};
	
	static Field STATIC_FIELD_modelCache;
	static Field FIELD_format;
	
	@SuppressWarnings("unchecked")
	static Cache<String, ModelCoresample> getSampleCache(){
		if(STATIC_FIELD_modelCache == null){
			try{
				STATIC_FIELD_modelCache = ModelCoresample.class.getDeclaredField("modelCache"); // Don't judge me alright? :c
				STATIC_FIELD_modelCache.setAccessible(true);
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		
		try{
			return (Cache<String, ModelCoresample>) STATIC_FIELD_modelCache.get(null);
		}catch(Exception e){
			throw new RuntimeException("This shouldnt happen! Report this immediately!", e);
		}
	}
	
	static VertexFormat getVertexFormat(ModelCoresample instance){
		if(FIELD_format == null){
			try{
				FIELD_format = ModelCoresample.class.getDeclaredField("format"); // Don't judge me alright? :c
				FIELD_format.setAccessible(true);
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		
		try{
			return (VertexFormat) FIELD_format.get(instance);
		}catch(Exception e){
			throw new RuntimeException("This shouldnt happen! Report this immediately!", e);
		}
	}
}
