package flaxbeard.immersivepetroleum.common.sound;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.items.EarmuffsItem;
import blusunrize.immersiveengineering.common.register.IEItems;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class IPEntitySound implements TickableSoundInstance {
	protected Sound sound;
	public Attenuation attenuation;
	public final ResourceLocation resource;
	public float volume;
	public float pitch;
	
	public Entity entity;
	public boolean canRepeat;
	public int repeatDelay;
	public float volumeAjustment = 1;
	
	public IPEntitySound(SoundEvent event, float volume, float pitch, boolean repeat, int repeatDelay, Entity e, Attenuation attenuation){
		this(event.getRegistryName(), volume, pitch, repeat, repeatDelay, e, attenuation);
	}
	
	public IPEntitySound(ResourceLocation sound, float volume, float pitch, boolean repeat, int repeatDelay, Entity e, Attenuation attenuation){
		this.attenuation = attenuation;
		this.resource = sound;
		this.volume = volume;
		this.pitch = pitch;
		this.entity = e;
		this.canRepeat = repeat;
		this.repeatDelay = repeatDelay;
	}
	
	@Override
	public Attenuation getAttenuation(){
		return attenuation;
	}

	@Override
	public boolean canStartSilent() {
		return TickableSoundInstance.super.canStartSilent();
	}

	@Override
	public boolean canPlaySound() {
		return TickableSoundInstance.super.canPlaySound();
	}

	@Override
	public ResourceLocation getLocation(){
		return resource;
	}

	@org.jetbrains.annotations.Nullable
	@Override
	public WeighedSoundEvents resolve(SoundManager p_119841_) {
		return null;
	}
	
	@Override
	public Sound getSound(){
		return sound;
	}

	@Override
	public SoundSource getSource() {
		return null;
	}

	@Override
	public boolean isLooping() {
		return false;
	}

	@Override
	public boolean isRelative() {
		return false;
	}

	@Override
	public int getDelay() {
		return 0;
	}

	@Override
	public float getVolume(){
		return volume * volumeAjustment;
	}
	
	@Override
	public float getPitch(){
		return pitch;
	}
	
	@Override
	public double getX(){
		return (float) entity.getX();
	}
	
	@Override
	public double getY(){
		return (float) entity.getY();
	}
	
	@Override
	public double getZ(){
		return (float) entity.getZ();
	}
	
	public void evaluateVolume(){
		volumeAjustment = 1f;
		if(ClientUtils.mc().player != null && ClientUtils.mc().player.getItemBySlot(EquipmentSlot.HEAD) != ItemStack.EMPTY){
			ItemStack stack = ClientUtils.mc().player.getItemBySlot(EquipmentSlot.HEAD);
			if(ItemNBTHelper.hasKey(stack, "IE:Earmuffs")) {
				stack = ItemNBTHelper.getItemStack(stack, "IE:Earmuffs");
			}
			if(stack != null && IEItems.Misc.EARMUFFS.equals(stack.getItem())) {
				volumeAjustment = EarmuffsItem.getVolumeMod(stack);
			}
		}
		
		if(volumeAjustment > .1f) {
			for (int dx = (int) Math.floor(entity.getX() - 8) >> 4; dx <= (int) Math.floor(entity.getX() + 8) >> 4; dx++) {
				for (int dz = (int) Math.floor(entity.getZ() - 8) >> 4; dz <= (int) Math.floor(entity.getZ() + 8) >> 4; dz++) {
					for (BlockEntity tile : ClientUtils.mc().player.level.getChunk(dx, dz).getBlockEntities().values()) {
						if (tile != null && tile.getClass().getName().contains("SoundMuffler")) {
							BlockPos tPos = tile.getBlockPos();
							double d = entity.position().distanceTo(new Vec3(tPos.getX() + .5, tPos.getY() + .5, tPos.getZ() + .5));
							if (d <= 64 && d > 0) {
								volumeAjustment = .1f;
							}
						}
					}
				}
			}
		}
		if(!entity.isAlive())
			donePlaying = true;
	}



	@Override
	public void tick(){
		if(ClientUtils.mc().player != null && ClientUtils.mc().player.level.getDayTime() % 40 == 0)
			evaluateVolume();
	}
	
	public boolean donePlaying = false;

	@Override
	public boolean isStopped() {
		return false;
	}
}
