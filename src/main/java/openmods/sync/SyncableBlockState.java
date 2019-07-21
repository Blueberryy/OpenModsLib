package openmods.sync;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

public class SyncableBlockState extends SyncableObjectBase implements ISyncableValueProvider<BlockState> {

	private static final String TAG_BLOCK_META = "Meta";
	private static final String TAG_BLOCK_ID = "Id";
	private BlockState state = Blocks.AIR.getDefaultState();

	@Override
	public void readFromStream(PacketBuffer buf) {
		final int id = buf.readVarInt();
		state = Block.getStateById(id);
	}

	@Override
	public void writeToStream(PacketBuffer buf) {
		final int id = Block.getStateId(state);
		buf.writeVarInt(id);
	}

	@Override
	public void writeToNBT(CompoundNBT nbt, String name) {
		final CompoundNBT tag = new CompoundNBT();

		final Block block = state.getBlock();

		final int meta = block.getMetaFromState(state);
		tag.setByte(TAG_BLOCK_META, (byte)meta);

		final ResourceLocation blockId = Block.REGISTRY.getNameForObject(block);
		tag.setString(TAG_BLOCK_ID, blockId.toString());

		nbt.setTag(name, tag);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void readFromNBT(CompoundNBT nbt, String name) {
		state = Blocks.AIR.getDefaultState();

		if (nbt.hasKey(name, Constants.NBT.TAG_COMPOUND)) {
			final CompoundNBT tag = nbt.getCompoundTag(name);

			if (tag.hasKey(TAG_BLOCK_ID, Constants.NBT.TAG_STRING)) {
				final ResourceLocation blockId = new ResourceLocation(tag.getString(TAG_BLOCK_ID));
				final Block block = Block.REGISTRY.getObject(blockId);

				int meta = tag.getByte(TAG_BLOCK_META) & 255;
				state = block.getStateFromMeta(meta);
			}
		}
	}

	@Override
	public BlockState getValue() {
		return state;
	}

	public void setValue(BlockState state) {
		this.state = state;
		markDirty();
	}

	public boolean isAir() {
		return state == Blocks.AIR.getDefaultState();
	}

}
