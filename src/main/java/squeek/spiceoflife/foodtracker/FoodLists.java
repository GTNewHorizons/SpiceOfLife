package squeek.spiceoflife.foodtracker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import squeek.applecore.api.food.FoodValues;
import squeek.spiceoflife.ModConfig;
import squeek.spiceoflife.ModSpiceOfLife;
import squeek.spiceoflife.helpers.FoodHelper;

public final class FoodLists {

    private static List<ItemStack> allFoods;

    private static final List<ItemStack> greatFoods = new ArrayList<>();
    private static final List<ItemStack> goodFoods = new ArrayList<>();
    private static final List<ItemStack> normalFoods = new ArrayList<>();
    private static final List<ItemStack> lowFoods = new ArrayList<>();
    private static final List<ItemStack> poorFoods = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static void setUp() {
        if (ModConfig.DEV_LOGGING_ENABLED) ModSpiceOfLife.Log.info("Starting populating food list.");
        Stream<Item> stream = StreamSupport.stream(Item.itemRegistry.spliterator(), false);
        allFoods = stream.map(ItemStack::new)
            .filter(FoodHelper::isFood)
            .sorted(Comparator.comparing(ItemStack::getDisplayName))
            .collect(Collectors.toList());

        allFoods.forEach((i -> {
            FoodValues fv = FoodHelper.getFoodValues(i);
            String nourishmentGroup = "";

            if (ModConfig.IRON_CHEF_MODE_ENABLE) {
                float saturationModifier = fv.saturationModifier;
                if (saturationModifier >= ModConfig.IRON_CHEF_MODE_FOOD_VALUE_GREAT) {
                    greatFoods.add(i);
                    nourishmentGroup += " | Great";
                } else if (saturationModifier >= ModConfig.IRON_CHEF_MODE_FOOD_VALUE_GOOD) {
                    goodFoods.add(i);
                    nourishmentGroup = " | Good";
                } else if (saturationModifier >= ModConfig.IRON_CHEF_MODE_FOOD_VALUE_NORMAL) {
                    normalFoods.add(i);
                    nourishmentGroup = " | Normal";
                } else if (saturationModifier >= ModConfig.IRON_CHEF_MODE_FOOD_VALUE_LOW) {
                    lowFoods.add(i);
                    nourishmentGroup = " | Low";
                } else if (saturationModifier >= ModConfig.IRON_CHEF_MODE_FOOD_VALUE_POOR) {
                    poorFoods.add(i);
                    nourishmentGroup = " | Poor";
                }
            }

            if (ModConfig.DEV_LOGGING_ENABLED) {
                ModSpiceOfLife.Log
                    .info(i.getDisplayName() + ", " + fv.hunger + ", " + fv.saturationModifier + nourishmentGroup);
            }
        }));
        if (ModConfig.DEV_LOGGING_ENABLED) ModSpiceOfLife.Log.info("Done populating food lists.");
    }

    public static List<ItemStack> getAllFoods() {
        return new ArrayList<>(allFoods);
    }

    public static List<ItemStack> getGreatFoods() {
        return greatFoods;
    }

    public static List<ItemStack> getGoodFoods() {
        return goodFoods;
    }

    public static List<ItemStack> getNormalFoods() {
        return normalFoods;
    }

    public static List<ItemStack> getLowFoods() {
        return lowFoods;
    }

    public static List<ItemStack> getPoorFoods() {
        return poorFoods;
    }
}
