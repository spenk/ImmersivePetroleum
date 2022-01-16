package flaxbeard.immersivepetroleum.client.gui;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.utils.GuiHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity;
import flaxbeard.immersivepetroleum.common.gui.CokerUnitContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.gui.GuiUtils;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity.*;

public class CokerUnitScreen extends IEContainerScreen<CokerUnitContainer>{
	static final ResourceLocation GUI_TEXTURE = new ResourceLocation("immersivepetroleum", "textures/gui/coker.png");
	
	CokerUnitTileEntity tile;
	public CokerUnitScreen(CokerUnitContainer inventorySlotsIn, Inventory inv, TextComponent title){
		super(inventorySlotsIn, inv, title, GUI_TEXTURE);
		this.tile = menu.tile;
		
		this.imageWidth = 200;
		this.imageHeight = 187;
	}
	
	@Override
	public void render(PoseStack matrix, int mx, int my, float partialTicks){
		this.renderBackground(matrix);
		super.render(matrix, mx, my, partialTicks);
		this.renderTooltip(matrix, mx, my);
		
		List<TextComponent> tooltip = new ArrayList<>();
		
		// Buffer tank displays
		GuiHelper.handleGuiTank(matrix, tile.bufferTanks[TANK_INPUT], getGuiLeft() + 32, getGuiTop() + 14, 16, 47, 0, 0, 0, 0, mx, my, GUI_TEXTURE, tooltip);
		GuiHelper.handleGuiTank(matrix, tile.bufferTanks[TANK_OUTPUT], getGuiLeft() + 152, getGuiTop() + 14, 16, 47, 0, 0, 0, 0, mx, my, GUI_TEXTURE, tooltip);
		
		// Chamber Stats
		chamberDisplay(matrix, getGuiLeft() + 74, getGuiTop() + 24, 6, 38, CHAMBER_A, mx, my, partialTicks, tooltip);
		chamberDisplay(matrix, getGuiLeft() + 120, getGuiTop() + 24, 6, 38, CHAMBER_B, mx, my, partialTicks, tooltip);
		
		// Power Stored
		if(mx > getGuiLeft() + 167 && mx < getGuiLeft() + 175 && my > getGuiTop() + 66 && my < getGuiTop() + 88){
			tooltip.add(new TextComponent(tile.energyStorage.getEnergyStored() + "/" + tile.energyStorage.getMaxEnergyStored() + " IF"));
		}
		
		if(!tooltip.isEmpty()){
			GuiUtils.drawHoveringText(matrix, tooltip, mx, my, width, height, -1, font);
		}
	}
	
	private void chamberDisplay(PoseStack matrix, int x, int y, int w, int h, int chamberId, int mx, int my, float partialTicks, List<Component> tooltip){
		CokingChamber chamber = tile.chambers[chamberId];
		
		// Vertical Bar for Content amount.
		ClientUtils.bindTexture(GUI_TEXTURE);
		int scale = 38;
		int off = (int) (chamber.getTotalAmount() / (float) chamber.getCapacity() * scale);
		this.blit(matrix, x, y + scale - off, 200, 51, 6, off);
		
		// Vertical Overlay to visualize progress
		off = (int)(chamber.getTotalAmount() > 0 ? scale * (chamber.getOutputAmount() / (float)chamber.getCapacity()) : 0);
		this.blit(matrix, x, y + scale - off, 206, 51 + (scale - off), 6, off);
		
		// Chamber Tank
		GuiHelper.handleGuiTank(matrix, chamber.getTank(), x, y, 6, 38, 0, 0, 0, 0, mx, my, GUI_TEXTURE, null);
	}
	
	@Override
	protected void drawContainerBackgroundPre(PoseStack matrix, float partialTicks, int mx, int my){
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		ClientUtils.bindTexture(GUI_TEXTURE);
		this.blit(matrix, getGuiLeft(), getGuiTop(), 0, 0, getXSize(), getYSize());
		
		GuiHelper.handleGuiTank(matrix, tile.bufferTanks[TANK_INPUT], getGuiLeft() + 32, getGuiTop() + 14, 16, 47, 202, 2, 16, 47, mx, my, GUI_TEXTURE, null);
		GuiHelper.handleGuiTank(matrix, tile.bufferTanks[TANK_OUTPUT], getGuiLeft() + 152, getGuiTop() + 14, 16, 47, 202, 2, 16, 47, mx, my, GUI_TEXTURE, null);
		
		int x = getGuiLeft() + 168;
		int y = getGuiTop() + 67;
		int stored = (int) (tile.energyStorage.getEnergyStored() / (float) tile.energyStorage.getMaxEnergyStored() * 21);
		fillGradient(matrix, x, y + 21 - stored, x + 7, y + 21, 0xffb51500, 0xff600b00);
	}
}
