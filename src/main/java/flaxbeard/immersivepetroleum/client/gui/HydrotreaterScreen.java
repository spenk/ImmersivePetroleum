package flaxbeard.immersivepetroleum.client.gui;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.utils.GuiHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.HydrotreaterTileEntity;
import flaxbeard.immersivepetroleum.common.gui.HydrotreaterContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fml.client.gui.GuiUtils;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class HydrotreaterScreen extends IEContainerScreen<HydrotreaterContainer>{
	static final ResourceLocation GUI_TEXTURE = new ResourceLocation("immersivepetroleum", "textures/gui/hydrotreater.png");
	
	HydrotreaterTileEntity tile;
	public HydrotreaterScreen(HydrotreaterContainer inventorySlotsIn, Inventory inv, TextComponent title){
		super(inventorySlotsIn, inv, title, GUI_TEXTURE);
		this.tile = this.menu.tile;
		
		this.imageWidth = 140;
		this.imageHeight = 69;
	}
	
	@Override
	public void render(PoseStack matrix, int mx, int my, float partialTicks){
		this.renderBackground(matrix);
		super.render(matrix, mx, my, partialTicks);
//		this.renderHoveredTooltip(matrix, mx, my); // Not needed
		
		List<Component> tooltip = new ArrayList<>();
		
		// Tank displays
		GuiHelper.handleGuiTank(matrix, this.tile.tanks[HydrotreaterTileEntity.TANK_INPUT_A], this.getGuiLeft() + 34, this.getGuiTop() + 11, 16, 47, 0, 0, 0, 0, mx, my, GUI_TEXTURE, tooltip);
		GuiHelper.handleGuiTank(matrix, this.tile.tanks[HydrotreaterTileEntity.TANK_INPUT_B], this.getGuiLeft() + 11, this.getGuiTop() + 11, 16, 47, 0, 0, 0, 0, mx, my, GUI_TEXTURE, tooltip);
		GuiHelper.handleGuiTank(matrix, this.tile.tanks[HydrotreaterTileEntity.TANK_OUTPUT], this.getGuiLeft() + 92, this.getGuiTop() + 11, 16, 47, 0, 0, 0, 0, mx, my, GUI_TEXTURE, tooltip);
		
		// Power Stored
		if(mx > this.getGuiLeft() + 121 && mx < this.getGuiLeft() + 129 && my > this.getGuiTop() + 11 && my < this.getGuiTop() + 58){
			tooltip.add(new TextComponent(this.tile.energyStorage.getEnergyStored() + "/" + this.tile.energyStorage.getMaxEnergyStored() + " IF"));
		}
		
		if(!tooltip.isEmpty()){
			GuiUtils.drawHoveringText(matrix, tooltip, mx, my, this.width, this.height, -1, this.font);
		}
	}
	
	@Override
	protected void drawContainerBackgroundPre(PoseStack matrix, float partialTicks, int mx, int my){
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		ClientUtils.bindTexture(GUI_TEXTURE);
		this.blit(matrix, this.getGuiLeft(), this.getGuiTop(), 0, 0, this.getXSize(), this.getYSize());
		
		GuiHelper.handleGuiTank(matrix, this.tile.tanks[HydrotreaterTileEntity.TANK_INPUT_A], this.getGuiLeft() + 34, this.getGuiTop() + 11, 16, 47, 140, 0, 20, 51, mx, my, GUI_TEXTURE, null);
		GuiHelper.handleGuiTank(matrix, this.tile.tanks[HydrotreaterTileEntity.TANK_INPUT_B], this.getGuiLeft() + 11, this.getGuiTop() + 11, 16, 47, 140, 0, 20, 51, mx, my, GUI_TEXTURE, null);
		GuiHelper.handleGuiTank(matrix, this.tile.tanks[HydrotreaterTileEntity.TANK_OUTPUT], this.getGuiLeft() + 92, this.getGuiTop() + 11, 16, 47, 140, 0, 20, 51, mx, my, GUI_TEXTURE, null);
		
		int stored = (int) (46 * (tile.energyStorage.getEnergyStored() / (float) tile.energyStorage.getMaxEnergyStored()));
		fillGradient(matrix, getGuiLeft() + 122, getGuiTop() + 12 + (46 - stored), getGuiLeft() + 129, getGuiTop() + 58, 0xffb51500, 0xff600b00);
	}
}
