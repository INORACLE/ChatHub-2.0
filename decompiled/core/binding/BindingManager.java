/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.core.binding;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

public class BindingManager {
    private static final String BINDING_FILE = "bindings.json";
    private static final int MAX_BINDINGS_PER_QQ = 2;
    private final Map<String, List<String>> qqToMcBindings;
    private final Map<String, String> mcToQqBindings;
    private final Path dataDirectory;
    private Logger logger;

    public BindingManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.qqToMcBindings = new ConcurrentHashMap<String, List<String>>();
        this.mcToQqBindings = new ConcurrentHashMap<String, String>();
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void loadBindings() {
        block10: {
            File bindingFile = this.dataDirectory.resolve(BINDING_FILE).toFile();
            if (!bindingFile.exists()) {
                if (this.logger != null) {
                    this.logger.info("[Binding] No binding file found, starting with empty bindings");
                }
                return;
            }
            try {
                String content = Files.readString(bindingFile.toPath());
                JSONObject json = JSON.parseObject(content);
                this.qqToMcBindings.clear();
                this.mcToQqBindings.clear();
                for (Map.Entry entry : json.entrySet()) {
                    String qqId = (String)entry.getKey();
                    Object value = entry.getValue();
                    ArrayList<String> mcNames = new ArrayList<String>();
                    if (value instanceof List) {
                        List list = (List)value;
                        for (Object item : list) {
                            mcNames.add(item.toString());
                        }
                    } else {
                        mcNames.add(value.toString());
                    }
                    this.qqToMcBindings.put(qqId, mcNames);
                    for (String mcName : mcNames) {
                        this.mcToQqBindings.put(mcName, qqId);
                    }
                }
                if (this.logger != null) {
                    this.logger.info("[Binding] Loaded {} QQ users with {} total bindings", (Object)this.qqToMcBindings.size(), (Object)this.mcToQqBindings.size());
                }
            }
            catch (Exception e) {
                if (this.logger == null) break block10;
                this.logger.error("[Binding] Failed to load bindings", e);
            }
        }
    }

    public void saveBindings() {
        block3: {
            try {
                JSONObject json = new JSONObject();
                json.putAll(this.qqToMcBindings);
                File bindingFile = this.dataDirectory.resolve(BINDING_FILE).toFile();
                Files.writeString(bindingFile.toPath(), (CharSequence)json.toJSONString(new JSONWriter.Feature[0]), new OpenOption[0]);
                if (this.logger != null) {
                    this.logger.debug("[Binding] Saved {} QQ users with {} total bindings", (Object)this.qqToMcBindings.size(), (Object)this.mcToQqBindings.size());
                }
            }
            catch (IOException e) {
                if (this.logger == null) break block3;
                this.logger.error("[Binding] Failed to save bindings", e);
            }
        }
    }

    public int bindOnce(String qqId, String mcName) {
        if (qqId == null || mcName == null || qqId.isEmpty() || mcName.isEmpty()) {
            return 2;
        }
        List<String> existingMcNames = this.qqToMcBindings.get(qqId);
        if (existingMcNames != null && existingMcNames.contains(mcName)) {
            if (this.logger != null) {
                this.logger.info("[Binding] QQ {} already bound to {}, cannot rebind", (Object)qqId, (Object)mcName);
            }
            return 1;
        }
        if (existingMcNames != null && existingMcNames.size() >= 2) {
            if (this.logger != null) {
                this.logger.info("[Binding] QQ {} has reached max bindings ({}/{}), cannot bind more", qqId, existingMcNames.size(), 2);
            }
            return 1;
        }
        String existingQqId = this.mcToQqBindings.get(mcName);
        if (existingQqId != null && !existingQqId.equals(qqId)) {
            if (this.logger != null) {
                this.logger.info("[Binding] Minecraft name {} already bound to QQ {}, cannot bind", (Object)mcName, (Object)existingQqId);
            }
            return 2;
        }
        if (existingMcNames == null) {
            existingMcNames = new ArrayList<String>();
            this.qqToMcBindings.put(qqId, existingMcNames);
        }
        existingMcNames.add(mcName);
        this.mcToQqBindings.put(mcName, qqId);
        this.saveBindings();
        if (this.logger != null) {
            this.logger.info("[Binding] Bound QQ {} to Minecraft {} ({}/{})", qqId, mcName, existingMcNames.size(), 2);
        }
        return 0;
    }

    public boolean adminBind(String qqId, String mcName) {
        List mcNames;
        if (qqId == null || mcName == null || qqId.isEmpty() || mcName.isEmpty()) {
            return false;
        }
        String existingQqId = this.mcToQqBindings.get(mcName);
        if (existingQqId != null && !existingQqId.equals(qqId)) {
            List<String> otherMcNames = this.qqToMcBindings.get(existingQqId);
            if (otherMcNames != null) {
                otherMcNames.remove(mcName);
                if (otherMcNames.isEmpty()) {
                    this.qqToMcBindings.remove(existingQqId);
                }
            }
            this.mcToQqBindings.remove(mcName);
            if (this.logger != null) {
                this.logger.warn("[Binding] Admin unbound {} from {} (force rebinding to {})", existingQqId, mcName, qqId);
            }
        }
        if ((mcNames = this.qqToMcBindings.computeIfAbsent(qqId, k -> new ArrayList())).contains(mcName)) {
            return true;
        }
        if (mcNames.size() >= 2) {
            String removedMcName = (String)mcNames.remove(0);
            this.mcToQqBindings.remove(removedMcName);
            if (this.logger != null) {
                this.logger.warn("[Binding] Admin removed oldest binding {} from QQ {} (max limit reached)", (Object)removedMcName, (Object)qqId);
            }
        }
        mcNames.add(mcName);
        this.mcToQqBindings.put(mcName, qqId);
        this.saveBindings();
        if (this.logger != null) {
            this.logger.info("[Binding] Admin bound QQ {} to Minecraft {} ({}/{})", qqId, mcName, mcNames.size(), 2);
        }
        return true;
    }

    public boolean adminUnbind(String qqId, String mcName) {
        List<String> mcNames = this.qqToMcBindings.get(qqId);
        if (mcNames != null && mcNames.remove(mcName)) {
            this.mcToQqBindings.remove(mcName);
            if (mcNames.isEmpty()) {
                this.qqToMcBindings.remove(qqId);
            }
            this.saveBindings();
            if (this.logger != null) {
                this.logger.info("[Binding] Admin unbound QQ {} from Minecraft {}", (Object)qqId, (Object)mcName);
            }
            return true;
        }
        return false;
    }

    public boolean adminUnbindAll(String qqId) {
        List<String> mcNames = this.qqToMcBindings.remove(qqId);
        if (mcNames != null) {
            for (String mcName : mcNames) {
                this.mcToQqBindings.remove(mcName);
            }
            this.saveBindings();
            if (this.logger != null) {
                this.logger.info("[Binding] Admin unbound all {} accounts from QQ {}", (Object)mcNames.size(), (Object)qqId);
            }
            return true;
        }
        return false;
    }

    public List<String> getMcNamesByQQ(String qqId) {
        return this.qqToMcBindings.getOrDefault(qqId, new ArrayList());
    }

    public String getMcNameByQQ(String qqId) {
        List<String> names = this.qqToMcBindings.get(qqId);
        if (names != null && !names.isEmpty()) {
            return names.get(0);
        }
        return null;
    }

    public String getQQByMcName(String mcName) {
        return this.mcToQqBindings.get(mcName);
    }

    public boolean isQQBound(String qqId) {
        List<String> names = this.qqToMcBindings.get(qqId);
        return names != null && !names.isEmpty();
    }

    public int getBindingCountByQQ(String qqId) {
        List<String> names = this.qqToMcBindings.get(qqId);
        return names != null ? names.size() : 0;
    }

    public boolean isMcNameBound(String mcName) {
        return this.mcToQqBindings.containsKey(mcName);
    }

    public Map<String, List<String>> getAllBindings() {
        return new ConcurrentHashMap<String, List<String>>(this.qqToMcBindings);
    }

    public int getBindingCount() {
        return this.qqToMcBindings.size();
    }
}

