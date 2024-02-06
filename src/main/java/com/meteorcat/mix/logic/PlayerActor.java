package com.meteorcat.mix.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.meteorcat.mix.WebsocketApplication;
import com.meteorcat.mix.constant.ActorStatus;
import com.meteorcat.mix.constant.Protocols;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorMapping;
import com.meteorcat.spring.boot.starter.EnableActor;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;

/**
 * 玩家信息
 */
@EnableActor(owner = PlayerActor.class)
public class PlayerActor extends ActorConfigurer {
    @Override
    public void init() {

    }

    @Override
    public void destroy() {

    }


    /**
     * 校验玩家存在
     *
     * @param app     应用
     * @param session 会话
     * @param uid     玩家ID
     */
    @ActorMapping(value = Protocols.SYS_PLAYER_EXISTS, state = ActorStatus.Memory)
    public void exists(WebsocketApplication app, WebSocketSession session, Long uid) {
        app.push(session, Protocols.AUTH_LOGIN_SUCCESS, new HashMap<>(2) {{
            put("timestamp", System.currentTimeMillis());
            put("scene", 1);// 确定切换的关卡
        }});
    }


    /**
     * 数据库生成实体
     * 示例: { "value: 200, "args": { "nickname":"昵称","gender":0|1|2,....... } }
     */
    @ActorMapping(value = Protocols.PLAYER_CREATE, state = ActorStatus.Authorized)
    public void create(WebsocketApplication app, WebSocketSession session, JsonNode data) {
        // 玩家实体对象比较复杂, 如果简单游戏只有昵称性别, 如果复杂则有玩家头像|加点倾向|玩家职业等

    }
}
