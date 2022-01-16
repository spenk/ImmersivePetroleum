package flaxbeard.immersivepetroleum.client.render.debugging;

import blusunrize.immersiveengineering.client.utils.GuiHelper;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartBlockEntity;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockBlockEntity;
import blusunrize.immersiveengineering.common.util.inventory.MultiFluidTank;
import com.mojang.blaze3d.vertex.PoseStack;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler.LubricatedTileInfo;
import flaxbeard.immersivepetroleum.common.IPContent;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity.CokingChamber;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.DistillationTowerTileEntity;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.HydrotreaterTileEntity;
import flaxbeard.immersivepetroleum.common.entity.MotorboatEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;

public class DebugRenderHandler{
	public DebugRenderHandler(){
	}
	
	private boolean isHoldingDebugItem(Player player){
		ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
		ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
		
		return (main != ItemStack.EMPTY && main.getItem() == IPContent.debugItem) || (off != ItemStack.EMPTY && off.getItem() == IPContent.debugItem);
	}
	
	@SubscribeEvent
	public void renderDebuggingOverlay(RenderGameOverlayEvent.Post event){
		Minecraft mc = Minecraft.getInstance();
		
		if(mc.player != null && event.getType() == RenderGameOverlayEvent.ElementType.TEXT){
			Player player = mc.player;
			
			if(isHoldingDebugItem(player)){
				HitResult rt = mc.hitResult;
				if(rt != null){
					switch(rt.getType()){
						case BLOCK:{
							BlockHitResult result = (BlockHitResult) rt;
							Level world = player.level;
							
							List<MutableComponent> debugOut = new ArrayList<>();
							
							BlockEntity te = world.getBlockEntity(result.getBlockPos());
							boolean isMBPart = te instanceof MultiblockPartBlockEntity;
							if(isMBPart){
								MultiblockPartBlockEntity<?> multiblock = (MultiblockPartBlockEntity<?>) te;
								
								if(!multiblock.offsetToMaster.equals(BlockPos.ZERO)){
									multiblock = multiblock.master();
								}
								
								if(te instanceof DistillationTowerTileEntity){
									distillationtower(debugOut, (DistillationTowerTileEntity) multiblock);
									
								}else if(te instanceof CokerUnitTileEntity){
									cokerunit(debugOut, (CokerUnitTileEntity) multiblock);
									
								}else if(te instanceof HydrotreaterTileEntity){
									hydrotreater(debugOut, (HydrotreaterTileEntity) multiblock);
								}
							}
							
							if(!debugOut.isEmpty() || isMBPart){
								if(isMBPart){
									MultiblockPartBlockEntity<?> generic = (MultiblockPartBlockEntity<?>) te;
									BlockPos tPos = generic.posInMultiblock;
									
									if(!generic.offsetToMaster.equals(BlockPos.ZERO)){
										generic = generic.master();
									}
									
									BlockPos hit = result.getBlockPos();
									Block block = generic.getBlockState().getBlock();
									
									debugOut.add(0, toText("World XYZ: " + hit.getX() + ", " + hit.getY() + ", " + hit.getZ()));
									debugOut.add(1, toText("Template XYZ: " + tPos.getX() + ", " + tPos.getY() + ", " + tPos.getZ()));
									
									TextComponent name = toTranslation(block.getName()).mergeStyle(ChatFormatting.GOLD);
									
									try{
										name.append(toText(generic.isRSDisabled() ? " (Redstoned)" : "").withStyle(ChatFormatting.RED));
									}catch(UnsupportedOperationException e){
										// Don't care, skip if this is thrown
									}
									
									if(generic instanceof PoweredMultiblockBlockEntity<?,?>){
										PoweredMultiblockBlockEntity<?, ?> poweredGeneric = (PoweredMultiblockBlockEntity<?, ?>) generic;
										
										name.append(toText(poweredGeneric.shouldRenderAsActive() ? " (Active)" : "").withStyle(ChatFormatting.GREEN));
										
										debugOut.add(2, toText(poweredGeneric.energyStorage.getEnergyStored() + "/" + poweredGeneric.energyStorage.getMaxEnergyStored() + "RF"));
									}
									
									synchronized(LubricatedHandler.lubricatedTiles){
										for(LubricatedTileInfo info:LubricatedHandler.lubricatedTiles){
											if(info.pos.equals(generic.getBlockPos())){
												name.append(toText(" (Lubricated " + info.ticks + ")").withStyle(ChatFormatting.YELLOW));
											}
										}
									}
									
									debugOut.add(2, name);
								}
								
								renderOverlay(event.getMatrixStack(), debugOut);
							}
							break;
						}
						case ENTITY:{
							EntityHitResult result = (EntityHitResult) rt;
							
							if(result.getEntity() instanceof MotorboatEntity){
								MotorboatEntity boat = (MotorboatEntity) result.getEntity();
								
								List<MutableComponent> debugOut = new ArrayList<>();
								
								debugOut.add(toText("").append(boat.getDisplayName()).withStyle(ChatFormatting.GOLD));
								
								FluidStack fluid = boat.getContainedFluid();
								if(fluid == FluidStack.EMPTY){
									debugOut.add(toText("Tank: Empty"));
								}else{
									debugOut.add(toText("Tank: " + fluid.getAmount() + "/" + boat.getMaxFuel() + "mB of ").append(fluid.getDisplayName()));
								}
								
								NonNullList<ItemStack> upgrades = boat.getUpgrades();
								int i = 0;
								for(ItemStack upgrade:upgrades){
									if(upgrade == null || upgrade == ItemStack.EMPTY){
										debugOut.add(toText("Upgrade " + (++i) + ": Empty"));
									}else{
										debugOut.add(toText("Upgrade " + (++i) + ": ").append(upgrade.getDisplayName()));
									}
								}
								
								renderOverlay(event.getMatrixStack(), debugOut);
							}
							break;
						}
						default:
							break;
					}
				}
			}
		}
	}
	
	private static void renderOverlay(PoseStack matrix, List<MutableComponent> debugOut){
		Minecraft mc = Minecraft.getInstance();
		
		matrix.pushPose();
		{
			IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
			for(int i = 0;i < debugOut.size();i++){
				int w = mc.fontRenderer.getStringWidth(debugOut.get(i).getString());
				int yOff = i * (mc.fontRenderer.FONT_HEIGHT + 2);
				
				matrix.pushPose();
				{
					matrix.translate(0, 0, 1);
					GuiHelper.drawColouredRect(1, 1 + yOff, w + 1, 10, 0xAF_000000, buffer, matrix);
					buffer.finish();
					// Draw string without shadow
					mc.fontRenderer.drawText(matrix, debugOut.get(i), 2, 2 + yOff, -1);
				}
				matrix.popPose();
			}
		}
		matrix.popPose();
	}
	
	private static void distillationtower(List<MutableComponent> text, DistillationTowerTileEntity tower){
		for(int i = 0;i < tower.tanks.length;i++){
			text.add(toText("Tank " + (i + 1)).withStyle(ChatFormatting.UNDERLINE));
			
			MultiFluidTank tank = tower.tanks[i];
			if(tank.fluids.size() > 0){
				for(int j = 0;j < tank.fluids.size();j++){
					FluidStack fstack = tank.fluids.get(j);
					text.add(toText("  " + fstack.getDisplayName().getString() + " (" + fstack.getAmount() + "mB)"));
				}
			}else{
				text.add(toText("  Empty"));
			}
		}
	}
	
	private static void cokerunit(List<MutableComponent> text, CokerUnitTileEntity coker){
		{
			FluidTank tank = coker.bufferTanks[CokerUnitTileEntity.TANK_INPUT];
			FluidStack fs = tank.getFluid();
			text.add(toText("In Buffer: " + (fs.getAmount() + "/" + tank.getCapacity() + "mB " + (fs.isEmpty() ? "" : "(" + fs.getDisplayName().getString() + ")"))));
		}
		
		{
			FluidTank tank = coker.bufferTanks[CokerUnitTileEntity.TANK_OUTPUT];
			FluidStack fs = tank.getFluid();
			text.add(toText("Out Buffer: " + (fs.getAmount() + "/" + tank.getCapacity() + "mB " + (fs.isEmpty() ? "" : "(" + fs.getDisplayName().getString() + ")"))));
		}
		
		for(int i = 0;i < coker.chambers.length;i++){
			CokingChamber chamber = coker.chambers[i];
			FluidTank tank = chamber.getTank();
			FluidStack fs = tank.getFluid();
			
			float completed = chamber.getTotalAmount() > 0 ? 100 * (chamber.getOutputAmount() / (float) chamber.getTotalAmount()) : 0;
			
			text.add(toText("Chamber " + i).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.AQUA));
			text.add(toText("State: " + chamber.getState().toString()));
			text.add(toText("  Tank: " + (fs.getAmount() + "/" + tank.getCapacity() + "mB " + (fs.isEmpty() ? "" : "(" + fs.getDisplayName().getString() + ")"))));
			text.add(toText("  Content: " + chamber.getTotalAmount() + " / " + chamber.getCapacity()).append(" (" + chamber.getInputItem().getDisplayName().getString() + ")"));
			text.add(toText("  Out: " + chamber.getOutputItem().getDisplayName().getString()));
			text.add(toText("  " + Mth.floor(completed) + "% Completed. (Raw: " + completed + ")"));
		}
	}
	
	private static void hydrotreater(List<MutableComponent> text, HydrotreaterTileEntity treater){
		IFluidTank[] tanks = treater.getInternalTanks();
		if(tanks != null && tanks.length > 0){
			for(int i = 0;i < tanks.length;i++){
				FluidStack fs = tanks[i].getFluid();
				text.add(toText("Tank " + i + ": " + (fs.getAmount() + "/" + tanks[i].getCapacity() + "mB " + (fs.isEmpty() ? "" : "(" + fs.getDisplayName().getString() + ")"))));
			}
		}
	}
	
	static MutableComponent toText(String string){
		return new TextComponent(string);
	}
	
	static TranslatableComponent toTranslation(String translationKey, Object... args){
		return new TranslatableComponent(translationKey, args);
	}
}
