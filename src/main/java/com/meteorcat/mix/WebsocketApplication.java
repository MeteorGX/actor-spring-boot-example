package com.meteorcat.mix;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meteorcat.mix.constant.LogicStatus;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
     * 玩家目前的登录状态 - 采用线程安全
     */
    final Map<WebSocketSession, Integer> online = new ConcurrentHashMap<>();

    /**
     * 玩家登录 UID 标识 - 采用线程安全
     */
    final Map<WebSocketSession, Long> users = new ConcurrentHashMap<>();


    /**
     * Json 解析器
     */
    final ObjectMapper mapper = new ObjectMapper();


    /**
     * 消息队列 - 数据采用线程安全处理
     */
    final Queue<MessageFrame> messages = new ConcurrentLinkedDeque<>();


    /**
     * 切换玩家会话状态
     *
     * @param session 会话
     * @param state   状态
     */
    public void setState(WebSocketSession session, Integer state) {
        if (online.containsKey(session)) {
            online.put(session, state);
        }
    }

    /**
     * 设置玩家ID
     *
     * @param session 会话
     * @param uid     Long
     */
    public void setUid(WebSocketSession session, Long uid) {
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
     * @return WebSocketSession
     */
    public WebSocketSession getSession(Long uid) {
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
    public Long getUid(WebSocketSession session) {
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
        this.container.scheduleAtFixedRate(() -> {
            // 获取消息队列数据
            MessageFrame frame = messages.poll();
            if (frame != null && frame.session().isOpen()) {
                try {
                    // 推送数据 OR 关闭会话
                    if (frame.message() == null) {
                        frame.session().close();
                    } else {
                        frame.session().sendMessage(frame.message());
                    }
                } catch (IOException e) {
                    logger.warn(e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }, 0, 1000L, TimeUnit.MILLISECONDS);
    }


    /**
     * Established
     *
     * @param session handler
     * @throws Exception Error
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        logger.debug("Established = {}", session);
        online.put(session, LogicStatus.None);
    }


    /**
     * Closed
     *
     * @param session handler
     * @param status  close state
     * @throws Exception Error
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        logger.debug("Close = {},{}", session, status);
        online.remove(session);
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
        configurer.invoke(value, online.get(session), this, session, args);
    }


    /**
     * 推送消息给队列处理
     *
     * @param session 会话
     * @param value   响应协议值
     * @param args    响应JSON
     * @throws IOException Error
     */
    public void push(WebSocketSession session, Integer value, Map<String, Object> args) throws IOException {
        Map<String, Object> response = new HashMap<>() {{
            put("value", value);
            put("args", args);
        }};
        push(session, mapper.writeValueAsString(response));
    }

    /**
     * 推送消息给队列处理
     *
     * @param session 会话
     * @param text    数据
     */
    public void push(WebSocketSession session, String text) {
        messages.add(new MessageFrame(session, new TextMessage(text)));
    }

    /**
     * 等待关闭
     *
     * @param session 会话
     */
    public void quit(WebSocketSession session) {
        messages.add(new MessageFrame(session, null));
    }
}
