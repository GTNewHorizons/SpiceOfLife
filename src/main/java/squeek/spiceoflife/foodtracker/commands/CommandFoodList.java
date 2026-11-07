package squeek.spiceoflife.foodtracker.commands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import squeek.spiceoflife.ModConfig;
import squeek.spiceoflife.foodtracker.FoodHistory;
import squeek.spiceoflife.foodtracker.FoodTracker;
import squeek.spiceoflife.foodtracker.ProgressInfo;
import squeek.spiceoflife.foodtracker.foodqueue.FixedSizeQueue;

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
        if (curArgs.length == 1) return Arrays.asList("size", "sync");
        else if (curArgs.length == 2) return getListOfStringsMatchingLastWord(
            curArgs,
            MinecraftServer.getServer()
                .getAllUsernames());
        else return null;
    }

    @Override
    public int compareTo(Object obj) {
        if (obj instanceof ICommand) return super.compareTo((ICommand) obj);
        else return 0;
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
            final EntityPlayerMP player = (isOp && args.length > 1) ? getPlayer(commandSender, args[1])
                : getCommandSenderAsPlayer(commandSender);
            final FoodHistory foodHistory = FoodHistory.get(player);

            if (args[0].equals("size")) {
                final ProgressInfo progressInfo = foodHistory.getProgressInfo();

                commandSender.addChatMessage(
                    new ChatComponentTranslation("spiceoflife.chat.foodstats", player.getDisplayName()));

                if (ModConfig.IRON_CHEF_MODE_ENABLE) {
                    final int milestonesAchieved = progressInfo.milestoneAwards;
                    final double timeRemaining = foodHistory.ticksActive / 24000F;

                    FixedSizeQueue currentChallenge = foodHistory.getMilestoneTargets();
                    boolean hasCompletedChallenge = currentChallenge.isEmpty() && currentChallenge.getMaxSize() == 0;

                    List<String> activeChallenge = foodHistory.getMilestoneTargets()
                        .stream()
                        .map(food -> food.itemStack.getDisplayName())
                        .collect(Collectors.toList());

                    commandSender.addChatMessage(
                        new ChatComponentTranslation("spiceoflife.chat.foodstats.bonus", milestonesAchieved));

                    commandSender.addChatMessage(
                        new ChatComponentTranslation(
                            "spiceoflife.chat.foodstats.time",
                            String.format("%.1f", timeRemaining)));

                    if (hasCompletedChallenge) commandSender
                        .addChatMessage(new ChatComponentTranslation("spiceoflife.chat.foodstats.complete"));
                    else if (activeChallenge.isEmpty())
                        commandSender.addChatMessage(new ChatComponentTranslation("spiceoflife.chat.foodstats.null"));
                    else commandSender.addChatMessage(
                        new ChatComponentTranslation("spiceoflife.chat.foodstats.active", activeChallenge));

                    return;
                } else {
                    final int foodsEaten = progressInfo.foodsPointsEaten;
                    final int milestone = progressInfo.milestonesAchieved();
                    final int foodsUntilNextMilestone = progressInfo.foodPointsUntilNextMilestone();

                    commandSender
                        .addChatMessage(new ChatComponentTranslation("spiceoflife.chat.foodstats.eaten", foodsEaten));
                    new ChatComponentTranslation(
                        "spiceoflife.chat.foodstats.bonus",
                        (milestone * ProgressInfo.HEARTS_PER_MILESTONE));
                    commandSender.addChatMessage(
                        new ChatComponentTranslation("spiceoflife.chat.foodstats.next", foodsUntilNextMilestone));
                    return;
                }
            } else if (args[0].equals("sync")) {
                FoodTracker.syncFoodHistory(foodHistory);
                commandSender.addChatMessage(
                    new ChatComponentTranslation("spiceoflife.chat.foodstats.sync", player.getDisplayName()));
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
