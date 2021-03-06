package squeek.spiceoflife.foodtracker;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import squeek.applecore.api.food.FoodValues;
import squeek.spiceoflife.ModConfig;
import squeek.spiceoflife.ModSpiceOfLife;
import squeek.spiceoflife.helpers.FoodHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class FoodLists {
    private static List<ItemStack> allFoods;

    public static void setUp() {
        if(ModConfig.DEV_LOGGING_ENABLED)
            ModSpiceOfLife.Log.info("Starting populating food list.");
        Stream<Item> stream = StreamSupport.stream(Item.itemRegistry.spliterator(), false);
        allFoods = stream
            .map(ItemStack::new)
            .filter(FoodHelper::isFood)
            .sorted(Comparator.comparing(ItemStack::getDisplayName))
            .collect(Collectors.toList());

        allFoods.forEach((i -> {
            FoodValues fv = FoodHelper.getFoodValues(i);
            if(ModConfig.DEV_LOGGING_ENABLED)
                ModSpiceOfLife.Log.info(i.getDisplayName() + ", " + fv.hunger + ", " + fv.saturationModifier);
        }));
        if(ModConfig.DEV_LOGGING_ENABLED)
            ModSpiceOfLife.Log.info("Done populating food list.");
    }

    public static List<ItemStack> getAllFoods() {
        return new ArrayList<>(allFoods);
    }
}
