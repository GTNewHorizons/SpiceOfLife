package squeek.spiceoflife.foodtracker;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import net.minecraft.item.ItemStack;

/**
 * contains all relevant variables for current progress
 */
public final class ProgressInfo {

    public static final int HAUNCHES_PER_MILESTONE = 50;
    public static final double INCREMENT_RATIO = 0.02;
    public static final int HEARTS_PER_MILESTONE = 1;
    /**
     * the number of haunches from unique foods eaten
     */
    public final int foodsHaunchesEaten;

    ProgressInfo(FoodHistory foodList) {
        foodsHaunchesEaten = foodList.getFullHistory().stream().filter(eaten -> shouldCount(eaten.itemStack))
                .mapToInt(eaten -> eaten.foodValues.hunger).sum();
    }

    public static boolean shouldCount(ItemStack food) {
        return true;
    }

    /**
     * the number of foods remaining until the next milestone, or a negative value if the maximum has been reached
     */
    public int foodsUntilNextMilestone() {
        return nextMilestoneHaunches() - foodsHaunchesEaten;
    }

    /**
     * the next milestone to reach, or a negative value if the maximum has been reached
     */
    public int nextMilestoneHaunches() {
        int nextMilestone = (milestonesAchieved() + 1);

        // Quadratic Progression
        if (INCREMENT_RATIO > 0) {
            double quadraticIncrement = HAUNCHES_PER_MILESTONE * INCREMENT_RATIO;
            double quadraticBase = HAUNCHES_PER_MILESTONE - quadraticIncrement;

            return (int) ((quadraticBase * nextMilestone) + (quadraticIncrement * pow(nextMilestone, 2)));
        }

        // Linear Progression
        return nextMilestone * HAUNCHES_PER_MILESTONE;
    }

    /**
     * the number of milestones achieved based on foodsHaunchesEaten, doubling as the index of the next milestone
     */
    public int milestonesAchieved() {

        // Quadratic Progression
        if (INCREMENT_RATIO > 0) {
            double quadraticIncrement = HAUNCHES_PER_MILESTONE * INCREMENT_RATIO;
            double quadraticBase = HAUNCHES_PER_MILESTONE - quadraticIncrement;

            double discriminant = sqrt(pow(quadraticBase, 2) + 4 * quadraticIncrement * foodsHaunchesEaten);

            double milestone1 = (-quadraticBase + discriminant) / (2 * quadraticIncrement);
            double milestone2 = (-quadraticBase - discriminant) / (2 * quadraticIncrement);

            return (int) floor(max(milestone1, milestone2));
        }

        // Linear Progression
        return (int) Math.floor((double) foodsHaunchesEaten / (double) HAUNCHES_PER_MILESTONE);
    }
}
