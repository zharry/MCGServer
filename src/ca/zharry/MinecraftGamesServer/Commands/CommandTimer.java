package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.Timer.Timer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandTimer implements TabExecutor {

	public final Timer[] timers;

	public CommandTimer(Timer... timers) {
		this.timers = timers;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length < 1) {
			return false;
		}
		int argsOffset = 0;
		long setTime = 0;
		if(args[0].equalsIgnoreCase("set")) {
			argsOffset = 1;
			if(args.length < 2) {
				return false;
			}
			String value = args[1];
			if(value.equalsIgnoreCase("default")) {
				setTime = -1;
			} else {
				try {
					setTime = Timer.parseTimeTicks(value);
				} catch(NumberFormatException e) {
					sender.sendMessage(ChatColor.RED + "Time value is invalid: " + value);
					return true;
				}
			}
		}

		Timer timer = null;
		if(args.length >= 2 + argsOffset) {
			for(Timer t : timers) {
				if(t.name.equalsIgnoreCase(args[1 + argsOffset])) {
					timer = t;
					break;
				}
			}
		} else {
			for(Timer t : timers) {
				if(!t.isPaused()) {
					timer = t;
					break;
				}
			}
			if(timer == null) {
				timer = timers[0];
			}
		}

		if(timer == null) {
			sender.sendMessage(ChatColor.RED + "Timer not found");
			return true;
		}

		switch(args[0].toLowerCase()) {
			case "start":
				timer.start();
				break;
			case "end":
				timer.end();
				break;
			case "cancel":
				timer.cancel();
				break;
			case "pause":
				timer.pause();
				break;
			case "resume":
				timer.resume();
				break;
			case "set":
				timer.set(setTime == -1 ? timer.defaultTicks : setTime);
				break;
		}

		sender.sendMessage("Timer '" + timer.name + "' set to " + timer);

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		String currArg = (args.length > 0 ? args[args.length - 1] : "").toLowerCase();
		if(args.length <= 1) {
			return Stream.of("start", "end", "cancel", "pause", "resume", "set")
					.filter(c -> c.startsWith(currArg))
					.sorted()
					.collect(Collectors.toList());
		}
		int argsOffset = 0;
		if(args[0].equalsIgnoreCase("set")) {
			argsOffset = 1;
			if(args.length == 2) {
				return Stream.of("default")
						.filter(t -> t.toLowerCase().startsWith(currArg))
						.collect(Collectors.toList());
			}
		}
		if(args.length == 2 + argsOffset) {
			return Arrays.stream(timers)
					.map(t -> t.name)
					.filter(t -> t.toLowerCase().startsWith(currArg))
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
}
