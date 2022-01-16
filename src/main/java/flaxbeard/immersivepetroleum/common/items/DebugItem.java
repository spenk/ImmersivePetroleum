package flaxbeard.immersivepetroleum.common.items;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockBlockEntity;
import blusunrize.immersiveengineering.common.util.inventory.MultiFluidTank;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.DimensionChunkCoords;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.PumpjackHandler;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.PumpjackHandler.ReservoirType;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.ReservoirWorldInfo;
import flaxbeard.immersivepetroleum.client.model.IPModels;
import flaxbeard.immersivepetroleum.common.IPContent;
import flaxbeard.immersivepetroleum.common.IPSaveData;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.AutoLubricatorTileEntity;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.DistillationTowerTileEntity;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.GasGeneratorTileEntity;
import flaxbeard.immersivepetroleum.common.entity.MotorboatEntity;
import flaxbeard.immersivepetroleum.common.network.IPPacketHandler;
import flaxbeard.immersivepetroleum.common.network.MessageDebugSync;
import flaxbeard.immersivepetroleum.common.particle.IPParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DebugItem extends IPItemBase{
	protected static enum Modes{
		DISABLED("Disabled"),
		INFO_SPEEDBOAT("Info: Speedboat."),
		INFO_TE_AUTOLUBE("Info: AutoLubricator."),
		INFO_TE_GASGEN("Info: Portable Generator."),
		INFO_TE_MULTIBLOCK("Info: Powered Multiblock."),
		INFO_TE_DISTILLATION_TOWER("Info: Distillation Tower."),
		RESERVOIR("Create/Get Reservoir"),
		RESERVOIR_BIG_SCAN("Scan 5 Block Radius Area"),
		CLEAR_RESERVOIR_CACHE("Clear Reservoir Cache"),
		REFRESH_ALL_IPMODELS("Refresh all IPModels"),
		GENERAL_TEST("You may not want to trigger this.")
		;
		
		public final String display;
		private Modes(String display){
			this.display = display;
		}
	}
	
	public DebugItem(){
		super("debug");
	}
	
	@Override
	public @NotNull Component getName(@NotNull ItemStack stack){
		return new TextComponent("IP Debugging Tool").withStyle(ChatFormatting.LIGHT_PURPLE);
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(@NotNull ItemStack stack, Level worldIn, List<Component> tooltip, @NotNull TooltipFlag flagIn){
		tooltip.add(new TextComponent("[Shift + Scroll-UP/DOWN] Change mode.").withStyle(ChatFormatting.GRAY));
		Modes mode = getMode(stack);
		if(mode == Modes.DISABLED){
			tooltip.add(new TextComponent("  Disabled.").withStyle(ChatFormatting.DARK_GRAY));
		}else{
			tooltip.add(new TextComponent("  " + mode.display).withStyle(ChatFormatting.DARK_GRAY));
		}
		
		tooltip.add(new TextComponent("You're not supposed to have this.").withStyle(ChatFormatting.DARK_RED));
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
	}
	
	@Override
	public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items){
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn){
		if(worldIn.isClientSide){
			Modes mode = DebugItem.getMode(playerIn.getItemInHand(handIn));
			
			switch(mode){
				case REFRESH_ALL_IPMODELS:{
					IPModels.getModels().forEach(m -> m.init());
					
					playerIn.displayClientMessage(new TextComponent("Models refreshed."), true);
					
					return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
				}
				case RESERVOIR_BIG_SCAN:{
					BlockPos pos = playerIn.blockPosition();
					int r = 5;
					int cx = (pos.getX() >> 4);
					int cz = (pos.getZ() >> 4);
					ImmersivePetroleum.log.info(worldIn.dimension());
					for(int i = -r;i <= r;i++){
						for(int j = -r;j <= r;j++){
							int x = cx + i;
							int z = cz + j;
							
							DimensionChunkCoords coords = new DimensionChunkCoords(worldIn.dimension(), x, z);
							
							ReservoirWorldInfo info = PumpjackHandler.getOrCreateOilWorldInfo(worldIn, coords, false);
							if(info != null && info.getType() != null){
								ReservoirType type = info.getType();
								
								int cap = info.capacity;
								int cur = info.current;
								
								String out = String.format(Locale.ENGLISH, "%s %s:\t%.3f/%.3f Buckets of %s", coords.x, coords.z, cur / 1000D, cap / 1000D, type.name);
								
								ImmersivePetroleum.log.info(out);
							}
						}
					}
					
					return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
				}
				case CLEAR_RESERVOIR_CACHE:{
					int contentSize = PumpjackHandler.reservoirsCache.size();
					
					PumpjackHandler.reservoirsCache.clear();
					PumpjackHandler.recalculateChances();
					
					IPSaveData.markInstanceAsDirty();
					
					playerIn.displayClientMessage(new TextComponent("Cleared Oil Cache. (Removed " + contentSize + ")"), true);
					
					return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
				}
				case RESERVOIR:{
					BlockPos pos = playerIn.blockPosition();
					DimensionChunkCoords coords = new DimensionChunkCoords(worldIn.dimension(), (pos.getX() >> 4), (pos.getZ() >> 4));
					
					int last = PumpjackHandler.reservoirsCache.size();
					ReservoirWorldInfo info = PumpjackHandler.getOrCreateOilWorldInfo(worldIn, coords, false);
					boolean isNew = PumpjackHandler.reservoirsCache.size() != last;
					
					if(info != null){
						int cap = info.capacity;
						int cur = info.current;
						ReservoirType type = info.getType();
						
						if(type!=null){
							String out = String.format(Locale.ENGLISH,
									"%s %s: %.3f/%.3f Buckets of %s%s%s",
									coords.x,
									coords.z,
									cur/1000D,
									cap/1000D,
									type.name,
									(info.overrideType!=null?" [OVERRIDDEN]":""),
									(isNew?" [NEW]":"")
							);
							
							playerIn.displayClientMessage(new TextComponent(out), true);
							
							return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
						}
					}
					
					playerIn.displayClientMessage(new TextComponent(String.format(Locale.ENGLISH, "%s %s: Nothing.", coords.x, coords.z)), true);
					
					return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
				}
				default:
					break;
			}
			return new InteractionResultHolder<ItemStack>(InteractionResult.PASS, playerIn.getItemInHand(handIn));
		}
		
		return super.use(worldIn, playerIn, handIn);
	}
	
	@Override
	public InteractionResult useOn(UseOnContext context){
		Player player = context.getPlayer();
		if(player == null){
			return InteractionResult.PASS;
		}
		
		ItemStack held = player.getItemInHand(context.getHand());
		Modes mode = DebugItem.getMode(held);
		
		BlockEntity te = context.getLevel().getBlockEntity(context.getClickedPos());
		switch(mode){
			case GENERAL_TEST:{
				Level world = context.getLevel();
				if(!world.isClientSide){
					// Client
					BlockPos pos = context.getClickedPos();

					float xa = 0.0625F * (float) Math.random();
					float ya = 0.0625F;
					float za = 0.0625F * (float) Math.random();
					
					world.addParticle(IPParticleTypes.FLARE_FIRE.get(), true, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, xa, ya, za);
				}else{
					// Server
				}
				
				return InteractionResult.SUCCESS;
			}
			case INFO_TE_DISTILLATION_TOWER:{
				if(te instanceof DistillationTowerTileEntity && context.getLevel().isClientSide){
					DistillationTowerTileEntity tower = (DistillationTowerTileEntity) te;
					if(!tower.offsetToMaster.equals(BlockPos.ZERO)){
						tower = tower.master();
					}
					
					MutableComponent tankInText = new TextComponent("\nInputFluids: ");
					{
						MultiFluidTank tank = tower.tanks[DistillationTowerTileEntity.TANK_OUTPUT];
						for(int i = 0;i < tank.fluids.size();i++){
							FluidStack fstack = tank.fluids.get(i);
							tankInText.append(" ").append(fstack.getDisplayName()).append(" " + fstack.getAmount() + "mB,");
						}
					}

					MutableComponent tankOutText = new TextComponent("\nOutputFluids: ");
					{
						MultiFluidTank tank = tower.tanks[DistillationTowerTileEntity.TANK_INPUT];
						for(int i = 0;i < tank.fluids.size();i++){
							FluidStack fstack = tank.fluids.get(i);
							tankOutText.append(" ").append(fstack.getDisplayName()).append(" " + fstack.getAmount() + "mB,");
						}
					}
					
					player.sendMessage(new TextComponent("DistillationTower:\n").append(tankInText).append(tankOutText), Util.NIL_UUID);
				}
				return InteractionResult.PASS;
			}
			case INFO_TE_MULTIBLOCK:{
				if(te instanceof PoweredMultiblockBlockEntity && context.getLevel().isClientSide){ // Generic
					PoweredMultiblockBlockEntity<?, ?> poweredMultiblock = (PoweredMultiblockBlockEntity<?, ?>) te;
					
					BlockPos loc = poweredMultiblock.posInMultiblock;
					Set<PoweredMultiblockBlockEntity.MultiblockFace> energyInputs = poweredMultiblock.getEnergyPos();
					Set<BlockPos> redstoneInputs = poweredMultiblock.getRedstonePos();
					
					MutableComponent out = new TextComponent("[" + loc.getX() + " " + loc.getY() + " " + loc.getZ() + "]: ");

					for(PoweredMultiblockBlockEntity.MultiblockFace pos : energyInputs){
						if(pos.equals(loc)){
							out.append("Energy Port.");
						}
					}
					
					for(BlockPos pos:redstoneInputs){
						if(pos.equals(loc)){
							out.append("Redstone Port.");
						}
					}
					
					if(poweredMultiblock.offsetToMaster.equals(BlockPos.ZERO)){
						out.append("Master.");
					}
					
					out.append(" (Facing: " + poweredMultiblock.getFacing() + ", Block-Face: " + context.getClickedFace() + ")");
					
					player.displayClientMessage(out, true);
					return InteractionResult.SUCCESS;
				}
				break;
			}
			case INFO_TE_AUTOLUBE:{
				if(te instanceof AutoLubricatorTileEntity){
					AutoLubricatorTileEntity lube = (AutoLubricatorTileEntity) te;
					
					MutableComponent out = new TextComponent(!context.getLevel().isClientSide ? "CLIENT: " : "SERVER: ");
					out.append(lube.facing + ", ");
					out.append((lube.isActive ? "Active" : "Inactive") + ", ");
					out.append((lube.isSlave ? "Slave" : "Master") + ", ");
					out.append((lube.predictablyDraining ? "Predictably Draining, " : ""));
					if(!lube.tank.isEmpty()){
						out.append(lube.tank.getFluid().getDisplayName()).append(" " + lube.tank.getFluidAmount() + "/" + lube.tank.getCapacity() + "mB");
					}else{
						out.append("Empty");
					}
					
					player.sendMessage(out, Util.NIL_UUID);
					
					return InteractionResult.SUCCESS;
				}
				break;
			}
			case INFO_TE_GASGEN:{
				if(te instanceof GasGeneratorTileEntity){
					GasGeneratorTileEntity gas = (GasGeneratorTileEntity) te;
					
					MutableComponent out = new TextComponent(!context.getLevel().isClientSide ? "CLIENT: " : "SERVER: ");
					out.append(gas.getFacing() + ", ");
					out.append(gas.getEnergyStored(null) + ", ");
					out.append(gas.getMaxEnergyStored(null) + ", ");
					
					player.sendMessage(out, Util.NIL_UUID);
					
					return InteractionResult.SUCCESS;
				}
				break;
			}
			default:
				break;
		}
		
		return InteractionResult.PASS;
	}
	
	public void onSpeedboatClick(MotorboatEntity speedboatEntity, Player player, ItemStack debugStack){
		if(!speedboatEntity.level.isClientSide || DebugItem.getMode(debugStack) != Modes.INFO_SPEEDBOAT){
			return;
		}
		
		MutableComponent textOut = new TextComponent("-- Speedboat --\n");
		
		FluidStack fluid = speedboatEntity.getContainedFluid();
		if(fluid == FluidStack.EMPTY){
			textOut.append("Tank: Empty");
		}else{
			textOut.append("Tank: " + fluid.getAmount() + "/" + speedboatEntity.getMaxFuel() + "mB of ").append(fluid.getDisplayName());
		}
		
		MutableComponent upgradesText = new TextComponent("\n");
		NonNullList<ItemStack> upgrades = speedboatEntity.getUpgrades();
		int i = 0;
		for(ItemStack upgrade:upgrades){
			if(upgrade == null || upgrade == ItemStack.EMPTY){
				upgradesText.append("Upgrade " + (++i) + ": Empty\n");
			}else{
				upgradesText.append("Upgrade " + (i++) + ": ").append(upgrade.getDisplayName()).append("\n");
			}
		}
		textOut.append(upgradesText);
		
		player.sendMessage(textOut, Util.NIL_UUID);
	}
	
	public static void setModeServer(ItemStack stack, Modes mode){
		CompoundTag nbt = getSettings(stack);
		nbt.putInt("mode", mode.ordinal());
	}
	
	public static void setModeClient(ItemStack stack, Modes mode){
		CompoundTag nbt = getSettings(stack);
		nbt.putInt("mode", mode.ordinal());
		IPPacketHandler.sendToServer(new MessageDebugSync(nbt));
	}
	
	public static Modes getMode(ItemStack stack){
		CompoundTag nbt = getSettings(stack);
		if(nbt.contains("mode")){
			int mode = nbt.getInt("mode");
			
			if(mode < 0 || mode >= Modes.values().length)
				mode = 0;
			
			return Modes.values()[mode];
		}
		return Modes.DISABLED;
	}
	
	public static CompoundTag getSettings(ItemStack stack){
		return stack.getOrCreateTagElement("settings");
	}
	
	@Mod.EventBusSubscriber(modid = ImmersivePetroleum.MODID, value = Dist.CLIENT)
	public static class ClientInputHandler{
		static boolean shiftHeld = false;
		
		@SubscribeEvent
		public static void handleScroll(InputEvent.MouseScrollEvent event){
			double delta = event.getScrollDelta();
			
			if(shiftHeld && delta != 0.0){
				Player player = ClientUtils.mc().player;
				ItemStack mainItem = player.getMainHandItem();
				ItemStack secondItem = player.getOffhandItem();
				boolean main = !mainItem.isEmpty() && mainItem.getItem() == IPContent.debugItem;
				boolean off = !secondItem.isEmpty() && secondItem.getItem() == IPContent.debugItem;
				
				if(main || off){
					ItemStack target = main ? mainItem : secondItem;
					
					Modes mode = DebugItem.getMode(target);
					int id = mode.ordinal() + (int) delta;
					if(id < 0){
						id = Modes.values().length - 1;
					}
					if(id >= Modes.values().length){
						id = 0;
					}
					mode = Modes.values()[id];
					
					DebugItem.setModeClient(target, mode);
					player.displayClientMessage(new TextComponent(mode.display), true);
					event.setCanceled(true);
				}
			}
		}
		
		@SubscribeEvent
		public static void handleKey(InputEvent.KeyInputEvent event){
			if(event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT || event.getKey() == GLFW.GLFW_KEY_LEFT_SHIFT){
				switch(event.getAction()){
					case GLFW.GLFW_PRESS:{
						shiftHeld = true;
						return;
					}
					case GLFW.GLFW_RELEASE:{
						shiftHeld = false;
						return;
					}
				}
			}
		}
	}
}
