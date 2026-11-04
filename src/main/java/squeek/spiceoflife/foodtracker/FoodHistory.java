package squeek.spiceoflife.foodtracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

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
    protected FoodQueue recentHistory = FoodHistory.getNewFoodQueue();
    protected FoodSet fullHistory = new FoodSet();
    protected FixedSizeQueue milestoneHistory = new FixedSizeQueue(MAX_MILESTONE_VALUE);

    public static final int MAX_MILESTONE_VALUE = ModConfig.MAX_MILESTONE_VALUE < 0 ? 255
        : ModConfig.MAX_MILESTONE_VALUE;

    public long ticksActive = 0;
    public FixedSizeQueue milestoneTargets = new FixedSizeQueue(ModConfig.COOKOFF_MODE_MENU_SIZE);
    public static final int advancementsSaturation = MAX_MILESTONE_VALUE / 5; // 5 is the number of food tiers in MC
                                                                              // {POOR,LOW,NORMAL,GOOD,GREAT}

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
        return fullHistory.contains(new FoodEaten(food));
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
        return ModConfig.USE_HUNGER_QUEUE ? new FixedHungerQueue(ModConfig.FOOD_HISTORY_LENGTH)
            : new FixedSizeQueue(ModConfig.FOOD_HISTORY_LENGTH);
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

        milestoneHistory.clear();

        milestoneTargets.clear();
        milestoneTargets.setMaxSize(ModConfig.COOKOFF_MODE_MENU_SIZE);
    }

    public void invalidateProgressInfo() {
        cachedProgressInfo = null;
    }

    public void validate() {
        validateRecentHistory();

        if (ModConfig.COOKOFF_MODE_ENABLE) validateMilestoneTargets();
    }

    public void validateRecentHistory() {
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

    public void validateMilestoneTargets() {
        List<FoodEaten> invalidFoods = new ArrayList<>();

        for (FoodEaten target : milestoneTargets) {
            if (!FoodHelper.isValidFood(target.itemStack)) {
                invalidFoods.add(target);
            }
        }
        if (invalidFoods.isEmpty()) return;

        invalidFoods.forEach(milestoneTargets::remove);
        populateMilestoneTargets();
    }

    public FoodQueue getRecentHistory() {
        return recentHistory;
    }

    public Set<FoodEaten> getFullHistory() {
        return fullHistory;
    }

    public FoodQueue getMilestoneHistory() {
        return milestoneHistory;
    }

    public FixedSizeQueue getMilestoneTargets() {
        return milestoneTargets;
    }

    /**
     * Clears the list of all milestone awarding foods in this run and prevents
     * new entries from being added until the countdown is over.
     */
    public void setChallengeCompleted() {
        milestoneTargets.clear();
        milestoneTargets.setMaxSize(0);
    }

    /**
     * Checks if the current challenge has been completed.
     */
    public boolean isChallengeCompleted() {
        return milestoneTargets.getMaxSize() == 0;
    }

    public void addFood(FoodEaten foodEaten) {
        addFood(foodEaten, true);

        if (ModConfig.COOKOFF_MODE_ENABLE) processMilestoneFood(foodEaten);
    }

    public void addFood(FoodEaten foodEaten, boolean countsTowardsAllTime) {
        if (countsTowardsAllTime) totalFoodsEatenAllTime++;

        addFoodRecent(foodEaten);
        addFoodFullHistory(foodEaten);
    }

    public void addFoodRecent(FoodEaten foodEaten) {
        recentHistory.add(foodEaten);
    }

    public boolean addMilestoneHistory(FoodEaten foodEaten) {
        if (!milestoneHistory.contains(foodEaten)) {
            milestoneHistory.add(foodEaten);
            return true;
        }
        return false;
    }

    public void addMilestoneTarget(FoodEaten foodEaten) {
        milestoneTargets.add(foodEaten);
    };

    public void addFoodFullHistory(FoodEaten foodEaten) {
        final boolean hasTriedNewFood = fullHistory.add(foodEaten);
        if (ModConfig.COOKOFF_MODE_ENABLE) return;

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

    /**
     * Attempts to award a milestone checking if the food is a valid milestone target.
     * If successful
     */
    public void processMilestoneFood(FoodEaten foodEaten) {
        if (player != null) {
            if (isMilestoneTarget(foodEaten)) {
                setChallengeCompleted();
                invalidateProgressInfo();
                MaxHealthHandler.updateFoodHPModifier(player);
                spawnParticles(this.player, "heart", 12);
                spawnParticles(this.player, "happyVillager", 12);
                player.worldObj.playSoundAtEntity(player, "random.levelup", 1.0f, 1.0f);
            }
        }
    }

    /**
     * Resets current milestone list after the allocated time is up, allowing new food milestones to
     * be defined.
     * If no milestone is achieved in the timeframe, the last awarded milestone is removed.
     */
    public void milestoneCountdown() {
        if (ticksActive > 1) ticksActive--;
        else {
            FoodQueue currentMilestones = getMilestoneHistory();

            if (!currentMilestones.isEmpty()) {
                boolean hasFailedChallenge = Collections.disjoint(currentMilestones, milestoneTargets);

                if (hasFailedChallenge) {
                    milestoneHistory.removeLast();
                    invalidateProgressInfo();

                    spawnParticles(this.player, "angryVillager", 6);
                    player.worldObj.playSoundAtEntity(player, "game.player.hurt.fall.small", 1.0f, 1.0f);
                } else spawnParticles(this.player, "happyVillager", 6); // This notification needs to be reinvented into
                                                                        // something clearer and less ambiguous
            }

            ticksActive = 24000L * ModConfig.COOKOFF_MODE_COOLDOWN;
            milestoneTargets.setMaxSize(ModConfig.COOKOFF_MODE_MENU_SIZE);
        }
    }

    /**
     * Generates food targets that award milestones based on saturation tier for the current milestone challenge should
     * it
     * not have been started this run.
     * The requirement saturation tier is increased every 1/5 max milestones or when the current food tier used is
     * exhausted.
     * The desired effect is as players progress in milestones, new ones require progressively higher quality foods.
     */
    public void populateMilestoneTargets() {
        if (!isChallengeCompleted() && !milestoneTargets.isFull()) {

            Random random = new Random();
            int currentMilestones = getProgressInfo().milestonesAchieved();
            int startingTier = ModConfig.COOKOFF_MODE_TIER;

            if (advancementsSaturation != 0) startingTier += currentMilestones / advancementsSaturation;

            for (int tier = startingTier; tier < 5; tier++) {
                int remainingSlots = ModConfig.COOKOFF_MODE_MENU_SIZE - milestoneTargets.size();
                if (remainingSlots < 1) break;

                List<ItemStack> tierFoods = getTieredFoodList(tier);

                getMilestoneHistory().stream()
                    .map(food -> food.itemStack)
                    .filter(Objects::nonNull)
                    .forEach(tierFoods::remove);

                Collections.shuffle(tierFoods, random);

                for (ItemStack food : tierFoods.subList(0, Math.min(remainingSlots, tierFoods.size()))) {
                    addMilestoneTarget(new FoodEaten(food));
                }
            }
        }
    }

    /**
     * Provides the list of foods in the provided tier of saturation value (poor, low, normal, good, great)
     */
    public List<ItemStack> getTieredFoodList(int index) {
        switch (index) {
            case 0:
                return FoodLists.getPoorFoods();
            case 1:
                return FoodLists.getLowFoods();
            case 2:
                return FoodLists.getNormalFoods();
            case 3:
                return FoodLists.getGoodFoods();
            default:
                return FoodLists.getGreatFoods();
        }
    }

    /**
     * Checks if the given food is contained in the current list of foods that award a milestone.
     */
    public boolean isMilestoneTarget(FoodEaten foodEaten) {
        if (milestoneTargets.contains(foodEaten) && !milestoneHistory.contains(foodEaten)) {
            return awardMilestone(foodEaten);
        }
        return false;
    }

    /**
     * Adds the food to the achieved milestone list.
     * If the milestone cap has been achieved, a random milestone of the current tier is removed or
     * the latest one if a common food in the current tier cannot be found.
     */
    public boolean awardMilestone(FoodEaten foodEaten) {
        if (milestoneHistory.isFull()) {
            int currentFoodTier = ModConfig.COOKOFF_MODE_TIER;

            if (advancementsSaturation != 0)
                currentFoodTier += getProgressInfo().milestonesAchieved() / advancementsSaturation;

            List<ItemStack> tierFoods = getTieredFoodList(currentFoodTier);
            List<FoodEaten> tierMilestoneHistory = getMilestoneHistory().stream()
                .filter(food -> food.itemStack != null)
                .filter(food -> tierFoods.contains(food.itemStack))
                .collect(Collectors.toList());

            if (!tierMilestoneHistory.isEmpty()) {
                FoodEaten toRemove = tierMilestoneHistory.get(new Random().nextInt(tierMilestoneHistory.size()));

                milestoneHistory.remove(toRemove);
            } else milestoneHistory.removeLast();
        }

        return addMilestoneHistory(foodEaten);
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
    public void pack(IByteIO data) {
        data.writeLong(ticksActive);
        data.writeShort(getRecentHistory().size());
        for (FoodEaten foodEaten : getRecentHistory()) {
            foodEaten.pack(data);
        }

        data.writeShort(getFullHistory().size());
        getFullHistory().forEach(f -> f.pack(data));

        data.writeShort(getMilestoneHistory().size());
        getMilestoneHistory().forEach(f -> f.pack(data));

        data.writeShort(getMilestoneTargets().size());
        for (FoodEaten foodEaten : getMilestoneTargets()) {
            foodEaten.pack(data);
        }
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

        short milestoneHistorySize = data.readShort();
        for (int i = 0; i < milestoneHistorySize; i++) {
            FoodEaten foodEaten = new FoodEaten();
            foodEaten.unpack(data);
            addMilestoneHistory(foodEaten);
        }

        short currentMilestoneSize = data.readShort();
        for (int i = 0; i < currentMilestoneSize; i++) {
            FoodEaten target = new FoodEaten();
            target.unpack(data);

            if (milestoneTargets.size() < ModConfig.COOKOFF_MODE_MENU_SIZE) milestoneTargets.add(target);
        }
    }

    @Override
    // null compound parameter means save persistent data only
    public void writeToNBTData(NBTTagCompound data) {
        NBTTagCompound rootPersistentCompound = player.getEntityData()
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagCompound nonPersistentCompound = new NBTTagCompound();
        NBTTagCompound persistentCompound = new NBTTagCompound();

        if (!recentHistory.isEmpty()) {
            if (data != null || ModConfig.FOOD_HISTORY_PERSISTS_THROUGH_DEATH) {
                NBTTagCompound nbtHistory = new NBTTagCompound();

                recentHistory.writeToNBTData(nbtHistory);

                if (ModConfig.FOOD_HISTORY_PERSISTS_THROUGH_DEATH) persistentCompound.setTag("History", nbtHistory);
                else nonPersistentCompound.setTag("History", nbtHistory);
            }
        }
        if (!fullHistory.isEmpty()) {
            if (data != null || ModConfig.FOOD_MILESTONES_PERSISTS_THROUGH_DEATH) {
                NBTTagCompound nbtFullHistory = new NBTTagCompound();

                fullHistory.writeToNBTData(nbtFullHistory);

                if (ModConfig.FOOD_MILESTONES_PERSISTS_THROUGH_DEATH)
                    persistentCompound.setTag("FullHistory", nbtFullHistory);
                else nonPersistentCompound.setTag("FullHistory", nbtFullHistory);
            }
        }
        if (!milestoneTargets.isEmpty()) {
            if (data != null || ModConfig.COOKOFF_MODE_MILESTONES_PERSISTS_THROUGH_DEATH) {
                NBTTagCompound nbtMilestoneTargets = new NBTTagCompound();

                milestoneTargets.writeToNBTData(nbtMilestoneTargets);

                if (ModConfig.COOKOFF_MODE_MILESTONES_PERSISTS_THROUGH_DEATH)
                    persistentCompound.setTag("MilestoneTargets", nbtMilestoneTargets);
                else nonPersistentCompound.setTag("MilestoneTargets", nbtMilestoneTargets);
            }
        }
        if (!milestoneHistory.isEmpty()) {
            if (data != null || ModConfig.COOKOFF_MODE_MILESTONES_PERSISTS_THROUGH_DEATH) {
                NBTTagCompound nbtMilestoneHistory = new NBTTagCompound();

                milestoneHistory.writeToNBTData(nbtMilestoneHistory);

                if (ModConfig.COOKOFF_MODE_MILESTONES_PERSISTS_THROUGH_DEATH)
                    persistentCompound.setTag("MilestoneHistory", nbtMilestoneHistory);
                else nonPersistentCompound.setTag("MilestoneHistory", nbtMilestoneHistory);
            }
        }
        if (totalFoodsEatenAllTime > 0) {
            persistentCompound.setInteger("Total", totalFoodsEatenAllTime);
        }
        if (wasGivenFoodJournal) {
            persistentCompound.setBoolean("FoodJournal", true);
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

            NBTTagCompound nbtMilestoneTargets = ModConfig.FOOD_MILESTONES_PERSISTS_THROUGH_DEATH
                ? persistentCompound.getCompoundTag("MilestoneTargets")
                : nonPersistentCompound.getCompoundTag("MilestoneTargets");

            NBTTagCompound nbtMilestoneHistory = ModConfig.FOOD_MILESTONES_PERSISTS_THROUGH_DEATH
                ? persistentCompound.getCompoundTag("MilestoneHistory")
                : nonPersistentCompound.getCompoundTag("MilestoneHistory");

            recentHistory.readFromNBTData(nbtHistory);
            fullHistory.readFromNBTData(nbtFullHistory);
            milestoneTargets.readFromNBTData(nbtMilestoneTargets);
            milestoneHistory.readFromNBTData(nbtMilestoneHistory);

            totalFoodsEatenAllTime = persistentCompound.getInteger("Total");
            wasGivenFoodJournal = persistentCompound.getBoolean("FoodJournal");
            ticksActive = persistentCompound.getLong("Ticks");

            milestoneTargets.setMaxSize(milestoneTargets.size());
        }

    }
}
