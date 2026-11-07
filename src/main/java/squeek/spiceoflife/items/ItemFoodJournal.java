package squeek.spiceoflife.items;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import squeek.spiceoflife.ModConfig;
import squeek.spiceoflife.ModContent;
import squeek.spiceoflife.ModInfo;
import squeek.spiceoflife.foodtracker.FoodHistory;
import squeek.spiceoflife.foodtracker.FoodTracker;
import squeek.spiceoflife.gui.GuiScreenFoodJournal;
import squeek.spiceoflife.gui.GuiScreenMilestoneJournal;

public class ItemFoodJournal extends Item {

    private IIcon iconHistory;
    private IIcon iconMilestone;

    public ItemFoodJournal() {
        super();
        setMaxStackSize(1);
        setTextureName(ModInfo.MODID.toLowerCase(Locale.ROOT) + ":" + ModConfig.ITEM_FOOD_JOURNAL_NAME);
        setUnlocalizedName(ModInfo.MODID.toLowerCase(Locale.ROOT) + "." + ModConfig.ITEM_FOOD_JOURNAL_NAME);
        setCreativeTab(CreativeTabs.tabMisc);
        this.setHasSubtypes(true);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        if (ModConfig.IRON_CHEF_MODE_ENABLE) list.add(new ItemStack(item, 1, 1));
    }

    @Override
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);

        iconHistory = register.registerIcon("spiceoflife:bookfoodjournal");
        iconMilestone = register.registerIcon("spiceoflife:bookmilestonejournal");
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        if (damage == 1) {
            return iconMilestone;
        }
        return iconHistory;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        if (stack.getItemDamage() == 1) {
            return "item.spiceoflife.bookmilestonejournal";
        }
        return "item.spiceoflife.bookfoodjournal";
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer player, List toolTip, boolean isAdvanced) {
        super.addInformation(itemStack, player, toolTip, isAdvanced);

        int meta = itemStack.getItemDamage();
        String description;

        if (meta == 1)
            description = StatCollector.translateToLocalFormatted("item.spiceoflife.bookmilestonejournal.desc");
        else description = StatCollector.translateToLocalFormatted("item.spiceoflife.bookfoodjournal.desc");

        for (Object line : Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(description, 200)) {
            toolTip.add(EnumChatFormatting.GRAY.toString() + line);
        }

    }

    public static void giveToPlayer(EntityPlayer player) {
        if (player != null && !player.worldObj.isRemote) {
            ItemStack itemStack = new ItemStack(ModContent.foodJournal, 1, 0);
            // try add, otherwise spawn in the world
            if (!player.inventory.addItemStackToInventory(itemStack)) {
                EntityItem entityItem = new EntityItem(
                    player.worldObj,
                    player.posX + 0.5f,
                    player.posY + 0.5f,
                    player.posZ + 0.5f,
                    itemStack);
                player.worldObj.spawnEntityInWorld(entityItem);
            }
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        int meta = itemStack.getItemDamage();

        if (world.isRemote) Minecraft.getMinecraft()
            .displayGuiScreen(getGuiJournal(meta));
        else if (meta == 1) {

            FoodHistory.get(player)
                .populateMilestoneTargets();
            FoodTracker.syncFoodHistory(FoodHistory.get(player));
        }
        return super.onItemRightClick(itemStack, world, player);
    }

    @SideOnly(Side.CLIENT)
    public static GuiScreen getGuiJournal(int meta) {
        if (meta == 1) return new GuiScreenMilestoneJournal();

        return new GuiScreenFoodJournal();
    }
}
