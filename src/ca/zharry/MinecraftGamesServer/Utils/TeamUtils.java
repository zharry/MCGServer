package ca.zharry.MinecraftGamesServer.Utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TeamUtils {
    public static final void copyTeamInformation(Scoreboard from, Scoreboard to) {
        // If the team exists in "to" but not "from", delete team from "to".
        for(Team toTeam : to.getTeams()) {
            // Ignore all fake teams
            if(toTeam.getName().startsWith(Character.toString(ChatColor.COLOR_CHAR))) {
                continue;
            }
            if(from.getTeam(toTeam.getName()) == null) {
                toTeam.unregister();
            }
        }

        for(Team fromTeam : from.getTeams()) {
            // Ignore all fake teams
            if(fromTeam.getName().startsWith(Character.toString(ChatColor.COLOR_CHAR))) {
                continue;
            }
            Team toTeam = to.getTeam(fromTeam.getName());
            // Team does not exist in "to", create it
            if(toTeam == null) {
                toTeam = to.registerNewTeam(fromTeam.getName());
                toTeam.setPrefix(fromTeam.getPrefix());
                toTeam.setDisplayName(fromTeam.getDisplayName());
                toTeam.setCanSeeFriendlyInvisibles(fromTeam.canSeeFriendlyInvisibles());
                toTeam.setAllowFriendlyFire(fromTeam.allowFriendlyFire());
                toTeam.setColor(fromTeam.getColor());
                toTeam.setSuffix(fromTeam.getSuffix());
                toTeam.setOption(Team.Option.COLLISION_RULE, fromTeam.getOption(Team.Option.COLLISION_RULE));
                toTeam.setOption(Team.Option.DEATH_MESSAGE_VISIBILITY, fromTeam.getOption(Team.Option.DEATH_MESSAGE_VISIBILITY));
                toTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, fromTeam.getOption(Team.Option.NAME_TAG_VISIBILITY));
            }

            // If the entry exists in "toTeam" but not "fromTeam", delete entry from "toTeam"
            for(String entry : toTeam.getEntries()) {
                if(!fromTeam.hasEntry(entry)) {
                    toTeam.removeEntry(entry);
                }
            }

//            for(String entry : )
        }
    }
}
