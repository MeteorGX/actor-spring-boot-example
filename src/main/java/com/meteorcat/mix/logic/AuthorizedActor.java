package com.meteorcat.mix.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.meteorcat.mix.WebsocketApplication;
import com.meteorcat.mix.constant.ActorStatus;
import com.meteorcat.mix.constant.Protocols;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorEventContainer;
import com.meteorcat.spring.boot.starter.ActorMapping;
import com.meteorcat.spring.boot.starter.EnableActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import org.springframework.web.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 授权登录相关
 */
@EnableActor(owner = AuthorizedActor.class)
public class AuthorizedActor extends ActorConfigurer {

    /**
     * 日志对象
     */
    final Logger logger = LoggerFactory.getLogger(getClass());


    /**
     * 玩家授权UID
     */
    final static String UID_NAME = "uid";

    /**
     * 玩家授权SECRET
     */
    final static String SECRET_NAME = "secret";


    /**
     * 心跳包推送周期
     */
    final static long HEARTBEAT_SEC = 30L;


    /**
     * 登录授权码 - 先采用静态
     */
    String secret;

    /**
     * 初始化
     */
    @Override
    public void init() {
        secret = DigestUtils.md5DigestAsHex(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        logger.debug("启动登录验证 = {}", secret);
    }

    /**
     * 退出
     */
    @Override
    public void destroy() {
        logger.debug("退出登录验证");
    }


    /**
     * 请求登录
     * 示例: { "value": 100, "args": { "uid":1,"secret":"" } }
     *
     * @param app     应用
     * @param session 会话
     * @param data    数据
     */
    @ActorMapping(value = Protocols.AUTH_LOGIN, state = {ActorStatus.None})
    public void login(WebsocketApplication app, WebSocketSession session, JsonNode data) {
        // 确认参数
        if (!data.has(UID_NAME) || !data.has(SECRET_NAME)) {
            app.push(session, Protocols.AUTH_ERROR_BY_SECRET);
            return;
        }

        // 获取参数
        JsonNode uidNode = data.get(UID_NAME);
        JsonNode secretNode = data.get(SECRET_NAME);
        if (!uidNode.isNumber() || !secretNode.isTextual()) {
            app.push(session, Protocols.AUTH_ERROR_BY_SECRET);
            return;
        }


        // 验证登录
        if (secret.isBlank() || !secret.equals(secretNode.asText())) {
            app.push(session, Protocols.AUTH_ERROR_BY_EXISTS);
            return;
        }

        // 获取玩家ID
        Long uid = uidNode.asLong();


        // 确认是否有其他目前在登录, 直接顶号提出
        WebSocketSession other = app.getSessionUid(uid);
        if (other != null) {
            app.push(other, Protocols.AUTH_ERROR_BY_OTHER);
            app.quit(other);
        }


        // 设置目前在线并且切换状态
        app.setSessionUid(session, uid);
        app.setSessionState(session, ActorStatus.Authorized);


        // 推送心跳包定时事件
        ActorEventContainer container = getContainer();
        container.schedule(
                () -> heartbeat(app, session, container),
                HEARTBEAT_SEC,
                TimeUnit.SECONDS
        );


        // 确定是否需要创建账号
        ActorConfigurer configurer = container.get(Protocols.SYS_PLAYER_EXISTS);
        if (configurer != null) {
            configurer.invoke(Protocols.SYS_PLAYER_EXISTS, ActorStatus.Memory, app, session, uid);
        }
    }


    /**
     * 心跳包推送 - 采用定时递归
     *
     * @param app       应用
     * @param session   会话
     * @param container 定时器
     */
    private void heartbeat(WebsocketApplication app, WebSocketSession session, ActorEventContainer container) {
        if (!session.isOpen()) {
            return;
        }

        // 推送心跳包, 延时等待调用
        app.push(session, Protocols.SYS_HEARTBEAT);
        container.schedule(
                () -> heartbeat(app, session, container),
                HEARTBEAT_SEC,
                TimeUnit.SECONDS
        );
    }

}
