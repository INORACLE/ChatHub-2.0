package com.zhanganzhi.chathub.platforms.qq.dto;

import com.google.gson.annotations.SerializedName;

public class Sender {

    @SerializedName("user_id")
    private Long userId;

    private String nickname;

    private String card;

    private String role;

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getCard() {
        return this.card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}