package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.Servers.ServerLobby;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandLobbySetNextGame implements CommandExecutor {

    private ServerLobby server;
    private Timer timer;
    public CommandLobbySetNextGame() {};
    public CommandLobbySetNextGame(ServerLobby server, Timer timer) {
        this.server = server;
        this.timer = timer;
    }

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp())
             return false;

        server.nextMinigame = args[0];
        timer.start();
        return true;
    }
}