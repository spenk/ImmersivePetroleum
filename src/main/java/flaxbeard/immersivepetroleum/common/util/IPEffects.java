package flaxbeard.immersivepetroleum.common.util;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.entity.MotorboatEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public class IPEffects{
	/**
	 * This is only as a burn prevention for when someone dismounts the
	 * {@link MotorboatEntity} while that is in lava<br>
	 */
	public static MobEffect ANTI_DISMOUNT_FIRE;
	
	public static void init(){
		ANTI_DISMOUNT_FIRE = new AntiFireEffect();
	}
	
	private static class AntiFireEffect extends IPEffect {
		public AntiFireEffect(){
			super("anti_fire", MobEffectCategory.BENEFICIAL, 0x7F7F7F);
		}
		
		@Override
		public void applyEffectTick(LivingEntity living, int amplifier){
			living.clearFire();
		}
	}
	
	public static class IPEffect extends MobEffect {
		protected IPEffect(String name, MobEffectCategory type, int color){
			super(type, color);
			ForgeRegistries.MOB_EFFECTS.register(this.setRegistryName(new ResourceLocation(ImmersivePetroleum.MODID, name)));
		}
	}
}
