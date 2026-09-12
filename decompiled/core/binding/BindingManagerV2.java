/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.core.binding;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
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

public class BindingManagerV2 {
    private static final String BINDING_FILE = "bindings.json";
    private static final int MAX_BINDINGS_PER_QQ = 1;
    private final Map<String, List<String>> qqToMcBindings;
    private final Map<String, String> mcToQqBindings;
    private final Map<String, VerificationCode> verificationCodes;
    private final Map<String, String> gameNameToCode;
    private final Path dataDirectory;
    private Logger logger;

    public BindingManagerV2(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.qqToMcBindings = new ConcurrentHashMap<String, List<String>>();
        this.mcToQqBindings = new ConcurrentHashMap<String, String>();
        this.verificationCodes = new ConcurrentHashMap<String, VerificationCode>();
        this.gameNameToCode = new ConcurrentHashMap<String, String>();
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void loadBindings() {
        block9: {
            File bindingFile = this.dataDirectory.resolve(BINDING_FILE).toFile();
            if (!bindingFile.exists()) {
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
                    if (value instanceof JSONArray) {
                        JSONArray array = (JSONArray)value;
                        for (Object item : array) {
                            mcNames.add(item.toString());
                        }
                    } else if (value instanceof String) {
                        mcNames.add(value.toString());
                    }
                    this.qqToMcBindings.put(qqId, mcNames);
                    for (String mcName : mcNames) {
                        this.mcToQqBindings.put(mcName, qqId);
                    }
                }
            }
            catch (Exception e) {
                if (this.logger == null) break block9;
                this.logger.error("[Binding] Failed to load bindings", e);
            }
        }
    }

    public void saveBindings() {
        block3: {
            try {
                JSONObject json = new JSONObject();
                for (Map.Entry<String, List<String>> entry : this.qqToMcBindings.entrySet()) {
                    json.put(entry.getKey(), entry.getValue());
                }
                File bindingFile = this.dataDirectory.resolve(BINDING_FILE).toFile();
                Files.writeString(bindingFile.toPath(), (CharSequence)json.toJSONString(new JSONWriter.Feature[0]), new OpenOption[0]);
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
        List boundList = this.qqToMcBindings.computeIfAbsent(qqId, k -> new ArrayList());
        if (boundList.contains(mcName)) {
            return 1;
        }
        if (boundList.size() >= 1) {
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
        List boundList;
        List<String> existingList;
        if (qqId == null || mcName == null || qqId.isEmpty() || mcName.isEmpty()) {
            return false;
        }
        String existingQqId = this.mcToQqBindings.get(mcName);
        if (existingQqId != null && !existingQqId.equals(qqId) && (existingList = this.qqToMcBindings.get(existingQqId)) != null) {
            existingList.remove(mcName);
            if (existingList.isEmpty()) {
                this.qqToMcBindings.remove(existingQqId);
            }
        }
        if ((boundList = this.qqToMcBindings.computeIfAbsent(qqId, k -> new ArrayList())).size() >= 1) {
            String removed = (String)boundList.remove(0);
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
        return this.qqToMcBindings.getOrDefault(qqId, new ArrayList());
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
        return new ConcurrentHashMap<String, List<String>>(this.qqToMcBindings);
    }

    public int getTotalQQUsers() {
        return this.qqToMcBindings.size();
    }

    public int getTotalBindings() {
        return this.mcToQqBindings.size();
    }

    public String generateVerificationCode(String gameName) {
        String code = String.format("%04d", (int)(Math.random() * 10000.0));
        while (this.verificationCodes.containsKey(code)) {
            code = String.format("%04d", (int)(Math.random() * 10000.0));
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

    public int getTotalBindingCount() {
        return this.mcToQqBindings.size();
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

