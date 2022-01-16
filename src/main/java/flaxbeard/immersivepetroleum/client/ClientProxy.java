package flaxbeard.immersivepetroleum.client;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.client.manual.ManualElementMultiblock;
import blusunrize.immersiveengineering.client.models.ModelCoresample;
import blusunrize.immersiveengineering.common.blocks.metal.MetalScaffoldingType;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IEMultiblocks;
import blusunrize.immersiveengineering.common.register.IEBlocks;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import blusunrize.lib.manual.*;
import blusunrize.lib.manual.ManualEntry.EntryData;
import blusunrize.lib.manual.Tree.InnerNode;
import com.electronwill.nightconfig.core.Config;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.crafting.DistillationRecipe;
import flaxbeard.immersivepetroleum.api.crafting.FlarestackHandler;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.PumpjackHandler;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.PumpjackHandler.ReservoirType;
import flaxbeard.immersivepetroleum.api.energy.FuelHandler;
import flaxbeard.immersivepetroleum.client.gui.CokerUnitScreen;
import flaxbeard.immersivepetroleum.client.gui.DistillationTowerScreen;
import flaxbeard.immersivepetroleum.client.gui.HydrotreaterScreen;
import flaxbeard.immersivepetroleum.client.gui.ProjectorScreen;
import flaxbeard.immersivepetroleum.client.render.AutoLubricatorRenderer;
import flaxbeard.immersivepetroleum.client.render.MotorboatRenderer;
import flaxbeard.immersivepetroleum.client.render.MultiblockDistillationTowerRenderer;
import flaxbeard.immersivepetroleum.client.render.MultiblockPumpjackRenderer;
import flaxbeard.immersivepetroleum.client.render.debugging.DebugRenderHandler;
import flaxbeard.immersivepetroleum.common.CommonProxy;
import flaxbeard.immersivepetroleum.common.IPContent;
import flaxbeard.immersivepetroleum.common.IPContent.Items;
import flaxbeard.immersivepetroleum.common.IPTileTypes;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.PumpjackTileEntity;
import flaxbeard.immersivepetroleum.common.cfg.IPServerConfig;
import flaxbeard.immersivepetroleum.common.crafting.RecipeReloadListener;
import flaxbeard.immersivepetroleum.common.entity.MotorboatEntity;
import flaxbeard.immersivepetroleum.common.multiblocks.CokerUnitMultiblock;
import flaxbeard.immersivepetroleum.common.multiblocks.DistillationTowerMultiblock;
import flaxbeard.immersivepetroleum.common.multiblocks.HydroTreaterMultiblock;
import flaxbeard.immersivepetroleum.common.multiblocks.PumpjackMultiblock;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ImmersivePetroleum.MODID)
public class ClientProxy extends CommonProxy{
	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(ImmersivePetroleum.MODID + "/ClientProxy");
	public static final String CAT_IP = "ip";
	
	public static final KeyMapping keybind_preview_flip = new KeyMapping("key.immersivepetroleum.projector.flip", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.immersivepetroleum");
	
	@Override
	public void setup(){
		RenderingRegistry.registerEntityRenderingHandler(MotorboatEntity.TYPE, MotorboatRenderer::new);
	}
	
	@Override
	public void registerContainersAndScreens(){
		super.registerContainersAndScreens();
		
		registerScreen(new ResourceLocation(ImmersivePetroleum.MODID, "distillationtower"), DistillationTowerScreen::new);
		registerScreen(new ResourceLocation(ImmersivePetroleum.MODID, "cokerunit"), CokerUnitScreen::new);
		registerScreen(new ResourceLocation(ImmersivePetroleum.MODID, "hydrotreater"), HydrotreaterScreen::new);
	}
	
	@SuppressWarnings("unchecked")
	public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerScreen(ResourceLocation name, MenuScreens.ScreenConstructor<M, U> factory){
		MenuType<M> type = (MenuType<M>) GuiHandler.getContainerType(name);
		MenuScreens.register(type, factory);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void completed(){
		DeferredWorkQueue.runLater(() -> ManualHelper.addConfigGetter(str -> {
			switch(str){
				case "distillationtower_operationcost":{
					return Integer.valueOf((int) (2048 * IPServerConfig.REFINING.distillationTower_energyModifier.get()));
				}
				case "coker_operationcost":{
					return Integer.valueOf((int) (1024 * IPServerConfig.REFINING.cokerUnit_energyModifier.get()));
				}
				case "hydrotreater_operationcost":{
					return Integer.valueOf((int) (512 * IPServerConfig.REFINING.hydrotreater_energyModifier.get()));
				}
				case "pumpjack_consumption":{
					return IPServerConfig.EXTRACTION.pumpjack_consumption.get();
				}
				case "pumpjack_speed":{
					return IPServerConfig.EXTRACTION.pumpjack_speed.get();
				}
				case "pumpjack_days":{
					int oil_min = 1000000;
					int oil_max = 5000000;
					for(ReservoirType type:PumpjackHandler.reservoirs.values()){
						if(type.name.equals("oil")){
							oil_min = type.minSize;
							oil_max = type.maxSize;
							break;
						}
					}
					
					float averageSize = (oil_min + oil_max) / 2F;
					float pumpspeed = IPServerConfig.EXTRACTION.pumpjack_speed.get();
					return Integer.valueOf(Mth.floor((averageSize / pumpspeed) / 24000F));
				}
				case "autolubricant_speedup":{
					return Double.valueOf(1.25D);
				}
				case "portablegenerator_flux":{
					return FuelHandler.getFluxGeneratedPerTick(IPContent.Fluids.gasoline.getFlowing());
				}
				default:
					break;
			}
			
			// Last resort
			Config cfg = IPServerConfig.getRawConfig();
			if(cfg.contains(str)){
				return cfg.get(str);
			}
			return null;
		}));
		
		setupManualPages();
	}
	
	@Override
	public void preInit(){
	}
	
	@Override
	public void preInitEnd(){
	}
	
	@Override
	public void init(){
		MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
		MinecraftForge.EVENT_BUS.register(new RecipeReloadListener(null));
		
		MinecraftForge.EVENT_BUS.register(new DebugRenderHandler());
		
		keybind_preview_flip.setKeyConflictContext(KeyConflictContext.IN_GAME);
		ClientRegistry.registerKeyBinding(keybind_preview_flip);
		
		ClientRegistry.bindTileEntityRenderer(IPTileTypes.TOWER.get(), MultiblockDistillationTowerRenderer::new);
		ClientRegistry.bindTileEntityRenderer(IPTileTypes.PUMP.get(), MultiblockPumpjackRenderer::new);
		ClientRegistry.bindTileEntityRenderer(IPTileTypes.AUTOLUBE.get(), AutoLubricatorRenderer::new);
	}
	
	/** ImmersivePetroleum's Manual Category */
	private static InnerNode<ResourceLocation, ManualEntry> IP_CATEGORY;
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onModelBakeEvent(ModelBakeEvent event){
		ModelResourceLocation mLoc = new ModelResourceLocation(IEBlocks.StoneDecoration.CORESAMPLE.getId(), "inventory");
		BakedModel model = event.getModelRegistry().get(mLoc);
		if(model instanceof ModelCoresample){
			// It'll be a while until that is in working conditions again
			// event.getModelRegistry().put(mLoc, new ModelCoresampleExtended());
		}
	}
	
	@Override
	public void renderTile(BlockEntity te, VertexConsumer iVertexBuilder, PoseStack transform, MultiBufferSource buffer){
		BlockEntityRenderer<BlockEntity> tesr = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer((BlockEntity) te);
		
		if(te instanceof PumpjackTileEntity){
			transform.pushPose();
			transform.mulPose(new Quaternion(0, -90, 0, true));
			transform.translate(1, 1, -2);
			
			float pt = 0;
			if(Minecraft.getInstance().player != null){
				((PumpjackTileEntity) te).activeTicks = Minecraft.getInstance().player.tickCount;
				pt = Minecraft.getInstance().getFrameTime();
			}
			
			tesr.render(te, pt, transform, buffer, 0xF000F0, 0);
			transform.popPose();
		}else{
			transform.pushPose();
			transform.mulPose(new Quaternion(0, -90, 0, true));
			transform.translate(0, 1, -4);
			
			tesr.render(te, 0, transform, buffer, 0xF000F0, 0);
			transform.popPose();
		}
	}
	
	@Override
	public void drawUpperHalfSlab(PoseStack transform, ItemStack stack){
		
		// Render slabs on top half
		BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
		BlockState state = IEBlocks.MetalDecoration.STEEL_SCAFFOLDING.get(MetalScaffoldingType.STANDARD).defaultBlockState();
		BakedModel model = blockRenderer.getBlockModelShaper().getBlockModel(state);

		MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		
		transform.pushPose();
		transform.translate(0.0F, 0.5F, 1.0F);
		blockRenderer.getModelRenderer().renderModel(transform.last(), buffers.getBuffer(RenderType.solid()), state, model, 1.0F, 1.0F, 1.0F, -1, -1, EmptyModelData.INSTANCE);
		transform.popPose();
	}
	
	@Override
	public void openProjectorGui(InteractionHand hand, ItemStack held){
		Minecraft.getInstance().displayGuiScreen(new ProjectorScreen(hand, held));
	}
	
	@Override
	public Level getClientWorld(){
		return Minecraft.getInstance().level;
	}
	
	@Override
	public Player getClientPlayer(){
		return Minecraft.getInstance().player;
	}
	
	@Override
	public void handleEntitySound(SoundEvent soundEvent, Entity entity, boolean active, float volume, float pitch){
		// TODO Restore sound for the Motorboat
	}
	
	@Override
	public void handleTileSound(SoundEvent soundEvent, BlockEntity te, boolean active, float volume, float pitch){
		// TODO
	}
	
	public void setupManualPages(){
		ManualInstance man = ManualHelper.getManual();
		
		IP_CATEGORY = man.getRoot().getOrCreateSubnode(modLoc("main"), 100);
		
		pumpjack(modLoc("pumpjack"), 0);
		distillation(modLoc("distillationtower"), 1);
		coker(modLoc("cokerunit"), 2);
		hydrotreater(modLoc("hydrotreater"), 3);
		
		handleReservoirManual(modLoc("reservoir"), 3);
		
		lubricant(modLoc("lubricant"), 4);
		man.addEntry(IP_CATEGORY, modLoc("asphalt"), 5);
		projector(modLoc("projector"), 5);
		speedboat(modLoc("speedboat"), 6);
		man.addEntry(IP_CATEGORY, modLoc("napalm"), 7);
		generator(modLoc("portablegenerator"), 8);
		autolube(modLoc("automaticlubricator"), 9);
		flarestack(modLoc("flarestack"), 10);
	}
	
	private static void flarestack(ResourceLocation location, int priority){
		ManualInstance man = ManualHelper.getManual();
		
		ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
		builder.addSpecialElement(new ManualEntry.SpecialElementData("flarestack0", 0, new ManualElementCrafting(man, new ItemStack(IPContent.Blocks.flarestack))));
		builder.addSpecialElement(new ManualEntry.SpecialElementData("flarestack1", 0, () -> {
			Set<Tag<Fluid>> fluids = FlarestackHandler.getSet();
			List<Component[]> list = new ArrayList<Component[]>();
			for(Tag<Fluid> tag:fluids){
				if(tag instanceof Tag){
					List<Fluid> fl = tag.getValues();
					for(Fluid f:fl){
						Component[] entry = new Component[]{
								TextComponent.EMPTY, new FluidStack(f, 1).getDisplayName()
						};
						
						list.add(entry);
					}
				}
			}
			
			return new ManualElementTable(man, list.toArray(new Component[0][]), false);
		}));
		builder.readFromFile(location);
		man.addEntry(IP_CATEGORY, builder.create(), priority);
	}
	
	private static void autolube(ResourceLocation location, int priority){
		ManualInstance man = ManualHelper.getManual();
		
		ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
		builder.addSpecialElement(new ManualEntry.SpecialElementData("automaticlubricator0", 0, new ManualElementCrafting(man, new ItemStack(IPContent.Blocks.auto_lubricator))));
		builder.readFromFile(location);
		man.addEntry(IP_CATEGORY, builder.create(), priority);
	}
	
	private static void generator(ResourceLocation location, int priority){
		ManualInstance man = ManualHelper.getManual();
		
		ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
		builder.addSpecialElement(new ManualEntry.SpecialElementData("portablegenerator0", 0, new ManualElementCrafting(man, new ItemStack(IPContent.Blocks.gas_generator))));
		builder.readFromFile(location);
		man.addEntry(IP_CATEGORY, builder.create(), priority);
	}
	
	private static void speedboat(ResourceLocation location, int priority){
		ManualInstance man = ManualHelper.getManual();
		
		ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
		builder.addSpecialElement(new ManualEntry.SpecialElementData("speedboat0", 0, new ManualElementCrafting(man, new ItemStack(IPContent.Items.speedboat))));
		builder.addSpecialElement(new ManualEntry.SpecialElementData("speedboat1", 0, new ManualElementCrafting(man, new ItemStack(IPContent.BoatUpgrades.tank))));
		builder.addSpecialElement(new ManualEntry.SpecialElementData("speedboat2", 0, new ManualElementCrafting(man, new ItemStack(IPContent.BoatUpgrades.rudders))));
		builder.addSpecialElement(new ManualEntry.SpecialElementData("speedboat3", 0, new ManualElementCrafting(man, new ItemStack(IPContent.BoatUpgrades.ice_breaker))));
		builder.addSpecialElement(new ManualEntry.SpecialElementData("speedboat4", 0, new ManualElementCrafting(man, new ItemStack(IPContent.BoatUpgrades.reinforced_hull))));
		builder.addSpecialElement(new ManualEntry.SpecialElementData("speedboat5", 0, new ManualElementCrafting(man, new ItemStack(IPContent.BoatUpgrades.paddles))));
		builder.readFromFile(location);
		man.addEntry(IP_CATEGORY, builder.create(), priority);
	}
	
	private static void lubricant(ResourceLocation location, int priority){
		ManualInstance man = ManualHelper.getManual();
		
		ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
		builder.addSpecialElement(new ManualEntry.SpecialElementData("lubricant1", 0, new ManualElementCrafting(man, new ItemStack(IPContent.Items.oil_can))));
		builder.readFromFile(location);
		man.addEntry(IP_CATEGORY, builder.create(), priority);
	}
	
	private static void pumpjack(ResourceLocation location, int priority){
		ManualInstance man = ManualHelper.getManual();
		
		ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
		builder.addSpecialElement(new ManualEntry.SpecialElementData("pumpjack0", 0, () -> new ManualElementMultiblock(man, PumpjackMultiblock.INSTANCE)));
		builder.readFromFile(location);
		man.addEntry(IP_CATEGORY, builder.create(), priority);
	}
	
	private static void distillation(ResourceLocation location, int priority){
		ManualInstance man = ManualHelper.getManual();
		
		ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
		builder.addSpecialElement(new ManualEntry.SpecialElementData("distillationtower0", 0, () -> new ManualElementMultiblock(man, DistillationTowerMultiblock.INSTANCE)));
		builder.addSpecialElement(new ManualEntry.SpecialElementData("distillationtower1", 0, () -> {
			Collection<DistillationRecipe> recipeList = DistillationRecipe.recipes.values();
			List<Component[]> list = new ArrayList<Component[]>();
			for(DistillationRecipe recipe:recipeList){
				boolean first = true;
				for(FluidStack output:recipe.getFluidOutputs()){
					Component outputName = output.getDisplayName();

					Component[] entry = new Component[]{
							first ? new TextComponent(recipe.getInputFluid().getAmount() + "mB ").append(recipe.getInputFluid().getMatchingFluidStacks().get(0).getDisplayName()) : TextComponent.EMPTY,
									new TextComponent(output.getAmount() + "mB ").append(outputName)
					};
					
					list.add(entry);
					first = false;
				}
			}
			
			return new ManualElementTable(man, list.toArray(new Component[0][]), false);
		}));
		builder.readFromFile(location);
		man.addEntry(IP_CATEGORY, builder.create(), priority);
	}
	
	protected static void coker(ResourceLocation location, int priority){
		ManualInstance man = ManualHelper.getManual();
		
		ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
		builder.addSpecialElement(new ManualEntry.SpecialElementData("cokerunit0", 0, () -> new ManualElementMultiblock(man, CokerUnitMultiblock.INSTANCE)));
		builder.readFromFile(location);
		man.addEntry(IP_CATEGORY, builder.create(), priority);
	}
	
	protected static void hydrotreater(ResourceLocation location, int priority){
		ManualInstance man = ManualHelper.getManual();
		
		ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
		builder.addSpecialElement(new ManualEntry.SpecialElementData("hydrotreater0", 0, () -> new ManualElementMultiblock(man, HydroTreaterMultiblock.INSTANCE)));
		builder.readFromFile(location);
		man.addEntry(IP_CATEGORY, builder.create(), priority);
	}
	
	protected static void projector(ResourceLocation location, int priority){
		ManualInstance man = ManualHelper.getManual();
		
		ItemStack projectorWithNBT = new ItemStack(Items.projector);
		ItemNBTHelper.putString(projectorWithNBT, "multiblock", IEMultiblocks.ARC_FURNACE.getUniqueName().toString());
		
		ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
		builder.addSpecialElement(new ManualEntry.SpecialElementData("projector0", 0, new ManualElementCrafting(man, new ItemStack(Items.projector))));
		builder.addSpecialElement(new ManualEntry.SpecialElementData("projector1", 0, new ManualElementCrafting(man, projectorWithNBT)));
		builder.readFromFile(location);
		man.addEntry(IP_CATEGORY, builder.create(), priority);
	}
	
	private static void handleReservoirManual(ResourceLocation location, int priority){
		ManualInstance man = ManualHelper.getManual();
		
		ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
		builder.setContent(ClientProxy::createContent);
		builder.setLocation(location);
		man.addEntry(IP_CATEGORY, builder.create(), priority);
	}
	
	static final DecimalFormat FORMATTER = new DecimalFormat("#,###.##");
	static ManualEntry entry;
	protected static EntryData createContent(TextSplitter splitter){
		ArrayList<ItemStack> list = new ArrayList<>();
		final ReservoirType[] reservoirs = PumpjackHandler.reservoirs.values().toArray(new ReservoirType[0]);
		
		StringBuilder contentBuilder = new StringBuilder();
		contentBuilder.append(I18n.get("ie.manual.entry.reservoirs.oil0"));
		contentBuilder.append(I18n.get("ie.manual.entry.reservoirs.oil1"));
		
		for(int i = 0;i < reservoirs.length;i++){
			ReservoirType type = reservoirs[i];
			
			ImmersivePetroleum.log.debug("Creating entry for " + type);
			
			String name = "desc.immersivepetroleum.info.reservoir." + type.name;
			String localizedName = I18n.get(name);
			if(localizedName.equalsIgnoreCase(name))
				localizedName = type.name;
			
			char c = localizedName.toLowerCase().charAt(0);
			boolean isVowel = (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u');
			String aOrAn = I18n.get(isVowel ? "ie.manual.entry.reservoirs.vowel" : "ie.manual.entry.reservoirs.consonant");
			
			String dimBLWL = "";
			if(type.dimWhitelist != null && type.dimWhitelist.size() > 0){
				String validDims = "";
				for(ResourceLocation rl:type.dimWhitelist){
					validDims += (!validDims.isEmpty() ? ", " : "") + "<dim;" + rl + ">";
				}
				dimBLWL = I18n.get("ie.manual.entry.reservoirs.dim.valid", localizedName, validDims, aOrAn);
			}else if(type.dimBlacklist != null && type.dimBlacklist.size() > 0){
				String invalidDims = "";
				for(ResourceLocation rl:type.dimBlacklist){
					invalidDims += (!invalidDims.isEmpty() ? ", " : "") + "<dim;" + rl + ">";
				}
				dimBLWL = I18n.get("ie.manual.entry.reservoirs.dim.invalid", localizedName, invalidDims, aOrAn);
			}else{
				dimBLWL = I18n.get("ie.manual.entry.reservoirs.dim.any", localizedName, aOrAn);
			}
			
			String bioBLWL = "";
			if(type.bioWhitelist != null && type.bioWhitelist.size() > 0){
				String validBiomes = "";
				for(ResourceLocation rl:type.bioWhitelist){
					Biome bio = ForgeRegistries.BIOMES.getValue(rl);
					validBiomes += (!validBiomes.isEmpty() ? ", " : "") + (bio != null ? bio.toString() : rl);
				}
				bioBLWL = I18n.get("ie.manual.entry.reservoirs.bio.valid", validBiomes);
			}else if(type.bioBlacklist != null && type.bioBlacklist.size() > 0){
				String invalidBiomes = "";
				for(ResourceLocation rl:type.bioBlacklist){
					Biome bio = ForgeRegistries.BIOMES.getValue(rl);
					invalidBiomes += (!invalidBiomes.isEmpty() ? ", " : "") + (bio != null ? bio.toString() : rl);
				}
				bioBLWL = I18n.get("ie.manual.entry.reservoirs.bio.invalid", invalidBiomes);
			}else{
				bioBLWL = I18n.get("ie.manual.entry.reservoirs.bio.any");
			}
			
			String fluidName = "";
			Fluid fluid = type.getFluid();
			if(fluid != null){
				fluidName = new FluidStack(fluid, 1).getDisplayName().getString();
			}
			
			String repRate = "";
			if(type.replenishRate > 0){
				repRate = I18n.get("ie.manual.entry.reservoirs.replenish", type.replenishRate, fluidName);
			}
			contentBuilder.append(I18n.get("ie.manual.entry.reservoirs.content", dimBLWL, fluidName, FORMATTER.format(type.minSize), FORMATTER.format(type.maxSize), repRate, bioBLWL));
			
			if(i < (reservoirs.length - 1))
				contentBuilder.append("<np>");
			
			list.add(new ItemStack(fluid.getBucket()));
		}
		
		String translatedTitle = I18n.get("ie.manual.entry.reservoirs.title");
		String tanslatedSubtext = I18n.get("ie.manual.entry.reservoirs.subtitle");
		String formattedContent = contentBuilder.toString().replaceAll("\r\n|\r|\n", "\n");
		return new EntryData(translatedTitle, tanslatedSubtext, formattedContent);
	}
}
