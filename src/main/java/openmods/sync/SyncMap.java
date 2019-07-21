package openmods.sync;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public abstract class SyncMap {

	public static class SyncFieldException extends RuntimeException {
		private static final long serialVersionUID = -3154521464407191767L;

		public SyncFieldException(Throwable cause, String name) {
			super(String.format("Failed to sync field '%s'", name), cause);
		}

		public SyncFieldException(Throwable cause, int index) {
			super(String.format("Failed to sync field #%d", index), cause);
		}
	}

	// Note: try... methods are non-throwing variants of read/write methods
	// They are convenient when methods can be called via client-side HUD modifiers

	public abstract void read(CompoundNBT tag);

	public abstract boolean tryRead(CompoundNBT tag);

	public abstract void write(CompoundNBT tag);

	public abstract boolean tryWrite(CompoundNBT tag);

	public abstract void readIntializationData(PacketBuffer dis) throws IOException;

	public abstract void writeInitializationData(PacketBuffer dos) throws IOException;

	public abstract void readUpdate(PacketBuffer dis) throws IOException;

	public abstract void sendUpdates();

	public abstract boolean trySendUpdates();

	public abstract void registerObject(String name, ISyncableObject value);

	public abstract ISyncableObject getObjectById(int id);

	public abstract int getObjectId(ISyncableObject object);

	public abstract void addSyncListener(ISyncListener listener);

	protected static void notifySyncListeners(Collection<ISyncListener> listeners, Set<ISyncableObject> allChanges) {
		for (ISyncListener listener : listeners)
			listener.onSync(allChanges);
	}

	public abstract void addUpdateListener(ISyncListener listener);

	public abstract void removeUpdateListener(ISyncListener dispatcher);
}
