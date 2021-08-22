package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import ca.zharry.MinecraftGamesServer.Utils.RunnableThrows;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TabCompletedCommand implements TabExecutor {
//
//	protected ServerInterface<? extends PlayerInterface> server;
//
//	protected HashMap<String, Subcommand> subcommands = new HashMap<>();
//
//	protected TabCompletedCommand(ServerInterface<? extends PlayerInterface> server) {
//		this.server = server;
//	}
//
//	public abstract class ArgumentCompleter<T> {
//		public T cached;
//		public abstract List<String> completeArgument(CommandSender sender, String arg);
//		public abstract T getArgument(CommandSender sender, String arg);
//		public T get() {
//			return cached;
//		}
//		public T getOrElse(T other) {
//			if(cached != null) {
//				return cached;
//			}
//			return other;
//		}
//	}
//
//	private static List<String> filterListLowercase(Stream<String> stream, String arg) {
//		return stream.map(String::toLowerCase).filter(c -> c.startsWith(arg)).collect(Collectors.toList());
//	}
//
//	private static List<String> filterList(Stream<String> stream, String arg) {
//		return stream.filter(c -> c.toLowerCase().startsWith(arg.toLowerCase())).collect(Collectors.toList());
//	}
//
//	protected class ColorCompleter extends ArgumentCompleter<ChatColor> {
//		@Override
//		public List<String> completeArgument(CommandSender sender, String arg) {
//			return filterListLowercase(Arrays.stream(Arrays.copyOf(ChatColor.values(), 16)).map(ChatColor::toString), arg);
//		}
//
//		@Override
//		public ChatColor getArgument(CommandSender sender, String arg) {
//			return Arrays.stream(ChatColor.values())
//					.limit(16)
//					.filter(c -> c.name().equalsIgnoreCase(arg))
//					.findFirst()
//					.orElseThrow(() -> new CommandException("Not a valid color: " + arg));
//		}
//	}
//
//	protected @interface OptTeamID {
//		boolean allTeams();
//		boolean allowID();
//	}
//
//	protected class TeamIDCompleter extends ArgumentCompleter<Integer> {
//		private final boolean allTeams;
//		private final boolean allowID;
//		public TeamIDCompleter(boolean allTeams, boolean allowID) {
//			this.allTeams = allTeams;
//			this.allowID = allowID;
//		}
//		private String getIdent(MCGTeam team) {
//			return team.teamname.toLowerCase().replaceAll("[^0-9a-z ]", "").replaceAll(" +", "_");
//		}
//
//		private Stream<MCGTeam> teamsStream() {
//			return (allTeams ? server.getAllTeams() : server.getRealTeams()).stream();
//		}
//
//		@Override
//		public List<String> completeArgument(CommandSender sender, String arg) {
//			return filterListLowercase(teamsStream().map(this::getIdent), arg);
//		}
//
//		@Override
//		public Integer getArgument(CommandSender sender, String arg) {
//			if(allowID) {
//				try {
//					int teamId = Integer.parseInt(arg);
//					if (teamId == 0 && !allTeams) {
//						throw new CommandException("Not a valid team ID: " + arg);
//					}
//					return teamId;
//				} catch(NumberFormatException e) {
//				}
//			}
//			return teamsStream()
//					.filter(t -> getIdent(t).equalsIgnoreCase(arg))
//					.findFirst()
//					.orElseThrow(() -> new CommandException("Not a valid team: " + arg)).id;
//		}
//	}
//
//	protected class BooleanCompleter extends ArgumentCompleter<Boolean> {
//		@Override
//		public List<String> completeArgument(CommandSender sender, String arg) {
//			return filterListLowercase(Stream.of("true", "false"), arg);
//		}
//
//		@Override
//		public Boolean getArgument(CommandSender sender, String arg) {
//			if(arg.equalsIgnoreCase("true")) {
//				return true;
//			} else if(arg.equalsIgnoreCase("false")) {
//				return false;
//			}
//			throw new CommandException("Not a valid boolean: " + arg);
//		}
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Target(ElementType.PARAMETER)
//	protected @interface OptPlayers {
//		boolean allowMultiple();
//	}
//	protected class PlayersCompleter extends ArgumentCompleter<List<PlayerInterface>> {
//		private final boolean allowMultiple;
//		public PlayersCompleter(boolean allowMultiple) {
//			this.allowMultiple = allowMultiple;
//		}
//		@Override
//		public List<String> completeArgument(CommandSender sender, String arg) {
//			return filterListLowercase(
//					Stream.concat(
//							Stream.concat(server.players.stream(), server.offlinePlayers.stream()).map(p -> p.name),
//							Stream.of("@a", "@p", "@s", "@r", "*")
//					), arg);
//		}
//
//		@Override
//		public List<PlayerInterface> getArgument(CommandSender sender, String arg) {
//			if(arg.equals("*")) {
//				return Stream.concat(server.players.stream(), server.offlinePlayers.stream()).collect(Collectors.toList());
//			}
//			for(PlayerInterface p : server.offlinePlayers) {
//				if(p.name.equalsIgnoreCase(arg)) {
//					return List.of(p);
//				}
//			}
//			List<PlayerInterface> players = Bukkit.selectEntities(sender, arg).stream()
//					.filter(e -> e instanceof Player)
//					.map(e -> server.playerLookup.get(e.getUniqueId()))
//					.collect(Collectors.toList());
//			if(players.size() == 0) {
//				throw new CommandException("Player not found: " + arg);
//			}
//			if(players.size() > 1 && !allowMultiple) {
//				throw new CommandException("Too many matching players found: " + arg);
//			}
//			return players;
//		}
//	}
//
//	protected class ConstantCompleter extends ArgumentCompleter<String> {
//		private final String[] options;
//		public ConstantCompleter(String... options) {
//			this.options = options;
//		}
//
//		@Override
//		public List<String> completeArgument(CommandSender sender, String arg) {
//			return filterListLowercase(Arrays.stream(options), arg);
//		}
//
//		@Override
//		public String getArgument(CommandSender sender, String arg) {
//			if(Arrays.stream(options).noneMatch(e -> e.equalsIgnoreCase(arg))) {
//				throw new CommandException("Argument " + arg + " is invalid. Must be one of " + Arrays.toString(options));
//			}
//			return arg;
//		}
//	}
//
//	protected class LongCompleter extends ArgumentCompleter<Long> {
//		public Long[] longs;
//		public LongCompleter(Long... longs) {
//			this.longs = longs;
//		}
//
//		@Override
//		public List<String> completeArgument(CommandSender sender, String arg) {
//			return filterListLowercase(Arrays.stream(longs).map(Object::toString), arg);
//		}
//
//		@Override
//		public Long getArgument(CommandSender sender, String arg) {
//			try {
//				return Long.parseLong(arg);
//			} catch(NumberFormatException e) {
//				throw new CommandException("Argument " + arg + " is not a number");
//			}
//		}
//	}
//
//	private class OptionalCompleter<T> extends ArgumentCompleter<T> {
//		public final ArgumentCompleter<T> completer;
//		public OptionalCompleter(ArgumentCompleter<T> completer) {
//			this.completer = completer;
//		}
//
//		@Override
//		public List<String> completeArgument(CommandSender sender, String arg) {
//			return completer.completeArgument(sender, arg);
//		}
//
//		@Override
//		public T getArgument(CommandSender sender, String arg) {
//			return completer.getArgument(sender, arg);
//		}
//	}
//
//	public <T> ArgumentCompleter<T> optional(ArgumentCompleter<T> completer) {
//		return new OptionalCompleter<>(completer);
//	}
//
///*	public class Subcommand {
//		public String command;
//		public String desc;
//		public RunnableThrows executor;
//
//		public ArrayList<String> argName = new ArrayList<>();
//		public ArrayList<ArgumentCompleter<?>> argCompleter = new ArrayList<>();
//
//		public Subcommand(String command) {
//			this.command = command;
//		}
//		public Subcommand desc(String desc) {
//			this.desc = desc;
//			return this;
//		}
//		public Subcommand executor(RunnableThrows executor) {
//			this.executor = executor;
//			return this;
//		}
//		public Subcommand arg(String name, ArgumentCompleter<?> arg) {
//			argName.add(name);
//			argCompleter.add(arg);
//			return this;
//		}
//		public Subcommand argOpt(String name, ArgumentCompleter<?> arg) {
//			return arg(name, optional(arg));
//		}
//		public String getUsage() {
//			StringBuilder builder = new StringBuilder(command);
//			for(int i = 0; i < argName.size(); ++i) {
//				if(argCompleter.get(i) instanceof OptionalCompleter) {
//					builder.append(" [").append(argName.get(i)).append("]");
//				} else {
//					builder.append(" <").append(argName.get(i)).append(">");
//				}
//			}
//			return builder.toString();
//		}
//	}
//
//	protected Subcommand newCommand(String subcommand) {
//		Subcommand command = new Subcommand(subcommand);
//		subcommands.put(subcommand, command);
//		return command;
//	}*/
//
//	@Override
//	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
//		if(args.length < 1) {
//			sender.sendMessage(ChatColor.RED + subcommands.values().stream().map(c -> "/" + label + " " + c.getUsage()).reduce((s1, s2) -> s1 + "\n" + s2).get());
//			return true;
//		}
//		Subcommand subcommand = subcommands.get(args[0]);
//		if(subcommand == null) {
//			sender.sendMessage(ChatColor.RED + args[0] + " is not a valid subcommand. Must be one of " + subcommands.keySet());
//			return true;
//		}
//
//		String usage = "/" + label + " " + subcommand.getUsage();
//
//		if(args.length - 1 > subcommand.argCompleter.size()) {
//			sender.sendMessage(ChatColor.RED + "Too many arguments provided\n" + usage);
//		}
//
//		int ind = 1;
//
//		for(int i = 0; i < subcommand.argCompleter.size(); ++i) {
//			ArgumentCompleter completer = subcommand.argCompleter.get(i);
//			if(ind >= args.length) {
//				if(completer instanceof OptionalCompleter) {
//					break;
//				} else {
//					sender.sendMessage(ChatColor.RED + "Argument " + subcommand.argName.get(i) + " is required\n" + usage);
//					return true;
//				}
//			}
//			try {
//				completer.cached = completer.getArgument(sender, args[ind]);
//			} catch(CommandException e) {
//				sender.sendMessage(ChatColor.RED + e.getMessage());
//				return true;
//			}
//			ind++;
//		}
//
//		try {
//			subcommand.executor.run();
//		} catch(CommandException e) {
//			sender.sendMessage(ChatColor.RED + e.getMessage());
//		} catch(Exception e) {
//			sender.sendMessage(ChatColor.RED + e.toString());
//		}
//
//		return true;
//	}
//
//	@Override
//	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
//		String first = "";
//		if(args.length > 0) {
//			first = args[0];
//		}
//		if(args.length <= 1) {
//			return filterListLowercase(subcommands.keySet().stream(), first);
//		}
//		String last = args[args.length - 1];
//		Subcommand subcommand = subcommands.get(first);
//		if(subcommand == null) {
//			return List.of();
//		}
//
//		int ind = args.length - 2;
//
//		if(ind >= subcommand.argCompleter.size()) {
//			return List.of();
//		}
//
//		return subcommand.argCompleter.get(ind).completeArgument(sender, last);
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Target(ElementType.PARAMETER)
//	protected @interface Completer {
//		String value();
//	}
//
//	public ArgumentCompleter<?> customCompleter(String completer) {
//		return null;
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Target(ElementType.METHOD)
//	protected @interface Subcommand {
//		String name();
//		String[] args();
//		int required() default 0;
//		boolean variadic() default false;
//	}
}