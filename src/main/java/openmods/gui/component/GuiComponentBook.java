package openmods.gui.component;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.audio.SimpleSound;
import openmods.Sounds;
import openmods.gui.Icon;
import openmods.gui.component.page.BookScaleConfig;
import openmods.gui.component.page.PageBase;
import openmods.gui.listener.IMouseDownListener;
import openmods.utils.TranslationUtils;

public class GuiComponentBook extends BaseComposite {

	public static final Icon iconPageLeft = Icon.createSheetIcon(PageBase.BOOK_TEXTURE, 211, 0, -211, 180);
	public static final Icon iconPageRight = Icon.createSheetIcon(PageBase.BOOK_TEXTURE, 0, 0, 211, 180);
	public static final Icon iconPrev = Icon.createSheetIcon(PageBase.BOOK_TEXTURE, 57, 226, 18, 10);
	public static final Icon iconNext = Icon.createSheetIcon(PageBase.BOOK_TEXTURE, 57, 213, 18, 10);
	public static final Icon iconPrevHover = Icon.createSheetIcon(PageBase.BOOK_TEXTURE, 80, 226, 18, 10);
	public static final Icon iconNextHover = Icon.createSheetIcon(PageBase.BOOK_TEXTURE, 80, 213, 18, 10);

	private final GuiComponentSpriteButton imgPrev;
	private final GuiComponentSpriteButton imgNext;
	private final GuiComponentLabel pageNumberLeft;
	private final GuiComponentLabel pageNumberRight;

	public final List<BaseComponent> pages;

	private int index = 0;

	public GuiComponentBook() {
		super(0, 0);

		GuiComponentSprite imgLeftBackground = new GuiComponentSprite(0, 0, iconPageLeft);
		GuiComponentSprite imgRightBackground = new GuiComponentSprite(0, 0, iconPageRight);
		imgRightBackground.setX(iconPageRight.width);

		imgPrev = new GuiComponentSpriteButton(24, 158, iconPrev, iconPrevHover);
		imgPrev.setListener((IMouseDownListener)(component, x, y, button) -> prevPage());
		imgNext = new GuiComponentSpriteButton(380, 158, iconNext, iconNextHover);
		imgNext.setListener((IMouseDownListener)(component, x, y, button) -> nextPage());

		final float scalePageNumber = BookScaleConfig.getPageNumberScale();
		pageNumberLeft = new GuiComponentLabel(85, 163, 100, 10, "XXX");
		pageNumberLeft.setScale(scalePageNumber);
		pageNumberRight = new GuiComponentLabel(295, 163, 100, 10, "XXX");
		pageNumberRight.setScale(scalePageNumber);

		addComponent(imgLeftBackground);
		addComponent(imgRightBackground);
		addComponent(imgPrev);
		addComponent(imgNext);
		addComponent(pageNumberLeft);
		addComponent(pageNumberRight);

		pages = Lists.newArrayList();

	}

	public int getNumberOfPages() {
		return pages.size();
	}

	@Override
	public int getWidth() {
		return iconPageRight.width * 2;
	}

	@Override
	public int getHeight() {
		return iconPageRight.height;
	}

	public void addPage(BaseComponent page) {
		addComponent(page);
		page.setEnabled(false);
		pages.add(page);
	}

	public void enablePages() {
		int i = 0;
		for (BaseComponent page : pages) {
			final boolean isLeft = i == index;
			final boolean isRight = i == index + 1;

			if (isLeft) {
				page.setEnabled(true);
				page.setX(20);

			} else if (isRight) {
				page.setEnabled(true);
				page.setX(10 + iconPageRight.width);
			} else {
				page.setEnabled(false);
			}
			i++;
		}

		int totalPageCount = i % 2 == 0? i : i + 1;

		imgNext.setEnabled(index < pages.size() - 2);
		imgPrev.setEnabled(index > 0);
		pageNumberLeft.setText(TranslationUtils.translateToLocalFormatted("openmodslib.book.page", index + 1, totalPageCount));
		pageNumberRight.setText(TranslationUtils.translateToLocalFormatted("openmodslib.book.page", index + 2, totalPageCount));
	}

	public void changePage(int newPage) {
		newPage &= ~1;
		if (newPage != index) {
			index = newPage;
			enablePages();
			playPageTurnSound();
		}
	}

	private void playPageTurnSound() {
		parent.getSoundHandler().play(SimpleSound.master(Sounds.PAGE_TURN, 1.0f));
	}

	public IMouseDownListener createBookmarkListener(final int index) {
		return (component, x, y, button) -> changePage(index);
	}

	public void prevPage() {
		if (index > 0) changePage(index - 2);
	}

	public void nextPage() {
		if (index < pages.size() - 2) changePage(index + 2);
	}

	public void firstPage() {
		changePage(0);
	}

	public void lastPage() {
		changePage(getNumberOfPages() - 1);
	}
}
