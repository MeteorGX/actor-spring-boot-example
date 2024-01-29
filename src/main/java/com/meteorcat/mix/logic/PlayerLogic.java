package com.meteorcat.mix.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.meteorcat.mix.WebsocketApplication;
import com.meteorcat.mix.constant.LogicStatus;
import com.meteorcat.mix.model.PlayerInfoModel;
import com.meteorcat.mix.model.server.PlayerInfoServer;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorEventContainer;
import com.meteorcat.spring.boot.starter.ActorMapping;
import com.meteorcat.spring.boot.starter.EnableActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 玩家信息 Actor
 */
@EnableActor(owner = PlayerLogic.class)
public class PlayerLogic extends ActorConfigurer {

    /**
     * 日志句柄
     */
    final Logger logger = LoggerFactory.getLogger(PlayerLogic.class);

    /**
     * 玩家数据实体服务
     */
    final PlayerInfoServer playerInfoServer;


    /**
     * 落地任务, 用来取消任务
     */
    ScheduledFuture<?> event = null;


    /**
     * 初始化
     *
     * @param playerInfoServer 玩家数据实体服务
     */
    public PlayerLogic(PlayerInfoServer playerInfoServer) {
        this.playerInfoServer = playerInfoServer;
    }


    /**
     * 初始化方法
     * 这里其实应该加载测试配表, 提供给默认创建玩家信息数据
     *
     */
    @Override
    public void init() {
        // todo: 启动时加载策划配表


        // 启动的时候定时运行异步数据库写入任务
        // 这里3秒检索下需要异步落地的任务, 具体可以自己调整
        ActorEventContainer container = getContainer();
        if (container != null) {
            event = container.scheduleAtFixedRate(this::flushPlayer, 3L, 3L, TimeUnit.SECONDS);
        }
    }

    /**
     * 退出方法
     * 将挂载更新过的实体写入到数据库内部完成落地
     *
     */
    @Override
    public void destroy() {

        // 确认任务之后取消掉默认任务
        if (event != null) {
            event.cancel(false);
        }

        // 退出游戏进程的时候数据最后落地
        flushPlayer();
    }


    /**
     * 写入数据落地
     */
    public void flushPlayer() {
        for (Long mark : playerInfoServer.getMarks()) {
            // 获取目前内存挂载的数据
            PlayerInfoModel model = playerInfoServer.getByUid(mark);
            if (model != null) {
                // 落地保存进去
                logger.debug("写入数据落地数据 = {}", model);
                playerInfoServer.save(model);
                playerInfoServer.clearMark(mark);
            }
        }
    }


    /**
     * 只允许本内部调用
     * 服务端内部调用的指令, 确认玩家是否存在, 不存在就读表配置生成实体
     */
    @ActorMapping(value = 300, state = {LogicStatus.Program})
    public void check(Long uid) {

        // 测试同步写入数据库, 查找玩家数据, 如果查询到会被挂载在内存中等待以后调用
        PlayerInfoModel model = playerInfoServer.getByUid(uid);
        if (model == null) {
            // 不存在玩家就数据库同步生成玩家对象
            model = new PlayerInfoModel();
            model.setUid(uid);
            model.setLevel(1);
            model.setExp(0);
            model.setGold(1000);// 注册赠送基础资源
            model.setNickname(String.format("注册玩家%d", uid));
            model.setFatigue(100); // 默认体力值
            model.setScene(0);
            model.setCreateTime(System.currentTimeMillis());
            model.setUpdateTime(0L);

            // 数据先同步落地到数据库
            playerInfoServer.save(model);
        }
    }


    /**
     * 玩家追加游戏货币 - 用于测试
     * Example: { "value":302,"args":{ }}
     *
     * @param runtime 运行时
     * @param session 会话
     * @param args    参数
     */
    @ActorMapping(value = 302, state = {LogicStatus.Authorized, LogicStatus.Gaming})
    public void addGold(WebsocketApplication runtime, WebSocketSession session, JsonNode args) {
        // 测试追加100金币

        // 获取玩家实体
        Long uid = runtime.getUid(session);
        PlayerInfoModel model = playerInfoServer.getByUid(uid);

        // 直接玩家添加 100 金币等待延迟写入
        model.setGold(model.getGold() + 100);
        playerInfoServer.mark(uid, model);
    }
}

