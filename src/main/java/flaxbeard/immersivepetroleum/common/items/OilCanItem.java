package flaxbeard.immersivepetroleum.common.items;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import flaxbeard.immersivepetroleum.api.crafting.LubricantHandler;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import java.util.List;

public class OilCanItem extends IPItemBase {
	public OilCanItem(String name){
		super(name, new Item.Properties().stacksTo(1));
	}
	
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt){
		if(!stack.isEmpty()){
			return new FluidHandlerItemStack(stack, 8000);
		}
		
		return null;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn){
		if(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY == null)
			return;
		
		FluidUtil.getFluidContained(stack).ifPresent(fluid -> {
			if(fluid != null && fluid.getAmount() > 0){
				FluidAttributes att = fluid.getFluid().getAttributes();
				ChatFormatting rarity = att.getRarity() == Rarity.COMMON ? ChatFormatting.GRAY : att.getRarity().color;
				
				MutableComponent out = ((MutableComponent) fluid.getDisplayName()).withStyle(rarity)
						.append(new TextComponent(": " + fluid.getAmount() + "/8000mB").withStyle(ChatFormatting.GRAY));
				tooltip.add(out);
			}else{
				tooltip.add(new TextComponent(I18n.get(Lib.DESC_FLAVOUR + "drill.empty")));
			}
		});
	}
	
	@Override
	public InteractionResult useOn(UseOnContext context){
		ItemStack stack = context.getItemInHand();
		Player player = context.getPlayer();
		InteractionHand hand = context.getHand();
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		
		if(world.isClientSide){
			BlockEntity tileEntity = world.getBlockEntity(pos);
			if(tileEntity != null){
				IFluidHandler cap = tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).orElse(null);
				
				if(cap != null && FluidUtil.interactWithFluidHandler(player, hand, cap)){
					return InteractionResult.SUCCESS;
				}else{
					InteractionResult ret = FluidUtil.getFluidHandler(stack).map(handler -> {
						if(handler instanceof FluidHandlerItemStack){
							FluidHandlerItemStack can = (FluidHandlerItemStack) handler;
							
							if(can.getFluid() != null && LubricantHandler.isValidLube(can.getFluid().getFluid())){
								int amountNeeded = (LubricantHandler.getLubeAmount(can.getFluid().getFluid()) * 5 * 20);
								if(can.getFluid().getAmount() >= amountNeeded && LubricatedHandler.lubricateTile(world.getBlockEntity(pos), 20 * 30)){
									player.playSound(SoundEvents.BUCKET_EMPTY, 1f, 1f);
									if(!player.isCreative()){
										can.drain(amountNeeded, FluidAction.EXECUTE);
									}
									return InteractionResult.SUCCESS;
								}
							}
						}
						
						return InteractionResult.PASS;
					}).orElse(InteractionResult.PASS);
					
					return ret;
				}
			}
		}
		
		return InteractionResult.PASS;
	}
	
	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker){
		this.interactLivingEntity(stack, (Player) null, target, InteractionHand.MAIN_HAND);
		return true;
	}
	
	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand){
		if(target instanceof IronGolem){
			IronGolem golem = (IronGolem) target;
			
			FluidUtil.getFluidHandler(stack).ifPresent(con -> {
				if(con instanceof FluidHandlerItemStack){
					FluidHandlerItemStack handler = (FluidHandlerItemStack) con;
					
					if(handler.getFluid() != null && LubricantHandler.isValidLube(handler.getFluid().getFluid())){
						int amountNeeded = (LubricantHandler.getLubeAmount(handler.getFluid().getFluid()) * 5 * 20);
						if(handler.getFluid().getAmount() >= amountNeeded){
							player.playSound(SoundEvents.BUCKET_EMPTY, 1f, 1f);
							golem.setHealth(Math.max(golem.getHealth() + 2f, golem.getMaxHealth()));
							golem.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 1));
							golem.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20, 1));
							if(!player.isCreative()){
								handler.drain(amountNeeded, FluidAction.EXECUTE);
							}
						}
					}
				}
			});
			
			return InteractionResult.SUCCESS;
		}else
			return InteractionResult.FAIL;
	}
	
	@Override
	public boolean hasContainerItem(ItemStack stack){
		return ItemNBTHelper.hasKey(stack, "jerrycanDrain") || FluidUtil.getFluidContained(stack).isPresent();
	}
	
	@Override
	public ItemStack getContainerItem(ItemStack stack){
		if(ItemNBTHelper.hasKey(stack, "jerrycanDrain")){
			ItemStack ret = stack.copy();
			FluidUtil.getFluidHandler(ret).ifPresent(handler -> {
				handler.drain(ItemNBTHelper.getInt(ret, "jerrycanDrain"), FluidAction.EXECUTE);
				ItemNBTHelper.remove(ret, "jerrycanDrain");
			});
			return ret;
		}else if(FluidUtil.getFluidContained(stack) != null){
			ItemStack ret = stack.copy();
			FluidUtil.getFluidHandler(ret).ifPresent(handler -> handler.drain(1000, FluidAction.EXECUTE));
			return ret;
		}
		return stack;
	}
}
