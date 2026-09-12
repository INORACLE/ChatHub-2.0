package com.zhanganzhi.chathub.core.binding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent QQ <-> Minecraft account bindings, stored as JSON
 * ({@code bindings.json} in the mod data directory).
 */
public class BindingManagerV2 {

    private static final String BINDING_FILE = "bindings.json";
    private static final int MAX_BINDINGS_PER_QQ = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, List<String>> qqToMcBindings;
    private final Map<String, String> mcToQqBindings;
    private final Map<String, VerificationCode> verificationCodes;
    private final Map<String, String> gameNameToCode;
    private final Path dataDirectory;
    private Logger logger;

    public BindingManagerV2(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.qqToMcBindings = new ConcurrentHashMap<>();
        this.mcToQqBindings = new ConcurrentHashMap<>();
        this.verificationCodes = new ConcurrentHashMap<>();
        this.gameNameToCode = new ConcurrentHashMap<>();
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void loadBindings() {
        File bindingFile = this.dataDirectory.resolve(BINDING_FILE).toFile();
        if (!bindingFile.exists()) {
            return;
        }
        try {
            String content = Files.readString(bindingFile.toPath());
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            this.qqToMcBindings.clear();
            this.mcToQqBindings.clear();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String qqId = entry.getKey();
                JsonElement value = entry.getValue();
                List<String> mcNames = new ArrayList<>();
                if (value.isJsonArray()) {
                    JsonArray array = value.getAsJsonArray();
                    for (JsonElement item : array) {
                        mcNames.add(item.getAsString());
                    }
                } else if (value.isJsonPrimitive()) {
                    mcNames.add(value.getAsString());
                }
                this.qqToMcBindings.put(qqId, mcNames);
                for (String mcName : mcNames) {
                    this.mcToQqBindings.put(mcName, qqId);
                }
            }
        } catch (Exception e) {
            if (this.logger != null) {
                this.logger.error("[Binding] Failed to load bindings", e);
            }
        }
    }

    public void saveBindings() {
        try {
            JsonObject json = new JsonObject();
            for (Map.Entry<String, List<String>> entry : this.qqToMcBindings.entrySet()) {
                JsonArray array = new JsonArray();
                for (String mcName : entry.getValue()) {
                    array.add(mcName);
                }
                json.add(entry.getKey(), array);
            }
            File bindingFile = this.dataDirectory.resolve(BINDING_FILE).toFile();
            Files.writeString(bindingFile.toPath(), GSON.toJson(json));
        } catch (IOException e) {
            if (this.logger != null) {
                this.logger.error("[Binding] Failed to save bindings", e);
            }
        }
    }

    public int bindOnce(String qqId, String mcName) {
        if (qqId == null || mcName == null || qqId.isEmpty() || mcName.isEmpty()) {
            return 2;
        }
        List<String> boundList = this.qqToMcBindings.computeIfAbsent(qqId, key -> new ArrayList<>());
        if (boundList.contains(mcName)) {
            return 1;
        }
        if (boundList.size() >= MAX_BINDINGS_PER_QQ) {
            return 1;
        }
        String existingQqId = this.mcToQqBindings.get(mcName);
        if (existingQqId != null && !existingQqId.equals(qqId)) {
            return 2;
        }
        boundList.add(mcName);
        this.mcToQqBindings.put(mcName, qqId);
        this.saveBindings();
        return 0;
    }

    public boolean adminBind(String qqId, String mcName) {
        if (qqId == null || mcName == null || qqId.isEmpty() || mcName.isEmpty()) {
            return false;
        }
        String existingQqId = this.mcToQqBindings.get(mcName);
        if (existingQqId != null && !existingQqId.equals(qqId)) {
            List<String> existingList = this.qqToMcBindings.get(existingQqId);
            if (existingList != null) {
                existingList.remove(mcName);
                if (existingList.isEmpty()) {
                    this.qqToMcBindings.remove(existingQqId);
                }
            }
        }
        List<String> boundList = this.qqToMcBindings.computeIfAbsent(qqId, key -> new ArrayList<>());
        if (boundList.size() >= MAX_BINDINGS_PER_QQ) {
            String removed = boundList.remove(0);
            this.mcToQqBindings.remove(removed);
        }
        boundList.add(mcName);
        this.mcToQqBindings.put(mcName, qqId);
        this.saveBindings();
        return true;
    }

    public boolean adminUnbind(String qqId, String mcName) {
        List<String> boundList = this.qqToMcBindings.get(qqId);
        if (boundList != null && boundList.remove(mcName)) {
            this.mcToQqBindings.remove(mcName);
            if (boundList.isEmpty()) {
                this.qqToMcBindings.remove(qqId);
            }
            this.saveBindings();
            return true;
        }
        return false;
    }

    public boolean adminUnbindAll(String qqId) {
        List<String> boundList = this.qqToMcBindings.remove(qqId);
        if (boundList != null) {
            for (String mcName : boundList) {
                this.mcToQqBindings.remove(mcName);
            }
            this.saveBindings();
            return true;
        }
        return false;
    }

    public List<String> getMcNamesByQQ(String qqId) {
        return this.qqToMcBindings.getOrDefault(qqId, new ArrayList<>());
    }

    public String getQQByMcName(String mcName) {
        return this.mcToQqBindings.get(mcName);
    }

    public boolean isQQBound(String qqId) {
        List<String> boundList = this.qqToMcBindings.get(qqId);
        return boundList != null && !boundList.isEmpty();
    }

    public boolean isMcNameBound(String mcName) {
        return this.mcToQqBindings.containsKey(mcName);
    }

    public int getBoundCount(String qqId) {
        List<String> boundList = this.qqToMcBindings.get(qqId);
        return boundList != null ? boundList.size() : 0;
    }

    public Map<String, List<String>> getAllBindings() {
        return new ConcurrentHashMap<>(this.qqToMcBindings);
    }

    public int getTotalQQUsers() {
        return this.qqToMcBindings.size();
    }

    public int getTotalBindings() {
        return this.mcToQqBindings.size();
    }

    public String generateVerificationCode(String gameName) {
        String code = String.format("%04d", (int) (Math.random() * 10000.0));
        while (this.verificationCodes.containsKey(code)) {
            code = String.format("%04d", (int) (Math.random() * 10000.0));
        }
        this.verificationCodes.put(code, new VerificationCode(gameName, null, System.currentTimeMillis()));
        this.gameNameToCode.put(gameName, code);
        return code;
    }

    public String verifyCode(String code) {
        VerificationCode vc = this.verificationCodes.get(code);
        if (vc == null) {
            return null;
        }
        long elapsed = System.currentTimeMillis() - vc.timestamp;
        if (elapsed > 300000L) {
            this.verificationCodes.remove(code);
            this.gameNameToCode.remove(vc.gameName);
            return null;
        }
        return vc.gameName;
    }

    public String getQQByCode(String code) {
        VerificationCode vc = this.verificationCodes.get(code);
        return vc != null ? vc.qqId : null;
    }

    public void setQQForCode(String code, String qqId) {
        VerificationCode vc = this.verificationCodes.get(code);
        if (vc != null) {
            vc.qqId = qqId;
        }
    }

    public void clearVerificationCode(String code) {
        VerificationCode vc = this.verificationCodes.remove(code);
        if (vc != null) {
            this.gameNameToCode.remove(vc.gameName);
        }
    }

    public void reloadBindings() {
        this.qqToMcBindings.clear();
        this.mcToQqBindings.clear();
        this.loadBindings();
    }

    private static class VerificationCode {
        String gameName;
        String qqId;
        long timestamp;

        VerificationCode(String gameName, String qqId, long timestamp) {
            this.gameName = gameName;
            this.qqId = qqId;
            this.timestamp = timestamp;
        }
    }
}