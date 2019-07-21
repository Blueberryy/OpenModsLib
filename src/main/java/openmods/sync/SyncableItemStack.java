package openmods.sync;

import java.io.IOException;
import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;

public class SyncableItemStack extends SyncableObjectBase {

	@Nonnull
	private ItemStack stack = ItemStack.EMPTY;

	@Override
	public void readFromStream(PacketBuffer stream) throws IOException {
		this.stack = stream.readItemStack();
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeItemStack(this.stack);

	}

	@Override
	public void writeToNBT(CompoundNBT nbt, String name) {
		if (stack.isEmpty()) {
			CompoundNBT serialized = new CompoundNBT();
			stack.writeToNBT(serialized);
			nbt.setTag(name, serialized);
		}
	}

	@Override
	public void readFromNBT(CompoundNBT nbt, String name) {
		if (nbt.hasKey(name, Constants.NBT.TAG_COMPOUND)) {
			CompoundNBT serialized = nbt.getCompoundTag(name);
			stack = new ItemStack(serialized);
		} else {
			stack = ItemStack.EMPTY;
		}
	}

	public void set(@Nonnull ItemStack stack) {
		this.stack = stack.copy();
		markDirty();
	}

	@Nonnull
	public ItemStack get() {
		return stack;
	}
}
