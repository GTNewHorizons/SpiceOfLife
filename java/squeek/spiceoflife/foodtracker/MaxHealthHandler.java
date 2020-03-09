package squeek.spiceoflife.foodtracker;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;

import java.util.UUID;

public class MaxHealthHandler {
    private static final UUID SOL_HEALTH_MODIFIER_ID = UUID.fromString("f88d6ac1-4193-4ff0-85f5-f0357fe89d17");

    public static boolean updateFoodHPModifier(EntityPlayer player) {
        if (player.worldObj.isRemote) {
            return false;
        }

        final IAttributeInstance attribute = player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.maxHealth);
        final AttributeModifier prevModifier = attribute.getModifier(SOL_HEALTH_MODIFIER_ID);

        final FoodHistory foodHistory = FoodHistory.get(player);
        ProgressInfo progressInfo = foodHistory.getProgressInfo();
        final int milestonesAchieved = progressInfo.milestonesAchieved();
        final double totalHealthModifier = milestonesAchieved * 2 * ProgressInfo.HEARTS_PER_MILESTONE;

        boolean hasChanged = prevModifier == null || prevModifier.getAmount() != totalHealthModifier;

        AttributeModifier modifier = new AttributeModifier(
            SOL_HEALTH_MODIFIER_ID,
            "Health gained from trying new foods",
            totalHealthModifier,
            0);

        updateHealthModifier(player, modifier);

        return hasChanged;
    }

    private static void updateHealthModifier(EntityPlayer player, AttributeModifier modifier) {
        float oldMax = player.getMaxHealth();

        IAttributeInstance attribute = player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.maxHealth);
        attribute.removeModifier(modifier);
        attribute.applyModifier(modifier);

        float newHealth = player.getHealth() * player.getMaxHealth() / oldMax;
        // because apparently it doesn't update unless changed
        player.setHealth(0.1f);
        // adjust current health proportionally to increase in max health
        player.setHealth(newHealth);
    }
}
