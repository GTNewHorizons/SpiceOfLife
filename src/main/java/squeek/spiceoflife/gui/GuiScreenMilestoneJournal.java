package squeek.spiceoflife.gui;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import squeek.spiceoflife.foodtracker.FoodHistory;
import squeek.spiceoflife.foodtracker.ProgressInfo;
import squeek.spiceoflife.foodtracker.foodqueue.FixedSizeQueue;
import squeek.spiceoflife.gui.widget.WidgetButtonNextPage;

@SideOnly(Side.CLIENT)
public class GuiScreenMilestoneJournal extends GuiScreen {

    public static final DecimalFormat dfOne = new DecimalFormat("#.#");
    protected static final int numPerPage = 5;
    private static final ResourceLocation bookGuiTextures = new ResourceLocation("textures/gui/book.png");
    protected List<ItemStack> milestoneMenu = new ArrayList<>();
    protected int lastMilestone = -1;
    protected int pageNum = 0;
    protected int numPages;
    protected GuiButton buttonNextPage;
    protected GuiButton buttonPrevPage;
    private final int bookImageWidth = 192;
    private final int bookImageHeight = 192;

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();

        this.buttonList.add(
            buttonPrevPage = new WidgetButtonNextPage(1, (this.width - this.bookImageWidth) / 2 + 38, 2 + 154, false));
        this.buttonList.add(
            buttonNextPage = new WidgetButtonNextPage(2, (this.width - this.bookImageWidth) / 2 + 120, 2 + 154, true));

        updateMilestoneMenu();
        updateButtons();
    }

    private void updateButtons() {
        this.buttonNextPage.visible = this.pageNum < this.numPages - 1;
        this.buttonPrevPage.visible = this.pageNum > 0;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(bookGuiTextures);
        int x = (this.width - this.bookImageWidth) / 2;
        int y = 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.bookImageWidth, bookImageHeight);

        GL11.glDisable(GL11.GL_LIGHTING);
        for (Object objButton : this.buttonList) {
            ((GuiButton) objButton).drawButton(mc, mouseX, mouseY);
        }

        int leftMargin = 32;
        int rightMargin = 39;
        int leftPadding = 4;
        int leftOffset = leftMargin + leftPadding;

        int verticalIndent = 8;
        int drawWidth = (bookImageWidth - rightMargin) - leftMargin; // width in which text can be drawn, so it
        // doesn't step over the edges

        FoodHistory foodHistory = FoodHistory.get(mc.thePlayer);

        if (milestoneMenu.size() != foodHistory.getMilestoneTargets()
            .size()) {
            updateMilestoneMenu();
            updateButtons();
        } // Refreshes the food menu upon changing milestones.

        boolean challengeCompleted = milestoneMenu.isEmpty();

        if (pageNum == 0) {
            int currentMilestone = foodHistory.getMilestoneHistory()
                .size();
            final int extraHearts = currentMilestone * ProgressInfo.HEARTS_PER_MILESTONE;
            int blackColor = 0x000000;

            int localX = x + leftMargin;
            int localY = y + 32;

            String foodHistoryTitle = I18n.format("spiceoflife.gui.milestone_history.title");
            int foodTitleWidth = fontRendererObj.getStringWidth(foodHistoryTitle);
            GuiUtils.drawText(
                fontRendererObj,
                foodHistoryTitle,
                localX + drawWidth / 2 - foodTitleWidth / 2,
                localY,
                Color.red);

            localX = x + leftOffset;

            int hungerOffset = 13; // 9 - width of hunger icon, 4 - small offset
            int widthMinusPadding = drawWidth - leftPadding;

            localY += verticalIndent + fontRendererObj.FONT_HEIGHT * 2;

            String amountMilestone = I18n.format("spiceoflife.gui.milestone_history.milestone");
            List<String> list = GuiUtils.listFormattedStringToWidth(
                fontRendererObj,
                amountMilestone + " " + currentMilestone,
                widthMinusPadding);
            for (String s : list) {
                GuiUtils.drawText(fontRendererObj, s, localX, localY, blackColor);
                localY += fontRendererObj.FONT_HEIGHT;
            }

            localY += verticalIndent;

            drawHeart(localX, localY);

            String heartsEarned = I18n.format("spiceoflife.gui.bonus_hearts");
            list = splitWithDifWidth(
                heartsEarned + " " + extraHearts,
                widthMinusPadding - hungerOffset,
                widthMinusPadding);
            for (int i = 0; i < list.size(); i++) {
                GuiUtils
                    .drawText(fontRendererObj, list.get(i), localX + (i == 0 ? hungerOffset : 0), localY, blackColor);
                localY += fontRendererObj.FONT_HEIGHT;
            }

            localY += verticalIndent + fontRendererObj.FONT_HEIGHT;

            String statusHeader = I18n.format("spiceoflife.gui.milestone_history.status");
            GuiUtils.drawText(fontRendererObj, EnumChatFormatting.BOLD + statusHeader, localX, localY, Color.BLACK);

            localY += verticalIndent;

            String statusKey = challengeCompleted
                ? StatCollector.translateToLocal("spiceoflife.gui.milestone_history.statusComplete")
                : StatCollector.translateToLocal("spiceoflife.gui.milestone_history.statusInProgress");
            String currentStatus = I18n.format(statusKey);

            Color copper = new Color(180, 104, 77);
            GuiUtils
                .drawText(fontRendererObj, currentStatus, localX, localY, challengeCompleted ? Color.GREEN : copper);

            localY += verticalIndent + fontRendererObj.FONT_HEIGHT;

            final int minutesRemaining = (int) (foodHistory.ticksActive / 1200F);
            final String daysRemaining = String.format("%.1f", foodHistory.ticksActive / 24000F);
            String countdown = StatCollector.translateToLocalFormatted("spiceoflife.gui.expires.in.days", daysRemaining)
                + " ("
                + formatTime(minutesRemaining)
                + ")";
            list = GuiUtils.listFormattedStringToWidth(fontRendererObj, countdown, widthMinusPadding);
            for (String s : list) {
                GuiUtils.drawText(fontRendererObj, s, localX, localY, blackColor);
                localY += fontRendererObj.FONT_HEIGHT;
            }
        } else { // Menu Page
            int startIndex = Math.max(0, (pageNum - 1) * numPerPage);
            int endIndex = startIndex + numPerPage;

            int localX = x + leftOffset;

            String menuTitle = I18n.format("spiceoflife.gui.milestone_history.menu");
            int menuTitleWidth = fontRendererObj.getStringWidth(menuTitle);
            GuiUtils
                .drawText(fontRendererObj, menuTitle, localX + drawWidth / 2 - menuTitleWidth / 2, y + 32, Color.red);

            y += verticalIndent * 2;

            if (!challengeCompleted) {
                GL11.glPushMatrix();
                int menuIndex = startIndex;
                while (menuIndex < milestoneMenu.size() && menuIndex < endIndex) {
                    ItemStack menuItem = milestoneMenu.get(menuIndex);
                    int localY = y + 32 + (int) ((menuIndex - startIndex) * fontRendererObj.FONT_HEIGHT * 2.5f);
                    GuiUtils.drawText(fontRendererObj, menuItem.getDisplayName(), localX + 18, localY + 5, 0x666666);

                    drawItemStack(menuItem, localX, localY);

                    menuIndex++;
                }
                GL11.glPopMatrix();
            }
        }

        GL11.glDisable(GL11.GL_LIGHTING);
    }

    public void drawHeart(int x, int y) {
        GL11.glColor3f(1, 1, 1);
        mc.getTextureManager()
            .bindTexture(Gui.icons);
        GuiUtils.drawTexturedModalRect(this, x, y - 1, 52, 9, 9, 9);
        GuiUtils.drawTexturedModalRect(this, x, y - 1, 52, 0, 9, 9);
    }

    public List<String> splitWithDifWidth(String text, int firstLineWidth, int otherLineWidth) {
        List<String> out = new ArrayList<>();

        List<String> strings = GuiUtils.listFormattedStringToWidth(fontRendererObj, text, firstLineWidth);
        if (strings.isEmpty()) return out;

        String first = strings.get(0);
        out.add(first);

        String remaining = text.substring(first.length());

        if (!remaining.isEmpty()) {
            out.addAll(GuiUtils.listFormattedStringToWidth(fontRendererObj, remaining, otherLineWidth));
        }

        return out;
    }

    public String formatTime(double minutes) {
        int totalMinutes = (int) minutes;
        int hours = totalMinutes / 60;
        int remainingMinutes = totalMinutes % 60;

        if (hours == 0)
            return StatCollector.translateToLocalFormatted("spiceoflife.gui.expires.in.minutes", remainingMinutes);

        return StatCollector.translateToLocalFormatted("spiceoflife.gui.expires.in.hours", hours, remainingMinutes);
    }

    protected void drawItemStack(ItemStack par1ItemStack, int par2, int par3) {
        GL11.glTranslatef(0.0F, 0.0F, 32.0F);
        this.zLevel = 200.0F;
        itemRender.zLevel = 200.0F;
        FontRenderer font = null;
        if (par1ItemStack != null) font = par1ItemStack.getItem()
            .getFontRenderer(par1ItemStack);
        if (font == null) font = fontRendererObj;
        itemRender.renderItemAndEffectIntoGUI(font, this.mc.getTextureManager(), par1ItemStack, par2, par3);
        this.zLevel = 0.0F;
        itemRender.zLevel = 0.0F;
    }

    public static boolean isMouseInsideBox(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        super.actionPerformed(button);

        if (button.enabled) {
            if (button.id == 1) {
                this.pageNum--;
            } else if (button.id == 2) {
                this.pageNum++;
            }

            updateButtons();
        }
    }

    public void updateMilestoneMenu() {
        milestoneMenu.clear();
        FixedSizeQueue milestoneTargets = FoodHistory.get(mc.thePlayer)
            .getMilestoneTargets();

        milestoneTargets.forEach(target -> milestoneMenu.add(target.itemStack));

        numPages = 1 + (int) Math.ceil((float) milestoneMenu.size() / numPerPage);
    }
}
