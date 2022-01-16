package flaxbeard.immersivepetroleum.common.util.compat.crafttweaker;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.api.item.MCItemStack;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import com.blamejared.crafttweaker.api.tag.MCTag;
import com.blamejared.crafttweaker.api.util.Many;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import flaxbeard.immersivepetroleum.api.crafting.SulfurRecoveryRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import org.openzen.zencode.java.ZenCodeType.Method;
import org.openzen.zencode.java.ZenCodeType.Name;

@ZenRegister
@Document("mods/immersivepetroleum/SRU")
@Name("mods.immersivepetroleum.Hydrotreater")
public class SulfurRecoveryRecipeTweaker implements IRecipeManager {
	@Override
	public RecipeType<SulfurRecoveryRecipe> getRecipeType(){
		return SulfurRecoveryRecipe.TYPE;
	}
	
	/**
	 * Removes all recipes
	 */
	@Method
	public void removeAll(){
		SulfurRecoveryRecipe.recipes.clear();
	}
	
	@Method
	public void removeByOutputItem(IIngredient output){
		SulfurRecoveryRecipe.recipes.values().removeIf(recipe -> output.matches(new MCItemStack(recipe.outputItem)));
	}
	
	@Method
	public void removeByOutputFluid(IFluidStack output){
		SulfurRecoveryRecipe.recipes.values().removeIf(recipe -> recipe.output.isFluidEqual(output.getInternal()));
	}
	
	@Method
	public void addRecipe(String name, IFluidStack output, IItemStack outputItem, double chance, Many<MCTag<Fluid>> inputFluid, int energy){
		ResourceLocation id = TweakerUtils.ctLoc("hydrotreater/" + name);
		
		FluidTagInput primary = new FluidTagInput(inputFluid.getData().id(), inputFluid.getAmount());
		
		newRecipe(id, output, outputItem, chance, primary, null, energy);
	}
	
	@Method
	public void addRecipeWithSecondary(String name, IFluidStack output, IItemStack outputItem, double chance, Many<MCTag<Fluid>> inputFluid, Many<MCTag<Fluid>> inputFluidSecondary, int energy){
		ResourceLocation id = TweakerUtils.ctLoc("hydrotreater/" + name);
		
		FluidTagInput primary = new FluidTagInput(inputFluid.getData().id(), inputFluid.getAmount());
		FluidTagInput secondary = new FluidTagInput(inputFluidSecondary.getData().id(), inputFluidSecondary.getAmount());
		
		newRecipe(id, output, outputItem, chance, primary, secondary, energy);
	}
	
	private void newRecipe(ResourceLocation id, IFluidStack output, IItemStack outputItem, double chance, FluidTagInput primary, FluidTagInput secondary, int energy){
		SulfurRecoveryRecipe recipe = new SulfurRecoveryRecipe(id, output.getInternal(), outputItem.getInternal(), primary, secondary, chance, energy, 1);
		// Does NOT work with this
		//CraftTweakerAPI.apply(new ActionAddRecipe(this, recipe, null));
		
		// This however does, while it may not be the safest thing to do..
		SulfurRecoveryRecipe.recipes.put(id, recipe);
	}
}
