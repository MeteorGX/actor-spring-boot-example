package com.meteorcat.mix.logic.player;

import com.fasterxml.jackson.databind.JsonNode;
import com.meteorcat.mix.WebsocketApplication;
import com.meteorcat.mix.constant.ActorStatus;
import com.meteorcat.mix.constant.Protocols;
import com.meteorcat.mix.model.PlayerInfoModel;
import com.meteorcat.mix.server.PlayerInfoServer;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorEventContainer;
import com.meteorcat.spring.boot.starter.ActorMapping;
import com.meteorcat.spring.boot.starter.EnableActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 玩家信息
 */
@EnableActor(owner = PlayerDetailActor.class)
public class PlayerDetailActor extends ActorConfigurer {

    /**
     * 会话日志
     */
    final Logger logger = LoggerFactory.getLogger(getClass());


    /**
     * 玩家信息服务
     */
    final PlayerInfoServer playerInfoServer;


    /**
     * 玩家数据落地任务
     */
    ScheduledFuture<?> event = null;


    /**
     * 构造方法
     *
     * @param playerInfoServer 玩家信息服务
     */
    public PlayerDetailActor(PlayerInfoServer playerInfoServer) {
        this.playerInfoServer = playerInfoServer;
    }


    /**
     * 初始化
     */
    @Override
    public void init() throws Exception {
        // 启动的时候定时运行异步数据库写入任务
        // 这里3秒检索下需要异步落地的任务, 具体可以自己调整
        ActorEventContainer container = getContainer();
        if (container != null) {
            logger.info("创建玩家数据落地服务");
            event = container.scheduleAtFixedRate(playerInfoServer::flush, 3L, 3L, TimeUnit.SECONDS);
        }
    }

    /**
     * 服务退出
     */
    @Override
    public void destroy() throws Exception {
        logger.info("退出玩家数据落地服务");
        if (event != null) {
            event.cancel(false);
        }
        playerInfoServer.flush();
    }


    /**
     * 校验玩家存在
     *
     * @param app     应用
     * @param session 会话
     * @param uid     玩家ID
     */
    @ActorMapping(value = Protocols.SYS_PLAYER_EXISTS, state = ActorStatus.Memory)
    public void check(WebsocketApplication app, WebSocketSession session, Long uid) {
        PlayerInfoModel model = playerInfoServer.findByUid(uid);
        boolean create = false;
        int scene = 2;// 可能需要客户端切换创建玩家场景

        // 如果没有玩家数据代表需要跳转创建角色场景
        // 这里需要灵活配置, 因为场景切换也需要服务端确认
        if (model == null) {
            create = true;
            scene = 1;
        }


        // 确定响应的数据
        boolean finalCreate = create;
        int finalScene = scene;
        app.push(session, Protocols.AUTH_LOGIN_SUCCESS, new HashMap<>(3) {{
            put("timestamp", System.currentTimeMillis());
            put("scene", finalScene);// 确定切换的关卡
            put("create", finalCreate);
        }});
    }


    /**
     * 数据库生成实体
     * 现在越来越多游戏支持重复昵称, 所以支持类似 nickname#id 这种类型ID, 如 MeteorCat#1 这种类型
     * 示例: { "value": 200, "args": { "nickname":"昵称","gender":0|1|2,....... } }
     */
    @ActorMapping(value = Protocols.PLAYER_CREATE, state = ActorStatus.Authorized)
    public void create(WebsocketApplication app, WebSocketSession session, JsonNode data) {
        // 玩家实体对象比较复杂, 如果简单游戏只有昵称性别, 如果复杂则有玩家头像|加点倾向|玩家职业等
        // 所以这里需要判断具体的玩家类型在数据库存在与否
        Long uid = app.getSessionUid(session);

        // 确认玩家存在, 如果存在就直接跳过任务
        PlayerInfoModel model = playerInfoServer.findByUid(uid);
        if (model != null) {
            app.push(session,Protocols.PLAYER_EXISTS);
            return;
        }


        // 获取所需的参数
        JsonNode nicknameNode = data.get("nickname");
        if (nicknameNode == null || !nicknameNode.isTextual()) {
            app.push(session,Protocols.SYS_PARAM_ERROR);
            return;
        }

        // 昵称审核, 注意这里昵称可能要关键字屏蔽
        String nickname = nicknameNode.asText();
        if (nickname.isBlank()) {
            app.push(session,Protocols.SYS_PARAM_ERROR);
            return;
        }


        // 不存在则直接创建数据库实体
        PlayerInfoModel owner = new PlayerInfoModel();
        owner.setNickname(nickname);
        owner.setGold(0);
        owner.setCreateTime(System.currentTimeMillis());
        owner.setUpdateTime(0L);
        owner = playerInfoServer.create(owner);
        logger.debug("创建玩家实体: {}", owner);


        // 响应返回玩家实体用于客户端加载
        PlayerInfoModel finalOwner = owner;
        app.push(session, Protocols.PLAYER_INFO, new HashMap<>(1) {{
            put("player", finalOwner);
        }});
    }

    /**
     * 获取玩家实体信息, 用于客户端加载玩家数据
     * 客户端加载到玩家信息就可以在本地处理所需的资源构建同步
     *
     * @param app     应用
     * @param session 会话
     * @param data    数据
     */
    @ActorMapping(value = Protocols.PLAYER_INFO, state = ActorStatus.Authorized)
    public void information(WebsocketApplication app, WebSocketSession session, JsonNode data) {
        Long uid = app.getSessionUid(session);

        // todo: 获取出玩家实体如果没有直接返回错误
        PlayerInfoModel model = playerInfoServer.findByUid(uid);
        if (model == null) {
            return;
        }


        // 直接返回玩家所有数据
        app.push(session, Protocols.PLAYER_INFO, new HashMap<>(1) {{
            put("player", model);
        }});
    }

}
