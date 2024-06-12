package squeek.spiceoflife;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;
import squeek.spiceoflife.items.ItemFoodContainer;
import squeek.spiceoflife.items.ItemFoodJournal;

public class ModContent {

    public static ItemFoodJournal foodJournal;
    public static ItemFoodContainer lunchBox;
    public static ItemFoodContainer lunchBag;

    public static void registerItems() {
        foodJournal = new ItemFoodJournal();
        GameRegistry.registerItem(foodJournal, ModConfig.ITEM_FOOD_JOURNAL_NAME);

        lunchBox = new ItemFoodContainer(ModConfig.ITEM_LUNCH_BOX_NAME, 6);
        GameRegistry.registerItem(lunchBox, ModConfig.ITEM_LUNCH_BOX_NAME);

        lunchBag = new ItemFoodContainer(ModConfig.ITEM_LUNCH_BAG_NAME, 3);
        GameRegistry.registerItem(lunchBag, ModConfig.ITEM_LUNCH_BAG_NAME);
    }

    public static void registerRecipes() {
        GameRegistry
            .addShapelessRecipe(new ItemStack(foodJournal), new ItemStack(Items.wheat), new ItemStack(Items.paper));
        GameRegistry.addShapedRecipe(
            new ItemStack(lunchBox),
            "_ _",
            " _ ",
            '_',
            new ItemStack(Blocks.heavy_weighted_pressure_plate));
        GameRegistry.addShapedRecipe(new ItemStack(lunchBag), "p p", " p ", 'p', new ItemStack(Items.paper));
    }
}
