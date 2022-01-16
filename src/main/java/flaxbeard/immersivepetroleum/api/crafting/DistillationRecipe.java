package flaxbeard.immersivepetroleum.api.crafting;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.cfg.IPServerConfig;
import flaxbeard.immersivepetroleum.common.crafting.Serializers;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DistillationRecipe extends IPMultiblockRecipe{
	public static final RecipeType<DistillationRecipe> TYPE = RecipeType.register(ImmersivePetroleum.MODID + ":distillationtower");
	public static Map<ResourceLocation, DistillationRecipe> recipes = new HashMap<>();
	
	/** May return null! */
	public static DistillationRecipe findRecipe(FluidStack input){
		if(!recipes.isEmpty()){
			for(DistillationRecipe r:recipes.values()){
				if(r.input != null && r.input.testIgnoringAmount(input)){
					return r;
				}
			}
		}
		return null;
	}
	
	public static DistillationRecipe loadFromNBT(CompoundTag nbt){
		FluidStack input = FluidStack.loadFluidStackFromNBT(nbt.getCompound("input"));
		return findRecipe(input);
	}
	
	protected final FluidTagInput input;
	protected final FluidStack[] fluidOutput;
	protected final ItemStack[] itemOutput;
	protected final double[] chances;
	
	public DistillationRecipe(ResourceLocation id, FluidStack[] fluidOutput, ItemStack[] itemOutput, FluidTagInput input, int energy, int time, double[] chances){
		super(ItemStack.EMPTY, TYPE, id);
		this.fluidOutput = fluidOutput;
		this.itemOutput = itemOutput;
		this.chances = chances;
		
		this.input = input;
		this.fluidInputList = Collections.singletonList(input);
		this.fluidOutputList = Arrays.asList(this.fluidOutput);
		this.outputList = NonNullList.of(ItemStack.EMPTY, itemOutput);
		
		timeAndEnergy(time, energy);
		modifyTimeAndEnergy(IPServerConfig.REFINING.distillationTower_timeModifier::get, IPServerConfig.REFINING.distillationTower_energyModifier::get);
	}
	
	@Override
	protected IERecipeSerializer<DistillationRecipe> getIESerializer(){
		return Serializers.DISTILLATION_SERIALIZER.get();
	}
	
	@Override
	public int getMultipleProcessTicks(){
		return 0;
	}
	
	@Override
	public NonNullList<ItemStack> getActualItemOutputs(BlockEntity tile){
		NonNullList<ItemStack> output = NonNullList.create();
		for(int i = 0;i < itemOutput.length;i++){
			if(tile.getLevel().random.nextFloat() <= chances[i]){
				output.add(itemOutput[i]);
			}
		}
		return output;
	}
	
	public FluidTagInput getInputFluid(){
		return this.input;
	}
	
	public double[] chances(){
		return this.chances;
	}
}
