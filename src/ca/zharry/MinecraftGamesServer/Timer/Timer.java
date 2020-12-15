package ca.zharry.MinecraftGamesServer.Timer;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public abstract class Timer {

    private int ticks = -1;
    private boolean started = false;
    private boolean paused = true;

    public BukkitTask timerTask;

    public Timer(Plugin plugin) {
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!paused) {
                    ticks -= 1;
                    onTick();
                }
                if (!paused && ticks <= 0) {
                    ticks = -1;
                    started = false;
                    paused = true;
                    onEnd();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public abstract void onStart();

    public abstract void onTick();

    public abstract void onEnd();

    public Timer set(int ticks) {
        this.ticks = ticks;
        return this;
    }

    public int get() {
        return ticks;
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

    public String getString() {
        return ticksToTime(this.ticks);
    }

    static String ticksToTime(int ticks) {
        int minutes = (ticks / (20 * 60));
        int seconds = (ticks % (20 * 60) / 20);
        if (seconds < 10) {
            return minutes + ":0" + seconds;
        }
        return minutes + ":" + seconds;
    }

    static String secondsToTime(int sec) {
        int minutes = (sec / 60);
        int seconds = (sec % 60);
        if (seconds < 10) {
            return minutes + ":0" + seconds;
        }
        return minutes + ":" + seconds;
    }

}
