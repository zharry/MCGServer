package ca.zharry.MinecraftGamesServer.Utils;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import com.google.gson.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;

public class ClassSaveHandler {
	private static final HashMap<Class<? extends PlayerInterface>, HashMap<String, Field>> classFields = new HashMap<>();
	private static final JsonParser parser = new JsonParser();

	private static void putFields(Class<?> clazz, HashMap<String, Field> fieldNames) {
		if(clazz == null) {
			return;
		}
		for(Field field : clazz.getDeclaredFields()) {
			Saved annotation = field.getAnnotation(Saved.class);
			if(annotation != null) {
				field.trySetAccessible();
				fieldNames.put(annotation.name().length() == 0 ? field.getName() : annotation.name(), field);
			}
		}
		putFields(clazz.getSuperclass(), fieldNames);
	}

	private static final Gson gson = new GsonBuilder()
			.enableComplexMapKeySerialization()
			.serializeSpecialFloatingPointValues()
			.serializeNulls()
			.create();

	private static HashMap<String, Field> checkAndGenerateClassCache(Class<? extends PlayerInterface> clazz) {
		HashMap<String, Field> map = classFields.get(clazz);
		if(map == null) {
			map = new HashMap<>();
			putFields(clazz, map);
			classFields.put(clazz, map);
		}
		return map;
	}

	public static String toJSON(PlayerInterface player) {
		JsonObject obj = new JsonObject();
		checkAndGenerateClassCache(player.getClass()).forEach((n, f) -> {
			try {
				obj.add(n, gson.toJsonTree(f.get(player)));
			} catch(IllegalAccessException e) {
				throw new RuntimeException("Cannot get field '" + f + "' in " + player, e);
			}
		});
		return obj.toString();
	}

	public static void fromJSON(PlayerInterface player, String json) {
		JsonObject obj = parser.parse(json).getAsJsonObject();
		checkAndGenerateClassCache(player.getClass()).forEach((n, f) -> {
			JsonElement jsonVal = obj.get(n);
			if(jsonVal != null) {
				try {
					f.set(player, gson.fromJson(jsonVal, f.getType()));
				} catch(IllegalAccessException e) {
					throw new RuntimeException("Cannot set field '" + f + "' in " + player, e);
				}
			}
		});
	}
}
