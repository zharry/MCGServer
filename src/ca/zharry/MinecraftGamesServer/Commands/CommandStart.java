package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandStart extends CommandBase {

    private final Cutscene cutscene;

    public CommandStart(ServerInterface<? extends PlayerInterface> server, Cutscene cutscene) {
        super(server);
        this.cutscene = cutscene;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean skipCutscene = false;
        if(args.length >= 1) {
            skipCutscene = Boolean.parseBoolean(args[0]);
        }
        if(skipCutscene) {
            this.cutscene.onStart();
            this.cutscene.onEnd();
        } else {
            this.cutscene.start();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String currArg = (args.length > 0 ? args[args.length - 1] : "").toLowerCase();
        if(args.length <= 1) {
            return Stream.of("false", "true")
                    .filter(v -> v.startsWith(currArg))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}