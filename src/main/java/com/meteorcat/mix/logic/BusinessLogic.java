package com.meteorcat.mix.logic;

import ch.qos.logback.core.model.processor.ProcessorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meteorcat.mix.WebsocketApplication;
import com.meteorcat.mix.constant.LogicStatus;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorMapping;
import com.meteorcat.spring.boot.starter.EnableActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 运营商业指令
 */
@EnableActor(owner = BusinessLogic.class)
public class BusinessLogic extends ActorConfigurer {

    /**
     * 日志对象
     */
    final Logger logger = LoggerFactory.getLogger(BusinessLogic.class);


    /**
     * 全局会话句柄
     */
    WebsocketApplication application;


    /**
     * 定时任务
     */
    ScheduledFuture<?> task = null;


    /**
     * Redis监听队列
     */
    final RedisTemplate<String, String> redis;

    /**
     * 这里需要初始化指定Redis消息队列, 一般是 类型:服务器ID 做KEY
     */
    final static String productName = "business:1";


    /**
     * JSON数据解析器, 这里按照 { "value":xxx, "args":{ } } 转发到自己内部 Actor
     */
    final ObjectMapper mapper = new ObjectMapper();


    /**
     * 初始化
     *
     * @param redis Redis监听队列
     */
    public BusinessLogic(RedisTemplate<String, String> redis) {
        this.redis = redis;
    }


    /**
     * 系统初始化
     */
    @Override
    public void init() {
        if (task == null) {
            // 监听队列不需要太过频繁, 所以只需要1s执行确定下队列是否为空
            task = getMonitor().scheduleAtFixedRate(() -> {
                // 获取目前的最新消息
                String message = redis.opsForList().rightPop(productName);
                if (message == null || message.isBlank()) {
                    return;
                }

                // 确定消息是否能够解析
                JsonNode node;
                try {
                    node = mapper.readTree(message);
                } catch (Exception exception) {
                    logger.error("运营消息解析错误: {}", message);
                    return;
                }
                if (node == null) {
                    return;
                }


                // 解析出 { "value":xxx, "args":{ } } 格式
                JsonNode valueNode = node.get("value");
                JsonNode argNode = node.get("args");
                if (valueNode == null || !valueNode.isInt() || !argNode.isObject()) {
                    logger.error("运营消息解析错误: {}", message);
                    return;
                }


                // 推送到内部执行
                Integer value = valueNode.asInt();
                this.invoke(value, LogicStatus.Program, argNode);
            }, 1L, 1L, TimeUnit.SECONDS);
        }
    }


    /**
     * 系统退出
     */
    @Override
    public void destroy() {
        // 需要监听队列任务
        if (task != null) {
            task.cancel(false);
        }
    }


    /**
     * 设置目前登录
     */
    @ActorMapping(value = 900, state = LogicStatus.Program)
    public void load(WebsocketApplication application) {
        if (this.application == null) {
            this.application = application;
        }
    }


    /**
     * 发送消息给指定玩家
     *
     * @param data 数据内容
     */
    @ActorMapping(value = 901, state = LogicStatus.Program)
    public void mailToPlayer(JsonNode data) throws IOException {
        if (application == null) {
            return;
        }

        logger.info("发送消息给指定玩家: {}", data);
        JsonNode uid = data.get("uid");
        JsonNode title = data.get("title");
        JsonNode content = data.get("content");
        if (uid == null || title == null || content == null || !uid.isNumber()) {
            logger.error("消息格式错误: {}", data);
            return;
        }


        // 发送给客户端, 一般是移交给其他专用 Actor 处理, 这里先自己直接推送
        WebSocketSession session = application.getSession(uid.asLong());
        if (session == null) {
            logger.error("目前玩家不在线");
            return;
        }

        // 在线直接推送邮件
        application.push(session, 901, new HashMap<>(2) {{
            put("title", title.asText());
            put("content", content.asText());
        }});

    }

}
