package com.meteorcat.mix.config;

import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorEventContainer;
import com.meteorcat.spring.boot.starter.ActorEventMonitor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;


/**
 * Actor 配置类
 */
@Configuration
public class ActorConfig {

    /**
     * 服务启动线程数
     */
    @Value("${actor.monitor.core:4}")
    private Integer monitorCore;

    /**
     * Spring运行时
     */
    private final ApplicationContext context;


    /**
     * 构造方法引入, 新版本不再采用 @Autowired 或者 @Resource 注解
     *
     * @param context 运行时
     */
    public ActorConfig(ApplicationContext context) {
        this.context = context;
    }




    /**
     * 配置 Actor 加载
     *
     * @return ActorEventContainer
     */
    @Bean(initMethod = "init", destroyMethod = "destroy")
    public ActorEventContainer searchActor() {
        return new ActorEventContainer(new ActorEventMonitor(monitorCore),context);
    }
}
