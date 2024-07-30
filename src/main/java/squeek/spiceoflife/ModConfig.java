package squeek.spiceoflife;

import java.io.File;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import squeek.spiceoflife.compat.IByteIO;
import squeek.spiceoflife.compat.PacketDispatcher;
import squeek.spiceoflife.foodtracker.FoodHistory;
import squeek.spiceoflife.foodtracker.FoodModifier;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroupConfig;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroupRegistry;
import squeek.spiceoflife.interfaces.IPackable;
import squeek.spiceoflife.interfaces.IPacketProcessor;
import squeek.spiceoflife.network.PacketBase;
import squeek.spiceoflife.network.PacketConfigSync;

public class ModConfig implements IPackable, IPacketProcessor {

    public static final ModConfig instance = new ModConfig();
    public static final String ITEM_FOOD_JOURNAL_NAME = "bookfoodjournal";
    public static final String ITEM_LUNCH_BOX_NAME = "lunchbox";
    public static final String ITEM_LUNCH_BAG_NAME = "lunchbag";
    private static final String COMMENT_SERVER_SIDE_OPTIONS = "These config settings are server-side only\n"
        + "Their values will get synced to all clients on the server";
    /*
     * MAIN
     */
    private static final String CATEGORY_MAIN = " main ";
    private static final String CATEGORY_MAIN_COMMENT = COMMENT_SERVER_SIDE_OPTIONS;
    private static final String FOOD_MODIFIER_ENABLED_NAME = "food.modifier.enabled";
    private static final boolean FOOD_MODIFIER_ENABLED_DEFAULT = true;
    private static final String FOOD_MODIFIER_ENABLED_COMMENT = "If false, disables the entire diminishing returns part of the mod";
    /*
     * CARROT
     */
    private static final String CATEGORY_CARROT = " carrot module ";
    private static final String CATEGORY_CARROT_COMMENT = COMMENT_SERVER_SIDE_OPTIONS;
    private static final String EXTRA_HEARTS_NAME = "hearts.milestones.enable";
    private static final String EXTRA_HEARTS_COMMENT = "Enable extra hearts module";
    private static final boolean EXTRA_HEARTS_DEFAULT = true;
    public static boolean EXTRA_HEARTS_ENABLE = ModConfig.EXTRA_HEARTS_DEFAULT;
    private static final String FOOD_MILESTONE_NAME = "hearts.milestones.base";
    private static final String FOOD_MILESTONE_COMMENT = "Base requirement for each Food Milestones (in food points) to award extra hearts";
    private static final int FOOD_MILESTONE_DEFAULT = 50;
    public static int FOOD_MILESTONE_VALUE = ModConfig.FOOD_MILESTONE_DEFAULT;
    private static final String MILESTONE_INCREMENT_NAME = "hearts.milestones.increment";
    private static final String MILESTONE_INCREMENT_COMMENT = "The increase per Milestone to the base Food Milestones value (in food points)";
    private static final int MILESTONE_INCREMENT_DEFAULT = 0;
    public static int MILESTONE_INCREMENT_VALUE = ModConfig.MILESTONE_INCREMENT_DEFAULT;
    private static final String HEARTS_PER_MILESTONE_NAME = "hearts.milestones.reward";
    private static final String HEARTS_PER_MILESTONE_COMMENT = "Extra hearts awarded per milestone achieved";
    private static final int HEARTS_PER_MILESTONE_DEFAULT = 1;
    public static int HEARTS_PER_MILESTONE_VALUE = ModConfig.HEARTS_PER_MILESTONE_DEFAULT;

    private static final String MAX_MILESTONE_NAME = "hearts.milestones.maximum";
    private static final String MAX_MILESTONE_COMMENT = "The maximum allowed number of Milestones a player can achieve";
    private static final int MAX_MILESTONE_DEFAULT = -1;
    public static int MAX_MILESTONE_VALUE = ModConfig.MAX_MILESTONE_DEFAULT;
    private static final String FOOD_MILESTONES_PERSISTS_THROUGH_DEATH_NAME = "hearts.milestones.persists.through.death";
    private static final String FOOD_MILESTONES_PERSISTS_THROUGH_DEATH_COMMENT = "If true, milestone progress will not get reset after every death";
    private static final boolean FOOD_MILESTONES_PERSISTS_THROUGH_DEATH_DEFAULT = true;
    public static boolean FOOD_MILESTONES_PERSISTS_THROUGH_DEATH = ModConfig.FOOD_HISTORY_PERSISTS_THROUGH_DEATH_DEFAULT;
    /*
     * DEV
     */
    private static final String CATEGORY_DEV = "dev";
    private static final String CATEGORY_DEV_COMMENT = "These config settings are only for developers";
    private static final String DEV_LOGGING_ENABLED_NAME = "dev.logging.enabled";
    private static final boolean DEV_LOGGING_ENABLED_DEFAULT = false;
    private static final String DEV_LOGGING_ENABLED_COMMENT = "If true, enables extra logging to help modpack developers";
    public static boolean DEV_LOGGING_ENABLED = ModConfig.DEV_LOGGING_ENABLED_DEFAULT;
    /*
     * SERVER
     */
    private static final String CATEGORY_SERVER = "server";
    private static final String CATEGORY_SERVER_COMMENT = COMMENT_SERVER_SIDE_OPTIONS;
    private static final String FOOD_HISTORY_LENGTH_NAME = "food.history.length";
    private static final int FOOD_HISTORY_LENGTH_DEFAULT = 12;
    private static final String FOOD_HISTORY_LENGTH_COMMENT = "The maximum amount of eaten foods stored in the history at a time";
    private static final String FOOD_HISTORY_PERSISTS_THROUGH_DEATH_NAME = "food.history.persists.through.death";
    private static final boolean FOOD_HISTORY_PERSISTS_THROUGH_DEATH_DEFAULT = false;
    private static final String FOOD_HISTORY_PERSISTS_THROUGH_DEATH_COMMENT = "If true, food history will not get reset after every death";
    private static final String FOOD_EATEN_THRESHOLD_NAME = "new.player.food.eaten.threshold";
    private static final String FOOD_EATEN_THRESHOLD_COMMENT = "The number of times a new player (by World) needs to eat before this mod has any effect";
    private static final String USE_FOOD_GROUPS_AS_WHITELISTS_NAME = "use.food.groups.as.whitelists";
    private static final boolean USE_FOOD_GROUPS_AS_WHITELISTS_DEFAULT = false;
    private static final String USE_FOOD_GROUPS_AS_WHITELISTS_COMMENT = "If true, any foods not in a food group will be excluded from diminishing returns";
    private static final String FOOD_HUNGER_ROUNDING_MODE_NAME = "food.hunger.rounding.mode";
    private static final String FOOD_HUNGER_ROUNDING_MODE_DEFAULT = "round";
    private static final String FOOD_HUNGER_ROUNDING_MODE_COMMENT = "Rounding mode used on the hunger value of foods\n"
        + "Valid options: 'round', 'floor', 'ceiling'";
    private static final String AFFECT_FOOD_HUNGER_VALUES_NAME = "affect.food.hunger.values";
    private static final boolean AFFECT_FOOD_HUNGER_VALUES_DEFAULT = true;
    private static final String AFFECT_NEGATIVE_FOOD_HUNGER_VALUES_NAME = "affect.negative.food.hunger.values";
    private static final boolean AFFECT_NEGATIVE_FOOD_HUNGER_VALUES_DEFAULT = false;
    private static final String AFFECT_NEGATIVE_FOOD_HUNGER_VALUES_COMMENT = "If true, foods with negative hunger values will be made more negative as nutritional value decreases\n"
        + "NOTE: "
        + AFFECT_FOOD_HUNGER_VALUES_NAME
        + " must be true for this to have any affect";
    private static final String AFFECT_FOOD_SATURATION_MODIFIERS_NAME = "affect.food.saturation.modifiers";
    private static final String AFFECT_FOOD_HUNGER_VALUES_COMMENT = "If true, foods' hunger value will be multiplied by the current nutritional value\n"
        + "Setting this to false and "
        + ModConfig.AFFECT_FOOD_SATURATION_MODIFIERS_NAME
        + " to true will make diminishing returns affect saturation only";
    private static final boolean AFFECT_FOOD_SATURATION_MODIFIERS_DEFAULT = false;
    private static final String AFFECT_FOOD_SATURATION_MODIFIERS_COMMENT = "If true, foods' saturation modifier will be multiplied by the current nutritional value\n"
        + "NOTE: When "
        + ModConfig.AFFECT_FOOD_HUNGER_VALUES_NAME
        + " is true, saturation bonuses of foods will automatically decrease as the hunger value of the food decreases\n"
        + "Setting this to true when "
        + ModConfig.AFFECT_FOOD_HUNGER_VALUES_NAME
        + " is true will make saturation bonuses decrease disproportionately more than hunger values\n"
        + "Setting this to true and "
        + ModConfig.AFFECT_FOOD_HUNGER_VALUES_NAME
        + " to false will make diminishing returns affect saturation only";
    private static final String AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS_NAME = "affect.negative.food.saturation.modifiers";
    private static final boolean AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS_DEFAULT = false;
    private static final String AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS_COMMENT = "If true, foods with negative saturation modifiers will be made more negative as nutritional value decreases\n"
        + "NOTE: "
        + AFFECT_FOOD_SATURATION_MODIFIERS_NAME
        + " must be true for this to have any affect";
    private static final String FOOD_EATING_SPEED_MODIFIER_NAME = "food.eating.speed.modifier";
    private static final float FOOD_EATING_SPEED_MODIFIER_DEFAULT = 1;
    private static final String FOOD_EATING_SPEED_MODIFIER_COMMENT = "If set to greater than zero, food eating speed will be affected by nutritional value\n"
        + "(meaning the lower the nutrtional value, the longer it will take to eat it)\n"
        + "Eating duration is calcualted using the formula (eating_duration / (nutritional_value^eating_speed_modifier))";
    private static final String FOOD_EATING_DURATION_MAX_NAME = "food.eating.duration.max";
    private static final int FOOD_EATING_DURATION_MAX_DEFAULT = 0;
    private static final String FOOD_EATING_DURATION_MAX_COMMENT = "The maximum time it takes to eat a food after being modified by "
        + ModConfig.FOOD_EATING_SPEED_MODIFIER_NAME
        + "\n"
        + "The default eating duration is 32. Set this to 0 to remove the limit on eating speed.\n"
        + "Note: If this is set to 0 and "
        + ModConfig.FOOD_EATING_SPEED_MODIFIER_NAME
        + " is > 0, a food with 0% nutrtional value will take nearly infinite time to eat";
    private static final String USE_HUNGER_QUEUE_NAME = "use.hunger.restored.for.food.history.length";
    private static final boolean USE_HUNGER_QUEUE_DEFAULT = false;
    private static final String USE_HUNGER_QUEUE_COMMENT = "If true, " + FOOD_HISTORY_LENGTH_NAME
        + " will use amount of hunger restored instead of number of foods eaten for its maximum length\n"
        + "For example, a "
        + FOOD_HISTORY_LENGTH_NAME
        + " length of 12 will store a max of 2 foods that restored 6 hunger each, \n"
        + "3 foods that restored 4 hunger each, 12 foods that restored 1 hunger each, etc\n"
        + "NOTE: "
        + FOOD_HISTORY_LENGTH_NAME
        + " uses hunger units, where 1 hunger unit = 1/2 hunger bar";
    private static final String FOOD_MODIFIER_FORMULA_STRING_NAME = "food.modifier.formula";
    private static final String FOOD_MODIFIER_FORMULA_STRING_DEFAULT = "MAX(0, (1 - count/12))^MIN(8, food_hunger_value)";
    private static final String FOOD_MODIFIER_FORMULA_STRING_COMMENT = "Uses the EvalEx expression parser\n"
        + "See: https://github.com/uklimaschewski/EvalEx for syntax/function documentation\n\n"
        + "Available variables:\n"
        + "\tcount : The number of times the food (or its food group) has been eaten within the food history\n"
        + "\thunger_count : The total amount of hunger that the food (or its food group) has restored within the food history (1 hunger unit = 1/2 hunger bar)\n"
        + "\tsaturation_count : The total amount of saturation that the food (or its food group) has restored within the food history (1 saturation unit = 1/2 saturation bar)\n"
        + "\tmax_history_length : The maximum length of the food history (see "
        + FOOD_HISTORY_LENGTH_NAME
        + ")\n"
        + "\tcur_history_length : The current length of the food history (<= max_history_length)\n"
        + "\tfood_hunger_value : The default amount of hunger the food would restore in hunger units (1 hunger unit = 1/2 hunger bar)\n"
        + "\tfood_saturation_mod : The default saturation modifier of the food\n"
        + "\tcur_hunger : The current hunger value of the player in hunger units (20 = full)\n"
        + "\tcur_saturation : The current saturation value of the player\n"
        + "\ttotal_food_eaten : The all-time total number of times any food has been eaten by the player\n"
        + "\tfood_group_count : The number of food groups that the food belongs to\n"
        + "\tdistinct_food_groups_eaten : The number of distinct food groups in the player's current food history\n"
        + "\ttotal_food_groups : The total number of enabled food groups\n"
        + "\texact_count : The number of times the food (ignoring food groups) has been eaten within the food history\n";
    private static final String GIVE_FOOD_JOURNAL_ON_START_NAME = "give.food.journal.as.starting.item";
    private static final boolean GIVE_FOOD_JOURNAL_ON_START_DEFAULT = false;
    private static final String GIVE_FOOD_JOURNAL_ON_START_COMMENT = "If true, a food journal will be given to each player as a starting item";
    private static final String FOOD_CONTAINERS_MAX_STACKSIZE_NAME = "food.containers.max.stacksize";
    private static final int FOOD_CONTAINERS_MAX_STACKSIZE_DEFAULT = 2;
    private static final String FOOD_CONTAINERS_MAX_STACKSIZE_COMMENT = "The maximum stacksize per slot in a food container";

    /*
     * CLIENT
     */
    private static final String CATEGORY_CLIENT = "client";
    private static final String CATEGORY_CLIENT_COMMENT = "These config settings are client-side only";
    private static final String LEFT_CLICK_OPENS_FOOD_CONTAINERS_NAME = "left.click.opens.food.containers";
    private static final boolean LEFT_CLICK_OPENS_FOOD_CONTAINERS_DEFAULT = false;
    private static final String LEFT_CLICK_OPENS_FOOD_CONTAINERS_COMMENT = "If true, left clicking the air while holding a food container will open it (so that it can be eaten from)";
    /*
     * FOOD GROUPS
     */
    @Deprecated
    private static final String CATEGORY_FOODGROUPS = "foodgroups";

    private static final String CATEGORY_FOODGROUPS_COMMENT = "Food groups are defined using .json files in /config/SpiceOfLife/\n"
        + "See /config/SpiceOfLife/example-food-group.json";
    // whether or not food modifier is actually enabled (we either are the server or know the server has it enabled)
    public static boolean FOOD_MODIFIER_ENABLED = false;
    // the value written in the config file
    public static boolean FOOD_MODIFIER_ENABLED_CONFIG_VAL = ModConfig.FOOD_MODIFIER_ENABLED_DEFAULT;
    public static int FOOD_HISTORY_LENGTH = ModConfig.FOOD_HISTORY_LENGTH_DEFAULT;
    private static final int FOOD_EATEN_THRESHOLD_DEFAULT = ModConfig.FOOD_HISTORY_LENGTH / 2;
    public static boolean FOOD_HISTORY_PERSISTS_THROUGH_DEATH = ModConfig.FOOD_HISTORY_PERSISTS_THROUGH_DEATH_DEFAULT;
    public static int FOOD_EATEN_THRESHOLD = ModConfig.FOOD_EATEN_THRESHOLD_DEFAULT;
    public static boolean USE_FOOD_GROUPS_AS_WHITELISTS = ModConfig.USE_FOOD_GROUPS_AS_WHITELISTS_DEFAULT;
    public static RoundingMode FOOD_HUNGER_ROUNDING_MODE = null;
    public static String FOOD_HUNGER_ROUNDING_MODE_STRING = ModConfig.FOOD_HUNGER_ROUNDING_MODE_DEFAULT;
    public static boolean AFFECT_FOOD_HUNGER_VALUES = ModConfig.AFFECT_FOOD_HUNGER_VALUES_DEFAULT;
    public static boolean AFFECT_NEGATIVE_FOOD_HUNGER_VALUES = ModConfig.AFFECT_NEGATIVE_FOOD_HUNGER_VALUES_DEFAULT;
    public static boolean AFFECT_FOOD_SATURATION_MODIFIERS = ModConfig.AFFECT_FOOD_SATURATION_MODIFIERS_DEFAULT;
    public static boolean AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS = ModConfig.AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS_DEFAULT;
    public static float FOOD_EATING_SPEED_MODIFIER = ModConfig.FOOD_EATING_SPEED_MODIFIER_DEFAULT;
    public static int FOOD_EATING_DURATION_MAX = ModConfig.FOOD_EATING_DURATION_MAX_DEFAULT;
    public static boolean USE_HUNGER_QUEUE = USE_HUNGER_QUEUE_DEFAULT;
    public static String FOOD_MODIFIER_FORMULA = ModConfig.FOOD_MODIFIER_FORMULA_STRING_DEFAULT;
    public static boolean GIVE_FOOD_JOURNAL_ON_START = ModConfig.GIVE_FOOD_JOURNAL_ON_START_DEFAULT;

    /*
     * ITEMS
     */
    public static int FOOD_CONTAINERS_MAX_STACKSIZE = ModConfig.FOOD_CONTAINERS_MAX_STACKSIZE_DEFAULT;
    public static boolean LEFT_CLICK_OPENS_FOOD_CONTAINERS = ModConfig.LEFT_CLICK_OPENS_FOOD_CONTAINERS_DEFAULT;
    private static Configuration config;

    protected ModConfig() {}

    public static void init(File file) {
        config = new Configuration(file);

        load();

        /*
         * MAIN
         */
        config.getCategory(CATEGORY_MAIN)
            .setComment(CATEGORY_MAIN_COMMENT);
        FOOD_MODIFIER_ENABLED_CONFIG_VAL = config
            .get(
                CATEGORY_MAIN,
                FOOD_MODIFIER_ENABLED_NAME,
                FOOD_MODIFIER_ENABLED_DEFAULT,
                FOOD_MODIFIER_ENABLED_COMMENT)
            .getBoolean(FOOD_MODIFIER_ENABLED_DEFAULT);

        // only use the config value immediately when server-side; the client assumes false until the server syncs the
        // config
        if (FMLCommonHandler.instance()
            .getSide() == Side.SERVER) FOOD_MODIFIER_ENABLED = FOOD_MODIFIER_ENABLED_CONFIG_VAL;
        /*
         * CARROT
         */
        config.getCategory(CATEGORY_CARROT)
            .setComment(CATEGORY_CARROT_COMMENT);
        EXTRA_HEARTS_ENABLE = config.get(CATEGORY_CARROT, EXTRA_HEARTS_NAME, EXTRA_HEARTS_DEFAULT, EXTRA_HEARTS_COMMENT)
            .getBoolean(EXTRA_HEARTS_DEFAULT);

        FOOD_MILESTONE_VALUE = config
            .get(CATEGORY_CARROT, FOOD_MILESTONE_NAME, FOOD_MILESTONE_DEFAULT, FOOD_MILESTONE_COMMENT)
            .getInt(FOOD_MILESTONE_DEFAULT);

        MILESTONE_INCREMENT_VALUE = config
            .get(CATEGORY_CARROT, MILESTONE_INCREMENT_NAME, MILESTONE_INCREMENT_DEFAULT, MILESTONE_INCREMENT_COMMENT)
            .getInt(MILESTONE_INCREMENT_DEFAULT);

        HEARTS_PER_MILESTONE_VALUE = config
            .get(CATEGORY_CARROT, HEARTS_PER_MILESTONE_NAME, HEARTS_PER_MILESTONE_DEFAULT, HEARTS_PER_MILESTONE_COMMENT)
            .getInt(HEARTS_PER_MILESTONE_DEFAULT);

        MAX_MILESTONE_VALUE = config
            .get(CATEGORY_CARROT, MAX_MILESTONE_NAME, MAX_MILESTONE_DEFAULT, MAX_MILESTONE_COMMENT)
            .getInt(MAX_MILESTONE_DEFAULT);

        FOOD_MILESTONES_PERSISTS_THROUGH_DEATH = config
            .get(
                CATEGORY_CARROT,
                FOOD_MILESTONES_PERSISTS_THROUGH_DEATH_NAME,
                FOOD_MILESTONES_PERSISTS_THROUGH_DEATH_DEFAULT,
                FOOD_MILESTONES_PERSISTS_THROUGH_DEATH_COMMENT)
            .getBoolean(FOOD_MILESTONES_PERSISTS_THROUGH_DEATH_DEFAULT);
        /*
         * DEV
         */
        config.getCategory(CATEGORY_DEV)
            .setComment(CATEGORY_DEV_COMMENT);

        DEV_LOGGING_ENABLED = config
            .get(CATEGORY_DEV, DEV_LOGGING_ENABLED_NAME, DEV_LOGGING_ENABLED_DEFAULT, DEV_LOGGING_ENABLED_COMMENT)
            .getBoolean(DEV_LOGGING_ENABLED_DEFAULT);

        /*
         * SERVER
         */
        config.getCategory(CATEGORY_SERVER)
            .setComment(CATEGORY_SERVER_COMMENT);

        Property FOOD_MODIFIER_PROPERTY = config.get(
            CATEGORY_SERVER,
            FOOD_MODIFIER_FORMULA_STRING_NAME,
            FOOD_MODIFIER_FORMULA_STRING_DEFAULT,
            FOOD_MODIFIER_FORMULA_STRING_COMMENT);

        // enforce the new default if the config has the old default
        if (FOOD_MODIFIER_PROPERTY.getString()
            .equals("MAX(0, (1 - count/12))^MAX(0, food_hunger_value-ROUND(MAX(0, 1 - count/12), 0))"))
            FOOD_MODIFIER_PROPERTY.set(FOOD_MODIFIER_FORMULA_STRING_DEFAULT);

        FOOD_MODIFIER_FORMULA = FOOD_MODIFIER_PROPERTY.getString();

        FOOD_HISTORY_LENGTH = config
            .get(CATEGORY_SERVER, FOOD_HISTORY_LENGTH_NAME, FOOD_HISTORY_LENGTH_DEFAULT, FOOD_HISTORY_LENGTH_COMMENT)
            .getInt(FOOD_HISTORY_LENGTH_DEFAULT);
        FOOD_HISTORY_PERSISTS_THROUGH_DEATH = config
            .get(
                CATEGORY_SERVER,
                FOOD_HISTORY_PERSISTS_THROUGH_DEATH_NAME,
                FOOD_HISTORY_PERSISTS_THROUGH_DEATH_DEFAULT,
                FOOD_HISTORY_PERSISTS_THROUGH_DEATH_COMMENT)
            .getBoolean(FOOD_HISTORY_PERSISTS_THROUGH_DEATH_DEFAULT);
        FOOD_EATEN_THRESHOLD = config
            .get(CATEGORY_SERVER, FOOD_EATEN_THRESHOLD_NAME, FOOD_EATEN_THRESHOLD_DEFAULT, FOOD_EATEN_THRESHOLD_COMMENT)
            .getInt(FOOD_EATEN_THRESHOLD_DEFAULT);
        USE_FOOD_GROUPS_AS_WHITELISTS = config
            .get(
                CATEGORY_SERVER,
                USE_FOOD_GROUPS_AS_WHITELISTS_NAME,
                USE_FOOD_GROUPS_AS_WHITELISTS_DEFAULT,
                USE_FOOD_GROUPS_AS_WHITELISTS_COMMENT)
            .getBoolean(USE_FOOD_GROUPS_AS_WHITELISTS_DEFAULT);
        AFFECT_FOOD_HUNGER_VALUES = config
            .get(
                CATEGORY_SERVER,
                AFFECT_FOOD_HUNGER_VALUES_NAME,
                AFFECT_FOOD_HUNGER_VALUES_DEFAULT,
                AFFECT_FOOD_HUNGER_VALUES_COMMENT)
            .getBoolean(AFFECT_FOOD_HUNGER_VALUES_DEFAULT);
        AFFECT_NEGATIVE_FOOD_HUNGER_VALUES = config
            .get(
                CATEGORY_SERVER,
                AFFECT_NEGATIVE_FOOD_HUNGER_VALUES_NAME,
                AFFECT_NEGATIVE_FOOD_HUNGER_VALUES_DEFAULT,
                AFFECT_NEGATIVE_FOOD_HUNGER_VALUES_COMMENT)
            .getBoolean(AFFECT_NEGATIVE_FOOD_HUNGER_VALUES_DEFAULT);
        AFFECT_FOOD_SATURATION_MODIFIERS = config
            .get(
                CATEGORY_SERVER,
                AFFECT_FOOD_SATURATION_MODIFIERS_NAME,
                AFFECT_FOOD_SATURATION_MODIFIERS_DEFAULT,
                AFFECT_FOOD_SATURATION_MODIFIERS_COMMENT)
            .getBoolean(AFFECT_FOOD_SATURATION_MODIFIERS_DEFAULT);
        AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS = config
            .get(
                CATEGORY_SERVER,
                AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS_NAME,
                AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS_DEFAULT,
                AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS_COMMENT)
            .getBoolean(AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS_DEFAULT);
        FOOD_EATING_SPEED_MODIFIER = (float) config
            .get(
                CATEGORY_SERVER,
                FOOD_EATING_SPEED_MODIFIER_NAME,
                FOOD_EATING_SPEED_MODIFIER_DEFAULT,
                FOOD_EATING_SPEED_MODIFIER_COMMENT)
            .getDouble(FOOD_EATING_SPEED_MODIFIER_DEFAULT);
        FOOD_EATING_DURATION_MAX = config
            .get(
                CATEGORY_SERVER,
                FOOD_EATING_DURATION_MAX_NAME,
                FOOD_EATING_DURATION_MAX_DEFAULT,
                FOOD_EATING_DURATION_MAX_COMMENT)
            .getInt(FOOD_EATING_DURATION_MAX_DEFAULT);
        USE_HUNGER_QUEUE = config
            .get(CATEGORY_SERVER, USE_HUNGER_QUEUE_NAME, USE_HUNGER_QUEUE_DEFAULT, USE_HUNGER_QUEUE_COMMENT)
            .getBoolean(USE_HUNGER_QUEUE_DEFAULT);
        GIVE_FOOD_JOURNAL_ON_START = config
            .get(
                CATEGORY_SERVER,
                GIVE_FOOD_JOURNAL_ON_START_NAME,
                GIVE_FOOD_JOURNAL_ON_START_DEFAULT,
                GIVE_FOOD_JOURNAL_ON_START_COMMENT)
            .getBoolean(GIVE_FOOD_JOURNAL_ON_START_DEFAULT);
        FOOD_CONTAINERS_MAX_STACKSIZE = config
            .get(
                CATEGORY_SERVER,
                FOOD_CONTAINERS_MAX_STACKSIZE_NAME,
                FOOD_CONTAINERS_MAX_STACKSIZE_DEFAULT,
                FOOD_CONTAINERS_MAX_STACKSIZE_COMMENT)
            .getInt(FOOD_CONTAINERS_MAX_STACKSIZE_DEFAULT);

        FOOD_HUNGER_ROUNDING_MODE_STRING = config
            .get(
                CATEGORY_SERVER,
                FOOD_HUNGER_ROUNDING_MODE_NAME,
                FOOD_HUNGER_ROUNDING_MODE_DEFAULT,
                FOOD_HUNGER_ROUNDING_MODE_COMMENT)
            .getString();
        setRoundingMode();

        /*
         * CLIENT
         */
        config.getCategory(CATEGORY_CLIENT)
            .setComment(CATEGORY_CLIENT_COMMENT);

        LEFT_CLICK_OPENS_FOOD_CONTAINERS = config
            .get(
                CATEGORY_CLIENT,
                LEFT_CLICK_OPENS_FOOD_CONTAINERS_NAME,
                LEFT_CLICK_OPENS_FOOD_CONTAINERS_DEFAULT,
                LEFT_CLICK_OPENS_FOOD_CONTAINERS_COMMENT)
            .getBoolean(LEFT_CLICK_OPENS_FOOD_CONTAINERS_DEFAULT);

        /*
         * FOOD GROUPS
         */
        config.getCategory(CATEGORY_FOODGROUPS)
            .setComment(CATEGORY_FOODGROUPS_COMMENT);
        FoodGroupConfig.setup(file.getParentFile());

        // remove obsolete config options
        config.getCategory(CATEGORY_SERVER)
            .remove("use.food.groups");
        config.getCategory(CATEGORY_FOODGROUPS)
            .clear();

        save();
    }

    /*
     * OBSOLETED
     */
    // nothing here

    public static void load() {
        config.load();
    }

    public static void setRoundingMode() {
        for (RoundingMode roundingMode : RoundingMode.values()) {
            if (roundingMode.id.equals(FOOD_HUNGER_ROUNDING_MODE_STRING.toLowerCase(Locale.ROOT))) {
                FOOD_HUNGER_ROUNDING_MODE = roundingMode;
                break;
            }
        }
        if (FOOD_HUNGER_ROUNDING_MODE == null) {
            ModSpiceOfLife.Log
                .warn("Rounding mode '" + FOOD_HUNGER_ROUNDING_MODE_STRING + "' not recognized; defaulting to 'round'");
            FOOD_HUNGER_ROUNDING_MODE_STRING = "round";
            FOOD_HUNGER_ROUNDING_MODE = RoundingMode.ROUND;
        }
    }

    public static void save() {
        config.save();
    }

    public static void sync(EntityPlayerMP player) {
        PacketDispatcher.get()
            .sendTo(new PacketConfigSync(), player);
    }

    @SideOnly(Side.CLIENT)
    public static void assumeClientOnly() {
        // assume false until the server syncs
        FOOD_MODIFIER_ENABLED = false;
    }

    @Override
    public void pack(IByteIO data) {
        data.writeBoolean(FOOD_MODIFIER_ENABLED_CONFIG_VAL);
        if (FOOD_MODIFIER_ENABLED_CONFIG_VAL) {
            data.writeUTF(FOOD_MODIFIER_FORMULA);
            data.writeShort(FOOD_HISTORY_LENGTH);
            data.writeBoolean(FOOD_HISTORY_PERSISTS_THROUGH_DEATH);
            data.writeInt(FOOD_EATEN_THRESHOLD);
            data.writeBoolean(USE_FOOD_GROUPS_AS_WHITELISTS);
            data.writeBoolean(AFFECT_FOOD_SATURATION_MODIFIERS);
            data.writeBoolean(AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS);
            data.writeFloat(FOOD_EATING_SPEED_MODIFIER);
            data.writeInt(FOOD_EATING_DURATION_MAX);
            data.writeUTF(FOOD_HUNGER_ROUNDING_MODE_STRING);
        }
        data.writeInt(FOOD_CONTAINERS_MAX_STACKSIZE);
    }

    @Override
    public void unpack(IByteIO data) {
        FOOD_MODIFIER_ENABLED = data.readBoolean();
        if (FOOD_MODIFIER_ENABLED) {
            FOOD_MODIFIER_FORMULA = data.readUTF();
            FOOD_HISTORY_LENGTH = data.readShort();
            FOOD_HISTORY_PERSISTS_THROUGH_DEATH = data.readBoolean();
            FOOD_EATEN_THRESHOLD = data.readInt();
            USE_FOOD_GROUPS_AS_WHITELISTS = data.readBoolean();
            AFFECT_FOOD_SATURATION_MODIFIERS = data.readBoolean();
            AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS = data.readBoolean();
            FOOD_EATING_SPEED_MODIFIER = data.readFloat();
            FOOD_EATING_DURATION_MAX = data.readInt();
            FOOD_HUNGER_ROUNDING_MODE_STRING = data.readUTF();
        }
        FOOD_CONTAINERS_MAX_STACKSIZE = data.readInt();
    }

    @Override
    public PacketBase processAndReply(Side side, EntityPlayer player) {
        if (FOOD_MODIFIER_ENABLED) {
            setRoundingMode();
            FoodModifier.onGlobalFormulaChanged();
            FoodHistory.get(player)
                .onHistoryTypeChanged();
            FoodGroupRegistry.clear();
        }

        return null;
    }

    public enum RoundingMode {

        ROUND("round") {

            @Override
            public double round(double val) {
                return Math.round(val);
            }
        },
        FLOOR("floor") {

            @Override
            public double round(double val) {
                return Math.floor(val);
            }
        },
        CEILING("ceiling") {

            @Override
            public double round(double val) {
                return Math.ceil(val);
            }
        };

        public final String id;

        RoundingMode(String id) {
            this.id = id;
        }

        public abstract double round(double val);
    }
}
