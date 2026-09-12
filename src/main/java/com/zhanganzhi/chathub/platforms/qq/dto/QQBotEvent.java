package com.zhanganzhi.chathub.platforms.qq.dto;

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;

public class QQBotEvent {

    @SerializedName("post_type")
    private String postType;

    @SerializedName("message_type")
    private String messageType;

    @SerializedName("message_format")
    private String messageFormat;

    @SerializedName("group_id")
    private Long groupId;

    @SerializedName("message_id")
    private Long messageId;

    private Sender sender;

    private JsonArray message;

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

    public JsonArray getMessage() {
        return this.message;
    }

    public void setMessage(JsonArray message) {
        this.message = message;
    }
}