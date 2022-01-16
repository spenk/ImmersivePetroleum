package flaxbeard.immersivepetroleum.common.items;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.event.ProjectorEvent;
import flaxbeard.immersivepetroleum.client.ClientProxy;
import flaxbeard.immersivepetroleum.client.ShaderUtil;
import flaxbeard.immersivepetroleum.client.render.IPRenderTypes;
import flaxbeard.immersivepetroleum.common.IPContent;
import flaxbeard.immersivepetroleum.common.IPContent.Items;
import flaxbeard.immersivepetroleum.common.util.projector.MultiblockProjection;
import flaxbeard.immersivepetroleum.common.util.projector.Settings;
import flaxbeard.immersivepetroleum.common.util.projector.Settings.Mode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

public class ProjectorItem extends IPItemBase {
	public ProjectorItem(String name){
		super(name, new Item.Properties().stacksTo(1));
	}
	
	@Override
	public Component getName(@NotNull ItemStack stack){
		String selfKey = getDescriptionId(stack);
		if(stack.hasTag()){
			Settings settings = getSettings(stack);
			if(settings.getMultiblock() != null){
				TranslatableComponent name = new TranslatableComponent("desc.immersiveengineering.info.multiblock.IE:" + getActualMBName(settings.getMultiblock()));
				
				return new TranslatableComponent(selfKey + ".specific", name).withStyle(ChatFormatting.GOLD);
			}
		}
		return new TranslatableComponent(selfKey).withStyle(ChatFormatting.GOLD);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn){
		Settings settings = getSettings(stack);
		if(settings.getMultiblock() != null){
			Vec3i size = settings.getMultiblock().getSize(worldIn);
			
			String name = getActualMBName(settings.getMultiblock());
			tooltip.add(new TranslatableComponent("desc.immersivepetroleum.info.projector.build0"));
			tooltip.add(new TranslatableComponent("desc.immersivepetroleum.info.projector.build1", new TranslatableComponent("desc.immersiveengineering.info.multiblock.IE:" + name)));
			
			if(isPressing(GLFW.GLFW_KEY_LEFT_SHIFT) || isPressing(GLFW.GLFW_KEY_RIGHT_SHIFT)){
				Component title = new TranslatableComponent("desc.immersivepetroleum.info.projector.holdshift.text").withStyle(ChatFormatting.DARK_AQUA);
				tooltip.add(title);

				Component mbSize = new TranslatableComponent("desc.immersivepetroleum.info.projector.size", size.getX(), size.getY(), size.getZ()).withStyle(ChatFormatting.DARK_GRAY);
				tooltip.add(indent(mbSize));
				
				Direction dir = Direction.from2DDataValue(settings.getRotation().ordinal());
				Component rotation = new TranslatableComponent("desc.immersivepetroleum.info.projector.rotated." + dir).withStyle(ChatFormatting.DARK_GRAY);

				Component flip;
				if(settings.isMirrored()){
					flip = new TranslatableComponent("desc.immersivepetroleum.info.projector.flipped.true").withStyle(ChatFormatting.DARK_GRAY);
				}else{
					flip = new TranslatableComponent("desc.immersivepetroleum.info.projector.flipped.false").withStyle(ChatFormatting.DARK_GRAY);
				}
				
				if(settings.getPos() != null){
					int x = settings.getPos().getX();
					int y = settings.getPos().getY();
					int z = settings.getPos().getZ();

					Component centerText = new TranslatableComponent("desc.immersivepetroleum.info.projector.center", x, y, z).withStyle(ChatFormatting.DARK_GRAY);
					tooltip.add(indent(centerText));
				}
				
				tooltip.add(indent(rotation));
				tooltip.add(indent(flip));
			}else{
				Component text = new TextComponent("[")
						.append(new TranslatableComponent("desc.immersivepetroleum.info.projector.holdshift"))
						.append("] ")
						.append(new TranslatableComponent("desc.immersivepetroleum.info.projector.holdshift.text"))
						.withStyle(ChatFormatting.DARK_AQUA);
				tooltip.add(text);
			}
			
			if(isPressing(GLFW.GLFW_KEY_LEFT_CONTROL) || isPressing(GLFW.GLFW_KEY_RIGHT_CONTROL)){
				Component title = new TranslatableComponent("desc.immersivepetroleum.info.projector.holdctrl.text").withStyle(ChatFormatting.DARK_PURPLE);
				Component ctrl0 = new TranslatableComponent("desc.immersivepetroleum.info.projector.control1").withStyle(ChatFormatting.DARK_GRAY);
				Component ctrl1 = new TranslatableComponent("desc.immersivepetroleum.info.projector.control2", ClientProxy.keybind_preview_flip.getTranslatedKeyMessage()).withStyle(ChatFormatting.DARK_GRAY);
				Component ctrl2 = new TranslatableComponent("desc.immersivepetroleum.info.projector.control3").withStyle(ChatFormatting.DARK_GRAY);
				
				tooltip.add(title);
				tooltip.add(indent(ctrl0));
				tooltip.add(indent(ctrl1));
				tooltip.add(indent(ctrl2));
			}else{
				Component text = new TextComponent("[")
						.append(new TranslatableComponent("desc.immersivepetroleum.info.projector.holdctrl"))
						.append("] ")
						.append(new TranslatableComponent("desc.immersivepetroleum.info.projector.holdctrl.text"))
						.withStyle(ChatFormatting.DARK_PURPLE);
				tooltip.add(text);
			}
		}else{
			tooltip.add(new TranslatableComponent("desc.immersivepetroleum.info.projector.noMultiblock"));
		}
	}
	
	private Component indent(Component text){
		return new TextComponent("  ").append(text);
	}
	
	/** Find the key that is being pressed while minecraft is in focus */
	@OnlyIn(Dist.CLIENT)
	private boolean isPressing(int key){
		long window = Minecraft.getInstance().getWindow().getWindow();
		return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
	}
	
	/** Name cache for {@link ProjectorItem#getActualMBName(IMultiblock)} */
	static final Map<Class<? extends IMultiblock>, String> nameCache = new HashMap<>();
	/** Gets the name of the class */
	public static String getActualMBName(IMultiblock multiblock){
		if(!nameCache.containsKey(multiblock.getClass())){
			String name = multiblock.getClass().getSimpleName();
			name = name.substring(0, name.indexOf("Multiblock"));
			
			switch(name){
				case "LightningRod": name="Lightningrod"; break;
				case "ImprovedBlastfurnace": name="BlastFurnaceAdvanced"; break;
			}
			
			nameCache.put(multiblock.getClass(), name);
		}
		
		return nameCache.get(multiblock.getClass());
	}
	
	@Override
	public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items){
		if(this.allowdedIn(group)){
			items.add(new ItemStack(this, 1));
		}
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn){
		ItemStack held = playerIn.getItemInHand(handIn);
		
		boolean changeMode = false;
		Settings settings = getSettings(held);
		switch(settings.getMode()){
			case PROJECTION:{
				if(!worldIn.isClientSide){
					if(playerIn.isShiftKeyDown()){
						if(settings.getPos() != null){
							settings.setPos(null);
							settings.sendPacketToServer(handIn);
						}else{
							changeMode = true;
						}
					}
				}
				break;
			}
			case MULTIBLOCK_SELECTION:{
				if(!worldIn.isClientSide){
					if(!playerIn.isShiftKeyDown()){
						ImmersivePetroleum.proxy.openProjectorGui(handIn, held);
					}else{
						changeMode = true;
					}
				}
				break;
			}
			default:break;
		}
		
		if(!worldIn.isClientSide && changeMode){
			int modeId = settings.getMode().ordinal() + 1;
			settings.setMode(Mode.values()[modeId >= Mode.values().length ? 0 : modeId]);
			settings.applyTo(held);
			settings.sendPacketToServer(handIn);
			playerIn.displayClientMessage(settings.getMode().getTranslated(), true);
		}
		
		return InteractionResultHolder.success(held);
	}
	
	@Override
	public InteractionResult useOn(UseOnContext context){
		Level world = context.getLevel();
		Player playerIn = context.getPlayer();
		InteractionHand hand = context.getHand();
		BlockPos pos = context.getClickedPos();
		Direction facing = context.getClickedFace();
		
		ItemStack stack = playerIn.getItemInHand(hand);
		final Settings settings = ProjectorItem.getSettings(stack);
		if(playerIn.isShiftKeyDown() && settings.getPos() != null){
			if(!world.isClientSide){
				settings.setPos(null);
				settings.applyTo(stack);
				settings.sendPacketToServer(hand);
			}
			
			return InteractionResult.SUCCESS;
		}
		
		if(settings.getMode() == Mode.PROJECTION && settings.getPos() == null && settings.getMultiblock() != null){
			BlockState state = world.getBlockState(pos);
			
			final BlockPos.MutableBlockPos hit = pos.mutable();
			if(!state.getMaterial().isReplaceable() && facing == Direction.UP){
				hit.setWithOffset(hit, 0, 1, 0);
			}
			
			Vec3i size = settings.getMultiblock().getSize(world);
			alignHit(hit, playerIn, size, settings.getRotation(), settings.isMirrored());
			
			if(playerIn.isShiftKeyDown() && playerIn.isCreative()){
				if(world.isClientSide){
					if(settings.getMultiblock().getUniqueName().getPath().contains("excavator_demo") || settings.getMultiblock().getUniqueName().getPath().contains("bucket_wheel")){
						hit.setWithOffset(hit, 0, -2, 0);
					}
					
					// Creative Placement
					BiPredicate<Integer, MultiblockProjection.Info> pred = (layer, info) -> {
						BlockPos realPos = info.tPos.offset(hit);
						BlockState tstate0 = info.getModifiedState(world, realPos);
						
						ProjectorEvent.PlaceBlock event = new ProjectorEvent.PlaceBlock(info.multiblock, info.templateWorld, info.tBlockInfo.pos, world, realPos, tstate0, settings.getRotation());
						if(!MinecraftForge.EVENT_BUS.post(event)){
							BlockState tstate1 = event.getState();
							
							if(world.setBlockAndUpdate(realPos, tstate1)){
								if(tstate0 == tstate1 && info.tBlockInfo.nbt != null){
									BlockEntity te = world.getBlockEntity(realPos);
									//TODO functions were removed
									//te.mirror(info.settings.getMirror());
									//te.rotate(info.settings.getRotation());
									te.setChanged();
								}
								
								ProjectorEvent.PlaceBlockPost postEvent = new ProjectorEvent.PlaceBlockPost(info.multiblock, info.templateWorld, event.getTemplatePos(), world, realPos, tstate1, settings.getRotation());
								MinecraftForge.EVENT_BUS.post(postEvent);
							}
						}
						
						return false; // Don't ever skip a step.
					};
					
					MultiblockProjection projection = new MultiblockProjection(world, settings.getMultiblock());
					projection.setFlip(settings.isMirrored());
					projection.setRotation(settings.getRotation());
					projection.processAll(pred);
				}
				
				return InteractionResult.SUCCESS;
				
			}else{
				if(!world.isClientSide){
					settings.setPos(hit);
					settings.applyTo(stack);
					settings.sendPacketToServer(hand);
				}
				
				return InteractionResult.SUCCESS;
			}
		}
		
		return InteractionResult.PASS;
	}
	
	// STATIC METHODS
	
	public static Settings getSettings(@Nullable ItemStack stack){
		return new Settings(stack);
	}
	
	private static void alignHit(BlockPos.MutableBlockPos hit, Player playerIn, Vec3i size, Rotation rotation, boolean mirror){
		int x = ((rotation.ordinal() % 2 == 0) ? size.getX() : size.getZ()) / 2;
		int z = ((rotation.ordinal() % 2 == 0) ? size.getZ() : size.getX()) / 2;
		Direction facing = playerIn.getDirection();
		
		switch(facing){
			case NORTH:	hit.setWithOffset(hit, 0, 0, -z);break;
			case SOUTH:	hit.setWithOffset(hit, 0, 0, z);break;
			case EAST:	hit.setWithOffset(hit, x, 0, 0);break;
			case WEST:	hit.setWithOffset(hit, -x, 0, 0);break;
			default:break;
		}
	}
	
	// STATIC SUPPORT CLASSES
	
	/** Client Rendering Stuff */
	@Mod.EventBusSubscriber(modid = ImmersivePetroleum.MODID, value = Dist.CLIENT)
	public static class ClientRenderHandler{
		@SubscribeEvent
		public static void renderLast(RenderLevelLastEvent event){
			Minecraft mc = ClientUtils.mc();
			
			if(mc.player != null){
				PoseStack matrix = event.getPoseStack();
				matrix.pushPose();
				{
					// Anti-Jiggle when moving
					Vec3 renderView = ClientUtils.mc().gameRenderer.getMainCamera().getPosition();
					matrix.translate(-renderView.x, -renderView.y, -renderView.z);
					
					ItemStack secondItem = mc.player.getOffhandItem();
					boolean off = !secondItem.isEmpty() && secondItem.getItem() == Items.projector && ItemNBTHelper.hasKey(secondItem, "settings");
					
					for(int i = 0;i <= 10;i++){
						ItemStack stack = (i == 10 ? secondItem : mc.player.getInventory().getItem(i));
						if(!stack.isEmpty() && stack.getItem() == Items.projector && ItemNBTHelper.hasKey(stack, "settings")){
							Settings settings = getSettings(stack);
							matrix.pushPose();
							{
								boolean renderMoving = i == mc.player.getInventory().selected || (i == 10 && off);
								renderSchematic(matrix, settings, mc.player, mc.player.level, event.getPartialTick(), renderMoving);
							}
							matrix.popPose();
						}
					}
				}
				matrix.popPose();
			}
		}
		
		static final BlockPos.MutableBlockPos FULL_MAX = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		public static void renderSchematic(PoseStack matrix, Settings settings, Player player, Level world, float partialTicks, boolean renderMoving){
			if(settings.getMultiblock() == null)
				return;
			
			Vec3i size = settings.getMultiblock().getSize(world);
			final BlockPos.MutableBlockPos hit = new BlockPos.MutableBlockPos(FULL_MAX.getX(), FULL_MAX.getY(), FULL_MAX.getZ());
			final MutableBoolean isPlaced = new MutableBoolean(false);
			if(settings.getPos() != null){
				hit.set(settings.getPos());
				isPlaced.setTrue();
				
			}else if(renderMoving && ClientUtils.mc().hitResult != null && ClientUtils.mc().hitResult.getType() == HitResult.Type.BLOCK){
				BlockHitResult blockRTResult = (BlockHitResult) ClientUtils.mc().hitResult;
				
				BlockPos pos = (BlockPos) blockRTResult.getBlockPos();
				
				BlockState state = world.getBlockState(pos);
				if(state.getMaterial().isReplaceable() || blockRTResult.getDirection() != Direction.UP){
					hit.set(pos);
				}else{
					hit.setWithOffset(pos, 0, 1, 0);
				}
				
				alignHit(hit, player, size, settings.getRotation(), settings.isMirrored());
			}
			
			if(!hit.equals(FULL_MAX)){
				ResourceLocation name = settings.getMultiblock().getUniqueName();
				if(name.getPath().contains("excavator_demo") || name.getPath().contains("bucket_wheel")){
					hit.setWithOffset(hit, 0, -2, 0);
				}
				
				MultiblockProjection projection = new MultiblockProjection(world, settings.getMultiblock());
				projection.setRotation(settings.getRotation());
				projection.setFlip(settings.isMirrored());
				
				final List<Pair<RenderLayer, MultiblockProjection.Info>> toRender = new ArrayList<>();
				final MutableInt currentLayer = new MutableInt();
				final MutableInt badBlocks = new MutableInt();
				final MutableInt goodBlocks = new MutableInt();
				BiPredicate<Integer, MultiblockProjection.Info> bipred = (layer, info) -> {
					// Slice handling
					if(badBlocks.getValue() == 0 && layer > currentLayer.getValue()){
						currentLayer.setValue(layer);
					}else if(layer != currentLayer.getValue()){
						return true; // breaks the internal loop
					}
					
					if(isPlaced.booleanValue()){ // Render only slices when placed
						if(layer == currentLayer.getValue()){
							BlockPos realPos = info.tPos.offset(hit);
							BlockState toCompare = world.getBlockState(realPos);
							BlockState tState = info.getModifiedState(world, realPos);
							
							BlockEntity te = info.templateWorld.getBlockEntity(realPos);
							//TODO mirror functions were removed
							/*if(te != null){
								te.mirror(info.settings.getMirror());
								te.rotate(info.settings.getRotation());
							}*/
							
							boolean skip = false;
							if(tState == toCompare){
								toRender.add(Pair.of(RenderLayer.PERFECT, info));
								goodBlocks.increment();
								skip = true;
							}else{
								// Making it this far only needs an air check,
								// the other already proved to be false.
								if(toCompare.getMaterial() != Material.AIR){
									toRender.add(Pair.of(RenderLayer.BAD, info));
									skip = true;
								}else{
									badBlocks.increment();
								}
							}
							
							if(skip){
								return false;
							}
						}
					}
					
					toRender.add(Pair.of(RenderLayer.ALL, info));
					return false;
				};
				projection.processAll(bipred);
				
				boolean perfect = (goodBlocks.getValue() == projection.getBlockCount());

				BlockPos.MutableBlockPos min = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
				BlockPos.MutableBlockPos max = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
				float flicker = (world.random.nextInt(10) == 0) ? 0.75F : (world.random.nextInt(20) == 0 ? 0.5F : 1F);
				matrix.translate(hit.getX(), hit.getY(), hit.getZ());
				
				toRender.sort((a, b) -> {
					if(a.getLeft().ordinal() > b.getLeft().ordinal()){
						return 1;
					}else if(a.getLeft().ordinal() < b.getLeft().ordinal()){
						return -1;
					}
					return 0;
				});
				
				ItemStack heldStack = player.getMainHandItem();
				for(Pair<RenderLayer, MultiblockProjection.Info> pair:toRender){
					MultiblockProjection.Info rInfo = pair.getRight();
					
					switch(pair.getLeft()){
						case ALL:{ // All / Slice
							boolean held = heldStack.getItem() == rInfo.getRawState().getBlock().asItem();
							float alpha = held ? 0.55F : 0.25F;
							
							matrix.pushPose();
							{
								renderPhantom(matrix, world, rInfo, settings.isMirrored(), flicker, alpha, partialTicks);
								
								if(held){
									renderCenteredOutlineBox(matrix, 0xAFAFAF, flicker);
								}
							}
							matrix.popPose();
							break;
						}
						case BAD:{ // Bad block
							matrix.pushPose();
							{
								matrix.translate(rInfo.tPos.getX(), rInfo.tPos.getY(), rInfo.tPos.getZ());
								
								renderCenteredOutlineBox(matrix, 0xFF0000, flicker);
							}
							matrix.popPose();
							break;
						}
						case PERFECT:{
							int x = rInfo.tPos.getX();
							int y = rInfo.tPos.getY();
							int z = rInfo.tPos.getZ();
							
							min.set(
									(x < min.getX() ? x : min.getX()),
									(y < min.getY() ? y : min.getY()),
									(z < min.getZ() ? z : min.getZ()));
							
							max.set(
									(x > max.getX() ? x : max.getX()),
									(y > max.getY() ? y : max.getY()),
									(z > max.getZ() ? z : max.getZ()));
							break;
						}
					}
				}
				
				if(perfect){
					// Multiblock Correctly Built
					matrix.pushPose();
					{
						renderOutlineBox(matrix, min, max, 0x00BF00, flicker);
					}
					matrix.popPose();
					
					// Debugging Stuff
					if(!player.getOffhandItem().isEmpty() && player.getOffhandItem().getItem() == IPContent.debugItem){
						matrix.pushPose();
						{
							// Min (Red)
							matrix.translate(min.getX(), min.getY(), min.getZ());
							renderCenteredOutlineBox(matrix, 0xFF0000, flicker);
						}
						matrix.popPose();
						
						matrix.pushPose();
						{
							// Max (Greem)
							matrix.translate(max.getX(), max.getY(), max.getZ());
							renderCenteredOutlineBox(matrix, 0x00FF00, flicker);
						}
						matrix.popPose();
						
						matrix.pushPose();
						{
							// Center (Blue)
							BlockPos center = min.immutable().offset(max);
							matrix.translate(center.getX() / 2, center.getY() / 2, center.getZ() / 2);
							
							renderCenteredOutlineBox(matrix, 0x0000FF, flicker);
						}
						matrix.popPose();
					}
				}
			}
		}
		
		private static void renderPhantom(PoseStack matrix, Level realWorld, MultiblockProjection.Info rInfo, boolean mirror, float flicker, float alpha, float partialTicks){
			BlockModelShaper dispatcher = ClientUtils.mc().getBlockRenderer().getBlockModelShaper();
			BlockColors blockColors = ClientUtils.mc().getBlockColors();
			ModelBlockRenderer blockRenderer = new net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer(blockColors);
			
			// Centers the preview block
			matrix.translate(rInfo.tPos.getX(), rInfo.tPos.getY(), rInfo.tPos.getZ());
			
			MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
			
			BlockState state = rInfo.getModifiedState(realWorld, rInfo.tPos);
			
			ProjectorEvent.RenderBlock renderEvent = new ProjectorEvent.RenderBlock(rInfo.multiblock, rInfo.templateWorld, rInfo.tBlockInfo.pos, realWorld, rInfo.tPos, state, rInfo.settings.getRotation());
			if(!MinecraftForge.EVENT_BUS.post(renderEvent)){
				state = renderEvent.getState();
				state.updateNeighbourShapes(realWorld, rInfo.tPos, 3);
				
				IModelData modelData = EmptyModelData.INSTANCE;
				BlockEntity te = rInfo.templateWorld.getBlockEntity(rInfo.tBlockInfo.pos);
				if(te != null){
					te.setBlockState(state);
					modelData = te.getModelData();
				}

				RenderShape blockrendertype = state.getRenderShape();
				if(blockrendertype != RenderShape.INVISIBLE){
					if(blockrendertype == RenderShape.MODEL){
						BakedModel ibakedmodel = dispatcher.getBlockModel(state);
						int i = blockColors.getColor(state, null, null, 0);
						float red = (i >> 16 & 0xFF) / 255F;
						float green = (i >> 8 & 0xFF) / 255F;
						float blue = (i & 0xFF) / 255F;
						
						modelData = ibakedmodel.getModelData(rInfo.templateWorld, rInfo.tBlockInfo.pos, state, modelData);
						
						blockRenderer.renderModel(matrix.last(), buffer.getBuffer(RenderType.translucent()), state, ibakedmodel, red, green, blue, 0xF000F0, OverlayTexture.NO_OVERLAY, modelData);
						
					}else if(blockrendertype == RenderShape.ENTITYBLOCK_ANIMATED){
						ItemStack stack = new ItemStack(state.getBlock());
						net.minecraftforge.client.RenderProperties.get(stack.getItem()).getItemStackRenderer().renderByItem(stack, ItemTransforms.TransformType.NONE, matrix, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY);
					}
				}
			}
			
			ShaderUtil.alpha_static(flicker * alpha, ClientUtils.mc().player.tickCount + partialTicks);
			buffer.endBatch();
			ShaderUtil.releaseShader();
		}
		
		private static void renderOutlineBox(PoseStack matrix, BlockPos.MutableBlockPos min, BlockPos.MutableBlockPos max, int rgb, float flicker){
			MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
			VertexConsumer builder = buffer.getBuffer(IPRenderTypes.TRANSLUCENT_LINES);
			
			float alpha = 0.25F + (0.5F * flicker);
			
			float xMin = min.getX();
			float yMin = min.getY();
			float zMin = min.getZ();
			
			float xMax = max.getX() + 1F;
			float yMax = max.getY() + 1F;
			float zMax = max.getZ() + 1F;
			
			float r = ((rgb >> 16) & 0xFF) / 255F;
			float g = ((rgb >> 8) & 0xFF) / 255F;
			float b = ((rgb >> 0) & 0xFF) / 255F;
			
			// matrix.scale(xScale, yScale, zScale);
			Matrix4f mat = matrix.last().pose();
			
			builder.vertex(mat, xMin, yMax, zMin).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMax, zMin).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMax, zMin).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMax, zMax).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMax, zMax).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMin, yMax, zMax).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMin, yMax, zMax).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMin, yMax, zMin).color(r, g, b, alpha).endVertex();
			
			builder.vertex(mat, xMin, yMax, zMin).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMin, yMin, zMin).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMax, zMin).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMin, zMin).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMin, yMax, zMax).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMin, yMin, zMax).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMax, zMax).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMin, zMax).color(r, g, b, alpha).endVertex();
			
			builder.vertex(mat, xMin, yMin, zMin).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMin, zMin).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMin, zMin).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMin, zMax).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMax, yMin, zMax).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMin, yMin, zMax).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMin, yMin, zMax).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, xMin, yMin, zMin).color(r, g, b, alpha).endVertex();
			
			buffer.endBatch();
		}
		
		private static void renderCenteredOutlineBox(PoseStack matrix, int rgb, float flicker){
			MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
			VertexConsumer builder = buffer.getBuffer(IPRenderTypes.TRANSLUCENT_LINES);
			
			matrix.translate(0.5, 0.5, 0.5);
			matrix.scale(1.01F, 1.01F, 1.01F);
			Matrix4f mat = matrix.last().pose();
			
			float r = ((rgb >> 16) & 0xFF) / 255.0F;
			float g = ((rgb >> 8) & 0xFF) / 255.0F;
			float b = ((rgb >> 0) & 0xFF) / 255.0F;
			float alpha = .375F * flicker;
			float s = 0.5F;
			
			builder.vertex(mat, -s, s, -s)	.color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s, s, -s)	.color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s, s, -s)	.color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s, s,  s)	.color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s, s,  s)	.color(r, g, b, alpha).endVertex();
			builder.vertex(mat, -s, s,  s)	.color(r, g, b, alpha).endVertex();
			builder.vertex(mat, -s, s,  s)	.color(r, g, b, alpha).endVertex();
			builder.vertex(mat, -s, s, -s)	.color(r, g, b, alpha).endVertex();
			
			builder.vertex(mat, -s,  s, -s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, -s, -s, -s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s,  s, -s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s, -s, -s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, -s,  s,  s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, -s, -s,  s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s,  s,  s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s, -s,  s).color(r, g, b, alpha).endVertex();
			
			builder.vertex(mat, -s, -s, -s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s, -s, -s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s, -s, -s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s, -s,  s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat,  s, -s,  s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, -s, -s,  s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, -s, -s,  s).color(r, g, b, alpha).endVertex();
			builder.vertex(mat, -s, -s, -s).color(r, g, b, alpha).endVertex();
			
			buffer.endBatch();
		}
	}
	
	/** Client Input Stuff */
	@Mod.EventBusSubscriber(modid = ImmersivePetroleum.MODID, value = Dist.CLIENT)
	public static class ClientInputHandler{
		static boolean shiftHeld = false;
		
		@SubscribeEvent
		public static void onPlayerTick(TickEvent.PlayerTickEvent event){
			if(event.side == LogicalSide.CLIENT && event.player != null && event.player == ClientUtils.mc().getCameraEntity()){
				if(event.phase == Phase.END){
					if(!ClientProxy.keybind_preview_flip.isUnbound() && ClientProxy.keybind_preview_flip.consumeClick()){
						doAFlip();
					}
				}
			}
		}
		
		@SubscribeEvent
		public static void handleScroll(InputEvent.MouseScrollEvent event){
			double delta = event.getScrollDelta();
			
			if(shiftHeld && delta != 0.0){
				Player player = ClientUtils.mc().player;
				ItemStack mainItem = player.getMainHandItem();
				ItemStack secondItem = player.getMainHandItem();
				
				boolean main = !mainItem.isEmpty() && mainItem.getItem() == Items.projector && ItemNBTHelper.hasKey(mainItem, "settings", Tag.TAG_COMPOUND);
				boolean off = !secondItem.isEmpty() && secondItem.getItem() == Items.projector && ItemNBTHelper.hasKey(secondItem, "settings", Tag.TAG_COMPOUND);

				if(main || off){
					ItemStack target = main ? mainItem : secondItem;
					
					if(shiftHeld){
						Settings settings = getSettings(target);
						
						if(delta > 0){
							settings.rotateCCW();
						}else{
							settings.rotateCW();
						}
						
						settings.applyTo(target);
						settings.sendPacketToServer(main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
						
						Direction facing = Direction.from2DDataValue(settings.getRotation().ordinal());
						player.displayClientMessage(new TranslatableComponent("desc.immersivepetroleum.info.projector.rotated." + facing), true);
						
						event.setCanceled(true);
					}
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
		
		private static void doAFlip(){
			Player player = ClientUtils.mc().player;
			ItemStack mainItem = player.getMainHandItem();
			ItemStack secondItem = player.getMainHandItem();
			
			boolean main = !mainItem.isEmpty() && mainItem.getItem() == Items.projector && ItemNBTHelper.hasKey(mainItem, "settings", Tag.TAG_COMPOUND);
			boolean off = !secondItem.isEmpty() && secondItem.getItem() == Items.projector && ItemNBTHelper.hasKey(secondItem, "settings", Tag.TAG_COMPOUND);
			ItemStack target = main ? mainItem : secondItem;
			
			if(main || off){
				Settings settings = ProjectorItem.getSettings(target);
				
				settings.flip();
				settings.applyTo(target);
				settings.sendPacketToServer(main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
				
				Component flip;
				if(settings.isMirrored()){
					flip = new TranslatableComponent("desc.immersivepetroleum.info.projector.flipped.true");
				}else{
					flip = new TranslatableComponent("desc.immersivepetroleum.info.projector.flipped.false");
				}
				player.displayClientMessage(flip, true);
			}
		}
	}
	
	public static enum RenderLayer{
		ALL, BAD, PERFECT;
	}
}
