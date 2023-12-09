package squeek.spiceoflife.gui;

import java.awt.Color;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

public class GuiUtils {

    public static void drawText(FontRenderer fontRenderer, String text, int x, int y, Color color) {
        fontRenderer.drawString(text, x, y, color.getRGB());
    }

    public static void drawText(FontRenderer fontRenderer, String text, int x, int y, int argbColor) {
        fontRenderer.drawString(text, x, y, argbColor);
    }

    public static void drawCenteredText(FontRenderer fontRenderer, String text, int centerX, int y, int argbColor) {
        fontRenderer.drawString(text, centerX - fontRenderer.getStringWidth(text) / 2, y, argbColor);
    }

    @SuppressWarnings("unchecked")
    public static List<String> listFormattedStringToWidth(FontRenderer fontRenderer, String text, int requiredWidth) {
        return fontRenderer.listFormattedStringToWidth(text, requiredWidth);
    }

    public static void drawTexturedModalRect(Gui gui, int x, int y, int u, int v, int width, int height) {
        gui.drawTexturedModalRect(x, y, u, v, width, height);
    }
}
