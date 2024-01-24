package com.meteorcat.mix.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.meteorcat.mix.WebsocketApplication;
import com.meteorcat.mix.constant.LogicStatus;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorEventContainer;
import com.meteorcat.spring.boot.starter.ActorMapping;
import com.meteorcat.spring.boot.starter.EnableActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 授权 Actor 实例
 */
@EnableActor(owner = AuthorizedLogic.class)
public class AuthorizedLogic extends ActorConfigurer {

    /**
     * 日志
     */
    final Logger logger = LoggerFactory.getLogger(AuthorizedLogic.class);


    /**
     * 心跳超时, 单位:秒
     */
    private static final Long HEARTBEAT_TIMEOUT = 15L;


    /**
     * 测试登录密钥
     */
    private String secret;


    /**
     * 初始化
     *
     * @throws Exception Error
     */
    @Override
    public void init() throws Exception {
        super.init();
        // 注意:
        //  这里本来是应该第三方认证授权后客户端提交上来 uid + secret
        //  但是开发阶段并没有接入第三方授权机制, 所以先采用随机生成字符串做固定授权
        secret = DigestUtils.md5DigestAsHex(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        logger.warn("login secret = {}", secret);
    }

    /**
     * 退出进程
     *
     * @throws Exception Error
     */
    @Override
    public void destroy() throws Exception {
        super.destroy();
        logger.warn("server quit");
    }

    /**
     * 登录
     * Example: { "value":100,"args":{ "secret": "0451d36d412830afb521fc3a3f828350", "uid": 1}}
     */
    @ActorMapping(value = 100)
    public void login(WebsocketApplication runtime, WebSocketSession session, JsonNode args) throws Exception {
        // 没有传递JSON参数最好直接关闭掉链接防止掉无意义的嗅探
        if (args == null) {
            session.close();
            return;
        }


        // 获取JSON内部参数 - uid
        JsonNode uidNode = args.get("uid");
        if (uidNode == null || !uidNode.isNumber()) {
            session.close();
            return;
        }

        // 获取JSON内部参数 - secret
        JsonNode secretNode = args.get("secret");
        if (secretNode == null || !secretNode.isTextual()) {
            session.close();
            return;
        }


        // 判断用户提交授权码是否一致
        // 这里可以考虑返回错误消息之后才中断链接, 但是后续可以自己处理优化
        // TODO: 如果上线第三方SDK的时候需要在这里验证 Redis 内部授权码, 匹配完成才能切换成登录状态
        if (!secret.equals(secretNode.asText())) {
            session.close();
            return;
        }


        // 切换 Websocket 到设置在线UID和在线状态
        Long uid = uidNode.asLong();

        // 这时候就要判断玩家是否在线, 如果在线就推送强制下线并关闭之前链接
        WebSocketSession owner = runtime.getSession(uid);
        if (owner != null) {
            // 找到会话返回消息并且强制踢下线
            runtime.push(owner, 110, new HashMap<>(0));
            runtime.quit(owner);
        }

        // 重新写入在线状态
        runtime.setUid(session, uid);
        runtime.setState(session, LogicStatus.Authorized);// 切换已登录



        // 获取全局 Actor 容器
        ActorEventContainer container = runtime.getContainer();


        // 通知玩家模块挂载实体数据到内存
        ActorConfigurer playerConfigurer = container.get(300);
        if (playerConfigurer != null){
            playerConfigurer.invoke(300,3,uid);
        }



        // 通知聊天频道已经在线
        ActorConfigurer chatConfigurer = container.get(200);
        if (chatConfigurer != null) {
            chatConfigurer.invoke(200, LogicStatus.Program, session, uid);
        }



        // 验证完成, 服务端推送心跳包用来维持链接活跃
        container.schedule(() -> {
            try {
                heartbeat(runtime, session);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
    }


    /**
     * 心跳包方法, 用于玩家保持在线
     *
     * @param runtime 运行时
     * @param session 会话
     * @throws IOException Error
     */
    public void heartbeat(WebsocketApplication runtime, WebSocketSession session) throws IOException {
        // 如果会话关闭的话
        if (!session.isOpen()) {
            return;
        }

        // 这里推送给外部会话队列
        // 这里响应代码可以自己处理, 后续优化可能需要定义全局静态码表
        runtime.push(session, 100, new HashMap<>() {{
            put("heartbeat", System.currentTimeMillis());
        }});

        // 延迟下一次递归进入该方法
        runtime.getContainer().schedule(() -> {
            try {
                heartbeat(runtime, session);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
    }


    /**
     * 这里编写授权样例, 可以看到 state 设定成只能被 {1,2,3} 其中之一访问
     * 如果没有先跑 100 登录授权接口, 那么这个接口是无法被访问到的
     */
    @ActorMapping(value = 111, state = {1, 2, 3})
    public void tick(WebsocketApplication runtime, WebSocketSession session, JsonNode args) throws IOException {
        logger.info("已被授权, 可以被访问到了");
        runtime.push(session, 111, new HashMap<>() {{
            put("message", "hello.world");
        }});
    }

}
