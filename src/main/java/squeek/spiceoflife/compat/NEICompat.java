package squeek.spiceoflife.compat;

import net.minecraft.item.ItemStack;

import codechicken.nei.search.TooltipFilter;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;

public class NEICompat {

    public static final boolean LOADED = Loader.isModLoaded("NotEnoughItems");

    @Optional.Method(modid = "NotEnoughItems")
    public static void updateTooltipCache(ItemStack stack) {
        TooltipFilter.putItem(stack);
    }
}
