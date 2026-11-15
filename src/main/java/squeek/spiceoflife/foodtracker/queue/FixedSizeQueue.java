package squeek.spiceoflife.foodtracker.queue;

import java.util.LinkedList;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import squeek.spiceoflife.compat.IByteIO;
import squeek.spiceoflife.interfaces.IPackable;
import squeek.spiceoflife.interfaces.ISaveable;

public class FixedSizeQueue extends LinkedList<ItemStack> implements IPackable, ISaveable {

    private static final long serialVersionUID = 2666900280639735575L;
    protected int limit;

    public FixedSizeQueue(int limit) {
        setMaxSize(limit);
    }

    protected void trimToMaxSize() {
        while (size() > 0 && size() > limit) {
            super.remove();
        }
    }

    @Override
    public boolean add(ItemStack o) {
        boolean added = super.add(o);
        if (added) trimToMaxSize();
        return added;
    }

    public int getMaxSize() {
        return limit;
    }

    public void setMaxSize(int limit) {
        this.limit = limit;
        trimToMaxSize();
    }

    public boolean isFull() {
        return size() == getMaxSize();
    }

    @Override
    public void writeToNBTData(NBTTagCompound data) {
        NBTTagList nbtHistory = new NBTTagList();
        for (ItemStack milestoneTarget : this) {
            if (milestoneTarget == null) continue;

            NBTTagCompound nbtFood = new NBTTagCompound();
            milestoneTarget.writeToNBT(nbtFood);
            nbtHistory.appendTag(nbtFood);
        }
        data.setTag("Items", nbtHistory);
    }

    @Override
    public void readFromNBTData(NBTTagCompound data) {
        NBTTagList nbtHistory = data.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < nbtHistory.tagCount(); i++) {
            NBTTagCompound nbtFood = nbtHistory.getCompoundTagAt(i);
            ItemStack milestoneTarget = ItemStack.loadItemStackFromNBT(nbtFood);
            if (milestoneTarget == null) continue;
            add(milestoneTarget);
        }
    }

    public boolean containsItemStack(ItemStack stack) {
        return this.stream()
            .anyMatch(target -> ItemStack.areItemStacksEqual(target, stack));
    }

    @Override
    public void pack(IByteIO data) {}

    @Override
    public void unpack(IByteIO data) {}
}
