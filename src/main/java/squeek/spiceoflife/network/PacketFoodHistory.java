package squeek.spiceoflife.network;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import squeek.spiceoflife.compat.IByteIO;
import squeek.spiceoflife.foodtracker.FoodEaten;
import squeek.spiceoflife.foodtracker.FoodHistory;

public class PacketFoodHistory extends PacketBase {

    private FoodHistory foodHistory = null;
    private boolean shouldOverwrite = false;

    public PacketFoodHistory() {}

    public PacketFoodHistory(FoodHistory foodHistory, boolean shouldOverwrite) {
        this(foodHistory);
        this.shouldOverwrite = shouldOverwrite;
    }

    public PacketFoodHistory(FoodHistory foodHistory) {
        this.foodHistory = foodHistory;
    }

    public PacketFoodHistory(FoodEaten foodEaten) {
        this.foodHistory = new FoodHistory();
        foodHistory.addFood(foodEaten);
    }

    @Override
    public void pack(IByteIO data) {
        if (foodHistory == null) return;

        data.writeBoolean(shouldOverwrite);
        foodHistory.pack(data);
    }

    @Override
    public void unpack(IByteIO data) {
        this.foodHistory = new FoodHistory();
        shouldOverwrite = data.readBoolean();
        foodHistory.unpack(data);
    }

    @Override
    public PacketBase processAndReply(Side side, EntityPlayer player) {
        FoodHistory foodHistory = FoodHistory.get(player);

        if (shouldOverwrite) {
            foodHistory.getRecentHistory().clear();
            foodHistory.ticksActive = this.foodHistory.ticksActive;
        } else {
            foodHistory.totalFoodsEatenAllTime++;
        }

        this.foodHistory.getRecentHistory().forEach(foodHistory::addFoodRecent);
        this.foodHistory.getFullHistory().forEach(foodHistory::addFoodFullHistory);

        return null;
    }
}
