package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandCutscene implements TabExecutor {

	public final ServerInterface server;

	public CommandCutscene(ServerInterface server) {
		this.server = server;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length < 1) {
			return false;
		}

		Cutscene cutscene = server.currentCutscene;
		if(cutscene == null) {
			sender.sendMessage(ChatColor.RED + "No cutscenes are playing");
			return true;
		}

		switch(args[0].toLowerCase()) {
			case "seek":
				if(args.length < 2) {
					return false;
				}
				try {
					cutscene.seek(Timer.parseTimeTicks(args[1]));
				} catch(NumberFormatException e) {
					sender.sendMessage(ChatColor.RED + "Invalid time: " + args[1]);
					return true;
				}
				break;
			case "speed":
				if(args.length < 2) {
					return false;
				}
				try {
					cutscene.setSpeed(Double.parseDouble(args[1]));
				} catch(NumberFormatException e) {
					sender.sendMessage(ChatColor.RED + "Invalid speed: " + args[1]);
					return true;
				}
				break;
			case "skip":
				cutscene.end();
				break;
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		String currArg = (args.length > 0 ? args[args.length - 1] : "").toLowerCase();
		if(args.length <= 1) {
			return Stream.of("seek", "skip", "speed")
					.filter(t -> t.startsWith(currArg))
					.collect(Collectors.toList());
		}
		if(args.length == 2) {
			if(currArg.length() == 0) {
				if(args[0].equalsIgnoreCase("speed")) {
					return Collections.singletonList("1");
				} else if(args[0].equalsIgnoreCase("seek")) {
					return Collections.singletonList("0");
				}
			}
		}
		return new ArrayList<>();
	}
}
