package com.meteorcat.mix.logic.player;

import com.meteorcat.mix.server.PlayerInfoServer;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.EnableActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 玩家资源管理
 */
@EnableActor(owner = PlayerResourceActor.class)
public class PlayerResourceActor extends ActorConfigurer {

    /**
     * 会话日志
     */
    final Logger logger = LoggerFactory.getLogger(getClass());


    /**
     * 玩家信息服务
     */
    final PlayerInfoServer playerInfoServer;


    /**
     * 构造方法
     *
     * @param playerInfoServer 玩家信息服务
     */
    public PlayerResourceActor(PlayerInfoServer playerInfoServer) {
        this.playerInfoServer = playerInfoServer;
    }


    /**
     * 服务启动
     */
    @Override
    public void init() {
        logger.info("启动玩家资源服务");
    }

    /**
     * 服务退出
     */
    @Override
    public void destroy() {
        logger.info("退出玩家资源服务");
    }
}
