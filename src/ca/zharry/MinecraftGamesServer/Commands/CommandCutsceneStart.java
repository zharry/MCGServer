package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandCutsceneStart implements CommandExecutor {

    private Cutscene cutscene;

    public CommandCutsceneStart() {
    }

    public CommandCutsceneStart(Cutscene cutscene) {
        this.cutscene = cutscene;
    }

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp())
            return false;

        this.cutscene.start();
        return true;
    }
}