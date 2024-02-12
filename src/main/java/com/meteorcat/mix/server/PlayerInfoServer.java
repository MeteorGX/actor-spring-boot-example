package com.meteorcat.mix.server;

import com.meteorcat.mix.model.PlayerInfoModel;
import com.meteorcat.mix.model.repository.PlayerInfoRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PlayerInfoServer {

    /**
     * 数据工厂
     */
    final PlayerInfoRepository repository;

    /**
     * 挂载内存玩家
     */
    final HashMap<Long, PlayerInfoModel> players = new HashMap<>();

    /**
     * 需要线程同步的更新标识
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
     * 检索玩家实体, 内存不存在就挂载到内存
     *
     * @param uid 玩家ID
     * @return PlayerInfoModel|null
     */
    public PlayerInfoModel findByUid(@NonNull Long uid) {
        // 检索内存实体
        PlayerInfoModel model = players.get(uid);
        if (model != null) {
            return model;
        }

        // 检索数据库
        model = repository.findById(uid).orElse(null);
        if (model != null) {
            players.put(uid, model);
        }
        return model;
    }

    /**
     * 保存数据
     *
     * @param model 玩家实体
     * @return PlayerInfoModel
     */
    public PlayerInfoModel save(@NonNull PlayerInfoModel model) {
        return repository.save(model);
    }


    /**
     * 创建玩家实体
     *
     * @param model 玩家实体
     * @return PlayerInfoModel | null
     */
    public PlayerInfoModel create(@NonNull PlayerInfoModel model) {
        if (model.getUid() != null) {
            return null;
        }
        PlayerInfoModel owner = repository.save(model);
        Long uid = owner.getUid();
        players.put(uid, owner);
        return owner;
    }


    /**
     * 标识数据落地
     *
     * @param uid 玩家ID
     */
    public void mark(@NonNull Long uid) {
        if (!marks.contains(uid)) {
            marks.add(uid);
        }
    }

    /**
     * 更新内存数据并且标识数据落地
     *
     * @param uid   玩家ID
     * @param model 玩家实体
     */
    public void mark(@NonNull Long uid, @NonNull PlayerInfoModel model) {
        players.put(uid, model);
        mark(uid);
    }

    /**
     * 将内存落地到数据库
     */
    public void flush() {
        // 如果没有任务跳过
        if (marks.isEmpty()) return;

        // 获取标识准备落地
        for (Long mark : marks) {
            PlayerInfoModel model = findByUid(mark);
            if (model != null) {
                repository.save(model);
            }
        }

        // 完成任务清空标识
        marks.clear();
    }

}
