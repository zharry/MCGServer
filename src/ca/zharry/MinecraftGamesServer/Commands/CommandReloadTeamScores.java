package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandReloadTeamScores implements CommandExecutor {

    public ServerInterface server;

    public CommandReloadTeamScores(ServerInterface server) {
        this.server = server;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp())
            return false;

        try {
            server.reloadTeamsAndPlayers();

            sender.sendMessage("Team and players reloaded");
        } catch(Exception e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Reload failed: " + e.toString());
        }
        return true;
    }
}
