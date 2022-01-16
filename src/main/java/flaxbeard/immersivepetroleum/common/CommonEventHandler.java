package flaxbeard.immersivepetroleum.common;

import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartBlockEntity;
import blusunrize.immersiveengineering.common.blocks.metal.SampleDrillBlockEntity;
import blusunrize.immersiveengineering.common.items.CoresampleItem;
import blusunrize.immersiveengineering.common.register.IEBlocks;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.DimensionChunkCoords;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler.ILubricationHandler;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler.LubricatedTileInfo;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.PumpjackHandler;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.ReservoirWorldInfo;
import flaxbeard.immersivepetroleum.common.cfg.IPServerConfig;
import flaxbeard.immersivepetroleum.common.entity.MotorboatEntity;
import flaxbeard.immersivepetroleum.common.fluids.NapalmFluid;
import flaxbeard.immersivepetroleum.common.util.IPEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class CommonEventHandler{
	@SubscribeEvent
	public void onSave(WorldEvent.Save event){
		if(event.getWorld().isClientSide()){
			IPSaveData.markInstanceAsDirty();
		}
	}
	
	@SubscribeEvent
	public void onUnload(WorldEvent.Unload event){
		if(event.getWorld().isClientSide()){
			IPSaveData.markInstanceAsDirty();
		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void handlePickupItem(RightClickBlock event){
		BlockPos pos = event.getPos();
		BlockState state = event.getWorld().getBlockState(pos);
		if(state.getBlock() == IEBlocks.MetalDevices.SAMPLE_DRILL.get()){
			BlockEntity te = event.getWorld().getBlockEntity(pos);
			if(te instanceof SampleDrillBlockEntity){
				SampleDrillBlockEntity drill = (SampleDrillBlockEntity) te;
				if(drill.isDummy()){
					drill = (SampleDrillBlockEntity) drill.master();
				}
				
				if(!drill.sample.isEmpty()){
					ColumnPos cPos = CoresampleItem.getCoords(drill.sample);
					if(cPos != null){
						try{
							Level world = event.getWorld();
							DimensionChunkCoords coords = new DimensionChunkCoords(world.dimension(), cPos.x >> 4, cPos.z >> 4);
							
							ReservoirWorldInfo info = PumpjackHandler.getOrCreateOilWorldInfo(world, coords, false);
							if(info != null && info.getType() != null){
								ItemNBTHelper.putString(drill.sample, "resType", info.getType().name);
								ItemNBTHelper.putInt(drill.sample, "resAmount", info.current);
							}else{
								ItemNBTHelper.putInt(drill.sample, "resAmount", 0);
							}
							
						}catch(Exception e){
							ImmersivePetroleum.log.warn("This aint good!", e);
						}
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public void handleBoatImmunity(LivingAttackEvent event){
		if(event.getSource() == DamageSource.LAVA || event.getSource() == DamageSource.ON_FIRE || event.getSource() == DamageSource.IN_FIRE){
			LivingEntity entity = event.getEntityLiving();
			if(entity.getVehicle() instanceof MotorboatEntity){
				MotorboatEntity boat = (MotorboatEntity) entity.getVehicle();
				if(boat.isFireproof){
					event.setCanceled(true);
					return;
				}
			}
			
			if(entity.getRemainingFireTicks() > 0 && entity.getEffect(IPEffects.ANTI_DISMOUNT_FIRE) != null){
				entity.clearFire();
				entity.removeEffect(IPEffects.ANTI_DISMOUNT_FIRE);
				event.setCanceled(true);
			}
		}
	}
	
	@SubscribeEvent
	public void handleBoatImmunity(PlayerTickEvent event){
		Player entity = event.player;
		if(entity.isOnFire() && entity.getVehicle() instanceof MotorboatEntity){
			MotorboatEntity boat = (MotorboatEntity) entity.getVehicle();
			if(boat.isFireproof){
				entity.clearFire();
				boat.setFlag(0, false);
			}
		}
	}
	
	/**
	 * Handles dismounting the Speedboat while in lava to trying avoid getting
	 * burned
	 */
	@SubscribeEvent
	public void handleDismountingBoat(EntityMountEvent event){
		if(event.getEntityMounting() == null){
			return;
		}
		
		if(event.getEntityMounting() instanceof LivingEntity && event.getEntityBeingMounted() instanceof MotorboatEntity){
			if(event.isDismounting()){
				MotorboatEntity boat = (MotorboatEntity) event.getEntityBeingMounted();
				
				if(boat.isFireproof){
					FluidState fluidstate = event.getWorldObj().getBlockState(new BlockPos(boat.position().add(0.5, 0, 0.5))).getFluidState();
					if(fluidstate != Fluids.EMPTY.defaultFluidState() && fluidstate.is(FluidTags.LAVA)){
						LivingEntity living = (LivingEntity) event.getEntityMounting();
						
						living.addEffect(new MobEffectInstance(IPEffects.ANTI_DISMOUNT_FIRE, 1, 0, false, false));
						return;
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public void handleLubricatingMachinesServer(WorldTickEvent event){
		if(event.phase == Phase.END){
			handleLubricatingMachines(event.world);
		}
	}
	
	static final Random random = new Random();
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void handleLubricatingMachines(Level world){
		Set<LubricatedTileInfo> toRemove = new HashSet<LubricatedTileInfo>();
		for(LubricatedTileInfo info:LubricatedHandler.lubricatedTiles){
			if(info.world == world.dimension() && world.isAreaLoaded(info.pos, 0)){
				BlockEntity te = world.getBlockEntity(info.pos);
				ILubricationHandler lubeHandler = LubricatedHandler.getHandlerForTile(te);
				if(lubeHandler != null){
					if(lubeHandler.isMachineEnabled(world, te)){
						lubeHandler.lubricate(world, info.ticks, te);
					}
					
					if(!world.isClientSide){
						if(te instanceof MultiblockPartBlockEntity){
							MultiblockPartBlockEntity<?> part = (MultiblockPartBlockEntity<?>) te;

							BlockParticleOption lubeParticle = new BlockParticleOption(ParticleTypes.FALLING_DUST, IPContent.Fluids.lubricant.block.defaultBlockState());
							Vec3i size = lubeHandler.getStructureDimensions();
							
							int numBlocks = (int) (size.getX() * size.getY() * size.getZ() * 0.25F);
							
							for(int i = 0;i < numBlocks;i++){
								BlockPos pos = part.getBlockPosForPos(new BlockPos(size.getX() * random.nextFloat(), size.getY() * random.nextFloat(), size.getZ() * random.nextFloat()));
								if(world.getBlockState(pos) == Blocks.AIR.defaultBlockState())
									continue;
								
								BlockEntity te2 = world.getBlockEntity(info.pos);
								if(te2 != null && te2 instanceof MultiblockPartBlockEntity){
									if(((MultiblockPartBlockEntity<?>) te2).master() == part.master()){
										for(Direction facing:Direction.Plane.HORIZONTAL){
											if(world.random.nextInt(30) == 0){// && world.getBlockState(pos.offset(facing)).getBlock().isReplaceable(world, pos.offset(facing))){
												Vec3i direction = facing.getNormal();
												world.addParticle(lubeParticle,
														pos.getX() + .5f + direction.getX() * .65f,
														pos.getY() + 1,
														pos.getZ() + .5f + direction.getZ() * .65f,
														0, 0, 0);
											}
										}
									}
								}
							}
						}
					}
					
					info.ticks--;
					if(info.ticks == 0)
						toRemove.add(info);
				}
			}
		}
		
		for(LubricatedTileInfo info:toRemove){
			LubricatedHandler.lubricatedTiles.remove(info);
		}
	}
	
	@SubscribeEvent
	public void onEntityJoiningWorld(EntityJoinWorldEvent event){
		if(event.getEntity() instanceof Player){
			if(event.getEntity() instanceof FakePlayer){
				return;
			}
			
			if(IPServerConfig.MISCELLANEOUS.autounlock_recipes.get()){
				List<Recipe<?>> l = new ArrayList<Recipe<?>>();
				Collection<Recipe<?>> recipes = event.getWorld().getRecipeManager().getRecipes();
				recipes.forEach(recipe -> {
					ResourceLocation name = recipe.getId();
					if(name.getNamespace() == ImmersivePetroleum.MODID){
						if(recipe.getResultItem() != ItemStack.EMPTY){
							l.add(recipe);
						}
					}
				});
				
				((Player) event.getEntity()).awardRecipes(l);
			}
		}
	}
	
	@SubscribeEvent
	public void test(LivingEvent.LivingUpdateEvent event){
		if(event.getEntityLiving() instanceof Player){
			// event.getEntityLiving().setFire(1);
		}
	}
	
	public static Map<ResourceLocation, List<BlockPos>> napalmPositions = new HashMap<>();
	public static Map<ResourceLocation, List<BlockPos>> toRemove = new HashMap<>();
	
	@SubscribeEvent
	public void handleNapalm(WorldTickEvent event){
		ResourceLocation d = event.world.dimension().getRegistryName();
		
		if(event.phase == Phase.START){
			toRemove.put(d, new ArrayList<>());
			if(napalmPositions.get(d) != null){
				List<BlockPos> iterate = new ArrayList<>(napalmPositions.get(d));
				for(BlockPos position:iterate){
					BlockState state = event.world.getBlockState(position);
					if(state.getBlock() instanceof LiquidBlock && state.getBlock() == IPContent.Fluids.napalm.block){
						((NapalmFluid) IPContent.Fluids.napalm).processFire(event.world, position);
					}
					toRemove.get(d).add(position);
				}
			}
		}else if(event.phase == Phase.END){
			if(toRemove.get(d) != null && napalmPositions.get(d) != null){
				for(BlockPos position:toRemove.get(d)){
					napalmPositions.get(d).remove(position);
				}
			}
		}
	}
}
