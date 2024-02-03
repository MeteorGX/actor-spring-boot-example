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
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 竞速游戏 Actor
 */
@EnableActor(owner = RunLogic.class)
public class RunLogic extends ActorConfigurer {


    /**
     * 日志记录
     */
    final Logger logger = LoggerFactory.getLogger(RunLogic.class);


    /**
     * 线程安全的同步哈希表
     */
    final Map<Long, ScheduledFuture<?>> frames = new HashMap<>();


    /**
     * 暂停状态合集, 提供给玩家触发暂停游戏选项
     */
    final Map<Long, Boolean> pauses = new HashMap<>();

    /**
     * 退出游戏状态
     */
    final Map<Long, Boolean> cancel = new HashMap<>();


    /**
     * 这里设定该赛道长度, 超过长度表示到达终点可以结算并准备退出赛道场景了.
     */
    final static int length = 8000;

    /**
     * 目前玩家所在的位置
     */
    final Map<Long, Integer> positions = new HashMap<>();


    /**
     * 启动调用
     */
    @Override
    public void init() {

    }

    /**
     * 退出调用
     */
    @Override
    public void destroy() {

    }


    /**
     * 进入竞速场景
     * Example: { "value":700,"args":{}}
     *
     * @param runtime 运行时
     * @param session 会话
     * @param args    参数
     */
    @ActorMapping(value = 700, state = {LogicStatus.Authorized, LogicStatus.Gaming})
    public void enter(WebsocketApplication runtime, WebSocketSession session, JsonNode args) {
        // 获取玩家ID和切换成游戏中状态
        Long uid = runtime.getUid(session);
        runtime.setState(session, LogicStatus.Gaming);


        // 判断之前是否有没有需要取消的任务
        ScheduledFuture<?> task = frames.get(uid);
        if (task != null) {
            // todo: 取消并结算之前的帧任务并结算
            task.cancel(false);
        }


        // 初始化对应帧状态
        pauses.put(uid, false);
        cancel.put(uid, false);
        positions.put(uid, 0);


        // 进去竞速场景需要知道玩家速度并换算每帧调用移动到所在位置
        // 这里本来是玩家身上属性决定, 这里采用随机处理
        Random random = new Random();
        int increment = random.nextInt(10);
        long start = System.currentTimeMillis();


        // 创建帧同步更新
        // 注意这里同步帧参照 Unity3d 的更新帧数周期
        // Unity默认 update 方法默认每秒执行 60 次
        // 1s=1000ms, 折算 1000/60 ≈ 17ms 就需要调用1次
        // 注意这里仅仅做示范, 具体情况帧提交更新是需要优化
        long frameMill = 17L;
        long startMill = 1000L; // 一般需要等待触发时间, 这里设定为 1s 之后就开始起跑
        ActorEventContainer container = getContainer();
        frames.put(uid, container.scheduleAtFixedRate(() -> {
            try {
                update(runtime, uid, session, increment, start);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }, startMill, frameMill, TimeUnit.MILLISECONDS));
    }


    /**
     * 定时更新帧数函数
     *
     * @param runtime 运行时
     * @param uid     玩家ID
     * @param session 会话
     */
    private void update(WebsocketApplication runtime, Long uid, WebSocketSession session, int increment, long start) throws IOException {
        // 如果链接关闭直接算作退出游戏计算
        if (!session.isOpen() || cancel.get(uid)) {
            runtime.push(session, 703, new HashMap<>(1) {{
                put("message", "竞速失败");
            }});
            clear(uid);
            return;
        }

        // 暂停时候不需要执行逻辑
        if (pauses.get(uid)) {
            return;
        }


        // 游戏进行中
        Integer pos = positions.get(uid);
        pos += increment;
        positions.put(uid, pos);


        // 确定是否到达终点
        if (pos > length) {
            runtime.push(session, 704, new HashMap<>(3) {{
                put("message", "竞速完成");
                put("start", start); // 比赛起始时间戳
                put("end", System.currentTimeMillis());// 比赛结束时间戳
            }});
            clear(uid);
            return;
        }


        // 还在移动, 返回给客户端目前位置
        Integer finalPos = pos;

        runtime.push(session, 705, new HashMap<>(1) {{
            put("position", finalPos);
        }});
    }


    /**
     * 暂停游戏
     * Example: { "value":701,"args":{}}
     *
     * @param runtime 运行时
     * @param session 会话
     * @param args    参数
     */
    @ActorMapping(value = 701, state = {LogicStatus.Authorized, LogicStatus.Gaming})
    public void pause(WebsocketApplication runtime, WebSocketSession session, JsonNode args) {
        Long uid = runtime.getUid(session);
        if (pauses.containsKey(uid)) {
            // 如果暂停就切换继续游戏, 如果游戏中切换成暂停
            pauses.put(uid, !pauses.get(uid));
        }
    }


    /**
     * 退出游戏
     * Example: { "value":702,"args":{}}
     *
     * @param runtime 运行时
     * @param session 会话
     * @param args    参数
     */
    @ActorMapping(value = 702, state = {LogicStatus.Authorized, LogicStatus.Gaming})
    public void quit(WebsocketApplication runtime, WebSocketSession session, JsonNode args) {
        Long uid = runtime.getUid(session);
        if (cancel.containsKey(uid)) {
            cancel.put(uid, true);
        }
    }

    /**
     * 清空状态
     *
     * @param uid 玩家ID
     */
    private void clear(Long uid) {
        ScheduledFuture<?> task = frames.get(uid);
        if (task != null) {
            // 清空状态
            task.cancel(false);
            frames.remove(uid);
            cancel.remove(uid);
            pauses.remove(uid);
            positions.remove(uid);
        }
    }
}
