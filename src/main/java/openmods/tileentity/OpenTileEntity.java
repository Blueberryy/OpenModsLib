package openmods.tileentity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import openmods.api.IInventoryCallback;
import openmods.block.BlockRotationMode;
import openmods.block.IBlockRotationMode;
import openmods.block.OpenBlock;
import openmods.geometry.LocalDirections;
import openmods.geometry.Orientation;
import openmods.inventory.GenericInventory;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.IRpcTargetProvider;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.TileEntityRpcTarget;
import openmods.network.senders.IPacketSender;
import openmods.reflection.TypeUtils;
import openmods.utils.BlockUtils;

public abstract class OpenTileEntity extends TileEntity implements IRpcTargetProvider {

	/** Place for TE specific setup. Called once upon creation */
	public void setup() {}

	public TargetPoint getDimCoords() {
		return new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 0);
	}

	public Orientation getOrientation() {
		final BlockState state = world.getBlockState(pos);
		return getOrientation(state);
	}

	public Orientation getOrientation(BlockState state) {
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock)) return Orientation.XP_YP;
		final OpenBlock openBlock = (OpenBlock)block;
		return openBlock.getOrientation(state);
	}

	public IBlockRotationMode getRotationMode() {
		final BlockState state = world.getBlockState(pos);
		return getRotationMode(state);
	}

	public IBlockRotationMode getRotationMode(BlockState state) {
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock)) return BlockRotationMode.NONE;
		final OpenBlock openBlock = (OpenBlock)block;
		return openBlock.rotationMode;
	}

	public Direction getFront() {
		final BlockState state = world.getBlockState(pos);
		return getFront(state);
	}

	public Direction getFront(BlockState state) {
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock)) return Direction.NORTH;
		final OpenBlock openBlock = (OpenBlock)block;
		return openBlock.getFront(state);
	}

	public Direction getBack() {
		return getFront().getOpposite();
	}

	public LocalDirections getLocalDirections() {
		final BlockState state = world.getBlockState(pos);
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock)) return LocalDirections.fromFrontAndTop(Direction.NORTH, Direction.UP);
		final OpenBlock openBlock = (OpenBlock)block;
		return openBlock.getLocalDirections(state);
	}

	public boolean isAddedToWorld() {
		return world != null;
	}

	protected TileEntity getTileEntity(BlockPos blockPos) {
		return (world != null && world.isBlockLoaded(blockPos))? world.getTileEntity(blockPos) : null;
	}

	public TileEntity getTileInDirection(Direction direction) {
		return getTileEntity(pos.offset(direction));
	}

	public boolean isAirBlock(Direction direction) {
		return world != null && world.isAirBlock(getPos().offset(direction));
	}

	protected void playSoundAtBlock(SoundEvent sound, SoundCategory category, float volume, float pitch) {
		BlockUtils.playSoundAtPos(world, pos, sound, category, volume, pitch);
	}

	protected void playSoundAtBlock(SoundEvent sound, float volume, float pitch) {
		playSoundAtBlock(sound, SoundCategory.BLOCKS, volume, pitch);
	}

	protected void spawnParticle(EnumParticleTypes particle, double dx, double dy, double dz, double vx, double vy, double vz, int... args) {
		world.spawnParticle(particle, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, vx, vy, vz, args);
	}

	protected void spawnParticle(EnumParticleTypes particle, double vx, double vy, double vz, int... args) {
		spawnParticle(particle, 0.5, 0.5, 0.5, vx, vy, vz, args);
	}

	public void sendBlockEvent(int event, int param) {
		world.addBlockEvent(pos, getBlockType(), event, param);
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, BlockState oldState, BlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}

	public void openGui(Object instance, PlayerEntity player) {
		player.openGui(instance, -1, world, pos.getX(), pos.getY(), pos.getZ());
	}

	public AxisAlignedBB getBB() {
		return new AxisAlignedBB(pos, pos.add(1, 1, 1));
	}

	@Override
	public IRpcTarget createRpcTarget() {
		return new TileEntityRpcTarget(this);
	}

	public <T> T createProxy(final IPacketSender sender, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		TypeUtils.isInstance(this, mainIntf, extraIntf);
		return RpcCallDispatcher.instance().createProxy(createRpcTarget(), sender, mainIntf, extraIntf);
	}

	public <T> T createClientRpcProxy(Class<? extends T> mainIntf, Class<?>... extraIntf) {
		final IPacketSender sender = RpcCallDispatcher.instance().senders.client;
		return createProxy(sender, mainIntf, extraIntf);
	}

	public <T> T createServerRpcProxy(Class<? extends T> mainIntf, Class<?>... extraIntf) {
		final IPacketSender sender = RpcCallDispatcher.instance().senders.block.bind(getDimCoords());
		return createProxy(sender, mainIntf, extraIntf);
	}

	public void markUpdated() {
		world.markChunkDirty(pos, this);
	}

	protected IInventoryCallback createInventoryCallback() {
		return (inventory, slotNumber) -> markUpdated();
	}

	protected GenericInventory registerInventoryCallback(GenericInventory inventory) {
		return inventory.addCallback(createInventoryCallback());
	}

	public boolean isValid(PlayerEntity player) {
		return (world.getTileEntity(pos) == this) && (player.getDistanceSqToCenter(pos) <= 64.0D);
	}
}
