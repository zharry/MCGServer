package ca.zharry.MinecraftGamesServer.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Saved {
	String name() default "";
}
