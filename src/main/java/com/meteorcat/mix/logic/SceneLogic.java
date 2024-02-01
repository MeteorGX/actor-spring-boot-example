package com.meteorcat.mix.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.meteorcat.mix.WebsocketApplication;
import com.meteorcat.mix.constant.LogicStatus;
import com.meteorcat.spring.boot.starter.ActorConfigurer;
import com.meteorcat.spring.boot.starter.ActorMapping;
import com.meteorcat.spring.boot.starter.EnableActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;

/**
 * 游戏场景物品服务
 */
@EnableActor(owner = SceneLogic.class)
public class SceneLogic extends ActorConfigurer {

    /**
     * 日志句柄
     */
    final Logger logger = LoggerFactory.getLogger(SceneLogic.class);

    /**
     * 策划配置的场景奖励道具
     * 实际上这里应该是 道具ID+道具数量+奖励文本, 这里采用简约处理只记录道具ID
     */
    private final List<Integer> items = new ArrayList<>();


    /**
     * 进入内部场景之后的随机生成道具奖励id合集
     */
    private final Map<Long, List<Integer>> awards = new HashMap<>();


    /**
     * 玩家已领取道具信息
     */
    private final Map<Long, List<Integer>> received = new HashMap<>();


    @Override
    public void init() throws Exception {
        // 初始化默认奖励, 一般是读取策划配置奖励道具和概率|数量等
        Random random = new Random();
        int count = 10;
        logger.debug("战斗奖励数量: {}", count);
        for (int i = 0; i < count; i++) {
            int total = 1 + random.nextInt(99);
            logger.debug("奖励ID: {}", total);
            items.add(total);
        }


    }

    @Override
    public void destroy() throws Exception {

    }


    /**
     * 进入触发场景
     * Example: { "value":600,"args":{}}
     *
     * @param runtime 运行时
     * @param session 会话
     * @param args    参数
     */
    @ActorMapping(value = 600, state = {LogicStatus.Authorized, LogicStatus.Gaming})
    public void enter(WebsocketApplication runtime, WebSocketSession session, JsonNode args) throws IOException {
        // 获取玩家ID, 并且生成该玩家在这次进入地图的奖励道具
        Long uid = runtime.getUid(session);

        // todo: 这里一般策划配置奖励道具|最低-最高数量, 目前采用固定值
        int count = 5;// 假设策划配置每次更新场景新增道具数量写死为5
        List<Integer> item = new ArrayList<>(count);
        int sz = items.size();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            int value = random.nextInt(sz - 1);
            Integer element = items.get(value);
            if (item.contains(element)) {
                i--;
            } else {
                item.add(element);
            }
        }

        // 写入进场景后道具信息, 暂时先进入场景之后更新
        // todo: 这里有个问题就是场景道具进入后会导致更新道具, 正确应该由策划配置地图场景物品刷新配置
        // todo: 进入之后如果没满足更新条件, 自动加载上次场地道具并设置定时器准备更新, 如果满足则重新更新场景道具
        awards.put(uid, item);
        logger.debug("玩家({})更新场景道具:{}", uid, item);

        // 如果更新的时候需要清空目前玩家在该场景的领取信息
        if (received.containsKey(uid)) {
            received.get(uid).clear();
        } else {
            received.put(uid, new ArrayList<>(item.size()));
        }

        // 响应返回道具信息
        runtime.push(session, 600, new HashMap<>(1) {{
            put("awards", item);
        }});
    }

    /**
     * 打开场景内部道具奖励
     * Example: { "value":601,"args":{ "award": 60 }}
     *
     * @param runtime 运行时
     * @param session 会话
     * @param args    参数
     */
    @ActorMapping(value = 601, state = {LogicStatus.Authorized, LogicStatus.Gaming})
    public void open(WebsocketApplication runtime, WebSocketSession session, JsonNode args) {
        // 判断是否带有打开的场景道具ID
        JsonNode award = args == null ? null : args.get("award");
        if (award == null || !award.isInt()) {
            // 没有直接推送奖励错误
            return;
        }

        // 获取奖励ID识别进入房间之后的道具奖励是否在其中
        Integer awardId = award.asInt();


        // 识别道具领取列表是否存在, 没有可领取的道具不用管
        Long uid = runtime.getUid(session);
        List<Integer> item = awards.get(uid);
        if (item == null || !item.contains(awardId)) {
            return;
        }

        // 已领取, 也不需要管他, 让客户端自己做表现处理
        List<Integer> receive = received.get(uid);
        if (receive.contains(awardId)) {
            return;
        }


        // 把金币加入到玩家身上, 测试玩家追加金额元素, 假设 PlayerLogic 追加 303 修改金额方法
        ActorConfigurer configurer = getContainer().get(303);
        if (configurer != null) {
            configurer.invoke(303, LogicStatus.Program, uid, awardId);

            // 切换状态为已领取
            logger.debug("追加金币: {}", awardId);
            receive.add(awardId);
        }
    }
}
