package openmods.gui.component.page;

import com.google.common.base.Strings;
import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraftforge.common.crafting.IShapedRecipe;
import openmods.gui.Icon;
import openmods.gui.component.GuiComponentCraftingGrid;
import openmods.gui.component.GuiComponentHCenter;
import openmods.gui.component.GuiComponentItemStackSpinner;
import openmods.gui.component.GuiComponentLabel;
import openmods.gui.component.GuiComponentSprite;
import openmods.utils.RecipeUtils;
import openmods.utils.TranslationUtils;

public class StandardRecipePage extends PageBase {

	public static final Icon iconCraftingGrid = Icon.createSheetIcon(BOOK_TEXTURE, 0, 180, 56, 56);
	public static final Icon iconArrow = Icon.createSheetIcon(BOOK_TEXTURE, 60, 198, 48, 15);

	public StandardRecipePage(String title, String description, final RecipeManager manager, @Nonnull ItemStack resultingItem) {
		addComponent(new GuiComponentSprite(75, 40, iconArrow));
		addComponent(new GuiComponentItemStackSpinner(140, 30, resultingItem));

		{
			final IRecipe recipe = RecipeUtils.getFirstRecipeForItemStack(manager, resultingItem);
			if (recipe != null) {
				final ItemStack[][] input = RecipeUtils.getFullRecipeInput(recipe);
				if (input != null) {
					final int width = (recipe instanceof IShapedRecipe)? ((IShapedRecipe)recipe).getRecipeWidth() : 3;
					addComponent(new GuiComponentCraftingGrid(10, 20, input, width, iconCraftingGrid));
				}
			}
		}

		{
			String translatedTitle = TranslationUtils.translateToLocal(title);
			final GuiComponentLabel titleLabel = new GuiComponentLabel(0, 0, translatedTitle);
			titleLabel.setScale(BookScaleConfig.getPageTitleScale());
			addComponent(new GuiComponentHCenter(0, 2, getWidth()).addComponent(titleLabel));
		}

		{
			String translatedDescription = TranslationUtils.translateToLocal(description).replaceAll("\\\\n", "\n");
			GuiComponentLabel lblDescription = new GuiComponentLabel(10, 80, getWidth() - 5, 200, translatedDescription);
			lblDescription.setScale(BookScaleConfig.getPageContentScale());
			lblDescription.setAdditionalLineHeight(BookScaleConfig.getRecipePageSeparator());
			addComponent(lblDescription);
		}
	}

	public StandardRecipePage(String title, String description, String videoLink, final RecipeManager manager, @Nonnull ItemStack resultingItem) {
		this(title, description, manager, resultingItem);

		if (!Strings.isNullOrEmpty(videoLink)) {
			addActionButton(10, 133, videoLink, ActionIcon.YOUTUBE.icon, "openmodslib.gui.watch_video");
		}
	}

}
