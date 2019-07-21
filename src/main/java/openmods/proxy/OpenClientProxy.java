package openmods.proxy;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.IGuiHandler;
import openmods.LibConfig;
import openmods.OpenMods;
import openmods.block.BlockSelectionHandler;
import openmods.calc.CommandCalc;
import openmods.calc.CommandCalcFactory;
import openmods.calc.ICommandComponent;
import openmods.config.game.ICustomItemModelProvider;
import openmods.config.properties.CommandConfig;
import openmods.geometry.HitboxManager;
import openmods.geometry.IHitboxSupplier;
import openmods.gui.ClientGuiHandler;
import openmods.model.MappedModelLoader;
import openmods.model.ModelWithDependencies;
import openmods.model.MultiLayerModel;
import openmods.model.PerspectiveAwareModel;
import openmods.model.eval.EvalExpandModel;
import openmods.model.eval.EvalModel;
import openmods.model.itemstate.ItemStateModel;
import openmods.model.textureditem.TexturedItemModel;
import openmods.model.variant.VariantModel;
import openmods.renderer.CommandGlDebug;
import openmods.source.CommandSource;
import openmods.utils.CachedFactory;
import openmods.utils.SneakyThrower;
import openmods.utils.render.FramebufferBlitter;
import openmods.utils.render.RenderUtils;

public final class OpenClientProxy implements IOpenModsProxy {

	private final HitboxManager hitboxManager = new HitboxManager();

	@Override
	public PlayerEntity getThePlayer() {
		return FMLClientHandler.instance().getClient().player;
	}

	@Override
	public boolean isClientPlayer(Entity player) {
		return player instanceof ClientPlayerEntity;
	}

	@Override
	public long getTicks(World worldObj) {
		if (worldObj != null) { return worldObj.getTotalWorldTime(); }
		World cWorld = getClientWorld();
		if (cWorld != null) return cWorld.getTotalWorldTime();
		return 0;
	}

	@Override
	public World getClientWorld() {
		return Minecraft.getMinecraft().world;
	}

	@Override
	public World getServerWorld(int id) {
		return DimensionManager.getWorld(id);
	}

	@Override
	public File getMinecraftDir() {
		return Minecraft.getMinecraft().mcDataDir;
	}

	@Override
	public String getLogFileName() {
		return "ForgeModLoader-client-0.log";
	}

	@Override
	public Optional<String> getLanguage() {
		return Optional.fromNullable(Minecraft.getMinecraft().gameSettings.language);
	}

	@Override
	public IGuiHandler wrapHandler(IGuiHandler modSpecificHandler) {
		return new ClientGuiHandler(modSpecificHandler);
	}

	@Override
	public void preInit() {
		ClientCommandHandler.instance.registerCommand(new CommandConfig("om_config_c", false));
		ClientCommandHandler.instance.registerCommand(new CommandSource("om_source_c", false, OpenMods.instance.getCollector()));
		ClientCommandHandler.instance.registerCommand(new CommandGlDebug());

		if (LibConfig.enableCalculatorCommands) {
			final ICommandComponent commandRoot = new CommandCalcFactory(new File(getMinecraftDir(), "scripts")).getRoot();
			ClientCommandHandler.instance.registerCommand(new CommandCalc(commandRoot, "config"));
			ClientCommandHandler.instance.registerCommand(new CommandCalc(commandRoot, "eval", "="));
			ClientCommandHandler.instance.registerCommand(new CommandCalc(commandRoot, "fun"));
			ClientCommandHandler.instance.registerCommand(new CommandCalc(commandRoot, "let"));
			ClientCommandHandler.instance.registerCommand(new CommandCalc(commandRoot, "execute"));
		}

		RenderUtils.registerFogUpdater();

		MinecraftForge.EVENT_BUS.register(new BlockSelectionHandler());

		ModelLoaderRegistry.registerLoader(MappedModelLoader.builder()
				.put("with-dependencies", ModelWithDependencies.EMPTY)
				.put("multi-layer", MultiLayerModel.EMPTY)
				.put("variantmodel", VariantModel.EMPTY_MODEL)
				.put("textureditem", TexturedItemModel.INSTANCE)
				.put("stateitem", ItemStateModel.EMPTY)
				.put("eval", EvalModel.EMPTY)
				.put("eval-expand", EvalExpandModel.EMPTY)
				.put("perspective-aware", PerspectiveAwareModel.EMPTY)
				.build(OpenMods.MODID));

		((IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(hitboxManager);

		FramebufferBlitter.setup();
	}

	@Override
	public void init() {}

	@Override
	public void postInit() {}

	@Override
	public void setNowPlayingTitle(String nowPlaying) {
		Minecraft.getMinecraft().ingameGUI.setRecordPlayingMessage(nowPlaying);
	}

	@Override
	public PlayerEntity getPlayerFromHandler(INetHandler handler) {
		if (handler instanceof ServerPlayNetHandler) return ((ServerPlayNetHandler)handler).player;

		if (handler instanceof ClientPlayNetHandler) return getThePlayer();

		return null;
	}

	@Override
	public void bindItemModelToItemMeta(Item item, int metadata, ResourceLocation model) {
		final ItemModelMesher mesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
		mesher.register(item, metadata, new ModelResourceLocation(model, "inventory"));
	}

	@Override
	public void registerCustomItemModel(Item item, int metadata, ResourceLocation resourceLocation) {
		final ModelResourceLocation modelLoc = (resourceLocation instanceof ModelResourceLocation)
				? (ModelResourceLocation)resourceLocation
				: new ModelResourceLocation(resourceLocation, "inventory");
		ModelLoader.setCustomModelResourceLocation(item, metadata, modelLoc);
	}

	private static final CachedFactory<Class<? extends ICustomItemModelProvider>, ICustomItemModelProvider> customItemModelProviders = new CachedFactory<Class<? extends ICustomItemModelProvider>, ICustomItemModelProvider>() {
		@Override
		protected ICustomItemModelProvider create(Class<? extends ICustomItemModelProvider> key) {
			try {
				return key.newInstance();
			} catch (Exception e) {
				throw SneakyThrower.sneakyThrow(e);
			}
		}
	};

	@Override
	public void runCustomItemModelProvider(final ResourceLocation location, final Item item, Class<? extends ICustomItemModelProvider> providerCls) {
		final ICustomItemModelProvider provider = customItemModelProviders.getOrCreate(providerCls);
		provider.addCustomItemModels(item, location, (meta, modelLocation) -> OpenMods.proxy.registerCustomItemModel(item, meta, modelLocation));
	}

	@Override
	public IHitboxSupplier getHitboxes(ResourceLocation location) {
		return hitboxManager.get(location);
	}

	@Override
	public IAnimationStateMachine loadAsm(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters) {
		return ModelLoaderRegistry.loadASM(location, parameters);
	}

}
