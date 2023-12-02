package squeek.spiceoflife.gui;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import squeek.spiceoflife.foodtracker.FoodEaten;
import squeek.spiceoflife.foodtracker.FoodHistory;
import squeek.spiceoflife.foodtracker.ProgressInfo;
import squeek.spiceoflife.gui.widget.WidgetButtonNextPage;
import squeek.spiceoflife.gui.widget.WidgetFoodEaten;

@SideOnly(Side.CLIENT)
public class GuiScreenFoodJournal extends GuiScreen {

    public static final DecimalFormat dfOne = new DecimalFormat("#.#");
    protected static final int numPerPage = 5;
    private static final ResourceLocation bookGuiTextures = new ResourceLocation("textures/gui/book.png");
    public ItemStack hoveredStack = null;
    protected List<WidgetFoodEaten> foodEatenWidgets = new ArrayList<>();
    protected int pageNum = 0;
    protected int numPages;
    protected GuiButton buttonNextPage;
    protected GuiButton buttonPrevPage;
    private int bookImageWidth = 192;
    private int bookImageHeight = 192;

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();

        this.buttonList.add(
                buttonPrevPage = new WidgetButtonNextPage(
                        1,
                        (this.width - this.bookImageWidth) / 2 + 38,
                        2 + 154,
                        false));
        this.buttonList.add(
                buttonNextPage = new WidgetButtonNextPage(
                        2,
                        (this.width - this.bookImageWidth) / 2 + 120,
                        2 + 154,
                        true));

        foodEatenWidgets.clear();
        FoodHistory foodHistory = FoodHistory.get(mc.thePlayer);
        Set<FoodEaten> recent = new HashSet<>(foodHistory.getRecentHistory());
        foodHistory.getFullHistory().stream().sorted(Comparator.comparing(i -> i.itemStack.getDisplayName()))
                .forEach(f -> foodEatenWidgets.add(new WidgetFoodEaten(f, recent.contains(f))));

        numPages = 1 + (int) Math.ceil((float) foodEatenWidgets.size() / numPerPage);

        updateButtons();
    }

    private void updateButtons() {
        this.buttonNextPage.visible = this.pageNum < this.numPages - 1;
        this.buttonPrevPage.visible = this.pageNum > 0;
    }

    public void drawHalfShank(int x, int y) {
        GL11.glColor3f(1, 1, 1);
        mc.getTextureManager().bindTexture(Gui.icons);
        GuiUtils.drawTexturedModalRect(this, x, y, 16, 27, 9, 9);
        GuiUtils.drawTexturedModalRect(this, x, y, 61, 27, 9, 9);
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

    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(bookGuiTextures);
        int x = (this.width - this.bookImageWidth) / 2;
        int y = 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.bookImageWidth, this.bookImageHeight);

        GL11.glDisable(GL11.GL_LIGHTING);
        for (Object objButton : this.buttonList) {
            ((GuiButton) objButton).drawButton(mc, mouseX, mouseY);
        }

        int leftMargin = 32;
        int rightMargin = 39;
        int leftPadding = 4;
        int leftOffset = leftMargin + leftPadding;

        FoodHistory foodHistory = FoodHistory.get(mc.thePlayer);
        if (pageNum == 0) {
            final ProgressInfo progressInfo = foodHistory.getProgressInfo();
            final int foodsEaten = progressInfo.foodsPointsEaten;
            final int extraHearts = progressInfo.milestonesAchieved() * ProgressInfo.HEARTS_PER_MILESTONE;
            final int foodsUntilNextMilestone = progressInfo.foodPointsUntilNextMilestone();

            int localX = x + leftMargin;
            int drawWidth = (bookImageWidth - rightMargin) - leftMargin; // width in which text can be drawn, so it
                                                                         // doesn't step over the edges
            int localY = y + 32;
            int blackColor = 0x000000;

            String foodHistoryTitle = I18n.format("spiceoflife.gui.food_history.title");
            int foodTitleWidth = fontRendererObj.getStringWidth(foodHistoryTitle);
            GuiUtils.drawText(
                    fontRendererObj,
                    foodHistoryTitle,
                    localX + drawWidth / 2 - foodTitleWidth / 2,
                    localY,
                    Color.BLUE);

            int verticalIndent = 8;

            localX = x + leftOffset;

            int hungerOffset = 13; // 9 - width of hunger icon, 4 - small offset
            int widthMinusPadding = drawWidth - leftPadding;

            localY += verticalIndent + fontRendererObj.FONT_HEIGHT * 2;
            drawHalfShank(localX, localY);
            String worthEaten = I18n.format("spiceoflife.gui.food_history.worth_eaten");
            List<String> list = splitWithDifWidth(
                    worthEaten + " " + foodsEaten,
                    widthMinusPadding - hungerOffset,
                    widthMinusPadding);
            for (int i = 0; i < list.size(); i++) {
                GuiUtils.drawText(
                        fontRendererObj,
                        list.get(i),
                        localX + (i == 0 ? hungerOffset : 0),
                        localY,
                        blackColor);
                localY += fontRendererObj.FONT_HEIGHT;
            }

            localY += verticalIndent;
            String bonusHearts = I18n.format("spiceoflife.gui.food_history.bonus_hearts");
            list = GuiUtils
                    .listFormattedStringToWidth(fontRendererObj, bonusHearts + " " + extraHearts, widthMinusPadding);
            for (String s : list) {
                GuiUtils.drawText(fontRendererObj, s, localX, localY, blackColor);
                localY += fontRendererObj.FONT_HEIGHT;
            }

            localY += verticalIndent;
            drawHalfShank(localX, localY);
            String nextHeart = I18n.format("spiceoflife.gui.food_history.next_heart");
            list = splitWithDifWidth(
                    nextHeart + " " + foodsUntilNextMilestone,
                    widthMinusPadding - hungerOffset,
                    widthMinusPadding);
            for (int i = 0; i < list.size(); i++) {
                GuiUtils.drawText(
                        fontRendererObj,
                        list.get(i),
                        localX + (i == 0 ? hungerOffset : 0),
                        localY,
                        blackColor);
                localY += fontRendererObj.FONT_HEIGHT;
            }
        } else {
            int startIndex = Math.max(0, (pageNum - 1) * numPerPage);
            int endIndex = startIndex + numPerPage;
            int totalNum = foodEatenWidgets.size();
            if (totalNum > 0) {
                int firstItemNum = startIndex + 1;
                int lastItemNum = Math.min(totalNum, endIndex);
                String pageIndicator = StatCollector.translateToLocalFormatted(
                        "spiceoflife.gui.items.on.page",
                        firstItemNum,
                        lastItemNum,
                        totalNum);
                fontRendererObj.drawString(
                        pageIndicator,
                        x + this.bookImageWidth - this.fontRendererObj.getStringWidth(pageIndicator) - 44,
                        y + 16,
                        0);
            }

            String numFoodsEatenAllTime = Integer.toString(foodHistory.totalFoodsEatenAllTime);
            int allTimeW = fontRendererObj.getStringWidth(numFoodsEatenAllTime);
            int allTimeX = width / 2 - allTimeW / 2 - 5;
            int allTimeY = y + 158;
            fontRendererObj.drawString(numFoodsEatenAllTime, allTimeX, allTimeY, 0xa0a0a0);

            if (foodEatenWidgets.size() > 0) {
                GL11.glPushMatrix();
                int foodEatenIndex = startIndex;
                while (foodEatenIndex < foodEatenWidgets.size() && foodEatenIndex < endIndex) {
                    WidgetFoodEaten foodEatenWidget = foodEatenWidgets.get(foodEatenIndex);
                    int localX = x + leftOffset;
                    int localY = y + 32 + (int) ((foodEatenIndex - startIndex) * fontRendererObj.FONT_HEIGHT * 2.5f);
                    foodEatenWidget.draw(localX, localY);
                    if (foodEatenWidget.foodEaten.itemStack != null)
                        drawItemStack(foodEatenWidget.foodEaten.itemStack, localX, localY);

                    foodEatenIndex++;
                }
                GL11.glPopMatrix();

                hoveredStack = null;
                foodEatenIndex = startIndex;
                while (foodEatenIndex < foodEatenWidgets.size() && foodEatenIndex < endIndex) {
                    WidgetFoodEaten foodEatenWidget = foodEatenWidgets.get(foodEatenIndex);

                    int localX = x + leftOffset;
                    int localY = y + 32 + (int) ((foodEatenIndex - startIndex) * fontRendererObj.FONT_HEIGHT * 2.5f);

                    if (isMouseInsideBox(mouseX, mouseY, localX, localY, 16, 16)) {
                        hoveredStack = foodEatenWidget.foodEaten.itemStack;
                        if (hoveredStack != null) this.renderToolTip(hoveredStack, mouseX, mouseY);
                    } else if (isMouseInsideBox(
                            mouseX,
                            mouseY,
                            localX + WidgetFoodEaten.PADDING_LEFT,
                            localY,
                            foodEatenWidget.width(),
                            16)) {
                                List<String> toolTipStrings = new ArrayList<>();
                                if (foodEatenWidget.eatenRecently) {
                                    toolTipStrings.add(
                                            EnumChatFormatting.DARK_PURPLE.toString() + EnumChatFormatting.ITALIC
                                                    + "Eaten Recently");
                                } else {
                                    toolTipStrings.add(
                                            EnumChatFormatting.DARK_AQUA.toString() + EnumChatFormatting.ITALIC
                                                    + "Not Eaten Recently!");
                                }
                                this.drawHoveringText(toolTipStrings, mouseX, mouseY, fontRendererObj);
                            }

                    foodEatenIndex++;
                }
            } else {
                this.fontRendererObj.drawSplitString(
                        StatCollector.translateToLocal("spiceoflife.gui.no.recent.food.eaten"),
                        x + 36,
                        y + 16 + 16,
                        116,
                        0x404040);
            }

            if (isMouseInsideBox(mouseX, mouseY, allTimeX, allTimeY, allTimeW, fontRendererObj.FONT_HEIGHT)) {
                this.drawHoveringText(
                        Collections.singletonList(StatCollector.translateToLocal("spiceoflife.gui.alltime.food.eaten")),
                        mouseX,
                        mouseY,
                        fontRendererObj);
            }
        }

        GL11.glDisable(GL11.GL_LIGHTING);
    }

    protected void drawItemStack(ItemStack par1ItemStack, int par2, int par3) {
        GL11.glTranslatef(0.0F, 0.0F, 32.0F);
        this.zLevel = 200.0F;
        itemRender.zLevel = 200.0F;
        FontRenderer font = null;
        if (par1ItemStack != null) font = par1ItemStack.getItem().getFontRenderer(par1ItemStack);
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
}
