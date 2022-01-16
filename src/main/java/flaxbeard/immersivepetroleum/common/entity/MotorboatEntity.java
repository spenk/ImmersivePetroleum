package flaxbeard.immersivepetroleum.common.entity;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.util.IESounds;
import blusunrize.immersiveengineering.common.util.Utils;
import com.mojang.math.Vector3d;
import com.mojang.math.Vector3f;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.energy.FuelHandler;
import flaxbeard.immersivepetroleum.common.IPContent.BoatUpgrades;
import flaxbeard.immersivepetroleum.common.IPContent.Items;
import flaxbeard.immersivepetroleum.common.items.DebugItem;
import flaxbeard.immersivepetroleum.common.items.MotorboatItem;
import flaxbeard.immersivepetroleum.common.network.IPPacketHandler;
import flaxbeard.immersivepetroleum.common.network.MessageConsumeBoatFuel;
import flaxbeard.immersivepetroleum.common.util.IPItemStackHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.client.CSteerBoatPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.GameRules;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class MotorboatEntity extends Boat implements IEntityAdditionalSpawnData {
	
	public static final EntityType<MotorboatEntity> TYPE = createType();
	
	private static EntityType<MotorboatEntity> createType(){
		EntityType<MotorboatEntity> ret = EntityType.Builder.<MotorboatEntity> create(MotorboatEntity::new, MobCategory.MISC).size(1.375F, 0.5625F).build(ImmersivePetroleum.MODID + ":speedboat");
		ret.setRegistryName(ImmersivePetroleum.MODID, "speedboat");
		return ret;
	}
	
	public static DataParameter<Byte> getFlags(){
		return FLAGS;
	}
	
	/**
	 * Storage for {@link ResourceLocation} using
	 * {@link ResourceLocation#toString()}
	 */
	static final DataParameter<String> TANK_FLUID = EntityDataManager.createKey(MotorboatEntity.class, DataSerializers.STRING);
	static final DataParameter<Integer> TANK_AMOUNT = EntityDataManager.createKey(MotorboatEntity.class, DataSerializers.VARINT);
	
	static final DataParameter<ItemStack> UPGRADE_0 = EntityDataManager.createKey(MotorboatEntity.class, DataSerializers.ITEMSTACK);
	static final DataParameter<ItemStack> UPGRADE_1 = EntityDataManager.createKey(MotorboatEntity.class, DataSerializers.ITEMSTACK);
	static final DataParameter<ItemStack> UPGRADE_2 = EntityDataManager.createKey(MotorboatEntity.class, DataSerializers.ITEMSTACK);
	static final DataParameter<ItemStack> UPGRADE_3 = EntityDataManager.createKey(MotorboatEntity.class, DataSerializers.ITEMSTACK);
	
	public boolean isFireproof = false;
	public boolean hasIcebreaker = false;
	public boolean hasTank = false;
	public boolean hasRudders = false;
	public boolean hasPaddles = false;
	public boolean isBoosting = false;
	public float lastMoving;
	public float propellerRotation = 0F;
	
	public MotorboatEntity(Level world){
		this(TYPE, world);
	}
	
	public MotorboatEntity(Level world, double x, double y, double z){
		this(TYPE, world);
		setPosition(x, y, z);
		this.prevPosX = x;
		this.prevPosY = y;
		this.prevPosZ = z;
	}
	
	public MotorboatEntity(EntityType<MotorboatEntity> type, Level world){
		super(type, world);
		this.preventEntitySpawning = true;
	}
	
	@Override
	public EntityType<?> getType(){
		return TYPE;
	}
	
	@Override
	protected void registerData(){
		super.registerData();
		this.dataManager.register(TANK_FLUID, "");
		this.dataManager.register(TANK_AMOUNT, Integer.valueOf(0));
		this.dataManager.register(UPGRADE_0, ItemStack.EMPTY);
		this.dataManager.register(UPGRADE_1, ItemStack.EMPTY);
		this.dataManager.register(UPGRADE_2, ItemStack.EMPTY);
		this.dataManager.register(UPGRADE_3, ItemStack.EMPTY);
	}
	
	public void setUpgrades(NonNullList<ItemStack> stacks){
		if(stacks != null && stacks.size() > 0){
			ItemStack o0 = stacks.get(0) == null ? ItemStack.EMPTY : stacks.get(0);
			ItemStack o1 = stacks.get(1) == null ? ItemStack.EMPTY : stacks.get(1);
			ItemStack o2 = stacks.get(2) == null ? ItemStack.EMPTY : stacks.get(2);
			ItemStack o3 = stacks.get(3) == null ? ItemStack.EMPTY : stacks.get(3);
			this.dataManager.set(UPGRADE_0, o0);
			this.dataManager.set(UPGRADE_1, o1);
			this.dataManager.set(UPGRADE_2, o2);
			this.dataManager.set(UPGRADE_3, o3);
		}
	}
	
	public boolean isLeftDown(){
		return this.leftInputDown;
	}
	
	public boolean isRightDown(){
		return this.rightInputDown;
	}
	
	public boolean isForwardDown(){
		return this.forwardInputDown;
	}
	
	public boolean isBackDown(){
		return this.backInputDown;
	}
	
	@Override
	public void notifyDataManagerChange(DataParameter<?> key){
		super.notifyDataManagerChange(key);
		if(key == UPGRADE_0 || key == UPGRADE_1 || key == UPGRADE_2 || key == UPGRADE_3){
			NonNullList<ItemStack> upgrades = getUpgrades();
			this.isFireproof = false;
			this.hasIcebreaker = false;
			for(ItemStack upgrade:upgrades){
				if(upgrade != null && upgrade != ItemStack.EMPTY){
					Item item = upgrade.getItem();
					if(item == BoatUpgrades.reinforced_hull){
						this.isFireproof = true;
					}else if(item == BoatUpgrades.ice_breaker){
						this.hasIcebreaker = true;
					}else if(item == BoatUpgrades.tank){
						this.hasTank = true;
					}else if(item == BoatUpgrades.rudders){
						this.hasRudders = true;
					}else if(item == BoatUpgrades.paddles){
						this.hasPaddles = true;
					}
				}
			}
		}
	}
	
	public void setContainedFluid(FluidStack stack){
		if(stack == null){
			this.dataManager.set(TANK_FLUID, "");
			this.dataManager.set(TANK_AMOUNT, 0);
		}else{
			this.dataManager.set(TANK_FLUID, stack.getFluid() == null ? "" : stack.getFluid().getRegistryName().toString());
			this.dataManager.set(TANK_AMOUNT, stack.getAmount());
		}
	}
	
	public FluidStack getContainedFluid(){
		String fluidName = this.dataManager.get(TANK_FLUID);
		int amount = this.dataManager.get(TANK_AMOUNT).intValue();
		
		if(fluidName == null || fluidName.isEmpty() || amount == 0)
			return FluidStack.EMPTY;
		
		Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidName));
		if(fluid == null)
			return FluidStack.EMPTY;
		
		return new FluidStack(fluid, amount);
	}
	
	@Override
	protected void readAdditional(CompoundTag compound){
		super.readAdditional(compound);
		
		String fluid = "";
		int amount = 0;
		ItemStack stack0 = ItemStack.EMPTY;
		ItemStack stack1 = ItemStack.EMPTY;
		ItemStack stack2 = ItemStack.EMPTY;
		ItemStack stack3 = ItemStack.EMPTY;
		
		if(compound.contains("tank")){
			CompoundTag tank = compound.getCompound("tank");
			fluid = tank.getString("fluid");
			amount = tank.getInt("amount");
		}
		
		if(compound.contains("upgrades")){
			CompoundTag upgrades = compound.getCompound("upgrades");
			stack0 = ItemStack.read(upgrades.getCompound("0"));
			stack1 = ItemStack.read(upgrades.getCompound("1"));
			stack2 = ItemStack.read(upgrades.getCompound("2"));
			stack3 = ItemStack.read(upgrades.getCompound("3"));
		}
		
		this.dataManager.set(TANK_FLUID, fluid);
		this.dataManager.set(TANK_AMOUNT, amount);
		this.dataManager.set(UPGRADE_0, stack0);
		this.dataManager.set(UPGRADE_1, stack1);
		this.dataManager.set(UPGRADE_2, stack2);
		this.dataManager.set(UPGRADE_3, stack3);
	}
	
	@Override
	protected void writeAdditional(CompoundTag compound){
		super.writeAdditional(compound);
		
		String fluid = this.dataManager.get(TANK_FLUID);
		int amount = this.dataManager.get(TANK_AMOUNT);
		ItemStack stack0 = this.dataManager.get(UPGRADE_0);
		ItemStack stack1 = this.dataManager.get(UPGRADE_1);
		ItemStack stack2 = this.dataManager.get(UPGRADE_2);
		ItemStack stack3 = this.dataManager.get(UPGRADE_3);
		
		CompoundTag tank = new CompoundTag();
		tank.putString("fluid", fluid);
		tank.putInt("amount", amount);
		compound.put("tank", tank);
		
		CompoundTag upgrades = new CompoundTag();
		upgrades.put("0", stack0.serializeNBT());
		upgrades.put("1", stack1.serializeNBT());
		upgrades.put("2", stack2.serializeNBT());
		upgrades.put("3", stack3.serializeNBT());
		compound.put("upgrades", upgrades);
	}
	
	@Override
	public double getMountedYOffset(){
		return isInLava() ? -0.1D + (3.9F / 16F) : -0.1D;
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float amount){
		if(isInvulnerableTo(source) || (this.isFireproof && source.isFireDamage())){
			return false;
		}else if(!this.level.isRemote && isAlive()){
			if(source instanceof IndirectEntityDamageSource && source.getImmediateSource() != null && isPassenger(source.getImmediateSource())){
				return false;
			}else{
				setForwardDirection(-getForwardDirection());
				setTimeSinceHit(10);
				setDamageTaken(getDamageTaken() + amount * 10.0F);
				markVelocityChanged();
				boolean isPlayer = source.getImmediateSource() instanceof Player;
				boolean isCreativePlayer = isPlayer && ((Player) source.getImmediateSource()).abilities.isCreativeMode;
				if((isCreativePlayer || getDamageTaken() > 40.0F) && (!this.isFireproof || isPlayer) || (getDamageTaken() > 240.0F)){
					if(!isCreativePlayer && this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)){
						MotorboatItem item = (MotorboatItem) getItemBoat();
						ItemStack stack = new ItemStack(item, 1);
						
						IItemHandler handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElse(null);
						if(handler != null && handler instanceof IPItemStackHandler){
							NonNullList<ItemStack> upgrades = getUpgrades();
							for(int i = 0;i < handler.getSlots();i++){
								handler.insertItem(i, upgrades.get(i), false);
							}
						}
						
						writeTank(stack.getOrCreateTag(), true);
						
						if(isPlayer){
							Player player = (Player) source.getImmediateSource();
							if(!player.addItemStackToInventory(stack)){
								ItemEntity itemEntity = new ItemEntity(this.world, player.getPosX(), player.getPosY(), player.getPosZ(), stack);
								itemEntity.setNoPickupDelay();
								this.world.addEntity(itemEntity);
							}
						}else{
							entityDropItem(stack, 0F);
						}
					}
					
					remove();
				}
				
				return true;
			}
		}else{
			return true;
		}
	}
	
	public void readTank(CompoundTag nbt){
		FluidTank tank = new FluidTank(getMaxFuel());
		if(nbt != null)
			tank.readFromNBT(nbt.getCompound("tank"));
		
		setContainedFluid(tank.getFluid());
	}
	
	public void writeTank(CompoundTag nbt, boolean toItem){
		FluidTank tank = new FluidTank(getMaxFuel());
		tank.setFluid(getContainedFluid());
		
		boolean write = tank.getFluidAmount() > 0;
		if(!toItem || write)
			nbt.put("tank", tank.writeToNBT(new CompoundTag()));
	}
	
	@Override
	public InteractionResult processInitialInteract(Player player, Hand hand){
		ItemStack stack = player.getHeldItem(hand);
		
		if(stack != ItemStack.EMPTY && stack.getItem() instanceof DebugItem){
			((DebugItem) stack.getItem()).onSpeedboatClick(this, player, stack);
			return InteractionResult.SUCCESS;
		}
		
		if(Utils.isFluidRelatedItemStack(stack)){
			FluidStack fstack = FluidUtil.getFluidContained(stack).orElse(null);
			if(fstack != null){
				FluidTank tank = new FluidTank(getMaxFuel()){
					@Override
					public boolean isFluidValid(FluidStack stack){
						return FuelHandler.isValidBoatFuel(stack.getFluid());
					}
				};
				
				FluidStack fs = getContainedFluid();
				tank.setFluid(fs);
				
				FluidUtil.interactWithFluidHandler(player, hand, tank);
				
				setContainedFluid(tank.getFluid());
			}
			return InteractionResult.SUCCESS;
		}
		
		if(!this.level.isRemote && !player.isShiftKeyDown() && this.outOfControlTicks < 60.0F && !player.isRidingSameEntity(this)){
			player.startRiding(this);
			return InteractionResult.SUCCESS;
		}
		
		return InteractionResult.FAIL;
	}
	
	@Override
	public void updateInputs(boolean p_184442_1_, boolean p_184442_2_, boolean p_184442_3_, boolean p_184442_4_){
		super.updateInputs(p_184442_1_, p_184442_2_, p_184442_3_, p_184442_4_);
		this.isBoosting = isEmergency() ? false : (forwardInputDown && Minecraft.getInstance().gameSettings.keyBindJump.isKeyDown());
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void tick(){
		this.previousStatus = this.status;
		this.status = this.getBoatStatus();
		if(this.status != BoatEntity.Status.UNDER_WATER && this.status != BoatEntity.Status.UNDER_FLOWING_WATER){
			this.outOfControlTicks = 0.0F;
		}else{
			++this.outOfControlTicks;
		}
		
		if(!this.level.isRemote && this.outOfControlTicks >= 60.0F){
			this.removePassengers();
		}
		
		if(this.getTimeSinceHit() > 0){
			this.setTimeSinceHit(this.getTimeSinceHit() - 1);
		}
		
		if(this.getDamageTaken() > 0.0F){
			this.setDamageTaken(this.getDamageTaken() - 1.0F);
		}
		
		this.prevPosX = this.getPosX();
		this.prevPosY = this.getPosY();
		this.prevPosZ = this.getPosZ();
		
		{ // From Entity.tick()
			if(!this.world.isRemote){
				this.setFlag(6, this.isGlowing());
			}
			this.baseTick();
		}
		this.tickLerp();
		
		if(this.canPassengerSteer()){
			if(this.getPassengers().isEmpty() || !(this.getPassengers().get(0) instanceof Player)){
				this.setPaddleState(false, false);
			}
			
			this.updateMotion();
			if(this.world.isRemote){
				this.controlBoat();
				this.world.sendPacketToServer(new CSteerBoatPacket(this.getPaddleState(0), this.getPaddleState(1)));
			}
			
			this.move(MoverType.SELF, this.getMotion());
		}else{
			this.setMotion(Vector3d.ZERO);
		}
		
		this.updateRocking();
		
		if(this.world.isRemote){
			if(!isEmergency()){
				float moving = (this.forwardInputDown || this.backInputDown) ? (isBoosting ? .9F : .7F) : 0.5F;
				if(lastMoving != moving){
					ImmersivePetroleum.proxy.handleEntitySound(IESounds.dieselGenerator, this, false, .5f, 0.5F);
				}
				ImmersivePetroleum.proxy.handleEntitySound(IESounds.dieselGenerator, this, this.isBeingRidden() && this.getContainedFluid() != FluidStack.EMPTY && this.getContainedFluid().getAmount() > 0, this.forwardInputDown || this.backInputDown ? .5f : .3f, moving);
				lastMoving = moving;
				
				if(this.forwardInputDown && this.level.random.nextInt(2) == 0){
					if(isInLava()){
						if(this.world.rand.nextInt(3) == 0){
							float xO = (float) (Mth.sin(-this.rotationYaw * 0.017453292F)) + (world.rand.nextFloat() - .5F) * .3F;
							float zO = (float) (Mth.cos(this.rotationYaw * 0.017453292F)) + (world.rand.nextFloat() - .5F) * .3F;
							float yO = .4F + (level.random.nextFloat() - .5F) * .3F;
							Vector3d motion = getMotion();
							level.addParticle(ParticleTypes.LAVA, getPosX() - xO * 1.5F, getPosY() + yO, getPosZ() - zO * 1.5F, -2 * motion.getX(), 0, -2 * motion.getZ());
						}
					}else{
						float xO = (float) (Mth.sin(-this.rotationYaw * 0.017453292F)) + (world.rand.nextFloat() - .5F) * .3F;
						float zO = (float) (Mth.cos(this.rotationYaw * 0.017453292F)) + (world.rand.nextFloat() - .5F) * .3F;
						float yO = .1F + (level.random.nextFloat() - .5F) * .3F;
						level.addParticle(ParticleTypes.BUBBLE, getPosX() - xO * 1.5F, getPosY() + yO, getPosZ() - zO * 1.5F, 0, 0, 0);
					}
				}
				if(isBoosting && this.level.random.nextInt(2) == 0){
					float xO = (float) (Mth.sin(-this.rotationYaw * 0.017453292F)) + (world.rand.nextFloat() - .5F) * .3F;
					float zO = (float) (Mth.cos(this.rotationYaw * 0.017453292F)) + (world.rand.nextFloat() - .5F) * .3F;
					float yO = .8F + (level.random.nextFloat() - .5F) * .3F;
					level.addParticle(ParticleTypes.SMOKE, getPosX() - xO * 1.3F, getPosY() + yO, getPosZ() - zO * 1.3F, 0, 0, 0);
				}
			}else{
				ImmersivePetroleum.proxy.handleEntitySound(IESounds.dieselGenerator, this, false, .5f, 0.5F);
			}
		}
		
		if(this.isEmergency()){
			for(int i = 0;i <= 1;++i){
				if(this.getPaddleState(i)){
					this.paddlePositions[i] = (float) ((double) this.paddlePositions[i] + (double) ((float) Math.PI / 4F));
				}else{
					this.paddlePositions[i] = 0.0F;
				}
			}
		}else{
			if(this.getPaddleState(0)){
				this.paddlePositions[0] = (float) ((double) this.paddlePositions[0] + (isBoosting ? 0.02D : 0.01D));
			}else if(this.getPaddleState(1)){
				this.paddlePositions[0] = (float) ((double) this.paddlePositions[0] - 0.01D);
			}
		}
		
		float xO = (float) (Mth.sin(-this.rotationYaw * 0.017453292F));
		float zO = (float) (Mth.cos(this.rotationYaw * 0.017453292F));
		Vector3f vec = normalizeVector(new Vector3f(xO, zO, 0.0F));
		
		if(this.hasIcebreaker && !isEmergency()){
			AABB bb = getBoundingBox().inflate(0.1);
			BlockPos.MutableBlockPos mutableBlockPos0 = new BlockPos.MutableBlockPos(bb.minX + 0.001D, bb.minY + 0.001D, bb.minZ + 0.001D);
			BlockPos.MutableBlockPos mutableBlockPos1 = new BlockPos.MutableBlockPos(bb.maxX - 0.001D, bb.maxY - 0.001D, bb.maxZ - 0.001D);
			BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
			
			if(this.world.isAreaLoaded(mutableBlockPos0, mutableBlockPos1)){
				for(int i = mutableBlockPos0.getX();i <= mutableBlockPos1.getX();++i){
					for(int j = mutableBlockPos0.getY();j <= mutableBlockPos1.getY();++j){
						for(int k = mutableBlockPos0.getZ();k <= mutableBlockPos1.getZ();++k){
							mutableBlockPos2.setPos(i, j, k);
							BlockState BlockState = this.level.getBlockState(mutableBlockPos2);
							
							Vector3f vec2 = new Vector3f((float) (i + 0.5f - getPosX()), (float) (k + 0.5f - getPosZ()), 0.0F);
							normalizeVector(vec2);
							
							float sim = dotVector(vec2, vec);
							
							if(BlockState.getBlock() == Blocks.ICE && sim > .3f){
								this.level.destroyBlock(mutableBlockPos2, false);
								this.level.setBlockState(mutableBlockPos2, Blocks.WATER.defaultBlockState());
							}
						}
					}
				}
			}
		}
		
		this.doBlockCollisions();
		List<Entity> list = this.level.getEntities(this, this.getBoundingBox().inflate((double) 0.2F, (double) -0.01F, (double) 0.2F), EntityPredicates.pushableBy(this));
		if(!list.isEmpty()){
			boolean flag = !this.level.isRemote && !(this.getControllingPassenger() instanceof Player);
			
			for(int j = 0;j < list.size();++j){
				Entity entity = list.get(j);
				
				if(!entity.isPassenger(this)){
					if(flag && this.getPassengers().size() < 2 && !entity.isPassenger() && entity.getWidth() < this.getWidth() && entity instanceof LivingEntity && !(entity instanceof WaterMobEntity) && !(entity instanceof Player)){
						entity.startRiding(this);
					}else{
						this.applyEntityCollision(entity);
						
						if(this.hasIcebreaker){
							if(entity instanceof LivingEntity && !(entity instanceof Player) && this.getControllingPassenger() instanceof Player){
								Vector3f vec2 = new Vector3f((float) (entity.getPosX() - getPosX()), (float) (entity.getPosZ() - getPosZ()), 0.0F);
								normalizeVector(vec2);
								
								float sim = dotVector(vec2, vec);
								
								if(sim > .5f){
									Vector3d motion = entity.getMotion();
									entity.attackEntityFrom(DamageSource.causePlayerDamage((Player) this.getControllingPassenger()), 4);
									entity.setMotion(new Vector3d(motion.x + (vec2.getX() * .75F), motion.y, motion.z + (vec2.getY() * .75F)));
								}
							}
						}
					}
				}
			}
		}
	}
	
	/** Because fuck you for making that client side only */
	private Vector3f normalizeVector(Vector3f vec){
		float f = vec.getX() * vec.getX() + vec.getY() * vec.getY() + vec.getZ() * vec.getZ();
		if(!((double) f < 1.0E-5D)){
			float f1 = 1 / Mth.sqrt(f);
			vec.setX(vec.getX() * f1);
			vec.setX(vec.getY() * f1);
			vec.setX(vec.getZ() * f1);
		}
		return vec;
	}
	
	/** Because fuck you for making that client side only */
	private float dotVector(Vector3f a, Vector3f b){
		return a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ();
	}
	
	@Override
	protected void controlBoat(){
		if(isBeingRidden()){
			float f = 0.0F;
			
			if(isEmergency()){
				if(this.leftInputDown){
					--this.deltaRotation;
				}
				
				if(this.rightInputDown){
					++this.deltaRotation;
				}
				
				if(this.rightInputDown != this.leftInputDown && !this.forwardInputDown && !this.backInputDown){
					f += 0.005F;
				}
				
				this.rotationYaw += this.deltaRotation;
				if(this.forwardInputDown){
					f += 0.04F;
				}
				
				if(this.backInputDown){
					f -= 0.005F;
				}
				
				this.setMotion(this.getMotion().add((double) (Mth.sin(-this.rotationYaw * ((float) Math.PI / 180F)) * f), 0.0D, (double) (MathHelper.cos(this.rotationYaw * ((float) Math.PI / 180F)) * f)));
				this.setPaddleState(this.rightInputDown && !this.leftInputDown || this.forwardInputDown, this.leftInputDown && !this.rightInputDown || this.forwardInputDown);
			}else{
				FluidStack fluid = getContainedFluid();
				int consumeAmount = 0;
				if(fluid != FluidStack.EMPTY){
					consumeAmount = FuelHandler.getBoatFuelUsedPerTick(fluid.getFluid());
				}
				
				if(fluid != FluidStack.EMPTY && fluid.getAmount() >= consumeAmount && (this.forwardInputDown || this.backInputDown)){
					int toConsume = consumeAmount;
					if(this.forwardInputDown){
						f += 0.05F;
						if(this.isBoosting && fluid.getAmount() >= 3 * consumeAmount){
							f *= 1.6;
							toConsume *= 3;
						}
					}
					
					if(this.backInputDown){
						f -= 0.01F;
					}
					
					fluid.setAmount(Math.max(0, fluid.getAmount() - toConsume));
					setContainedFluid(fluid);
					
					if(this.level.isRemote)
						IPPacketHandler.sendToServer(new MessageConsumeBoatFuel(toConsume));
					
					setPaddleState(this.forwardInputDown, this.backInputDown);
				}else{
					setPaddleState(false, false);
				}
				
				Vector3d motion = this.getMotion().add((double) (Mth.sin(-this.rotationYaw * ((float) Math.PI / 180F)) * f), 0.0D, (double) (MathHelper.cos(this.rotationYaw * ((float) Math.PI / 180F)) * f));
				
				if(this.leftInputDown || this.rightInputDown){
					float speed = Mth.sqrt(motion.x * motion.x + motion.z * motion.z);
					
					if(this.rightInputDown){
						this.deltaRotation += 1.1F * speed * (this.hasRudders ? 1.5F : 1F) * (this.isBoosting ? 0.5F : 1) * (this.backInputDown && !this.forwardInputDown ? 2F : 1F);

						this.propellerRotation = Mth.clamp(this.propellerRotation - 0.2F, -1.0F, 1.0F);
					}
					
					if(this.leftInputDown){
						this.deltaRotation -= 1.1F * speed * (this.hasRudders ? 1.5F : 1F) * (this.isBoosting ? 0.5F : 1) * (this.backInputDown && !this.forwardInputDown ? 2F : 1F);
						
						this.propellerRotation = Mth.clamp(this.propellerRotation + 0.2F, -1.0F, 1.0F);
					}
				}
				
				if(!this.leftInputDown && !this.rightInputDown && this.propellerRotation != 0.0F){
					this.propellerRotation *= 0.7F;
					if(this.propellerRotation > -1.0E-2F && this.propellerRotation < 1.0E-2F){
						this.propellerRotation = 0;
					}
				}
				
				this.rotationYaw += this.deltaRotation;
				
				this.setMotion(motion);
				this.setPaddleState((this.rightInputDown && !this.leftInputDown || this.forwardInputDown), (this.leftInputDown && !this.rightInputDown || this.forwardInputDown));
			}
		}
	}
	
	public int getMaxFuel(){
		return this.hasTank ? 16000 : 8000;
	}
	
	@Override
	public Item getItemBoat(){
		return Items.speedboat;
	}
	
	@Override
	public boolean isBurning(){
		if(this.isFireproof)
			return false;
		
		return super.isOnFire();
	}
	
	public boolean isEmergency(){
		FluidStack fluid = getContainedFluid();
		if(fluid != FluidStack.EMPTY){
			int consumeAmount = FuelHandler.getBoatFuelUsedPerTick(fluid.getFluid());
			return fluid.getAmount() < consumeAmount && this.hasPaddles;
		}
		
		return this.hasPaddles;
	}
	
	public NonNullList<ItemStack> getUpgrades(){
		NonNullList<ItemStack> stackList = NonNullList.withSize(4, ItemStack.EMPTY);
		stackList.set(0, this.entityData.get(UPGRADE_0));
		stackList.set(1, this.entityData.get(UPGRADE_1));
		stackList.set(2, this.entityData.get(UPGRADE_2));
		stackList.set(3, this.entityData.get(UPGRADE_3));
		return stackList;
	}
	
	public String[] getOverlayText(Player player, HitResult mop){
		if(Utils.isFluidRelatedItemStack(player.getItemInHand(InteractionHand.MAIN_HAND))){
			String s = null;
			FluidStack stack = getContainedFluid();
			if(stack != FluidStack.EMPTY){
				s = stack.getDisplayName().getString() + ": " + stack.getAmount() + "mB";
			}else{
				s = I18n.format(Lib.GUI + "empty");
			}
			return new String[]{s};
			
		}
		return null;
	}
	
	@Override
	public float getWaterLevelAbove(){
		AABB aabb = this.getBoundingBox();
		int i = Mth.floor(aabb.minX);
		int j = Mth.ceil(aabb.maxX);
		int k = Mth.floor(aabb.maxY);
		int l = Mth.ceil(aabb.maxY - this.lastYd);
		int i1 = Mth.floor(aabb.minZ);
		int j1 = Mth.ceil(aabb.maxZ);
		BlockPos.MutableBlockPos blockpos$mutable = new BlockPos.MutableBlockPos();
		
		label39: for(int k1 = k;k1 < l;++k1){
			float f = 0.0F;
			
			for(int l1 = i;l1 < j;++l1){
				for(int i2 = i1;i2 < j1;++i2){
					blockpos$mutable.setPos(l1, k1, i2);
					FluidState fluidstate = this.level.getFluidState(blockpos$mutable);
					if(fluidstate.isTagged(FluidTags.WATER) || (this.isFireproof && fluidstate.isTagged(FluidTags.LAVA))){
						f = Math.max(f, fluidstate.getActualHeight(this.world, blockpos$mutable));
					}
					
					if(f >= 1.0F){
						continue label39;
					}
				}
			}
			
			if(f < 1.0F){
				return (float) blockpos$mutable.getY() + f;
			}
		}
		
		return (float) (l + 1);
	}
	
	@Override
	protected boolean checkInWater(){
		AABB aabb = this.getBoundingBox();
		int i = Mth.floor(aabb.minX);
		int j = Mth.ceil(aabb.maxX);
		int k = Mth.floor(aabb.minY);
		int l = Mth.ceil(aabb.minY + 0.001D);
		int i1 = Mth.floor(aabb.minZ);
		int j1 = Mth.ceil(aabb.maxZ);
		boolean flag = false;
		this.waterLevel = Double.MIN_VALUE;
		BlockPos.MutableBlockPos blockpos$mutable = new BlockPos.MutableBlockPos();
		
		for(int k1 = i;k1 < j;++k1){
			for(int l1 = k;l1 < l;++l1){
				for(int i2 = i1;i2 < j1;++i2){
					blockpos$mutable.setPos(k1, l1, i2);
					FluidState fluidstate = this.world.getFluidState(blockpos$mutable);
					if(fluidstate.isTagged(FluidTags.WATER) || (this.isFireproof && fluidstate.isTagged(FluidTags.LAVA))){
						float f = (float) l1 + fluidstate.getActualHeight(this.world, blockpos$mutable);
						this.waterLevel = Math.max((double) f, this.waterLevel);
						flag |= aabb.minY < (double) f;
					}
				}
			}
		}
		
		return flag;
	}
	
	@Override
	protected Status getUnderwaterStatus(){
		AABB aabb = this.getBoundingBox();
		double d0 = aabb.maxY + 0.001D;
		int i = Mth.floor(aabb.minX);
		int j = Mth.ceil(aabb.maxX);
		int k = Mth.floor(aabb.maxY);
		int l = Mth.ceil(d0);
		int i1 = Mth.floor(aabb.minZ);
		int j1 = Mth.ceil(aabb.maxZ);
		boolean flag = false;
		BlockPos.MutableBlockPos blockpos$mutable = new BlockPos.MutableBlockPos();
		
		for(int k1 = i;k1 < j;++k1){
			for(int l1 = k;l1 < l;++l1){
				for(int i2 = i1;i2 < j1;++i2){
					blockpos$mutable.setPos(k1, l1, i2);
					FluidState fluidstate = this.level.getFluidState(blockpos$mutable);
					if((fluidstate.isTagged(FluidTags.WATER) || ((this.isFireproof && fluidstate.isTagged(FluidTags.LAVA)))) && d0 < (double) ((float) blockpos$mutable.getY() + fluidstate.getActualHeight(this.world, blockpos$mutable))){
						if(!fluidstate.isSource()){
							return BoatEntity.Status.UNDER_FLOWING_WATER;
						}
						
						flag = true;
					}
				}
			}
		}
		
		return flag ? BoatEntity.Status.UNDER_WATER : null;
	}
	
	public boolean isLeftInDown(){
		return this.leftInputDown;
	}
	
	public boolean isRightInDown(){
		return this.rightInputDown;
	}
	
	public boolean isForwardInDown(){
		return this.forwardInputDown;
	}
	
	public boolean isBackInDown(){
		return this.backInputDown;
	}
	
	@Override
	public boolean getFlag(int flag){
		return super.getFlag(flag);
	}
	
	@Override
	public void setFlag(int flag, boolean set){
		super.setFlag(flag, set);
	}
	
	@Override
	public IPacket<?> createSpawnPacket(){
		return NetworkHooks.getEntitySpawningPacket(this);
	}
	
	@Override
	public void readSpawnData(FriendlyByteBuf buffer){
		String fluid = buffer.readUtf();
		int amount = buffer.readInt();
		ItemStack stack0 = buffer.readItem();
		ItemStack stack1 = buffer.readItem();
		ItemStack stack2 = buffer.readItem();
		ItemStack stack3 = buffer.readItem();
		
		this.entityData.set(TANK_FLUID, fluid);
		this.entityData.set(TANK_AMOUNT, amount);
		this.entityData.set(UPGRADE_0, stack0);
		this.entityData.set(UPGRADE_1, stack1);
		this.entityData.set(UPGRADE_2, stack2);
		this.entityData.set(UPGRADE_3, stack3);
	}
	
	@Override
	public void writeSpawnData(FriendlyByteBuf buffer){
		String fluid = this.entityData.get(TANK_FLUID);
		int amount = this.entityData.get(TANK_AMOUNT);
		ItemStack stack0 = this.entityData.get(UPGRADE_0);
		ItemStack stack1 = this.entityData.get(UPGRADE_1);
		ItemStack stack2 = this.entityData.get(UPGRADE_2);
		ItemStack stack3 = this.entityData.get(UPGRADE_3);
		
		buffer.writeUtf(fluid);
		buffer.writeInt(amount);
		buffer.writeItem(stack0);
		buffer.writeItem(stack1);
		buffer.writeItem(stack2);
		buffer.writeItem(stack3);
	}
}
