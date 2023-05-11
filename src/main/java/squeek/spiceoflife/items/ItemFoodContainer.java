package squeek.spiceoflife.items;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.item.ItemTossEvent;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import squeek.applecore.api.food.FoodEvent;
import squeek.applecore.api.food.FoodValues;
import squeek.applecore.api.food.IEdible;
import squeek.spiceoflife.ModConfig;
import squeek.spiceoflife.ModInfo;
import squeek.spiceoflife.helpers.FoodHelper;
import squeek.spiceoflife.helpers.GuiHelper;
import squeek.spiceoflife.helpers.InventoryHelper;
import squeek.spiceoflife.helpers.MealPrioritizationHelper;
import squeek.spiceoflife.helpers.MealPrioritizationHelper.InventoryFoodInfo;
import squeek.spiceoflife.helpers.MiscHelper;
import squeek.spiceoflife.inventory.ContainerFoodContainer;
import squeek.spiceoflife.inventory.FoodContainerInventory;
import squeek.spiceoflife.inventory.INBTInventoryHaver;
import squeek.spiceoflife.inventory.NBTInventory;
import squeek.spiceoflife.network.NetworkHelper;
import squeek.spiceoflife.network.PacketHandler;
import squeek.spiceoflife.network.PacketToggleFoodContainer;

public class ItemFoodContainer extends Item implements INBTInventoryHaver, IEdible {

    public static final Random random = new Random();
    public static final String TAG_KEY_INVENTORY = "Inventory";
    public static final String TAG_KEY_OPEN = "Open";
    public static final String TAG_KEY_UUID = "UUID";
    public int numSlots;
    public String itemName;
    private IIcon iconOpenEmpty;
    private IIcon iconOpenFull;

    public ItemFoodContainer(String itemName, int numSlots) {
        super();
        this.itemName = itemName;
        this.numSlots = numSlots;
        setMaxStackSize(1);
        setTextureName(ModInfo.MODID.toLowerCase(Locale.ROOT) + ":" + this.itemName);
        setUnlocalizedName(ModInfo.MODID.toLowerCase(Locale.ROOT) + "." + this.itemName);
        setCreativeTab(CreativeTabs.tabMisc);

        // for ItemTossEvent
        MinecraftForge.EVENT_BUS.register(this);
    }

    public boolean isFull(ItemStack itemStack) {
        return getInventory(itemStack).isInventoryFull();
    }

    public FoodContainerInventory getInventory(ItemStack itemStack) {
        return new FoodContainerInventory(this, itemStack);
    }

    // necessary to catch tossing items while still in an inventory
    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (event.entityItem.getEntityItem().getItem() instanceof ItemFoodContainer) {
            onDroppedByPlayer(event.entityItem.getEntityItem(), event.player);
        }
    }

    @Override
    public String getInvName(NBTInventory inventory) {
        return this.getItemStackDisplayName(null);
    }

    public UUID getUUID(ItemStack itemStack) {
        return UUID.fromString(getOrInitBaseTag(itemStack).getString(TAG_KEY_UUID));
    }

    public NBTTagCompound getOrInitBaseTag(ItemStack itemStack) {
        if (!itemStack.hasTagCompound()) itemStack.setTagCompound(new NBTTagCompound());

        NBTTagCompound baseTag = itemStack.getTagCompound();

        if (!baseTag.hasKey(TAG_KEY_UUID)) baseTag.setString(TAG_KEY_UUID, UUID.randomUUID().toString());

        return baseTag;
    }

    @Override
    public int getSizeInventory() {
        return numSlots;
    }

    @Override
    public boolean isInvNameLocalized(NBTInventory inventory) {
        return false;
    }

    @Override
    public int getInventoryStackLimit(NBTInventory inventory) {
        return ModConfig.FOOD_CONTAINERS_MAX_STACKSIZE;
    }

    @Override
    public void onInventoryChanged(NBTInventory inventory) {}

    @Override
    public boolean isItemValidForSlot(NBTInventory inventory, int slotNum, ItemStack itemStack) {
        return FoodHelper.isFood(itemStack) && FoodHelper.isDirectlyEdible(itemStack);
    }

    public void setIsOpen(ItemStack itemStack, boolean isOpen) {
        NBTTagCompound baseTag = getOrInitBaseTag(itemStack);
        baseTag.setBoolean(TAG_KEY_OPEN, isOpen);
    }

    /*
     * IEdible implementation
     */
    @Override
    public FoodValues getFoodValues(ItemStack itemStack) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            // the client uses the food values for tooltips/etc, so it should
            // inherit them from the food that will be eaten
            return FoodValues.get(getBestFoodForPlayerToEat(itemStack, NetworkHelper.getClientPlayer()));
        } else {
            // the server only needs to know that food values are non-null
            // this is used for the isFood check
            return new FoodValues(0, 0f);
        }
    }

    public ItemStack getBestFoodForPlayerToEat(ItemStack itemStack, EntityPlayer player) {
        IInventory inventory = getInventory(itemStack);
        int slotWithBestFood = MealPrioritizationHelper.findBestFoodForPlayerToEat(player, inventory);
        return inventory.getStackInSlot(slotWithBestFood);
    }

    // necessary to stop food containers themselves being modified
    // for example, HO's modFoodDivider was being applied to the values
    // shown in the tooltips/overlay
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void getFoodValues(FoodEvent.GetFoodValues event) {
        if (FoodHelper.isFoodContainer(event.food)) {
            event.foodValues = event.unmodifiedFoodValues;
        }
    }

    public void tryDumpFoodInto(ItemStack itemStack, IInventory inventory, EntityPlayer player) {
        FoodContainerInventory foodContainerInventory = getInventory(itemStack);
        for (int slotNum = 0; slotNum < foodContainerInventory.getSizeInventory(); slotNum++) {
            ItemStack stackInSlot = foodContainerInventory.getStackInSlot(slotNum);

            if (stackInSlot == null) continue;

            stackInSlot = InventoryHelper.insertStackIntoInventory(stackInSlot, inventory);
            foodContainerInventory.setInventorySlotContents(slotNum, stackInSlot);
        }
    }

    public void tryPullFoodFrom(ItemStack itemStack, IInventory inventory, EntityPlayer player) {
        List<InventoryFoodInfo> foodsToPull = MealPrioritizationHelper
                .findBestFoodsForPlayerAccountingForVariety(player, inventory);
        if (foodsToPull.size() > 0) {
            FoodContainerInventory foodContainerInventory = getInventory(itemStack);
            for (InventoryFoodInfo foodToPull : foodsToPull) {
                ItemStack stackInSlot = inventory.getStackInSlot(foodToPull.slotNum);

                if (stackInSlot == null) continue;

                stackInSlot = InventoryHelper.insertStackIntoInventoryOnce(stackInSlot, foodContainerInventory);
                inventory.setInventorySlotContents(foodToPull.slotNum, stackInSlot);
            }
        }
    }

    public boolean canPlayerEatFrom(EntityPlayer player, ItemStack stack) {
        return canBeEatenFrom(stack) && player.canEat(false);
    }

    public boolean canBeEatenFrom(ItemStack stack) {
        return isOpen(stack) && !isEmpty(stack);
    }

    public boolean isOpen(ItemStack itemStack) {
        return itemStack.hasTagCompound() && itemStack.getTagCompound().getBoolean(TAG_KEY_OPEN);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack itemStack, EntityPlayer player) {
        if (!player.worldObj.isRemote && player.openContainer != null
                && player.openContainer instanceof ContainerFoodContainer) {
            ContainerFoodContainer openFoodContainer = (ContainerFoodContainer) player.openContainer;
            UUID droppedUUID = getUUID(itemStack);

            if (openFoodContainer.getUUID().equals(droppedUUID)) {
                // if the cursor item is the open food container, then it will create an infinite loop
                // due to the container dropping the cursor item when it is closed
                ItemStack itemOnTheCursor = player.inventory.getItemStack();
                if (itemOnTheCursor != null && itemOnTheCursor.getItem() instanceof ItemFoodContainer) {
                    if (((ItemFoodContainer) itemOnTheCursor.getItem()).getUUID(itemOnTheCursor).equals(droppedUUID)) {
                        player.inventory.setItemStack(null);
                    }
                }

                player.closeScreen();
            }
        }
        return super.onDroppedByPlayer(itemStack, player);
    }

    public boolean isEmpty(ItemStack itemStack) {
        return NBTInventory.isInventoryEmpty(getInventoryTag(itemStack));
    }

    public NBTTagCompound getInventoryTag(ItemStack itemStack) {
        NBTTagCompound baseTag = getOrInitBaseTag(itemStack);

        if (!baseTag.hasKey(TAG_KEY_INVENTORY)) baseTag.setTag(TAG_KEY_INVENTORY, new NBTTagCompound());

        return baseTag.getCompoundTag(TAG_KEY_INVENTORY);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer player, List toolTip, boolean isAdvanced) {
        super.addInformation(itemStack, player, toolTip, isAdvanced);

        String openCloseLineColor = EnumChatFormatting.GRAY.toString();
        if (isOpen(itemStack)) {
            toolTip.add(
                    openCloseLineColor
                            + StatCollector.translateToLocalFormatted("spiceoflife.tooltip.to.close.food.container"));

        } else toolTip.add(
                openCloseLineColor
                        + StatCollector.translateToLocalFormatted("spiceoflife.tooltip.to.open.food.container"));
    }

    @Override
    public IIcon getIconIndex(ItemStack itemStack) {
        if (isOpen(itemStack)) {
            return isEmpty(itemStack) ? iconOpenEmpty : iconOpenFull;
        }
        return super.getIconIndex(itemStack);
    }

    @Override
    public IIcon getIcon(ItemStack itemStack, int renderPass) {
        return getIconIndex(itemStack);
    }

    @Override
    public boolean onItemUseFirst(ItemStack itemStack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        if (!world.isRemote && isOpen(itemStack)) {
            IInventory inventoryHit = InventoryHelper.getInventoryAtLocation(world, x, y, z);
            if (inventoryHit != null && inventoryHit.isUseableByPlayer(player)) {
                tryDumpFoodInto(itemStack, inventoryHit, player);
                tryPullFoodFrom(itemStack, inventoryHit, player);

                return true;
            }
        }
        return super.onItemUseFirst(itemStack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public EnumAction getItemUseAction(ItemStack itemStack) {
        if (canBeEatenFrom(itemStack)) return EnumAction.eat;
        else return EnumAction.none;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (player.isSneaking()) {
            setIsOpen(itemStack, !isOpen(itemStack));
        } else if (canPlayerEatFrom(player, itemStack)) {
            player.setItemInUse(itemStack, getMaxItemUseDuration(itemStack));
        } else if (!isOpen(itemStack)) {
            GuiHelper.openGuiOfItemStack(player, itemStack);
            setIsOpen(itemStack, true);
        }
        return super.onItemRightClick(itemStack, world, player);
    }

    @Override
    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
        if (entityLiving.worldObj.isRemote && ModConfig.LEFT_CLICK_OPENS_FOOD_CONTAINERS
                && MiscHelper.isMouseOverNothing()) {
            PacketHandler.channel.sendToServer(new PacketToggleFoodContainer());
            return true;
        }

        return super.onEntitySwing(entityLiving, stack);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack itemStack) {
        return 32;
    }

    @Override
    public ItemStack onEaten(ItemStack itemStack, World world, EntityPlayer player) {
        IInventory inventory = getInventory(itemStack);
        int slotWithBestFood = MealPrioritizationHelper.findBestFoodForPlayerToEat(player, inventory);
        ItemStack foodToEat = inventory.getStackInSlot(slotWithBestFood);
        if (foodToEat != null) {
            ItemStack result = foodToEat.onFoodEaten(world, player);
            result = ForgeEventFactory.onItemUseFinish(player, foodToEat, 32, result);

            if (result == null || result.stackSize <= 0) result = null;

            inventory.setInventorySlotContents(slotWithBestFood, result);
        }
        return super.onEaten(itemStack, world, player);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister iconRegister) {
        super.registerIcons(iconRegister);
        iconOpenEmpty = iconRegister.registerIcon(getIconString() + "_open_empty");
        iconOpenFull = iconRegister.registerIcon(getIconString() + "_open_full");
    }
}
