/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.platforms.qq.dto;

import com.alibaba.fastjson2.JSONArray;
import com.zhanganzhi.chathub.platforms.qq.dto.Sender;

public class QQBotEvent {
    private String postType;
    private String messageType;
    private String messageFormat;
    private Long groupId;
    private Long messageId;
    private Sender sender;
    private JSONArray message;

    public String getPostType() {
        return this.postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getMessageType() {
        return this.messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageFormat() {
        return this.messageFormat;
    }

    public void setMessageFormat(String messageFormat) {
        this.messageFormat = messageFormat;
    }

    public Long getGroupId() {
        return this.groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getMessageId() {
        return this.messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Sender getSender() {
        return this.sender;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public JSONArray getMessage() {
        return this.message;
    }

    public void setMessage(JSONArray message) {
        this.message = message;
    }
}

