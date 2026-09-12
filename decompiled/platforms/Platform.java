/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.platforms;

public enum Platform {
    DISCORD("discord"),
    KOOK("kook"),
    QQ("QQ", "qq"),
    VELOCITY("velocity", "minecraft");

    private final String name;
    private final String configNamespace;

    private Platform(String name) {
        this(name, name);
    }

    private Platform(String name, String configNamespace) {
        this.name = name;
        this.configNamespace = configNamespace;
    }

    public String getName() {
        return this.name;
    }

    public String getConfigNamespace() {
        return this.configNamespace;
    }
}

