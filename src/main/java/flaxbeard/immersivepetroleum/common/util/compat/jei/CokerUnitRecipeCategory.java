package flaxbeard.immersivepetroleum.common.util.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.crafting.CokerUnitRecipe;
import flaxbeard.immersivepetroleum.common.IPContent;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiFluidStackGroup;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class CokerUnitRecipeCategory extends IPRecipeCategory<CokerUnitRecipe>{
	public static final ResourceLocation ID = new ResourceLocation(ImmersivePetroleum.MODID, "cokerunit");
	
	private final IDrawableStatic tankOverlay;
	public CokerUnitRecipeCategory(IGuiHelper guiHelper){
		super(CokerUnitRecipe.class, guiHelper, ID, "block.immersivepetroleum.cokerunit");
		ResourceLocation background = new ResourceLocation(ImmersivePetroleum.MODID, "textures/gui/jei/coker.png");
		ResourceLocation coker = new ResourceLocation(ImmersivePetroleum.MODID, "textures/gui/coker.png");
		
		setBackground(guiHelper.createDrawable(background, 0, 0, 150, 77));
		setIcon(new ItemStack(IPContent.Multiblock.cokerunit));
		
		this.tankOverlay = guiHelper.createDrawable(coker, 200, 0, 20, 51);
	}
	
	@Override
	public void setIngredients(CokerUnitRecipe recipe, IIngredients ingredients){
		ingredients.setInputs(VanillaTypes.FLUID, recipe.inputFluid.getMatchingFluidStacks());
		ingredients.setInputs(VanillaTypes.ITEM, Arrays.asList(recipe.inputItem.getMatchingStacks()));

		ingredients.setOutputs(VanillaTypes.FLUID, recipe.outputFluid.getMatchingFluidStacks());
		ingredients.setOutput(VanillaTypes.ITEM, recipe.outputItem);
	}
	
	@Override
	public void setRecipe(IRecipeLayout recipeLayout, CokerUnitRecipe recipe, IIngredients ingredients){
		IGuiFluidStackGroup guiFluidStacks = recipeLayout.getFluidStacks();
		IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
		
		{
			int total = 0;
			List<FluidStack> list = recipe.inputFluid.getMatchingFluidStacks();
			if(!list.isEmpty()){
				for(FluidStack f:list){
					total += f.getAmount();
				}
			}else{
				total = 100;
			}
			guiFluidStacks.init(0, true, 2, 2, 20, 51, total, false, this.tankOverlay);
			guiFluidStacks.set(0, list);
		}
		
		{
			int total = 0;
			List<FluidStack> list = recipe.outputFluid.getMatchingFluidStacks();
			if(!list.isEmpty()){
				for(FluidStack f:list){
					total += f.getAmount();
				}
			}else{
				total = 100;
			}
			guiFluidStacks.init(1, false, 50, 2, 20, 51, total, false, this.tankOverlay);
			guiFluidStacks.set(1, list);
		}
		
		guiItemStacks.init(0, true, 3, 57);
		guiItemStacks.set(0, Arrays.asList(recipe.inputItem.getMatchingStacks()));
		
		guiItemStacks.init(1, true, 51, 57);
		guiItemStacks.set(1, Arrays.asList(recipe.outputItem));
	}
	
	@Override
	public void draw(CokerUnitRecipe recipe, PoseStack matrix, double mouseX, double mouseY){
		super.draw(recipe, matrix, mouseX, mouseY);
		IDrawable background = getBackground();
		int bWidth = background.getWidth();
		int bHeight = background.getHeight();
		Font font = Minecraft.getInstance().font;
		
		int time = (recipe.getTotalProcessTime() + 2 + 5) * recipe.inputItem.getCount();
		int energy = recipe.getTotalProcessEnergy();
		
		String text0 = I18n.get("desc.immersiveengineering.info.ift", new DecimalFormat("#.##").format(energy));
		font.draw(matrix, text0, bWidth - 5 - font.width(text0), (bHeight / 3) + font.lineHeight, -1);
		
		String text1 = I18n.get("desc.immersiveengineering.info.seconds", new DecimalFormat("#.##").format(time / 20D));
		font.draw(matrix, text1, bWidth - 10 - font.width(text1), (bHeight / 3) + (font.lineHeight * 2), -1);
	}
}
