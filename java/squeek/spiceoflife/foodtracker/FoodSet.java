package squeek.spiceoflife.foodtracker;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import squeek.spiceoflife.interfaces.ISaveable;

import java.util.HashSet;

public class FoodSet extends HashSet<FoodEaten> implements ISaveable {

    @Override
    public void writeToNBTData(NBTTagCompound data) {
        NBTTagList nbtHistory = new NBTTagList();
        for (FoodEaten foodEaten : this) {
            NBTTagCompound nbtFood = new NBTTagCompound();
            foodEaten.writeToNBTData(nbtFood);
            nbtHistory.appendTag(nbtFood);
        }
        data.setTag("Foods", nbtHistory);
    }

    @Override
    public void readFromNBTData(NBTTagCompound data) {
        NBTTagList nbtHistory = data.getTagList("Foods", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < nbtHistory.tagCount(); i++) {
            NBTTagCompound nbtFood = nbtHistory.getCompoundTagAt(i);
            FoodEaten foodEaten = FoodEaten.loadFromNBTData(nbtFood);
            add(foodEaten);
        }
    }
}
