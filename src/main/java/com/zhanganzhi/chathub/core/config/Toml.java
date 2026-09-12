package com.zhanganzhi.chathub.core.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal TOML parser sufficient for the ChatHub config file.
 * Supports sections, string/number/boolean values and arrays of
 * strings/numbers, with '#' comments (inline and full-line).
 */
public class Toml {

    private final Map<String, Object> values = new HashMap<>();

    public static Toml read(File file) throws IOException {
        return read(Files.readString(file.toPath()));
    }

    public static Toml read(String content) {
        Toml toml = new Toml();
        String section = "";
        for (String rawLine : content.split("\r?\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            String path = section.isEmpty() ? key : section + "." + key;
            toml.values.put(path, parseValue(value));
        }
        return toml;
    }

    private static Object parseValue(String raw) {
        String value = raw;
        while (!value.isEmpty() && Character.isWhitespace(value.charAt(0))) {
            value = value.substring(1);
        }
        if (value.isEmpty()) {
            return "";
        }
        char first = value.charAt(0);
        if (first == '\'' || first == '"') {
            char quote = first;
            int end = -1;
            for (int i = 1; i < value.length(); i++) {
                if (value.charAt(i) == quote) {
                    end = i;
                    break;
                }
            }
            return end > 0 ? value.substring(1, end) : value.substring(1);
        }
        if (first == '[') {
            return parseArray(value);
        }
        int end = value.length();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '#' || c == ',' || Character.isWhitespace(c)) {
                end = i;
                break;
            }
        }
        String token = value.substring(0, end).trim();
        return convertScalar(token);
    }

    private static List<String> parseArray(String value) {
        List<String> result = new ArrayList<>();
        int i = value.indexOf('[');
        StringBuilder item = new StringBuilder();
        boolean inString = false;
        char stringQuote = 0;
        for (; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!inString && (c == '\'' || c == '"')) {
                inString = true;
                stringQuote = c;
                continue;
            }
            if (inString && c == stringQuote) {
                inString = false;
                continue;
            }
            if (!inString && c == ',') {
                addArrayItem(result, item.toString());
                item.setLength(0);
                continue;
            }
            if (!inString && c == ']') {
                addArrayItem(result, item.toString());
                break;
            }
            if (!inString && c == '#') {
                break;
            }
            item.append(c);
        }
        return result;
    }

    private static void addArrayItem(List<String> result, String item) {
        String trimmed = item.trim();
        if (!trimmed.isEmpty()) {
            result.add(trimmed);
        }
    }

    private static Object convertScalar(String token) {
        if (token.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (token.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (token.matches("-?\\d+")) {
            try {
                return Long.parseLong(token);
            } catch (NumberFormatException ignored) {
                return token;
            }
        }
        return token;
    }

    public boolean contains(String path) {
        return this.values.containsKey(path);
    }

    public String getString(String path) {
        Object value = this.values.get(path);
        if (value == null) {
            return null;
        }
        if (value instanceof String string) {
            return string;
        }
        if (value instanceof Boolean bool) {
            return Boolean.toString(bool);
        }
        if (value instanceof Long number) {
            return Long.toString(number);
        }
        return String.valueOf(value);
    }

    public String getString(String path, String def) {
        String value = getString(path);
        return value != null ? value : def;
    }

    public Boolean getBoolean(String path, Boolean def) {
        Object value = this.values.get(path);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return def;
    }

    public Long getLong(String path, Long def) {
        Object value = this.values.get(path);
        if (value instanceof Long number) {
            return number;
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    public List<?> getList(String path) {
        Object value = this.values.get(path);
        return value instanceof List<?> list ? list : null;
    }

    public Map<String, Object> asMap() {
        return new HashMap<>(this.values);
    }
}