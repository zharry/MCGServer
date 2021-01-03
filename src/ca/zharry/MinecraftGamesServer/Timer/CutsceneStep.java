package ca.zharry.MinecraftGamesServer.Timer;

public class CutsceneStep {

    enum Transition {
        CUT,
        JUMP_LERP,
        LINEAR,
        QUADRATIC
    }

    public int timestamp;
    public String title;
    public String subtitle;
    public int titleDelay;
    public boolean hasPosition;
    public double x, y, z, yaw, pitch;
    public int freeTick = 0;
    public Transition transition = Transition.CUT;
    public EventExecutor executor;

    public CutsceneStep(int timestamp) {
        this.timestamp = timestamp;
    }

    public CutsceneStep pos(double x, double y, double z, double yaw, double pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        hasPosition = true;
        return this;
    }

    public CutsceneStep title(String title, String subtitle, int titleDelay) {
        this.title = title;
        this.subtitle = subtitle;
        this.titleDelay = titleDelay;
        return this;
    }

    public CutsceneStep transition(Transition transition) {
        this.transition = transition;
        return this;
    }

    public CutsceneStep jumplerp() {
        this.transition = Transition.JUMP_LERP;
        return this;
    }

    public CutsceneStep linear() {
        this.transition = Transition.LINEAR;
        return this;
    }

    public CutsceneStep quad() {
        this.transition = Transition.QUADRATIC;
        return this;
    }

    public CutsceneStep comment(String s) {
        return this;
    }

    public CutsceneStep freeze(int ticks) {
        freeTick = ticks;
        return this;
    }

    public CutsceneStep action(EventExecutor executor) {
        this.executor = executor;
        return this;
    }

    public interface EventExecutor {
        void run(CutsceneStep step);
    }

}
