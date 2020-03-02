package squeek.spiceoflife.foodtracker;

import net.minecraft.item.ItemStack;

/**
 * contains all relevant variables for current progress
 */
public final class ProgressInfo {
    public static final int FOOD_PER_MILESTONE = 10;
    public static final int HEARTS_PER_MILESTONE = 1;
    /**
     * the number of unique foods eaten
     */
    public final int foodsEaten;

    ProgressInfo(FoodHistory foodList) {
        foodsEaten = (int) foodList.getFullHistory().stream()
            .filter(eaten -> shouldCount(eaten.itemStack))
            .count();
    }

    public static boolean shouldCount(ItemStack food) {
        return true;
    }

    /**
     * the number of foods remaining until the next milestone, or a negative value if the maximum has been reached
     */
    public int foodsUntilNextMilestone() {
        return nextMilestone() - foodsEaten;
    }

    /**
     * the next milestone to reach, or a negative value if the maximum has been reached
     */
    public int nextMilestone() {
        return (milestonesAchieved() + 1) * FOOD_PER_MILESTONE;
    }

    /**
     * the number of milestones achieved, doubling as the index of the next milestone
     */
    public int milestonesAchieved() {
        return (int) Math.floor((double) foodsEaten / (double) FOOD_PER_MILESTONE);
    }
}
