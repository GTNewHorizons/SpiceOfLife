package squeek.spiceoflife;

import org.junit.Test;
import squeek.applecore.api.food.FoodValues;
import squeek.spiceoflife.foodtracker.FoodEaten;
import squeek.spiceoflife.foodtracker.foodqueue.FixedSizeQueue;

import static org.junit.Assert.assertTrue;

public class TestFixedQueues {
    protected final FixedSizeQueue fixedQueue = new FixedSizeQueue(12);

    @Test
    public void testFixedSizeQueue() {
        for (int i = 1; i < 30; i++) {
            FoodEaten foodEaten = new FoodEaten();
            foodEaten.foodValues = new FoodValues(i, 0f);
            fixedQueue.add(foodEaten);
            assertTrue(fixedQueue.size() <= 12);
        }
    }

}
