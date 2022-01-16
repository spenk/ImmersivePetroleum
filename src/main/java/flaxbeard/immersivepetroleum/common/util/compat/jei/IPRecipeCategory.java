package flaxbeard.immersivepetroleum.common.util.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public abstract class IPRecipeCategory<T> implements IRecipeCategory<T>{
	public final ResourceLocation id;
	public String localizedName;
	protected final IGuiHelper guiHelper;
	private final Class<? extends T> recipeClass;
	private IDrawableStatic background;
	private IDrawable icon;
	
	public IPRecipeCategory(Class<? extends T> recipeClass, IGuiHelper guiHelper, ResourceLocation id, String localKey){
		this.recipeClass = recipeClass;
		this.guiHelper = guiHelper;
		this.id = id;
		this.localizedName = I18n.get(localKey);
	}
	
	public void setBackground(IDrawableStatic background){
		this.background = background;
	}
	
	public void setIcon(ItemStack stack){
		setIcon(this.guiHelper.createDrawableIngredient(stack));
	}
	
	public void setIcon(IDrawable icon){
		this.icon = icon;
	}
	
	@Override
	public IDrawable getBackground(){
		return this.background;
	}
	
	@Override
	public IDrawable getIcon(){
		return this.icon;
	}
	
	@Override
	public ResourceLocation getUid(){
		return this.id;
	}
	
	@Override
	public Component getTitle(){
		return new TextComponent(this.localizedName);
	}
	
	@Override
	public Class<? extends T> getRecipeClass(){
		return this.recipeClass;
	}
}
