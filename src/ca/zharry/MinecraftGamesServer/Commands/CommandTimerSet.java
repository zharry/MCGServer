package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.Timer.Timer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandTimerSet implements CommandExecutor {

    private Timer timer;
    public CommandTimerSet() {};
    public CommandTimerSet(Timer timer) {
        this.timer = timer;
    }

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp())
            return false;

        try {
            int ticks = Integer.parseInt(args[0]);
            this.timer.set(ticks);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}