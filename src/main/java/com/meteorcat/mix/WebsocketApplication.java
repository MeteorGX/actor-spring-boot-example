package com.meteorcat.mix;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meteorcat.mix.constant.ActorStatus;
import com.meteorcat.mix.core.ActorMessageQueue;
import com.meteorcat.mix.core.ActorStateHashMap;
import com.meteorcat.mix.core.ActorUserHashMap;
import com.meteorcat.mix.core.MessageFrame;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorEventContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 挂载 Websocket 服务
 */
@Order
@Component
public class WebsocketApplication extends TextWebSocketHandler {


    /**
     * 日志句柄
     */
    Logger logger = LoggerFactory.getLogger(WebsocketApplication.class);

    /**
     * Actor 运行时
     */
    final ActorEventContainer container;


    /**
     * Json 解析器
     */
    final ObjectMapper mapper = new ObjectMapper();


    /**
     * 玩家目前的状态
     */
    final ActorStateHashMap status = new ActorStateHashMap();


    /**
     * 玩家关联句柄
     */
    final ActorUserHashMap users = new ActorUserHashMap();


    /**
     * 消息队列 - 数据采用线程安全处理
     */
    final ActorMessageQueue messages = new ActorMessageQueue();


    /**
     * 切换玩家会话状态
     *
     * @param session 会话
     * @param state   状态
     */
    public void setSessionState(WebSocketSession session, Integer state) {
        if (status.containsKey(session)) {
            status.put(session, state);
        }
    }

    /**
     * 设置玩家ID
     *
     * @param session 会话
     * @param uid     Long
     */
    public void setSessionUid(WebSocketSession session, Long uid) {
        if (users.containsValue(uid)) {
            for (Map.Entry<WebSocketSession, Long> user : users.entrySet()) {
                if (user.getValue().equals(uid)) {
                    users.remove(user.getKey());
                    break;
                }
            }
        }
        users.put(session, uid);
    }


    /**
     * 通过UID获取目前在线的会话
     *
     * @param uid 在线ID
     * @return WebSocketSession|Null
     */
    public WebSocketSession getSessionUid(Long uid) {
        if (users.containsValue(uid)) {
            // 反查出UID绑定会话句柄
            for (Map.Entry<WebSocketSession, Long> user : users.entrySet()) {
                if (user.getValue().equals(uid)) {
                    return user.getKey();
                }
            }
        }
        return null;
    }


    /**
     * 获取玩家ID
     *
     * @param session 会话
     * @return Uid
     */
    public Long getSessionUid(WebSocketSession session) {
        return users.get(session);
    }


    /**
     * 构造方法
     *
     * @param container Actor events
     */
    public WebsocketApplication(ActorEventContainer container) {
        this.container = container;

        // 启动时候线程池追加定时服务
        // 当数据队列为空的时候可以休眠下, 当 push 功能可以考虑继续唤醒定时任务
        this.container.scheduleAtFixedRate(
                this::fetchMessageQueue,
                0,
                17L,
                TimeUnit.MILLISECONDS
        );
    }


    /**
     * 检出会话队列待推送数据
     */
    public void fetchMessageQueue() {
        // 获取消息队列数据
        if (!messages.isEmpty()) {
            // 没有消息或者会话已经关闭跳过
            MessageFrame frame = messages.poll();
            if (frame == null) {
                return;
            }

            // 获取会话,关闭跳过
            WebSocketSession session = frame.session();
            if (!session.isOpen()) {
                return;
            }

            // 获取消息内容, 如果位 null 代表关闭
            Optional<TextMessage> data = frame.message();
            try {
                if (data.isPresent()) {
                    session.sendMessage(data.get());
                } else {
                    session.close();
                }
            } catch (IOException e) {
                logger.warn(e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Established
     *
     * @param session handler
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        logger.debug("Established = {}", session);
        status.put(session, ActorStatus.None);
    }


    /**
     * Closed
     *
     * @param session handler
     * @param reason  close state
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus reason) {
        logger.debug("Close = {},{}", session, reason);
        status.remove(session);
        users.remove(session);
    }


    /**
     * 采用JSON数据接收 { "value": 100, args:{ data.... } }
     *
     * @param session handler
     * @param message text
     * @throws Exception Error
     */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        if (message.getPayloadLength() <= 0) {
            return;
        }

        // json: { "value": 100, args:{ data.... } }
        JsonNode json = mapper.readTree(message.asBytes());
        if (!json.isObject()) {
            return;
        }

        // json.value
        JsonNode valueNode = json.get("value");
        if (valueNode == null || !valueNode.isInt()) {
            return;
        }

        // container value
        Integer value = valueNode.intValue();
        ActorConfigurer configurer = container.get(value);
        if (configurer == null) {
            return;
        }

        // json.args
        JsonNode args = json.get("args");
        args = args.isObject() ? args : null;

        // forward configurer
        logger.debug("Frame: {}", message);
        configurer.invoke(value, status.get(session), this, session, args);
    }


    /**
     * 推送消息给队列处理
     *
     * @param session 会话
     * @param value   响应协议值
     */
    public void push(WebSocketSession session, Integer value) {
        push(session, value, new HashMap<>(0));
    }


    /**
     * 推送消息给队列处理
     *
     * @param session 会话
     * @param value   响应协议值
     * @param args    响应JSON
     */
    public void push(WebSocketSession session, Integer value, Map<String, Object> args) {
        Map<String, Object> response = new HashMap<>() {{
            put("value", value);
            put("args", args);
        }};
        try {
            push(session, mapper.writeValueAsString(response));
        } catch (JsonProcessingException exception) {
            logger.error(exception.getMessage());
        }
    }


    /**
     * 推送消息给队列处理
     *
     * @param session 会话
     * @param text    数据
     */
    public void push(WebSocketSession session, String text) {
        messages.add(new MessageFrame(session, Optional.of(new TextMessage(text))));
    }

    /**
     * 等待关闭
     *
     * @param session 会话
     */
    public void quit(WebSocketSession session) {
        messages.add(new MessageFrame(session, Optional.empty()));
    }
}
