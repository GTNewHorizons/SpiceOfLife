package squeek.spiceoflife.inventory;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import squeek.spiceoflife.helpers.GuiHelper;
import squeek.spiceoflife.items.ItemFoodContainer;

public class ContainerFoodContainer extends ContainerGeneric {

    public int slotsX;
    public int slotsY;
    protected FoodContainerInventory foodContainerInventory;
    private final int heldSlotId;
    private final InventoryPlayer playerInventory;
    private final UUID cachedUUID;

    public ContainerFoodContainer(InventoryPlayer playerInventory, FoodContainerInventory foodContainerInventory) {
        super(foodContainerInventory);
        this.foodContainerInventory = foodContainerInventory;
        this.heldSlotId = playerInventory.currentItem;
        this.playerInventory = playerInventory;

        ItemStack stack = getItemStack();
        UUID uuid = null;
        if (stack != null && stack.getItem() instanceof ItemFoodContainer) {
            uuid = ((ItemFoodContainer) stack.getItem()).getUUID(stack);
        }
        this.cachedUUID = uuid;

        slotsX = (int) (GuiHelper.STANDARD_GUI_WIDTH / 2f
            - (inventory.getSizeInventory() * GuiHelper.STANDARD_SLOT_WIDTH / 2f));
        slotsY = 19;

        this.addSlotsOfType(SlotFiltered.class, inventory, slotsX, slotsY);
        this.addPlayerInventorySlots(playerInventory, 51);
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        if (cachedUUID != null) {
            // the client could have a different ItemStack than the one the
            // container was initialized with (due to server syncing), so
            // we need to find the new one
            if (player.worldObj.isRemote) {
                setFoodContainerItemStack(findFoodContainerWithUUID(getUUID()));
            }

        }

        ItemStack stack = getItemStack();
        if (stack != null && stack.getItem() instanceof ItemFoodContainer) {
            ((ItemFoodContainer) stack.getItem()).setIsOpen(stack, false);
        }

        super.onContainerClosed(player);
    }

    public void setFoodContainerItemStack(ItemStack itemStack) {
        foodContainerInventory.itemStackFoodContainer = itemStack;
    }

    public ItemStack findFoodContainerWithUUID(UUID uuid) {
        if (uuid == null) return null;

        for (ItemStack stack : playerInventory.mainInventory) {
            if (isFoodContainerWithUUID(stack, uuid)) {
                return stack;
            }
        }
        return null;
    }

    public UUID getUUID() {
        return cachedUUID;
    }

    public ItemStack getItemStack() {
        return foodContainerInventory.itemStackFoodContainer;
    }

    public boolean isFoodContainerWithUUID(ItemStack itemStack, UUID uuid) {
        if (uuid == null) return false;

        return itemStack != null && itemStack.getItem() instanceof ItemFoodContainer
            && uuid.equals(((ItemFoodContainer) itemStack.getItem()).getUUID(itemStack));
    }

    @Override
    public ItemStack slotClick(int slotNum, int mouseButton, int modifier, EntityPlayer player) {
        // Don't allow picking up item currently opened
        if (slotNum - 9 * 3 - foodContainerInventory.getSizeInventory() == heldSlotId) return null;

        // make sure the correct ItemStack instance is always used when the player is moving
        // the food container around while they have it open
        ItemStack putDownStack = player.inventory.getItemStack();
        ItemStack pickedUpStack = super.slotClick(slotNum, mouseButton, modifier, player);

        if (isFoodContainerWithUUID(pickedUpStack, getUUID())) {
            setFoodContainerItemStack(pickedUpStack);
        } else if (slotNum >= 0 && isFoodContainerWithUUID(putDownStack, getUUID())
            && isFoodContainerWithUUID(getSlot(slotNum).getStack(), getUUID())) {
                setFoodContainerItemStack(getSlot(slotNum).getStack());
            }

        return pickedUpStack;
    }
}
