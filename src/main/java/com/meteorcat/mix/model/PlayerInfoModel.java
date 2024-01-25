package com.meteorcat.mix.model;

import jakarta.persistence.*;

import java.io.Serializable;

/**
 * 玩家实体数据
 */
@Entity
@Table(name = "player_info")
public class PlayerInfoModel implements Serializable {

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
    private Integer gold;


    /**
     * 玩家目前等级, 如果游戏是轻度没有等级体系可以删除
     */
    @Column(nullable = false, columnDefinition = "INT COMMENT '玩家目前等级'")
    private Integer level = 1;


    /**
     * 玩家目前经验,  如果游戏是轻度没有等级体系可以删除
     */
    private Integer exp = 0;


    /**
     * 玩家疲劳点数, 比较常见副本疲劳值设定
     * 二次元游戏比较多关卡消耗疲劳点数, 充值提升疲劳上限进入副本
     */
    @Column(nullable = false, columnDefinition = "INT COMMENT '玩家疲劳点数'")
    private Integer fatigue;


    /**
     * 玩家道具信息: { "1": 10, "2": 20 }, 后续这里会映射成MAP格式, 目前先采取String
     */
    @Column(nullable = false, columnDefinition = "JSON COMMENT 'JSON标识保存玩家道具'")
    private String item = "{}";


    /**
     * 场景ID,
     */
    @Column(nullable = false, columnDefinition = "INT COMMENT '某些战斗没结束需要重新进入需要读取该值继续战斗'")
    private Integer scene = 0;


    /**
     * 账号的创建时间
     */
    @Column(nullable = false, columnDefinition = "BIGINT COMMENT '账号创建时间'")
    private Long createTime;

    /**
     * 账号登录时间
     */
    @Column(nullable = false, columnDefinition = "BIGINT COMMENT '账号登录时间'")
    private Long updateTime;


    /* 以下是手动编写的 setter/getter/toString, 如果引入 lombok 就不用编写以下样板代码 ------------- */

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setScene(Integer scene) {
        this.scene = scene;
    }

    public Integer getScene() {
        return scene;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public Long getUid() {
        return uid;
    }

    public void setGold(Integer gold) {
        this.gold = gold;
    }

    public Integer getGold() {
        return gold;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setExp(Integer exp) {
        this.exp = exp;
    }

    public Integer getExp() {
        return exp;
    }


    public void setItem(String item) {
        this.item = item;
    }

    public String getItem() {
        return item;
    }


    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getLevel() {
        return level;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setFatigue(Integer fatigue) {
        this.fatigue = fatigue;
    }

    public Integer getFatigue() {
        return fatigue;
    }

    @Override
    public String toString() {
        return "PlayerInfoModel{" +
                "uid=" + uid +
                ", nickname='" + nickname + '\'' +
                ", gold=" + gold +
                ", level=" + level +
                ", exp=" + exp +
                ", fatigue=" + fatigue +
                ", item='" + item + '\'' +
                ", scene=" + scene +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
