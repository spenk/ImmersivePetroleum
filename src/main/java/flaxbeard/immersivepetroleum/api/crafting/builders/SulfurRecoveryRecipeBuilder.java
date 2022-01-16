package flaxbeard.immersivepetroleum.api.crafting.builders;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.builders.IEFinishedRecipe;
import com.google.gson.JsonObject;
import flaxbeard.immersivepetroleum.common.crafting.Serializers;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class SulfurRecoveryRecipeBuilder extends IEFinishedRecipe<SulfurRecoveryRecipeBuilder>{
	
	public static SulfurRecoveryRecipeBuilder builder(FluidStack fluidOutput, int energy, int time){
		return new SulfurRecoveryRecipeBuilder()
				.setTimeAndEnergy(time, energy)
				.addResultFluid(fluidOutput);
	}
	
	protected SulfurRecoveryRecipeBuilder(){
		super(Serializers.HYDROTREATER_SERIALIZER.get());
	}
	
	public SulfurRecoveryRecipeBuilder addResultFluid(FluidStack fluid){
		return addFluid("result", fluid);
	}
	
	public SulfurRecoveryRecipeBuilder addInputFluid(FluidStack fluid){
		return addFluid("input", fluid);
	}
	
	public SulfurRecoveryRecipeBuilder addInputFluid(FluidTagInput fluid){
		return addFluidTag("input", fluid);
	}
	
	public SulfurRecoveryRecipeBuilder addInputFluid(Tag.Named<Fluid> fluid, int amount){
		return addFluidTag("input", fluid, amount);
	}
	
	/** Optionaly add a second fluid to be pumped in */
	public SulfurRecoveryRecipeBuilder addSecondaryInputFluid(FluidStack fluid){
		return addFluid("secondary_input", fluid);
	}
	
	/** Optionaly add a second fluid to be pumped in */
	public SulfurRecoveryRecipeBuilder addSecondaryInputFluid(FluidTagInput fluid){
		return addFluidTag("secondary_input", fluid);
	}
	
	/** Optionaly add a second fluid to be pumped in */
	public SulfurRecoveryRecipeBuilder addSecondaryInputFluid(Tag.Named<Fluid> fluid, int amount){
		return addFluidTag("secondary_input", fluid, amount);
	}
	
	public SulfurRecoveryRecipeBuilder addItemWithChance(ItemStack item, double chance){
		return addWriter(jsonObject -> {
			jsonObject.add("secondary_result", this.serializerItemStackWithChance(item, chance));
		});
	}
	
	protected SulfurRecoveryRecipeBuilder setTimeAndEnergy(int time, int energy){
		return setTime(time).setEnergy(energy);
	}
	
	protected JsonObject serializerItemStackWithChance(ItemStack stack, double chance){
		JsonObject itemJson = this.serializeItemStack(stack);
		itemJson.addProperty("chance", Double.toString(chance));
		return itemJson;
	}
}
