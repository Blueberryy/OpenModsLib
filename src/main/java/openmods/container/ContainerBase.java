package openmods.container;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import openmods.inventory.GenericInventory;
import openmods.utils.InventoryUtils;

public abstract class ContainerBase<T> extends Container {

	protected final int inventorySize;
	protected final IInventory playerInventory;
	protected final T owner;
	protected final IInventory inventory;

	protected static class RestrictedSlot extends Slot {

		public RestrictedSlot(IInventory inventory, int slot, int x, int y) {
			super(inventory, slot, x, y);
		}

		@Override
		public boolean isItemValid(@Nonnull ItemStack itemstack) {
			return inventory.isItemValidForSlot(getSlotIndex(), itemstack);
		}

		@Override
		public void onSlotChanged() {
			super.onSlotChanged();
			// hackish, but required
			if (inventory instanceof GenericInventory) ((GenericInventory)inventory).onInventoryChanged(getSlotIndex());
		}
	}

	// TODO 1.14 Re-design (custom factory at registration, get TE from there)
	public ContainerBase(@Nullable ContainerType<?> type, int id, IInventory playerInventory, IInventory ownerInventory, T owner) {
		super(type, id);
		this.owner = owner;
		this.inventory = ownerInventory;
		this.inventorySize = inventory.getSizeInventory();
		this.playerInventory = playerInventory;
	}

	protected void addInventoryGrid(int xOffset, int yOffset, int width) {
		int height = (int)Math.ceil((double)inventorySize / width);
		for (int y = 0, slotId = 0; y < height; y++) {
			for (int x = 0; x < width; x++, slotId++) {
				addSlot(new RestrictedSlot(inventory, slotId,
						xOffset + x * 18,
						yOffset + y * 18));
			}
		}
	}

	protected void addInventoryLine(int xOffset, int yOffset, int start, int count) {
		addInventoryLine(xOffset, yOffset, start, count, 0);
	}

	protected void addInventoryLine(int xOffset, int yOffset, int start, int count, int margin) {
		for (int x = 0, slotId = start; x < count; x++, slotId++) {
			addSlot(new RestrictedSlot(inventory, slotId,
					xOffset + x * (18 + margin),
					yOffset));
		}
	}

	protected void addPlayerInventorySlots(int offsetY) {
		addPlayerInventorySlots(8, offsetY);
	}

	protected void addPlayerInventorySlots(int offsetX, int offsetY) {
		for (int row = 0; row < 3; row++)
			for (int column = 0; column < 9; column++)
				addSlot(new Slot(playerInventory,
						column + row * 9 + 9,
						offsetX + column * 18,
						offsetY + row * 18));

		for (int slot = 0; slot < 9; slot++)
			addSlot(new Slot(playerInventory, slot, offsetX + slot
					* 18, offsetY + 58));
	}

	@Override
	public boolean canInteractWith(PlayerEntity entityplayer) {
		return inventory.isUsableByPlayer(entityplayer);
	}

	public T getOwner() {
		return owner;
	}

	protected boolean mergeItemStackSafe(@Nonnull ItemStack stackToMerge, int start, int stop, boolean reverse) {
		boolean inventoryChanged = false;

		final int delta = reverse? -1 : 1;
		List<Slot> slots = getSlots();

		if (stackToMerge.isStackable()) {
			int slotId = reverse? stop - 1 : start;
			while (!stackToMerge.isEmpty() && ((!reverse && slotId < stop) || (reverse && slotId >= start))) {

				Slot slot = slots.get(slotId);

				if (canTransferItemsIn(slot)) {
					ItemStack stackInSlot = slot.getStack();

					if (InventoryUtils.tryMergeStacks(stackToMerge, stackInSlot)) {
						slot.onSlotChanged();
						inventoryChanged = true;
					}
				}

				slotId += delta;
			}
		}

		if (!stackToMerge.isEmpty()) {
			int slotId = reverse? stop - 1 : start;

			while ((!reverse && slotId < stop) || (reverse && slotId >= start)) {
				Slot slot = slots.get(slotId);
				ItemStack stackInSlot = slot.getStack();

				if (stackInSlot.isEmpty() && canTransferItemsIn(slot) && slot.isItemValid(stackToMerge)) {
					slot.putStack(stackToMerge.copy());
					slot.onSlotChanged();
					stackToMerge.setCount(0);
					return true;
				}

				slotId += delta;
			}
		}

		return inventoryChanged;
	}

	@Override
	@Nonnull
	public ItemStack transferStackInSlot(PlayerEntity player, int slotId) {
		final Slot slot = inventorySlots.get(slotId);

		if (slot != null && canTransferItemOut(slot) && slot.getHasStack()) {
			ItemStack itemToTransfer = slot.getStack();
			ItemStack copy = itemToTransfer.copy();
			if (slotId < inventorySize) {
				if (!mergeItemStackSafe(itemToTransfer, inventorySize, inventorySlots.size(), true)) return ItemStack.EMPTY;
			} else if (!mergeItemStackSafe(itemToTransfer, 0, inventorySize, false)) return ItemStack.EMPTY;

			slot.putStack(itemToTransfer);

			if (itemToTransfer.getCount() != copy.getCount()) return copy;
		}
		return ItemStack.EMPTY;
	}

	protected boolean canTransferItemOut(Slot slot) {
		if (slot instanceof ICustomSlot) return ((ICustomSlot)slot).canTransferItemsOut();
		return true;
	}

	protected boolean canTransferItemsIn(Slot slot) {
		if (slot instanceof ICustomSlot) return ((ICustomSlot)slot).canTransferItemsIn();
		return true;
	}

	public int getInventorySize() {
		return inventorySize;
	}

	protected List<Slot> getSlots() {
		return inventorySlots;
	}

	public void onButtonClicked(PlayerEntity player, int buttonId) {}

	@Override
	public boolean enchantItem(PlayerEntity player, int buttonId) {
		onButtonClicked(player, buttonId);
		return false;
	}

	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickType, PlayerEntity player) {
		if (slotId >= 0 && slotId < inventorySlots.size()) {
			Slot slot = getSlot(slotId);
			if (slot instanceof ICustomSlot) return ((ICustomSlot)slot).onClick(player, dragType, clickType);
		}

		return super.slotClick(slotId, dragType, clickType, player);
	}

	@Override
	public boolean canDragIntoSlot(Slot slot) {
		if (slot instanceof ICustomSlot) return ((ICustomSlot)slot).canDrag();

		return super.canDragIntoSlot(slot);
	}

}
