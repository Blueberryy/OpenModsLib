package openmods.gui.component;

import com.mojang.blaze3d.platform.GlStateManager;
import openmods.api.IValueReceiver;
import openmods.gui.listener.IValueChangedListener;

public class GuiComponentCheckbox extends BaseComponent implements IValueReceiver<Boolean> {
	private boolean value;
	private IValueChangedListener<Boolean> listener;

	public GuiComponentCheckbox(int x, int y, boolean initialValue) {
		super(x, y);
		this.value = initialValue;
	}

	@Override
	public void render(int offsetX, int offsetY, int mouseX, int mouseY) {
		GlStateManager.color4f(1, 1, 1, 1);
		bindComponentsSheet();
		blit(offsetX + x, offsetY + y, value? 16 : 0, 62, 8, 8);
	}

	@Override
	public void mouseDown(int x, int y, int button) {
		super.mouseDown(x, y, button);
		value = !value;
		if (listener != null) listener.valueChanged(value);
	}

	@Override
	public int getHeight() {
		return 8;
	}

	@Override
	public int getWidth() {
		return 8;
	}

	public boolean getValue() {
		return value;
	}

	@Override
	public void setValue(Boolean value) {
		this.value = value;
	}

	public void setListener(IValueChangedListener<Boolean> listener) {
		this.listener = listener;
	}
}
