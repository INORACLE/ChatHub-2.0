package com.zhanganzhi.chathub.core.formatter;

import java.util.List;

public class FormattingContent {

    private final String server;
    private final String dimension;
    private final String serverFrom;
    private final String serverTo;
    private final String name;
    private final String sender;
    private final String target;
    private final String message;
    private final Integer count;
    private final List<String> playerList;

    private FormattingContent(Builder builder) {
        this.server = builder.server;
        this.dimension = builder.dimension;
        this.serverFrom = builder.serverFrom;
        this.serverTo = builder.serverTo;
        this.name = builder.name;
        this.sender = builder.sender;
        this.target = builder.target;
        this.message = builder.message;
        this.count = builder.count;
        this.playerList = builder.playerList;
    }

    public String getServer() {
        return this.server;
    }

    public String getDimension() {
        return this.dimension;
    }

    public String getServerFrom() {
        return this.serverFrom;
    }

    public String getServerTo() {
        return this.serverTo;
    }

    public String getName() {
        return this.name;
    }

    public String getSender() {
        return this.sender;
    }

    public String getTarget() {
        return this.target;
    }

    public String getMessage() {
        return this.message;
    }

    public Integer getCount() {
        return this.count;
    }

    public List<String> getPlayerList() {
        return this.playerList;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String server;
        private String dimension;
        private String serverFrom;
        private String serverTo;
        private String name;
        private String sender;
        private String target;
        private String message;
        private Integer count;
        private List<String> playerList;

        public Builder server(String server) {
            this.server = server;
            return this;
        }

        public Builder dimension(String dimension) {
            this.dimension = dimension;
            return this;
        }

        public Builder serverFrom(String serverFrom) {
            this.serverFrom = serverFrom;
            return this;
        }

        public Builder serverTo(String serverTo) {
            this.serverTo = serverTo;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder sender(String sender) {
            this.sender = sender;
            return this;
        }

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder count(Integer count) {
            this.count = count;
            return this;
        }

        public Builder playerList(List<String> playerList) {
            this.playerList = playerList;
            return this;
        }

        public FormattingContent build() {
            return new FormattingContent(this);
        }
    }
}