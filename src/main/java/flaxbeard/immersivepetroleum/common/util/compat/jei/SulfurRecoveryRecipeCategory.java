package flaxbeard.immersivepetroleum.common.util.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.crafting.SulfurRecoveryRecipe;
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
import java.util.Locale;

public class SulfurRecoveryRecipeCategory extends IPRecipeCategory<SulfurRecoveryRecipe>{
	public static final ResourceLocation ID = new ResourceLocation(ImmersivePetroleum.MODID, "hydrotreater");
	
	private final IDrawableStatic tankOverlay;
	public SulfurRecoveryRecipeCategory(IGuiHelper guiHelper){
		super(SulfurRecoveryRecipe.class, guiHelper, ID, "block.immersivepetroleum.hydrotreater");
		ResourceLocation background = new ResourceLocation(ImmersivePetroleum.MODID, "textures/gui/jei/hydrotreater.png");
		setBackground(guiHelper.createDrawable(background, 0, 0, 113, 75));
		setIcon(new ItemStack(IPContent.Multiblock.hydrotreater));
		
		this.tankOverlay = guiHelper.createDrawable(background, 113, 0, 20, 51);
	}
	
	@Override
	public void setIngredients(SulfurRecoveryRecipe recipe, IIngredients ingredients){
		ingredients.setInputs(VanillaTypes.FLUID, recipe.inputFluid.getMatchingFluidStacks());
		if(recipe.inputFluidSecondary != null){
			ingredients.setInputs(VanillaTypes.FLUID, recipe.inputFluidSecondary.getMatchingFluidStacks());
		}
		
		ingredients.setOutputs(VanillaTypes.FLUID, Arrays.asList(recipe.output));
		ingredients.setOutputs(VanillaTypes.ITEM, Arrays.asList(recipe.outputItem));
	}
	
	@Override
	public void setRecipe(IRecipeLayout recipeLayout, SulfurRecoveryRecipe recipe, IIngredients ingredients){
		IGuiFluidStackGroup guiFluidStacks = recipeLayout.getFluidStacks();
		IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
		
		int id = 0;
		
		guiFluidStacks.init(id, true, 25, 3, 20, 51, 1, false, this.tankOverlay);
		guiFluidStacks.set(id++, recipe.inputFluid.getMatchingFluidStacks());
		
		guiFluidStacks.init(id, true, 3, 3, 20, 51, 1, false, this.tankOverlay);
		guiFluidStacks.set(id++, recipe.inputFluidSecondary != null ? recipe.inputFluidSecondary.getMatchingFluidStacks() : Arrays.asList(FluidStack.EMPTY));
		
		guiFluidStacks.init(id, false, 71, 3, 20, 51, 1, false, this.tankOverlay);
		guiFluidStacks.set(id++, recipe.output);
		
		guiItemStacks.init(id, false, 93, 20);
		guiItemStacks.set(id++, Arrays.asList(recipe.outputItem));
	}
	
	@Override
	public void draw(SulfurRecoveryRecipe recipe, PoseStack matrix, double mouseX, double mouseY){
		super.draw(recipe, matrix, mouseX, mouseY);
		DecimalFormat formatter = new DecimalFormat("#.##");
		
		IDrawable background = getBackground();
		int bWidth = background.getWidth();
		int bHeight = background.getHeight();
		Font font = Minecraft.getInstance().font;
		
		int time = recipe.getTotalProcessTime();
		int energy = recipe.getTotalProcessEnergy();
		int chance = (int)(100 * recipe.chance);
		
		String text0 = I18n.get("desc.immersiveengineering.info.ift", formatter.format(energy));
		font.draw(matrix, text0, bWidth / 2 - font.width(text0) / 2, bHeight - (font.lineHeight * 2), 0);

		String text1 = I18n.get("desc.immersiveengineering.info.seconds", formatter.format(time / 20D));
		font.draw(matrix, text1, bWidth / 2 - font.width(text1) / 2, bHeight - font.lineHeight, 0);
		
		String text2 = String.format(Locale.US, "%d%%", chance);
		font.draw(matrix, text2, bWidth+3 - font.width(text2), bHeight / 2 + 4, 0);
	}
}
