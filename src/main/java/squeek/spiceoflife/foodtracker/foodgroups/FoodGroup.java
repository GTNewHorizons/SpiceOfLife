package squeek.spiceoflife.foodtracker.foodgroups;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import com.google.gson.annotations.SerializedName;

import cpw.mods.fml.common.registry.GameRegistry;
import squeek.spiceoflife.compat.IByteIO;
import squeek.spiceoflife.foodtracker.FoodModifier;
import squeek.spiceoflife.helpers.OreDictionaryHelper;
import squeek.spiceoflife.interfaces.IPackable;

public class FoodGroup implements IPackable {

    static final transient EnumChatFormatting DEFAULT_FORMATTING = EnumChatFormatting.GRAY;
    public transient String identifier;
    public transient EnumChatFormatting formatting;
    public boolean enabled = true;
    public String name = null;
    public boolean blacklist = false;
    public String formula = null;
    public String color = DEFAULT_FORMATTING.getFriendlyName();

    @SerializedName("food")
    public Map<String, List<String>> foodStringsByType;

    @SerializedName("exclude")
    public Map<String, List<String>> excludedFoodStringsByType;

    private transient List<FoodGroupMember> included = new ArrayList<>();
    private transient List<FoodGroupMember> excluded = new ArrayList<>();
    private transient Set<Integer> matchingItemHashes = new HashSet<>();
    private transient Set<Integer> excludedItemHashes = new HashSet<>();
    private transient FoodModifier foodModifier;
    private boolean hidden = false;

    public FoodGroup() {}

    public FoodGroup(String identifier, String name) {
        this.identifier = identifier;
        this.name = name;
    }

    public void initFromConfig() {
        if (foodStringsByType == null) throw new RuntimeException(
                toString() + " food group (" + identifier + ".json) missing required \"food\" property");

        formatting = EnumChatFormatting.getValueByName(color);
        if (formatting == null) formatting = DEFAULT_FORMATTING;

        List<String> oredictStrings = foodStringsByType.get("oredict");
        if (oredictStrings != null) {
            for (String oredictString : oredictStrings) {
                addFood(oredictString);
            }
        }

        List<String> itemStrings = foodStringsByType.get("items");
        if (itemStrings != null) {
            for (String itemString : itemStrings) {
                ItemStack item = getItemFromString(itemString);
                if (item != null) addFood(item);
            }
        }

        if (excludedFoodStringsByType != null) {
            List<String> excludedOredictStrings = excludedFoodStringsByType.get("oredict");
            if (excludedOredictStrings != null) {
                for (String oredictString : excludedOredictStrings) {
                    excludeFood(oredictString);
                }
            }

            List<String> excludedItemStrings = excludedFoodStringsByType.get("items");
            if (excludedItemStrings != null) {
                for (String itemString : excludedItemStrings) {
                    ItemStack item = getItemFromString(itemString);
                    if (item != null) excludeFood(item);
                }
            }
        }
    }

    public void addFood(String oredictName) {
        addFood(new FoodGroupMember(oredictName));
    }

    public ItemStack getItemFromString(String itemString) {
        String[] itemStringParts = itemString.split(":");
        if (itemStringParts.length > 1) {
            Item item = GameRegistry.findItem(itemStringParts[0], itemStringParts[1]);
            if (item != null) {
                boolean exactMetadata = itemStringParts.length > 2 && !itemStringParts[2].equals("*");
                int metadata = exactMetadata ? Integer.parseInt(itemStringParts[2]) : OreDictionary.WILDCARD_VALUE;
                return new ItemStack(item, 1, metadata);
            }
        }
        return null;
    }

    public void addFood(ItemStack itemStack) {
        addFood(new FoodGroupMember(itemStack));
    }

    public void excludeFood(String oredictName) {
        excludeFood(new FoodGroupMember(oredictName));
    }

    public void excludeFood(ItemStack itemStack) {
        excludeFood(new FoodGroupMember(itemStack));
    }

    public String getLocalizedName() {
        if (name != null) return StatCollector.translateToLocal(name);
        else return StatCollector.translateToLocal("spiceoflife.foodgroup." + identifier);
    }

    public void addFood(FoodGroupMember foodMember) {
        included.add(foodMember);
    }

    public void excludeFood(FoodGroupMember foodMember) {
        excluded.add(foodMember);
    }

    public void init() {
        matchingItemHashes.clear();
        for (FoodGroupMember foodMember : included) {
            List<ItemStack> matchingItems = foodMember.getBaseItemList();
            for (ItemStack matchingItem : matchingItems) {
                matchingItemHashes.add(OreDictionaryHelper.getItemStackHash(matchingItem));
            }
        }
        for (FoodGroupMember foodMember : excluded) {
            List<ItemStack> matchingItems = foodMember.getBaseItemList();
            for (ItemStack matchingItem : matchingItems) {
                excludedItemHashes.add(OreDictionaryHelper.getItemStackHash(matchingItem));
            }
        }
        foodModifier = formula != null ? new FoodModifier(formula) : FoodModifier.GLOBAL;
    }

    public boolean isFoodIncluded(ItemStack food) {
        return !isFoodExcluded(food) && matchingItemHashes.contains(OreDictionaryHelper.getItemStackHash(food))
                || matchingItemHashes.contains(OreDictionaryHelper.getWildCardItemStackHash(food));
    }

    public boolean isFoodExcluded(ItemStack food) {
        return excludedItemHashes.contains(OreDictionaryHelper.getItemStackHash(food))
                || excludedItemHashes.contains(OreDictionaryHelper.getWildCardItemStackHash(food));
    }

    public Set<Integer> getMatchingItemStackHashes() {
        return matchingItemHashes;
    }

    public String getFormattedName() {
        return formatString(getLocalizedName());
    }

    public String formatString(String string) {
        return formatting + string;
    }

    public FoodModifier getFoodModifier() {
        return foodModifier;
    }

    public boolean hidden() {
        return hidden || blacklist;
    }

    @Override
    public void pack(IByteIO data) {
        data.writeUTF(identifier);
        data.writeUTF(name != null ? name : "");
        data.writeUTF(formula != null ? formula : "");
        data.writeBoolean(blacklist);
        data.writeBoolean(hidden);
        data.writeByte(formatting != null ? formatting.ordinal() : DEFAULT_FORMATTING.ordinal());
        data.writeShort(included.size());

        for (FoodGroupMember foodMember : included) {
            foodMember.pack(data);
        }

        data.writeShort(excluded.size());

        for (FoodGroupMember foodMember : excluded) {
            foodMember.pack(data);
        }
    }

    @Override
    public void unpack(IByteIO data) {
        identifier = data.readUTF();
        name = data.readUTF();
        name = !name.equals("") ? name : null;
        formula = data.readUTF();
        formula = !formula.equals("") ? formula : null;
        blacklist = data.readBoolean();
        hidden = data.readBoolean();
        formatting = EnumChatFormatting.values()[data.readByte()];
        int size = data.readShort();

        for (int i = 0; i < size; i++) {
            FoodGroupMember foodMember = new FoodGroupMember();
            foodMember.unpack(data);
            addFood(foodMember);
        }

        size = data.readShort();

        for (int i = 0; i < size; i++) {
            FoodGroupMember foodMember = new FoodGroupMember();
            foodMember.unpack(data);
            excludeFood(foodMember);
        }
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) return true;
        if (obj instanceof FoodGroup) return ((FoodGroup) obj).identifier.equals(identifier);

        return false;
    }

    @Override
    public String toString() {
        return getLocalizedName();
    }
}
