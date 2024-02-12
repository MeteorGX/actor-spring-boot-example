package com.meteorcat.mix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "tbl_player_info")
public class PlayerInfoModel {

    /**
     * 玩家第三方登录uid, 这里采用主键自递增记录
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, columnDefinition = "BIGINT COMMENT '主键ID,同时也是标识玩家UID'")
    private Long uid;

    /**
     * 玩家昵称
     */
    @Column(nullable = false, columnDefinition = "VARCHAR(64) COMMENT '玩家昵称'")
    private String nickname;

    /**
     * 玩家游戏内金币
     */
    @Column(nullable = false, columnDefinition = "BIGINT COMMENT '玩家游戏内金币数量'")
    private Integer gold = 0;

    /**
     * 账号的创建时间
     */
    @Column(nullable = false, columnDefinition = "BIGINT COMMENT '账号创建时间'")
    private Long createTime;

    /**
     * 账号最后登录时间
     */
    @Column(nullable = false, columnDefinition = "BIGINT COMMENT '账号登录时间'")
    private Long updateTime = 0L;

    /**
     * 最后登录场景
     */
    @Column(nullable = false, columnDefinition = "INT COMMENT '最后登录场景'")
    private Integer lastScene = 0;


    public Long getUid() {
        return uid;
    }

    public String getNickname() {
        return nickname;
    }

    public Integer getGold() {
        return gold;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setGold(Integer gold) {
        this.gold = gold;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "PlayerInfoModel{" +
                "uid=" + uid +
                ", nickname='" + nickname + '\'' +
                ", gold=" + gold +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
