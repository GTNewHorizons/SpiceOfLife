package squeek.spiceoflife.network;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import squeek.spiceoflife.compat.IByteIO;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroup;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroupRegistry;

public class PacketFoodGroup extends PacketBase {

    public static int foodGroupsRecieved = 0;
    private FoodGroup foodGroup = null;
    private int totalFoodGroups = 0;

    public PacketFoodGroup() {}

    public PacketFoodGroup(FoodGroup foodGroup) {
        this.foodGroup = foodGroup;
    }

    public static void resetCount() {
        foodGroupsRecieved = 0;
    }

    @Override
    public void pack(IByteIO data) {
        if (foodGroup == null) return;

        data.writeInt(FoodGroupRegistry.numFoodGroups());
        foodGroup.pack(data);
    }

    @Override
    public void unpack(IByteIO data) {
        totalFoodGroups = data.readInt();
        foodGroup = new FoodGroup();
        foodGroup.unpack(data);
    }

    @Override
    public PacketBase processAndReply(Side side, EntityPlayer player) {
        if (++foodGroupsRecieved > totalFoodGroups) throw new RuntimeException(
                "Recieved more food groups than should exist (recieved: " + foodGroupsRecieved
                        + ", total: "
                        + totalFoodGroups
                        + ")");

        FoodGroupRegistry.addFoodGroup(foodGroup);

        if (foodGroupsRecieved == totalFoodGroups) FoodGroupRegistry.setInStone();

        return null;
    }
}
