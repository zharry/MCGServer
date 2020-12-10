package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.Timer.Timer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandTimerResume implements CommandExecutor {

    private Timer timer;
    public CommandTimerResume() {};
    public CommandTimerResume(Timer timer) {
        this.timer = timer;
    }

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.timer.resume();
        return true;
    }
}