package com.meteorcat.mix.config;

import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorEventContainer;
import com.meteorcat.spring.boot.starter.ActorEventMonitor;
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
    @Bean
    public ActorEventContainer searchActor() {
        ActorEventContainer container = new ActorEventContainer(new ActorEventMonitor(5));
        container.setIdleThreads(1); // 预留线程处理

        // 加载目前编写逻辑 Actor
        Map<String, ActorConfigurer> classes = context.getBeansOfType(ActorConfigurer.class);
        for (Map.Entry<String, ActorConfigurer> clazz : classes.entrySet()) {
            ActorConfigurer configurer = clazz.getValue();
            for (Integer value : configurer.values()) {
                container.put(value, configurer);
            }
        }
        return container;
    }
}
