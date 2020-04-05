package openmods.sync;

import com.google.common.base.Preconditions;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import openmods.utils.ByteUtils;
import openmods.utils.bitmap.IBitMap;
import openmods.utils.bitmap.IRpcIntBitMap;

public abstract class SyncableFlags extends SyncableObjectBase implements IRpcIntBitMap, IBitMap<Integer> {

	public static class ByteFlags extends SyncableFlags {
		@Override
		public void readFromStream(PacketBuffer stream) {
			value = stream.readByte();
		}

		@Override
		public void writeToStream(PacketBuffer stream) {
			stream.writeByte(value);
		}

		@Override
		public void writeToNBT(CompoundNBT tag, String name) {
			tag.putByte(name, (byte)value);
		}

		@Override
		public void readFromNBT(CompoundNBT tag, String name) {
			value = tag.getByte(name);
		}
	}

	public static class ShortFlags extends SyncableFlags {
		@Override
		public void readFromStream(PacketBuffer stream) {
			value = stream.readShort();
		}

		@Override
		public void writeToStream(PacketBuffer stream) {
			stream.writeShort(value);
		}

		@Override
		public void writeToNBT(CompoundNBT tag, String name) {
			tag.putShort(name, (short)value);
		}

		@Override
		public void readFromNBT(CompoundNBT tag, String name) {
			value = tag.getShort(name);
		}
	}

	public static class IntFlags extends SyncableFlags {
		@Override
		public void readFromStream(PacketBuffer stream) {
			value = stream.readInt();
		}

		@Override
		public void writeToStream(PacketBuffer stream) {
			stream.writeInt(value);
		}

		@Override
		public void writeToNBT(CompoundNBT tag, String name) {
			tag.putInt(name, value);
		}

		@Override
		public void readFromNBT(CompoundNBT tag, String name) {
			value = tag.getInt(name);
		}
	}

	public static SyncableFlags create(int bitCount) {
		Preconditions.checkArgument(bitCount > 0, "Bit count must be positive");
		if (bitCount <= Byte.SIZE) return new ByteFlags();
		if (bitCount <= Short.SIZE) return new ShortFlags();
		if (bitCount <= Integer.SIZE) return new IntFlags();

		throw new IllegalArgumentException("Too many bits. Split some fields or implement LongFlags or BigIntFlags");
	}

	protected int value;
	private int previousValue;

	protected SyncableFlags() {}

	public void on(Enum<?> slot) {
		on(slot.ordinal());
	}

	public void on(int slot) {
		set(slot, true);
	}

	@Override
	public void mark(Integer value) {
		on(value);
	}

	public void off(Enum<?> slot) {
		off(slot.ordinal());
	}

	public void off(int slot) {
		set(slot, false);
	}

	@Override
	public void clear(Integer value) {
		off(value);
	}

	public void set(Enum<?> slot, boolean bool) {
		set(slot.ordinal(), bool);
	}

	public void toggle(int slot) {
		set(value ^ (1 << slot));
	}

	@Override
	public void toggle(Integer value) {
		toggle(value.intValue());
	}

	public void toggle(Enum<?> slot) {
		toggle(slot.ordinal());
	}

	private void set(int value) {
		if (value != this.value) {
			markDirty();
			this.value = (short)value;
		}
	}

	public void set(int slot, boolean bool) {
		short newVal = (short)ByteUtils.set(value, slot, bool);
		if (newVal != value) {
			markDirty();
			value = newVal;
		}
	}

	@Override
	public void set(Integer slot, boolean bool) {
		set(slot.intValue(), bool);
	}

	public boolean get(Enum<?> slot) {
		return get(slot.ordinal());
	}

	public boolean get(int slot) {
		return ByteUtils.get(value, slot);
	}

	@Override
	public boolean get(Integer value) {
		return get(value.intValue());
	}

	public boolean hasSlotChanged(Enum<?> slot) {
		return hasSlotChanged(slot.ordinal());
	}

	public boolean hasSlotChanged(int slot) {
		int mask = 1 << slot;
		return (value & mask) == (previousValue & mask);
	}

	@Override
	public void markClean() {
		previousValue = value;
		dirty = false;
	}

	@Override
	public void clearAll() {
		value = 0;
		markDirty();
	}
}
