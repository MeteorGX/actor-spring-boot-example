package com.meteorcat.mix.model;

import java.io.Serializable;

/**
 * 玩家实体数据
 */
public class PlayerModel implements Serializable {

    /**
     * 玩家第三方登录uid
     */
    private Long uid;

    /**
     * 玩家昵称
     */
    private String nickname;

    /**
     * 玩家游戏内金币
     */
    private Integer gold;

    /**
     * 场景ID
     */
    private Integer scene;

    public void setGold(Integer gold) {
        this.gold = gold;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setScene(Integer scene) {
        this.scene = scene;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public Integer getGold() {
        return gold;
    }

    public Integer getScene() {
        return scene;
    }

    public Long getUid() {
        return uid;
    }

    public String getNickname() {
        return nickname;
    }
}
