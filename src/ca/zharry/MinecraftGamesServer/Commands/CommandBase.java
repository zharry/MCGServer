package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CommandBase implements TabExecutor {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Subcommand {
		String name() default "";
		boolean reloadTeams() default false;
		int argsRequired() default 0;
		String[] params() default {};
		String desc() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface ArgTeamID {
		boolean allowDefaultTeam() default false;
		boolean allowNumericID() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface ArgPlayers {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface ArgInt {
		int def() default 0;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface ArgDouble {
		double def() default 0;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface ArgBoolean {
		boolean def() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface ArgMinigame {
		boolean wildcard() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface ArgChatColor {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface ArgVaarg {
	}

	public class SubcommandInfo {
		public Subcommand subcommand;
		public Class<?>[] args;
		public Annotation[][] argInfo;
		public Method method;
		public String subcommandName;
	}

	protected ServerInterface<? extends PlayerInterface> server;

	protected HashMap<String, SubcommandInfo> subcommands = new HashMap<>();

	public CommandBase(ServerInterface<? extends PlayerInterface> server) {
		this.server = server;
		for(Method method : getClass().getDeclaredMethods()) {
			Subcommand annotation = method.getAnnotation(Subcommand.class);
			if(annotation != null) {
				SubcommandInfo info = new SubcommandInfo();
				info.subcommand = annotation;
				info.args = Arrays.copyOfRange(method.getParameterTypes(), 1, method.getParameterCount());
				info.argInfo = Arrays.copyOfRange(method.getParameterAnnotations(), 1, method.getParameterCount());
				info.method = method;
				info.subcommandName = (annotation.name().length() == 0 ? method.getName() : annotation.name()).toLowerCase();
				subcommands.put(info.subcommandName, info);
			}
		}
	}

	public void sendUsage(CommandSender sender, String command, SubcommandInfo info) {
		StringBuilder usage = new StringBuilder();
		for(int i = 0; i < info.subcommand.params().length; ++i) {
			if(i < info.subcommand.argsRequired()) {
				usage.append(" <").append(info.subcommand.params()[i]).append('>');
			} else {
				usage.append(" [").append(info.subcommand.params()[i]).append(']');
			}
		}
		sender.sendMessage(ChatColor.GRAY + "Usage: /" + command + " " + info.subcommandName + usage + "\nDescription: " + info.subcommand.desc());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length < 1) {
			sender.sendMessage(ChatColor.RED + "A subcommand is required");
			return false;
		}
		SubcommandInfo info = subcommands.get(args[0].toLowerCase());
		if(info == null) {
			sender.sendMessage(ChatColor.RED + "Not a valid subcommand: " + args[0]);
			return false;
		}
		if(args.length - 1 < info.subcommand.argsRequired()) {
			sender.sendMessage(ChatColor.RED + "Missing arguments: required " + info.subcommand.argsRequired() + ", provided: " + (args.length - 1));
			sendUsage(sender, command.getName(), info);
			return true;
		}
		String[] partialArgs = Arrays.copyOfRange(args, 1, args.length);
		try {
			Object[] argParsed = new Object[info.args.length];
			for(int i = 0; i < argParsed.length; ++i) {
				String arg = null;
				if(i < partialArgs.length) {
					arg = partialArgs[i];
				}
				Annotation annotation = info.argInfo[i].length == 0 ? null : info.argInfo[i][0];
				if(annotation instanceof ArgInt) {
					if (arg == null) {
						argParsed[i] = ((ArgInt) annotation).def();
					} else {
						argParsed[i] = getInteger(arg);
					}
				} else if(annotation instanceof ArgDouble) {
					if (arg == null) {
						argParsed[i] = ((ArgDouble) annotation).def();
					} else {
						argParsed[i] = getDouble(arg);
					}
				} else if(annotation instanceof ArgBoolean) {
					if (arg == null) {
						argParsed[i] = ((ArgBoolean) annotation).def();
					} else {
						argParsed[i] = getBoolean(arg);
					}
				} else if(annotation instanceof ArgChatColor) {
					if (arg != null) {
						argParsed[i] = getColor(arg);
					}
				} else if(annotation instanceof ArgMinigame) {
					if(arg != null) {
						argParsed[i] = getMinigame(arg, ((ArgMinigame) annotation).wildcard());
					}
				} else if(annotation instanceof ArgPlayers) {
					if(arg != null) {
						argParsed[i] = getPlayer(arg, sender);
					}
				} else if(annotation instanceof ArgTeamID) {
					if(arg != null) {
						argParsed[i] = getTeamId(arg, ((ArgTeamID) annotation).allowDefaultTeam(), ((ArgTeamID) annotation).allowNumericID());
					}
				} else if(annotation instanceof ArgVaarg) {
					if(arg != null) {
						argParsed[i] = Arrays.copyOfRange(partialArgs, i, partialArgs.length);
						break;
					}
				}
			}

			info.method.invoke(this, Stream.concat(Stream.of(sender), Arrays.stream(argParsed)).toArray());

			if(info.subcommand.reloadTeams()) {
				server.reloadTeamsAndPlayers();
			}
			return true;
		} catch(CommandException e) {
			sender.sendMessage(ChatColor.RED + e.getMessage());
			sendUsage(sender, command.getName(), info);
			return true;
		} catch (InvocationTargetException e) {
			if(e.getCause() instanceof SQLException) {
				sender.sendMessage(ChatColor.RED + "SQL operation failed: " + e.toString());
				e.printStackTrace();
				return true;
			} else if(e.getCause() instanceof CommandException) {
				sender.sendMessage(ChatColor.RED + e.getMessage());
				return true;
			}

			sender.sendMessage(ChatColor.RED + e.getCause().toString());
			e.getCause().printStackTrace();
			return true;
		} catch (IllegalAccessException e) {
			sender.sendMessage(ChatColor.RED + e.toString());
			e.printStackTrace();
			return true;
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		String currArg = (args.length > 0 ? args[args.length - 1] : "").toLowerCase();
		if(args.length <= 1) {
			return subcommands.keySet().stream()
					.filter(p -> p.startsWith(currArg))
					.collect(Collectors.toList());
		} else {
			SubcommandInfo info = subcommands.get(args[0].toLowerCase());
			if(info != null) {
				String arg = args[args.length - 1];
				Annotation annotation = args.length - 2 >= info.argInfo.length || info.argInfo[args.length - 2].length == 0 ? null : info.argInfo[args.length - 2][0];
				if(annotation instanceof ArgInt) {
					return completeInteger(arg);
				} else if(annotation instanceof ArgDouble) {
					return completeDouble(arg);
				} else if(annotation instanceof ArgBoolean) {
					return completeBoolean(arg);
				} else if(annotation instanceof ArgChatColor) {
					return completeColor(arg);
				} else if(annotation instanceof ArgMinigame) {
					return completeMinigame(arg, ((ArgMinigame) annotation).wildcard());
				} else if(annotation instanceof ArgPlayers) {
					return completePlayer(arg);
				} else if(annotation instanceof ArgTeamID) {
					return completeTeam(arg, ((ArgTeamID) annotation).allowDefaultTeam());
				}
			}
		}
		return new ArrayList<>();
	}

	// Color ====================
	protected List<String> completeColor(String currArg) {
		return Arrays.stream(Arrays.copyOf(ChatColor.values(), 16))
				.map(e -> e.name().toLowerCase())
				.filter(c -> c.startsWith(currArg))
				.collect(Collectors.toList());
	}

	protected ChatColor getColor(String s) {
		ChatColor[] colors = ChatColor.values();
		for(int i = 0; i < 16; ++i) {
			if(colors[i].name().equalsIgnoreCase(s)) {
				return colors[i];
			}
		}
		throw new CommandException("Not a valid color: " + s);
	}

	// Team ====================
	protected List<String> completeTeam(String currArg, boolean allowDefaultTeam) {
		return (allowDefaultTeam ? server.getAllTeams() : server.getRealTeams()).stream()
				.map(t -> t.teamname.toLowerCase().replaceAll("[^0-9a-z ]", "").replaceAll(" +", "_"))
				.filter(t -> t.startsWith(currArg))
				.collect(Collectors.toList());
	}

	protected int getTeamId(String s, boolean allowDefaultTeam, boolean allowNumericID) {
		int teamId = -1;
		if(allowNumericID) {
			try {
				teamId = Integer.parseInt(s);
			} catch (NumberFormatException e) {
			}
		}
		if(teamId == -1) {
			ArrayList<MCGTeam> teams = server.getAllTeams();
			for(MCGTeam team : teams) {
				if(team.teamname.toLowerCase().replaceAll("[^0-9a-z ]", "").replaceAll(" +", "_").equals(s.toLowerCase())) {
					teamId = team.id;
					break;
				}
			}
		}
		if(teamId == -1 || (!allowDefaultTeam && teamId == 0)) {
			throw new CommandException("Not a valid team: " + s);
		}
		return teamId;
	}

	// Boolean ====================
	protected List<String> completeBoolean(String currArg) {
		return Stream.of("true", "false")
				.filter(t -> t.startsWith(currArg))
				.collect(Collectors.toList());
	}

	protected boolean getBoolean(String s) {
		if(s.equalsIgnoreCase("true")) {
			return true;
		} else if(s.equalsIgnoreCase("false")) {
			return false;
		}
		throw new CommandException("Not a valid boolean: " + s);
	}

	// Players ====================
	protected List<String> completePlayer(String currArg) {
		return Stream.concat(Stream.concat(server.players.stream(), server.offlinePlayers.stream())
				.map(p -> p.name), Stream.of("@a", "@p", "@s", "@r"))
				.filter(p -> p.toLowerCase().startsWith(currArg))
				.collect(Collectors.toList());
	}

	protected List<PlayerInterface> getPlayer(String s, CommandSender sender) {
		for(PlayerInterface p : server.offlinePlayers) {
			if(p.name.equalsIgnoreCase(s)) {
				return List.of(p);
			}
		}
		List<PlayerInterface> players = Bukkit.selectEntities(sender, s).stream()
				.filter(e -> e instanceof Player)
				.map(e -> server.playerLookup.get(e.getUniqueId()))
				.collect(Collectors.toList());
		if(players.size() == 0) {
			throw new CommandException("Player not found: " + s);
		}
		return players;
	}

	// Minigame ====================
	protected List<String> completeMinigame(String currArg, boolean wildcard) {
		Stream<String> minigameStream = MCGMain.getMinigames().stream();
		if(wildcard) {
			minigameStream = Stream.concat(Stream.of("*"), minigameStream);
		}
		return minigameStream
				.filter(g -> g.startsWith(currArg))
				.collect(Collectors.toList());
	}

	protected String getMinigame(String s, boolean wildcard) {
		if(wildcard && s.equals("*")) {
			return s;
		}
		if(!MCGMain.getMinigames().contains(s.toLowerCase())) {
			throw new CommandException("Minigame " + s + " does not exist");
		}
		return s.toLowerCase();
	}

	// Integer ====================
	protected List<String> completeInteger(String currArg) {
		return Stream.of("0", "1")
				.filter(g -> g.startsWith(currArg))
				.collect(Collectors.toList());
	}

	protected int getInteger(String s) {
		try {
			return Integer.parseInt(s);
		} catch(NumberFormatException e) {
			throw new CommandException("Not an integer: " + s);
		}
	}

	// Double ====================
	protected List<String> completeDouble(String currArg) {
		return Stream.of("0.0", "1.0")
				.filter(g -> g.startsWith(currArg))
				.collect(Collectors.toList());
	}

	protected double getDouble(String s) {
		try {
			return Double.parseDouble(s);
		} catch(NumberFormatException e) {
			throw new CommandException("Not a double: " + s);
		}
	}
}
