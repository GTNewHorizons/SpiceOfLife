package squeek.spiceoflife.foodtracker;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;

import net.minecraft.item.ItemStack;

import squeek.spiceoflife.ModConfig;

/**
 * contains all relevant variables for current progress
 */
public final class ProgressInfo {

    public static final int FOOD_POINTS_PER_MILESTONE = ModConfig.FOOD_MILESTONE_VALUE;
    public static final double INCREMENT_PER_MILESTONE = ModConfig.MILESTONE_INCREMENT_VALUE;
    public static final int HEARTS_PER_MILESTONE = ModConfig.HEARTS_PER_MILESTONE_VALUE;
    /**
     * the number of food points from unique foods eaten
     */
    public final int foodsPointsEaten;

    ProgressInfo(FoodHistory foodList) {
        foodsPointsEaten = foodList.getFullHistory().stream().filter(eaten -> shouldCount(eaten.itemStack))
                .mapToInt(eaten -> eaten.foodValues.hunger).sum();
    }

    public static boolean shouldCount(ItemStack food) {
        return true;
    }

    /**
     * the number of foods remaining until the next milestone, or a negative value if the maximum has been reached
     */
    public int foodPointsUntilNextMilestone() {
        return nextMilestoneFoodPoints() - foodsPointsEaten;
    }

    /**
     * the next milestone to reach, or a negative value if the maximum has been reached
     */
    public int nextMilestoneFoodPoints() {
        int nextMilestone = (milestonesAchieved() + 1);

        // Quadratic Progression
        if (INCREMENT_PER_MILESTONE > 0) {
            double quadraticIncrement = INCREMENT_PER_MILESTONE * 0.5;
            double quadraticBase = FOOD_POINTS_PER_MILESTONE - quadraticIncrement;

            return (int) ((quadraticBase * nextMilestone) + (quadraticIncrement * nextMilestone * nextMilestone));
        }

        // Linear Progression
        return nextMilestone * FOOD_POINTS_PER_MILESTONE;
    }

    /**
     * the number of milestones achieved based on foodPointsEaten, doubling as the index of the next milestone
     */
    public int milestonesAchieved() {

        // Quadratic Progression
        if (INCREMENT_PER_MILESTONE > 0) {
            double quadraticIncrement = INCREMENT_PER_MILESTONE * 0.5;
            double quadraticBase = FOOD_POINTS_PER_MILESTONE - quadraticIncrement;

            double discriminant = sqrt(quadraticBase * quadraticBase + 4 * quadraticIncrement * foodsPointsEaten);

            double milestone1 = (-quadraticBase + discriminant) / (2 * quadraticIncrement);
            double milestone2 = (-quadraticBase - discriminant) / (2 * quadraticIncrement);

            return (int) floor(max(milestone1, milestone2));
        }

        // Linear Progression
        return (int) Math.floor((double) foodsPointsEaten / (double) FOOD_POINTS_PER_MILESTONE);
    }
}
