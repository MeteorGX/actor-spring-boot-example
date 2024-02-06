package com.meteorcat.mix.core;

import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;


/**
 * 玩家信息哈希表
 */
public class ActorUserHashMap extends ConcurrentHashMap<WebSocketSession, Long> {
}
