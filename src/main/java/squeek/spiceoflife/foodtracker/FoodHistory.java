package squeek.spiceoflife.foodtracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.IExtendedEntityProperties;

import squeek.applecore.api.food.FoodValues;
import squeek.spiceoflife.ModConfig;
import squeek.spiceoflife.ModInfo;
import squeek.spiceoflife.compat.IByteIO;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroup;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroupRegistry;
import squeek.spiceoflife.foodtracker.foodqueue.FixedHungerQueue;
import squeek.spiceoflife.foodtracker.foodqueue.FixedSizeQueue;
import squeek.spiceoflife.foodtracker.foodqueue.FixedTimeQueue;
import squeek.spiceoflife.foodtracker.foodqueue.FoodQueue;
import squeek.spiceoflife.helpers.FoodHelper;
import squeek.spiceoflife.helpers.MiscHelper;
import squeek.spiceoflife.interfaces.IPackable;
import squeek.spiceoflife.interfaces.ISaveable;

public class FoodHistory implements IExtendedEntityProperties, ISaveable, IPackable {

    public static final String TAG_KEY = ModInfo.MODID + "History";
    public final EntityPlayer player;
    public int totalFoodsEatenAllTime = 0;
    public boolean wasGivenFoodJournal = false;
    public long ticksActive = 0;
    protected FoodQueue recentHistory = FoodHistory.getNewFoodQueue();
    protected FoodSet fullHistory = new FoodSet();

    @Nullable
    private ProgressInfo cachedProgressInfo;

    public FoodHistory() {
        this(null);
    }

    public FoodHistory(EntityPlayer player) {
        this.player = player;
        if (player != null) player.registerExtendedProperties(FoodHistory.TAG_KEY, this);
    }

    public static FoodHistory get(EntityPlayer player) {
        FoodHistory foodHistory = (FoodHistory) player.getExtendedProperties(TAG_KEY);
        if (foodHistory == null) {
            foodHistory = new FoodHistory(player);
        }
        return foodHistory;
    }

    public boolean hasEverEaten(ItemStack food) {
        return fullHistory.contains(new FoodEaten(food, player));
    }

    public ProgressInfo getProgressInfo() {
        if (cachedProgressInfo == null) {
            cachedProgressInfo = new ProgressInfo(this);
        }
        return cachedProgressInfo;
    }

    public void onHistoryTypeChanged() {
        FoodQueue oldHistory = recentHistory;
        recentHistory = FoodHistory.getNewFoodQueue();
        recentHistory.addAll(oldHistory);
    }

    public int getFoodCountForFoodGroup(ItemStack food, FoodGroup foodGroup) {
        int count = 0;

        for (FoodEaten foodEaten : recentHistory) {
            if (foodEaten.itemStack == null) continue;

            if (food.isItemEqual(foodEaten.itemStack) || foodEaten.getFoodGroups()
                .contains(foodGroup)) {
                count += 1;
            }
        }
        return count;
    }

    public static FoodQueue getNewFoodQueue() {
        if (ModConfig.USE_HUNGER_QUEUE) return new FixedHungerQueue(ModConfig.FOOD_HISTORY_LENGTH);
        else if (ModConfig.USE_TIME_QUEUE)
            return new FixedTimeQueue((long) ModConfig.FOOD_HISTORY_LENGTH * MiscHelper.TICKS_PER_DAY);
        else return new FixedSizeQueue(ModConfig.FOOD_HISTORY_LENGTH);
    }

    public void deltaTicksActive(long delta) {
        this.ticksActive += delta;
    }

    public int getFoodCountIgnoringFoodGroups(ItemStack food) {
        return getFoodCountForFoodGroup(food, null);
    }

    public boolean containsFoodOrItsFoodGroups(ItemStack food) {
        Set<FoodGroup> foodGroups = FoodGroupRegistry.getFoodGroupsForFood(food);
        for (FoodEaten foodEaten : recentHistory) {
            if (foodEaten.itemStack == null) continue;

            if (food.isItemEqual(foodEaten.itemStack)
                || MiscHelper.collectionsOverlap(foodGroups, foodEaten.getFoodGroups())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Note: the returned FoodValues is not a standard FoodValues. The saturationModifier is set to the total, not to a
     * modifier
     */
    public FoodValues getTotalFoodValuesForFoodGroup(ItemStack food, FoodGroup foodGroup) {
        int totalHunger = 0;
        float totalSaturation = 0f;

        for (FoodEaten foodEaten : recentHistory) {
            if (foodEaten.itemStack == null) continue;

            if (food.isItemEqual(foodEaten.itemStack) || foodEaten.getFoodGroups()
                .contains(foodGroup)) {
                totalHunger += foodEaten.foodValues.hunger;
                totalSaturation += foodEaten.foodValues.getSaturationIncrement();
            }
        }

        if (totalHunger == 0) return new FoodValues(0, 0f);
        else return new FoodValues(totalHunger, totalSaturation);
    }

    /**
     * See {@link #getTotalFoodValuesForFoodGroup}
     */
    public FoodValues getTotalFoodValuesIgnoringFoodGroups(ItemStack food) {
        return getTotalFoodValuesForFoodGroup(food, null);
    }

    public int getHistoryLength() {
        return ModConfig.USE_HUNGER_QUEUE ? ((FixedHungerQueue) recentHistory).hunger() : recentHistory.size();
    }

    public FoodEaten getLastEatenFood() {
        return recentHistory.peekLast();
    }

    public Set<FoodGroup> getDistinctFoodGroups() {
        Set<FoodGroup> distinctFoodGroups = new HashSet<>();
        for (FoodEaten foodEaten : recentHistory) {
            if (foodEaten.itemStack == null) continue;

            distinctFoodGroups.addAll(foodEaten.getFoodGroups());
        }
        return distinctFoodGroups;
    }

    public void reset() {
        recentHistory.clear();
        fullHistory.clear();
        invalidateProgressInfo();
        totalFoodsEatenAllTime = 0;
        wasGivenFoodJournal = false;
        ticksActive = 0;
    }

    public void invalidateProgressInfo() {
        cachedProgressInfo = null;
    }

    public void validate() {
        List<FoodEaten> invalidFoods = new ArrayList<>();
        // TODO(SoL): Check full history?
        for (FoodEaten foodEaten : recentHistory) {
            if (!FoodHelper.isValidFood(foodEaten.itemStack)) {
                invalidFoods.add(foodEaten);
            }
        }
        recentHistory.removeAll(invalidFoods);
        totalFoodsEatenAllTime -= invalidFoods.size();
    }

    @Override
    public void pack(IByteIO data) {
        data.writeLong(ticksActive);
        data.writeShort(getRecentHistory().size());
        for (FoodEaten foodEaten : getRecentHistory()) {
            foodEaten.pack(data);
        }

        data.writeShort(getFullHistory().size());
        getFullHistory().forEach(f -> f.pack(data));
    }

    public FoodQueue getRecentHistory() {
        return recentHistory;
    }

    public Set<FoodEaten> getFullHistory() {
        return fullHistory;
    }

    @Override
    public void unpack(IByteIO data) {
        ticksActive = data.readLong();
        short historySize = data.readShort();

        for (int i = 0; i < historySize; i++) {
            FoodEaten foodEaten = new FoodEaten();
            foodEaten.unpack(data);
            addFood(foodEaten);
        }

        short fullHistorySize = data.readShort();

        for (int i = 0; i < fullHistorySize; i++) {
            FoodEaten foodEaten = new FoodEaten();
            foodEaten.unpack(data);
            addFoodFullHistory(foodEaten);
        }
    }

    public void addFood(FoodEaten foodEaten) {
        addFood(foodEaten, true);
    }

    public void addFoodFullHistory(FoodEaten foodEaten) {
        final boolean hasTriedNewFood = fullHistory.add(foodEaten);
        if (player != null) {
            if (hasTriedNewFood) {
                invalidateProgressInfo();
                boolean newMilestoneReached = MaxHealthHandler.updateFoodHPModifier(player);
                if (newMilestoneReached) {
                    spawnParticles(this.player, "heart", 12);
                    spawnParticles(this.player, "happyVillager", 12);
                    player.worldObj.playSoundAtEntity(player, "random.levelup", 1.0f, 1.0f);
                } else {
                    spawnParticles(this.player, "heart", 12);
                }
            }
        }
    }

    public void addFood(FoodEaten foodEaten, boolean countsTowardsAllTime) {
        if (countsTowardsAllTime) totalFoodsEatenAllTime++;

        addFoodRecent(foodEaten);
        addFoodFullHistory(foodEaten);
    }

    public void addFoodRecent(FoodEaten foodEaten) {
        recentHistory.add(foodEaten);
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        writeToNBTData(compound);
    }

    private static void spawnParticles(EntityPlayer player, String type, int count) {
        if (player.worldObj.isRemote) return;
        if (!(player.worldObj instanceof WorldServer)) return;

        final WorldServer world = (WorldServer) player.worldObj;

        // this function sends a packet to the client
        world.func_147487_a(
            type,
            (float) player.posX,
            (float) player.posY + 2,
            (float) player.posZ,
            count,
            1F,
            1F,
            1F,
            0.20000000298023224D);
    }

    @Override
    // null compound parameter means save persistent data only
    public void writeToNBTData(NBTTagCompound data) {
        NBTTagCompound rootPersistentCompound = player.getEntityData()
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagCompound nonPersistentCompound = new NBTTagCompound();
        NBTTagCompound persistentCompound = new NBTTagCompound();

        if (recentHistory.size() > 0) {
            if (data != null || ModConfig.FOOD_HISTORY_PERSISTS_THROUGH_DEATH) {
                NBTTagCompound nbtHistory = new NBTTagCompound();

                recentHistory.writeToNBTData(nbtHistory);

                if (ModConfig.FOOD_HISTORY_PERSISTS_THROUGH_DEATH) persistentCompound.setTag("History", nbtHistory);
                else nonPersistentCompound.setTag("History", nbtHistory);
            }
        }
        if (fullHistory.size() > 0) {
            if (data != null || ModConfig.FOOD_MILESTONES_PERSISTS_THROUGH_DEATH) {
                NBTTagCompound nbtFullHistory = new NBTTagCompound();

                fullHistory.writeToNBTData(nbtFullHistory);

                if (ModConfig.FOOD_MILESTONES_PERSISTS_THROUGH_DEATH)
                    persistentCompound.setTag("FullHistory", nbtFullHistory);
                else nonPersistentCompound.setTag("FullHistory", nbtFullHistory);
            }
        }
        if (totalFoodsEatenAllTime > 0) {
            persistentCompound.setInteger("Total", totalFoodsEatenAllTime);
        }
        if (wasGivenFoodJournal) {
            persistentCompound.setBoolean("FoodJournal", wasGivenFoodJournal);
        }
        if (ticksActive > 0) {
            persistentCompound.setLong("Ticks", ticksActive);
        }

        if (data != null && !nonPersistentCompound.hasNoTags()) data.setTag(TAG_KEY, nonPersistentCompound);

        if (!persistentCompound.hasNoTags()) rootPersistentCompound.setTag(TAG_KEY, persistentCompound);

        if (!player.getEntityData()
            .hasKey(EntityPlayer.PERSISTED_NBT_TAG))
            player.getEntityData()
                .setTag(EntityPlayer.PERSISTED_NBT_TAG, rootPersistentCompound);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        readFromNBTData(compound);
    }

    @Override
    public void init(Entity entity, World world) {}

    @Override
    // null compound parameter means load persistent data only
    public void readFromNBTData(NBTTagCompound data) {
        NBTTagCompound rootPersistentCompound = player.getEntityData()
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagCompound persistentCompound = rootPersistentCompound.getCompoundTag(TAG_KEY);

        if ((data != null && data.hasKey(TAG_KEY)) || rootPersistentCompound.hasKey(TAG_KEY)) {
            NBTTagCompound nonPersistentCompound = data != null ? data.getCompoundTag(TAG_KEY) : new NBTTagCompound();

            NBTTagCompound nbtHistory = ModConfig.FOOD_HISTORY_PERSISTS_THROUGH_DEATH
                ? persistentCompound.getCompoundTag("History")
                : nonPersistentCompound.getCompoundTag("History");

            NBTTagCompound nbtFullHistory = ModConfig.FOOD_MILESTONES_PERSISTS_THROUGH_DEATH
                ? persistentCompound.getCompoundTag("FullHistory")
                : nonPersistentCompound.getCompoundTag("FullHistory");

            fullHistory.readFromNBTData(nbtHistory);
            fullHistory.readFromNBTData(nbtFullHistory);

            recentHistory.readFromNBTData(nbtHistory);

            totalFoodsEatenAllTime = persistentCompound.getInteger("Total");
            wasGivenFoodJournal = persistentCompound.getBoolean("FoodJournal");
            ticksActive = persistentCompound.getLong("Ticks");
        }

    }
}
