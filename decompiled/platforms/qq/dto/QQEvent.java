/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.platforms.qq.dto;

import com.alibaba.fastjson2.JSONArray;
import com.zhanganzhi.chathub.platforms.qq.dto.Sender;

public class QQEvent {
    private Long selfId;
    private Long messageId;
    private Long realId;
    private Long groupId;
    private Long time;
    private String postType;
    private String noticeType;
    private String metaEventType;
    private String messageType;
    private Sender sender;
    private String rawMessage;
    private String subType;
    private JSONArray message;
    private String messageFormat;
    private Long userId;

    public Long getSelfId() {
        return this.selfId;
    }

    public void setSelfId(Long selfId) {
        this.selfId = selfId;
    }

    public Long getMessageId() {
        return this.messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getRealId() {
        return this.realId;
    }

    public void setRealId(Long realId) {
        this.realId = realId;
    }

    public Long getGroupId() {
        return this.groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getTime() {
        return this.time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getPostType() {
        return this.postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getMetaEventType() {
        return this.metaEventType;
    }

    public void setMetaEventType(String metaEventType) {
        this.metaEventType = metaEventType;
    }

    public String getMessageType() {
        return this.messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Sender getSender() {
        return this.sender;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public String getRawMessage() {
        return this.rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getSubType() {
        return this.subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public JSONArray getMessage() {
        return this.message;
    }

    public void setMessage(JSONArray message) {
        this.message = message;
    }

    public String getMessageFormat() {
        return this.messageFormat;
    }

    public void setMessageFormat(String messageFormat) {
        this.messageFormat = messageFormat;
    }

    public String getNoticeType() {
        return this.noticeType;
    }

    public void setNoticeType(String noticeType) {
        this.noticeType = noticeType;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}

