package com.meteorcat.mix.logic.player;

import com.meteorcat.mix.constant.ActorStatus;
import com.meteorcat.mix.constant.Protocols;
import com.meteorcat.mix.model.PlayerInfoModel;
import com.meteorcat.mix.server.PlayerInfoServer;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorMapping;
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


    /**
     * 修改玩家金币
     *
     * @param uid  玩家UID
     * @param gold 修改金币, 正数为追加, 负数为扣除
     */
    @ActorMapping(value = Protocols.SYS_CHANGE_GOLD, state = ActorStatus.Memory)
    public void changeGold(Long uid, Integer gold) {
        PlayerInfoModel model = playerInfoServer.findByUid(uid);
        if (model != null) {
            logger.debug("玩家:{} 修改金币 {}", uid, gold);
            model.setGold(model.getGold() + gold);
            playerInfoServer.mark(uid, model);
        }
    }

}
