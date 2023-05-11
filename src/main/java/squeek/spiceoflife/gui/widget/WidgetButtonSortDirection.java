package squeek.spiceoflife.gui.widget;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import squeek.spiceoflife.ModInfo;

@SideOnly(Side.CLIENT)
public class WidgetButtonSortDirection extends GuiButton {

    private static final ResourceLocation modIcons = new ResourceLocation(
            ModInfo.MODID.toLowerCase(Locale.ROOT),
            "textures/icons.png");

    /**
     * True for sorted descending (newest first), false for sorted ascending (oldest first).
     */
    public boolean sortDesc;

    public WidgetButtonSortDirection(int id, int x, int y, boolean sortDesc) {
        super(id, x, y, 11, 8, "");
        this.sortDesc = sortDesc;
    }

    /**
     * Draws this button to the screen.
     */
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            boolean isHovered = mouseX >= this.xPosition && mouseY >= this.yPosition
                    && mouseX < this.xPosition + this.width
                    && mouseY < this.yPosition + this.height;
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(modIcons);
            int x = 0;
            int y = 0;

            if (isHovered) {
                x += this.width;
            }

            if (!sortDesc) {
                x += this.width * 2;
            }

            this.drawTexturedModalRect(this.xPosition, this.yPosition, x, y, width, height);
        }
    }
}
