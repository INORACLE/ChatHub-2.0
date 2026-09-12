package com.zhanganzhi.chathub.platforms;

public enum Platform {
    DISCORD("discord"),
    KOOK("kook"),
    QQ("QQ", "qq"),
    MINECRAFT("minecraft", "minecraft");

    private final String name;
    private final String configNamespace;

    Platform(String name) {
        this(name, name);
    }

    Platform(String name, String configNamespace) {
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