package openmods.proxy;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.server.FMLServerHandler;
import openmods.config.game.ICustomItemModelProvider;
import openmods.geometry.Hitbox;
import openmods.geometry.IHitboxSupplier;
import openmods.gui.CommonGuiHandler;

public final class OpenServerProxy implements IOpenModsProxy {

	@Override
	public PlayerEntity getThePlayer() {
		return null;
	}

	@Override
	public boolean isClientPlayer(Entity player) {
		return false;
	}

	@Override
	public long getTicks(World worldObj) {
		return worldObj.getTotalWorldTime();
	}

	@Override
	public World getClientWorld() {
		return null;
	}

	@Override
	public World getServerWorld(int id) {
		return DimensionManager.getWorld(id);
	}

	@Override
	public File getMinecraftDir() {
		// TODO may as well be used for client side
		return FMLServerHandler.instance().getServer().getFile("");
	}

	@Override
	public String getLogFileName() {
		return "ForgeModLoader-server-0.log";
	}

	@Override
	public Optional<String> getLanguage() {
		return Optional.absent();
	}

	@Override
	public IGuiHandler wrapHandler(IGuiHandler modSpecificHandler) {
		return new CommonGuiHandler(modSpecificHandler);
	}

	@Override
	public void preInit() {}

	@Override
	public void init() {}

	@Override
	public void postInit() {}

	@Override
	public void setNowPlayingTitle(String nowPlaying) {}

	@Override
	public PlayerEntity getPlayerFromHandler(INetHandler handler) {
		if (handler instanceof ServerPlayNetHandler) return ((ServerPlayNetHandler)handler).player;

		return null;
	}

	@Override
	public void bindItemModelToItemMeta(Item item, int metadata, ResourceLocation model) {}

	@Override
	public void registerCustomItemModel(Item item, int metadata, ResourceLocation resourceLocation) {}

	@Override
	public void runCustomItemModelProvider(ResourceLocation itemLocation, Item item, Class<? extends ICustomItemModelProvider> providerCls) {}

	private static final IHitboxSupplier DUMMY_HITBOX_SUPPLIER = new IHitboxSupplier() {
		@Override
		public List<Hitbox> asList() {
			throw new UnsupportedOperationException("Not available on server");
		}

		@Override
		public Map<String, Hitbox> asMap() {
			throw new UnsupportedOperationException("Not available on server");
		}
	};

	@Override
	public IHitboxSupplier getHitboxes(ResourceLocation location) {
		return DUMMY_HITBOX_SUPPLIER;
	}

	@Override
	public IAnimationStateMachine loadAsm(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters) {
		return null;
	}

}
