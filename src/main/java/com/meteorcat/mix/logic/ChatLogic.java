package com.meteorcat.mix.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.meteorcat.mix.WebsocketApplication;
import com.meteorcat.mix.constant.LogicStatus;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorMapping;
import com.meteorcat.spring.boot.starter.EnableActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@EnableActor(owner = ChatLogic.class)
public class ChatLogic extends ActorConfigurer {

    /**
     * 日志
     */
    final Logger logger = LoggerFactory.getLogger(ChatLogic.class);

    /**
     * 会话列表
     */
    final Map<WebSocketSession, Long> sessions = new HashMap<>();

    /**
     * 加入世界聊天队列
     *
     * @param session 会话
     */
    @ActorMapping(value = 200, state = {LogicStatus.Program})
    public void join(WebSocketSession session, Long uid) {
        if (!sessions.containsKey(session)) {
            sessions.put(session, uid);
        }
    }


    /**
     * 推送世界频道
     * Example: { "value":201,"args":{ "text": "Hello.World"}}
     * @param runtime 运行时
     * @param session 会话
     * @param args 会话
     * @throws IOException Error
     */
    @ActorMapping(value = 201, state = {LogicStatus.Authorized})
    public void talk(WebsocketApplication runtime, WebSocketSession session, JsonNode args) throws IOException {
        JsonNode textNode = args.get("text");
        if (textNode == null || !textNode.isTextual()) {
            return;
        }

        String message = textNode.asText();
        for (Map.Entry<WebSocketSession, Long> user : sessions.entrySet()) {
            WebSocketSession owner = user.getKey();
            if (!session.equals(owner)) {
                if (owner.isOpen()) {
                    runtime.push(owner, 201, new HashMap<>() {{
                        put("text", message);
                    }});
                }
            }
        }
    }
}
