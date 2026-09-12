package com.zhanganzhi.chathub.platforms.qq.dto;

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;

public class QQEvent {

    @SerializedName("self_id")
    private Long selfId;

    @SerializedName("message_id")
    private Long messageId;

    @SerializedName("real_id")
    private Long realId;

    @SerializedName("group_id")
    private Long groupId;

    private Long time;

    @SerializedName("post_type")
    private String postType;

    @SerializedName("notice_type")
    private String noticeType;

    @SerializedName("meta_event_type")
    private String metaEventType;

    @SerializedName("message_type")
    private String messageType;

    private Sender sender;

    @SerializedName("raw_message")
    private String rawMessage;

    @SerializedName("sub_type")
    private String subType;

    private JsonArray message;

    @SerializedName("message_format")
    private String messageFormat;

    @SerializedName("user_id")
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

    public String getNoticeType() {
        return this.noticeType;
    }

    public void setNoticeType(String noticeType) {
        this.noticeType = noticeType;
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

    public JsonArray getMessage() {
        return this.message;
    }

    public void setMessage(JsonArray message) {
        this.message = message;
    }

    public String getMessageFormat() {
        return this.messageFormat;
    }

    public void setMessageFormat(String messageFormat) {
        this.messageFormat = messageFormat;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}