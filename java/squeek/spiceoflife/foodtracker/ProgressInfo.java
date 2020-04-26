package squeek.spiceoflife.foodtracker;

import net.minecraft.item.ItemStack;

/**
 * contains all relevant variables for current progress
 */
public final class ProgressInfo {
    public static final int HAUNCHES_PER_MILESTONE = 50;
    public static final int HEARTS_PER_MILESTONE = 1;
    /**
     * the number of haunches from unique foods eaten
     */
    public final int foodsHaunchesEaten;

    ProgressInfo(FoodHistory foodList) {
        foodsHaunchesEaten = foodList.getFullHistory().stream()
            .filter(eaten -> shouldCount(eaten.itemStack))
            .mapToInt(eaten -> eaten.foodValues.hunger)
            .sum();
    }

    public static boolean shouldCount(ItemStack food) {
        return true;
    }

    /**
     * the number of foods remaining until the next milestone, or a negative value if the maximum has been reached
     */
    public int foodsUntilNextMilestone() {
        return nextMilestone() - foodsHaunchesEaten;
    }

    /**
     * the next milestone to reach, or a negative value if the maximum has been reached
     */
    public int nextMilestone() {
        return (milestonesAchieved() + 1) * HAUNCHES_PER_MILESTONE;
    }

    /**
     * the number of milestones achieved, doubling as the index of the next milestone
     */
    public int milestonesAchieved() {
        return (int) Math.floor((double) foodsHaunchesEaten / (double) HAUNCHES_PER_MILESTONE);
    }
}
