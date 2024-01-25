package com.meteorcat.mix.model.repository;

import com.meteorcat.mix.model.PlayerInfoModel;
import org.springframework.data.repository.CrudRepository;

/**
 * 玩家信息工厂, 用来给做数据 CRUD 操作
 */
public interface PlayerInfoRepository extends CrudRepository<PlayerInfoModel, Long> { }
