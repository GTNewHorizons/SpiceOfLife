package squeek.spiceoflife.foodtracker.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import squeek.spiceoflife.foodtracker.FoodHistory;
import squeek.spiceoflife.foodtracker.FoodTracker;
import squeek.spiceoflife.foodtracker.ProgressInfo;

import java.util.Arrays;
import java.util.List;

public class CommandFoodList extends CommandBase {
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender commandSender) {
        return true;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender commandSender, String[] curArgs) {
        if (curArgs.length == 1)
            return Arrays.asList("size", "sync");
        else if (curArgs.length == 2)
            return getListOfStringsMatchingLastWord(curArgs, MinecraftServer.getServer().getAllUsernames());
        else
            return null;
    }

    @Override
    public int compareTo(Object obj) {
        if (obj instanceof ICommand)
            return super.compareTo((ICommand) obj);
        else
            return 0;
    }

    @Override
    public int hashCode() {
        return getCommandName().hashCode();
    }

    @Override
    public String getCommandName() {
        return "foodlist";
    }

    @Override
    public String getCommandUsage(ICommandSender commandSender) {
        return "/foodlist <size|sync> [player]";
    }

    @Override
    public void processCommand(ICommandSender commandSender, String[] args) {
        if (args.length > 0) {
            final boolean isOp = commandSender.canCommandSenderUseCommand(4, "targetOtherPlayer");
            final EntityPlayerMP player = (isOp && args.length > 1) ? getPlayer(commandSender, args[1]) : getCommandSenderAsPlayer(commandSender);
            final FoodHistory foodHistory = FoodHistory.get(player);

            if (args[0].equals("size")) {
                final ProgressInfo progressInfo = foodHistory.getProgressInfo();
                final int foodsEaten = progressInfo.foodsEaten;
                final int milestone = progressInfo.milestonesAchieved();
                final int foodsUntilNextMilestone = progressInfo.foodsUntilNextMilestone();

                commandSender.addChatMessage(new ChatComponentText("" + EnumChatFormatting.BOLD + EnumChatFormatting.DARK_AQUA + player.getDisplayName() + "'s" + EnumChatFormatting.RESET + " food stats:"));
                commandSender.addChatMessage(new ChatComponentText("Food Eaten: " + foodsEaten));
                commandSender.addChatMessage(new ChatComponentText("Bonus Hearts: " + (milestone * ProgressInfo.HEARTS_PER_MILESTONE)));
                commandSender.addChatMessage(new ChatComponentText("Foods until next bonus heart: " + foodsUntilNextMilestone));
                return;
            } else if (args[0].equals("sync")) {
                FoodTracker.syncFoodHistory(foodHistory);
                commandSender.addChatMessage(new ChatComponentText("Synced food history for " + player.getDisplayName()));
                return;
            }
        }
        throw new WrongUsageException(getCommandUsage(commandSender));
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) || obj instanceof ICommand && compareTo(obj) == 0;
    }


}
