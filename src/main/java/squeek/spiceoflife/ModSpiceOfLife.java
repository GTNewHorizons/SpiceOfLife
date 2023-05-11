package squeek.spiceoflife;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.relauncher.Side;
import squeek.spiceoflife.foodtracker.FoodLists;
import squeek.spiceoflife.foodtracker.FoodModifier;
import squeek.spiceoflife.foodtracker.FoodTracker;
import squeek.spiceoflife.foodtracker.commands.CommandFoodList;
import squeek.spiceoflife.foodtracker.commands.CommandResetHistory;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroupConfig;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroupRegistry;
import squeek.spiceoflife.gui.TooltipHandler;
import squeek.spiceoflife.helpers.GuiHelper;
import squeek.spiceoflife.network.PacketHandler;

@Mod(modid = ModInfo.MODID, version = ModInfo.VERSION, dependencies = "required-after:AppleCore")
public class ModSpiceOfLife {

    public static final Logger Log = LogManager.getLogger(ModInfo.MODID);

    @Instance(ModInfo.MODID)
    public static ModSpiceOfLife instance;

    public File sourceFile;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        sourceFile = event.getSourceFile();
        ModConfig.init(event.getSuggestedConfigurationFile());
        ModContent.registerItems();
        if (!Loader.isModLoaded("dreamcraft")) {
            ModContent.registerRecipes();
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        GuiHelper.init();
        FoodTracker foodTracker = new FoodTracker();
        FMLCommonHandler.instance().bus().register(foodTracker);
        MinecraftForge.EVENT_BUS.register(foodTracker);
        MinecraftForge.EVENT_BUS.register(new FoodModifier());

        if (event.getSide() == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(new TooltipHandler());
        }

        // need to make sure that the packet types get registered before packets are received
        PacketHandler.PacketType.values();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        FoodGroupConfig.load();
        FoodLists.setUp();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        FoodGroupRegistry.setInStone();
        event.registerServerCommand(new CommandResetHistory());
        event.registerServerCommand(new CommandFoodList());
    }
}
