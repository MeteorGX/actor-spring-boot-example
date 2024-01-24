package com.meteorcat.mix.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.meteorcat.mix.WebsocketApplication;
import com.meteorcat.mix.model.PlayerModel;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorMapping;
import com.meteorcat.spring.boot.starter.EnableActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

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
     * 这里其实不是最终设计, 只是为了先临时挂载在进程内存当中测试业务逻辑
     */
    final Map<Long, PlayerModel> players = new HashMap<>();


    /**
     * 初始化方法
     * 这里其实应该加载测试配表, 提供给默认创建玩家信息数据
     *
     * @throws Exception Error
     */
    @Override
    public void init() throws Exception {
        super.init();
        // todo: 启动时加载策划配表
    }

    /**
     * 退出方法
     * 将挂载更新过的实体写入到数据库内部完成落地
     *
     * @throws Exception Error
     */
    @Override
    public void destroy() throws Exception {
        super.destroy();
        // todo: 退出时需要把之前变动数据写入数据库
    }


    /**
     * 服务端内部调用的指令, 确认玩家是否存在, 不存在就读表配置生成实体
     * 注意: 这里的 state 可以自己定义, 这里设为 3 用来提供给进程互相调用
     */
    @ActorMapping(value = 300, state = {3})
    public void check(Long uid) {
        // todo: 数据库先检索出样例加载到内存当中, 如果数据库不存在副本则需要帮助先在数据库落地实体

        // 这里模拟账号写入
        if (!players.containsKey(uid)) {
            PlayerModel player = new PlayerModel();
            player.setUid(uid);
            player.setNickname(String.format("玩家 - %d", uid));
            player.setGold(1000);// 创建账号获取资源, 这里其实应该读取策划配表
            player.setScene(0);// 默认账号应该切换客户端场景ID, 这里其实也是需要策划配表确认
            players.put(uid, player);
            logger.info("已经挂载玩家实体: {}", player);
        }
    }


    /**
     * 暴露给已经登录玩家接口
     * 用来登录之后加载时候获取玩家信息保存本地
     */
    @ActorMapping(value = 301, state = {1})
    public void info(WebsocketApplication runtime, WebSocketSession session, JsonNode args) {
        Long uid = runtime.getUid(session);

        // todo: 提供给客户端玩家所有道具|数值|红点等信息用于加载渲染
    }
}

