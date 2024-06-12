package squeek.spiceoflife.foodtracker;

import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import squeek.applecore.api.food.FoodValues;
import squeek.spiceoflife.compat.IByteIO;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroup;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroupRegistry;
import squeek.spiceoflife.interfaces.IPackable;
import squeek.spiceoflife.interfaces.ISaveable;

public class FoodEaten implements IPackable, ISaveable {

    public static final FoodValues dummyFoodValues = new FoodValues(0, 0.0f);
    public FoodValues foodValues = FoodEaten.dummyFoodValues;
    public ItemStack itemStack = null;

    public FoodEaten() {}

    public FoodEaten(ItemStack food) {
        this.itemStack = food;
    }

    @Override
    public String toString() {
        return itemStack.getDisplayName();
    }

    @Override
    public int hashCode() {
        return itemStack.getItem()
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FoodEaten)) return false;
        final FoodEaten other = ((FoodEaten) obj);
        final Item item = itemStack.getItem();
        final Item otherItem = other.itemStack.getItem();
        return (Item.itemRegistry.getNameForObject(item)
            .equals(Item.itemRegistry.getNameForObject(otherItem)) && item.equals(otherItem)
            && this.itemStack.getItemDamage() == other.itemStack.getItemDamage());
    }

    public static FoodEaten loadFromNBTData(NBTTagCompound nbtFood) {
        FoodEaten foodEaten = new FoodEaten();
        foodEaten.readFromNBTData(nbtFood);
        return foodEaten;
    }

    public Set<FoodGroup> getFoodGroups() {
        return FoodGroupRegistry.getFoodGroupsForFood(itemStack);
    }

    @Override
    public void writeToNBTData(NBTTagCompound nbtFood) {
        if (itemStack != null) itemStack.writeToNBT(nbtFood);
        if (foodValues != null && foodValues.hunger != 0) nbtFood.setShort("Hunger", (short) foodValues.hunger);
        if (foodValues != null && foodValues.saturationModifier != 0)
            nbtFood.setFloat("Saturation", foodValues.saturationModifier);
    }

    @Override
    public void readFromNBTData(NBTTagCompound nbtFood) {
        itemStack = ItemStack.loadItemStackFromNBT(nbtFood);
        foodValues = new FoodValues(nbtFood.getShort("Hunger"), nbtFood.getFloat("Saturation"));
    }

    @Override
    public void pack(IByteIO data) {
        data.writeShort(foodValues != null ? foodValues.hunger : 0);
        data.writeFloat(foodValues != null ? foodValues.saturationModifier : 0);
        data.writeItemStack(itemStack);
    }

    @Override
    public void unpack(IByteIO data) {
        int hunger = data.readShort();
        float saturationModifier = data.readFloat();
        foodValues = new FoodValues(hunger, saturationModifier);
        itemStack = data.readItemStack();
    }
}
