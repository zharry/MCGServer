package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerLobby;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandLobbySetNextGame implements TabExecutor {

    private ServerLobby server;
    private Timer timer;

    public CommandLobbySetNextGame(ServerLobby server, Timer timer) {
        this.server = server;
        this.timer = timer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length < 1) {
            return false;
        }
        if(!MCGMain.getMinigames().contains(args[0])) {
            sender.sendMessage(ChatColor.RED + "Minigame " + args[0] + " does not exist");
            return true;
        }
        server.nextMinigame = args[0];
        timer.start();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String currArg = (args.length > 0 ? args[args.length - 1] : "").toLowerCase();
        if(args.length <= 1) {
            return MCGMain.getMinigames().stream()
                    .filter(g -> g.startsWith(currArg))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}