package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.Timer.Timer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandTimerPause implements CommandExecutor {

    private Timer timer;
    public CommandTimerPause() {};
    public CommandTimerPause(Timer timer) {
        this.timer = timer;
    }

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp())
            return false;

        this.timer.pause();
        return true;
    }
}