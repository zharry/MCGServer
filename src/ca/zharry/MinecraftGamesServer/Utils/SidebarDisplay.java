package ca.zharry.MinecraftGamesServer.Utils;

import org.bukkit.ChatColor;
import org.bukkit.scoreboard.*;

public class SidebarDisplay {
    public static final int MAX_LINES = 15;

    public Scoreboard scoreboard;
    public Objective objective;
    public Team[] teams = new Team[MAX_LINES];
    public String[] players = new String[MAX_LINES];
    public Score[] scores = new Score[MAX_LINES];
    public int counter = 0;

    public SidebarDisplay(Scoreboard scoreboard, String name) {
        this.scoreboard = scoreboard;
        objective = scoreboard.registerNewObjective("sidebarDisplay", "dummy", name);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for(int i = 0; i < MAX_LINES; ++i) {
            teams[i] = scoreboard.registerNewTeam(ChatColor.BLACK + "sidebar" + i);
            players[i] = "§" + Integer.toHexString(i);
            teams[i].addEntry(players[i]);
            scores[i] = objective.getScore(players[i]);
        }
    }

    public void add(String s) {
        if(counter >= MAX_LINES) {
            new RuntimeException("Too many lines of text added").printStackTrace();
            return;
        }
        teams[counter].setPrefix(s);
        scores[counter].setScore(MAX_LINES - counter);
        counter++;
    }

    public void end() {
        while(counter < MAX_LINES) {
            scoreboard.resetScores(players[counter]);
            counter++;
        }
        counter = 0;
    }
}
