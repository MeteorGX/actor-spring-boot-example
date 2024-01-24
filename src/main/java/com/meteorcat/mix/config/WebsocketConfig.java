package com.meteorcat.mix.config;

import com.meteorcat.mix.WebsocketApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * Websocket加载配置
 */
@Configuration
@EnableWebSocket
public class WebsocketConfig implements WebSocketConfigurer {

    /**
     * 访问路径
     */
    @Value("${websocket.server.path:/}")
    private String serverPath;

    /**
     * 传输数据缓存大小
     */
    @Value("${websocket.buffer.max.size:8192}")
    private Integer bufferMaxSize;


    /**
     * 待机主动中断时间
     */
    @Value("${websocket.idle.timeout:600000}")
    private Long idleTimeout;


    /**
     * 允许跨域地址
     */
    @Value("${websocket.allowed.origins:*}")
    private String allowOrigins;

    /**
     * 默认加载的服务
     */
    private final WebsocketApplication handler;


    /**
     * 构筑方法
     * @param handler Websocket 实例
     */
    public WebsocketConfig(WebsocketApplication handler) {
        this.handler = handler;
    }

    /**
     * 注册运行时句柄
     *
     * @param registry 注册器
     */
    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        if (handler == null) {
            throw new RuntimeException("failed by WebSocketHandler: WebSocketHandler");
        }
        registry.addHandler(handler, serverPath).setAllowedOrigins(allowOrigins);
    }


    /**
     * 全局 Servlet 的配置容器
     *
     * @return ServletServerContainerFactoryBean
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(bufferMaxSize);
        container.setMaxBinaryMessageBufferSize(bufferMaxSize);
        container.setMaxSessionIdleTimeout(idleTimeout);
        return container;
    }
}
