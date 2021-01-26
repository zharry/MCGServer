package ca.zharry.MinecraftGamesServer.Timer;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Timer {

    private long ticks = -1;
    private boolean started = false;
    private boolean paused = true;

    public BukkitTask timerTask;
    public final String name;
    public final long defaultTicks;

    public Timer(Plugin plugin, String name, long defaultTicks) {
        this.name = name;
        this.defaultTicks = defaultTicks;
        this.ticks = defaultTicks;
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!paused) {
                    ticks -= 1;
                    onTick();
                }
                if (!paused && ticks <= 0) {
                    end();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public abstract void onStart();

    public abstract void onTick();

    public abstract void onEnd();

    public Timer set(long ticks) {
        this.ticks = ticks;
        return this;
    }

    public long get() {
        return ticks;
    }

    public void cancel() {
        ticks = -1;
        started = false;
        paused = true;
    }

    public void end() {
        cancel();
        onEnd();
    }

    public void start() {
        if (started) return;
        started = true;
        paused = false;
        onStart();
    }

    public void pause() {
        this.paused = true;
    }

    public void resume() {
        this.paused = false;
    }

    public boolean isPaused() {
        return this.paused;
    }

    public String toString() {
        return ticksToTime(this.ticks) + (isPaused() ? " (Paused)" : "");
    }

    public static String ticksToTime(long ticks) {
        boolean neg = ticks < 0;
        if(neg) {
            ticks = -ticks;
        }

//        long seconds = (ticks + 19) / 20;
        long seconds = ticks / 20;
        if(seconds < 3600) {
            return String.format("%d:%02d", seconds / 60, seconds % 60);
        } else {
            return String.format("%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
        }
    }

    public static String secondsToTime(long sec) {
        return ticksToTime(sec * 20);
    }

    public static final Pattern timePattern = Pattern.compile("^(?:(?:(\\d*):)?(\\d*):)?(\\d*)(?:(?:\\.(\\d*))|(?:,(\\d*)))?$");

    public static long parseDefaultLong(String fragment) {
        if(fragment == null || fragment.length() == 0) {
            return 0;
        }
        return Long.parseLong(fragment);
    }

    public static double parseDefaultDecimal(String fragment) {
        if(fragment == null || fragment.length() == 0) {
            return 0;
        }
        return Double.parseDouble("." + fragment);
    }

    public static long parseTimeTicks(String time) {
        Matcher matcher = timePattern.matcher(time);
        if(matcher.find()) {
            long h = parseDefaultLong(matcher.group(1));
            long m = parseDefaultLong(matcher.group(2));
            long s = parseDefaultLong(matcher.group(3));
            double ms = parseDefaultDecimal(matcher.group(4));
            long ticks = parseDefaultLong(matcher.group(5));
            return h * 3600 * 20 + m * 60 * 20 + s * 20 + (long) (ms * 20) + ticks;
        } else {
            throw new NumberFormatException("Invalid time format");
        }
    }
}
