package com.meteorcat.mix.model.server;

import com.meteorcat.mix.model.PlayerInfoModel;
import com.meteorcat.mix.model.repository.PlayerInfoRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 线上内存挂载服务
 * 注解 @Service 会把对象挂载在进程内存, 这也是我们将玩家实体挂载内存的关键服务
 * 注意这里先简单编写功能, 后续扩展|封装功能按照自己需求处理
 */
@Service
public class PlayerInfoServer {

    /**
     * 数据工厂
     */
    final PlayerInfoRepository repository;

    /**
     * 进程内存数据
     */
    final HashMap<Long, PlayerInfoModel> players = new HashMap<>();

    /**
     * 需要更新的玩家标示, 这里做好线程安全, 标识可能会多个线程共享
     */
    final List<Long> marks = new CopyOnWriteArrayList<>();

    /**
     * 构造方法
     *
     * @param repository 数据工厂
     */
    public PlayerInfoServer(PlayerInfoRepository repository) {
        this.repository = repository;
    }


    /**
     * 获取玩家实体
     *
     * @param uid 玩家ID
     * @return PlayerInfoModel|null
     */
    public PlayerInfoModel getByUid(@NonNull Long uid) {
        // 检索内存实体
        PlayerInfoModel model = players.get(uid);
        if (model != null) {
            return model;
        }

        // 检索数据库实体
        model = repository.findById(uid).orElse(null);
        if (model != null) {
            players.put(uid, model);
        }
        return model;
    }

    /**
     * 保存数据
     *
     * @param model 玩家模型
     * @return PlayerInfoModel
     */
    public PlayerInfoModel save(@NonNull PlayerInfoModel model) {
        return repository.save(model);
    }


    /**
     * 对指定玩家标识为需要数据落地
     *
     * @param uid 玩家ID
     */
    public void mark(@NonNull Long uid) {
        if (!marks.contains(uid)) {
            marks.add(uid);
        }
    }

    /**
     * 写入数据要求异步保存
     *
     * @param uid   玩家ID
     * @param model 新的数据模型
     */
    public void mark(@NonNull Long uid, @NonNull PlayerInfoModel model) {
        players.put(uid, model);
        mark(uid);
    }


    /**
     * 获取异步任务
     *
     * @return List
     */
    public List<Long> getMarks() {
        return marks;
    }

    /**
     * 清理标识
     *
     * @param uid 玩家标识
     */
    public void clearMark(Long uid) {
        marks.remove(uid);
    }

}
