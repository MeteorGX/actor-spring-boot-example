package com.meteorcat.mix.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.meteorcat.mix.WebsocketApplication;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorMapping;
import com.meteorcat.spring.boot.starter.EnableActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * 加载单元测试服务
 * EnableActor 声明这个类是挂载内存的实例
 * ActorConfigurer 是必须要集成的 Actor 集成类
 * 内部集成线程安全处理和消息队列, 也允许被 Container 扫描出这个类并且标识为 Actor
 */
@EnableActor(owner = TestLogic.class)
public class TestLogic extends ActorConfigurer {

    /**
     * 日志对象
     */
    final Logger logger = LoggerFactory.getLogger(TestLogic.class);

    /**
     * 服务启动时候加载初始化, ActorConfigurer 的初始化回调
     * 一般可以启动服务时候加载 xlsx/csv 表到类当中或者数据库加载游戏参数
     *
     * @throws Exception Error
     */
    @Override
    public void init() throws Exception {
        super.init();
        logger.info("测试单元启动");
    }

    /**
     * 服务退出时候退出运行, ActorConfigurer 的退出回调
     * 一般都是整个进程退出会调用的, 用于退出之前保存写入那些用户数据
     *
     * @throws Exception Error
     */
    @Override
    public void destroy() throws Exception {
        super.destroy();
        logger.info("测试单元退出");
    }

    /**
     * 回显服务, 这里就是自己编写业务代码
     * ActorMapping 就是声明 Actor 的入口, value 必须是全局唯一, state 则是允许访问的权限.
     * 默认 state 空代表允许被所有客户端直接访问
     * Example: { "value":10,"args":{ "text":"hello.world" } }
     *
     * @param runtime 运行时
     * @param session 会话
     * @param args    参数
     */
    @ActorMapping(value = 10)
    public void echo(WebsocketApplication runtime, WebSocketSession session, JsonNode args) throws IOException {
        // 现在不需要在 Actor 内部处理推送,只需要移交消息队列推送
        if (args != null) {
            runtime.push(session, args.toString());
        }
    }
}
