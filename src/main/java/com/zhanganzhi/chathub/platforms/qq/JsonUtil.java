package com.zhanganzhi.chathub.platforms.qq;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Simple helpers to read a message-part JSON array the same way the original
 * fastjson2 code did ({@code getString}/{@code getJSONObject} semantics).
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String getString(JsonObject json, String key) {
        if (json == null || !json.has(key)) {
            return null;
        }
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    public static Long getLong(JsonObject json, String key) {
        JsonElement element = json != null ? json.get(key) : null;
        return element != null && element.isJsonPrimitive() ? element.getAsLong() : null;
    }

    public static JsonObject getJsonObject(JsonObject json, String key) {
        if (json == null || !json.has(key)) {
            return null;
        }
        JsonElement element = json.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    public static JsonObject getObject(JsonArray array, int index) {
        if (array == null || index >= array.size()) {
            return null;
        }
        JsonElement element = array.get(index);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}